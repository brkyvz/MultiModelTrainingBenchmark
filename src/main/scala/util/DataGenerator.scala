package util

import java.util.Random

import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.random.{RandomRDDs, RandomDataGenerator}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

import scala.collection.mutable.ArrayBuffer

object DataGenerator {

  def generateLabeledPoints(
     sc: SparkContext,
     numRows: Long,
     numCols: Int,
     intercept: Double,
     eps: Double,
     numPartitions: Int,
     seed: Long = System.currentTimeMillis(),
     problem: String = "",
     useSparse: Boolean = false,
     density: Double = 1.0): RDD[LabeledPoint] = {

    RandomRDDs.randomRDD(sc,
      new LinearDataGenerator(numCols,intercept, seed, eps, problem, useSparse, density),
      numRows, numPartitions, seed)

  }

  def generateClassificationLabeledPoints(
     sc: SparkContext,
     numRows: Long,
     numCols: Int,
     threshold: Double,
     scaleFactor: Double,
     numPartitions: Int,
     seed: Long = System.currentTimeMillis(),
     chiSq: Boolean = false,
     useSparse: Boolean = false,
     density: Double = 1.0): RDD[LabeledPoint] = {

    RandomRDDs.randomRDD(sc,
      new ClassLabelGenerator(numCols, threshold, scaleFactor,chiSq, useSparse, density),
      numRows, numPartitions, seed)
  }

  def createMatMatInputData(seed: Long, m: Int, n: Int, p: Int, sparsity: Double): (Matrix, Matrix) = {

    val random = new Random(seed)
    val rawA = new Array[Double](m * n)
    val rawB = new Array[Double](n * p)
    var nnz = 0
    for (i <- 0 until m * n) {
      val x = random.nextDouble()
      if (x < sparsity) {
        rawA.update(i, x)
        nnz += 1
      }
    }

    val sparseA = rawA.filter( v => v != 0.0)

    for (i <- 0 until n * p) {
      rawB.update(i, random.nextDouble())
    }

    val sCols: ArrayBuffer[Int] = new ArrayBuffer(n+1)
    val sRows: ArrayBuffer[Int] = new ArrayBuffer(sparseA.length)

    var i = 0
    nnz = 0
    var lastCol = -1

    rawA.foreach { v =>
      val r = i % m
      val c = (i - r) / m
      if ( v != 0.0) {
        sRows.append(r)
        while (c != lastCol){
          sCols.append(nnz)
          lastCol += 1
        }
        nnz += 1
      }
      i += 1
    }
    sCols.append(sparseA.length)

    val denseB = rawB

    (new SparseMatrix(m, n, sCols.toArray, sRows.toArray, sparseA), new DenseMatrix(n, p, denseB))
  }

  def createVecMatInputData(seed: Long, m: Int, n: Int, p: Int, sparsity: Double): (Array[Vector], Matrix) = {

    val random = new Random(seed)
    new ArrayBuffer[Double](m * n)
    val rawB = new Array[Double](n * p)
    var nnz = 0
    val rawA = Array.tabulate(m)( i =>
      (0 until n).map { j =>
        val x = random.nextDouble()
        if (x < sparsity) {
          nnz += 1
          (j, random.nextDouble())
        } else (j, 0.0)
      }
    )
    val sparseA: Array[Vector] = rawA.map { v =>
      val nonzero = v.filter(_._2 != 0.0)
      val inds = nonzero.map(_._1)
      val values = nonzero.map(_._2)
      new SparseVector(n, inds.toArray, values.toArray)
    }.toArray

    for (i <- 0 until n * p) {
      rawB.update(i, random.nextDouble())
    }

    val denseB = rawB

    (sparseA, new DenseMatrix(n, p, denseB))
  }

}

class LinearDataGenerator(
   private val numFeatures: Int,
   private val intercept: Double,
   private val seed: Long,
   private val eps: Double,
   private val problem: String = "",
   private val useSparse: Boolean = false,
   private val density: Double = 1.0) extends RandomDataGenerator[LabeledPoint] {

  private val rng = new java.util.Random(seed)

  private val weights = Vectors.dense(Array.fill(numFeatures)(rng.nextDouble()))

  override def nextValue(): LabeledPoint = {
    if (useSparse) {
      val x =
        Vectors.dense((0 until numFeatures).map { i =>
          if (rng.nextDouble() <= density) 2 * rng.nextDouble() - 1 else 0.0
        }.toArray)

      val y = weights.toArray.zip(x.toArray).map(r => r._1 * r._2).sum + intercept + eps*rng.nextGaussian()
      val yD =
        if (problem == "SVM"){
          if (y < 0.0) 0.0 else 1.0
        } else{
          y
        }
      val xSparse = x.toArray.zipWithIndex.filter { case (v, ind) =>
        v != 0.0
      }
      LabeledPoint(yD, Vectors.sparse(numFeatures, xSparse.map(_._2), xSparse.map(_._1)))
    } else {
      val x = Vectors.dense(Array.fill[Double](numFeatures)(2*rng.nextDouble()-1))

      val y = weights.toArray.zip(x.toArray).map(r => r._1 * r._2).sum + intercept + eps*rng.nextGaussian()
      val yD =
        if (problem == "SVM"){
          if (y < 0.0) 0.0 else 1.0
        } else{
          y
        }

      LabeledPoint(yD, x)
    }
  }

  override def setSeed(seed: Long) {
    rng.setSeed(seed)
  }

  override def copy(): LinearDataGenerator =
    new LinearDataGenerator(numFeatures, intercept, seed, eps, problem, useSparse, density)
}

// For general classification
class ClassLabelGenerator(
   private val numFeatures: Int,
   private val threshold: Double,
   private val scaleFactor: Double,
   private val chiSq: Boolean,
   private val useSparse: Boolean = false,
   private val density: Double = 1.0) extends RandomDataGenerator[LabeledPoint] {

  private val rng = new java.util.Random()

  override def nextValue(): LabeledPoint = {
    val y = if (rng.nextDouble() < threshold) 0.0 else 1.0
    val x =
      if (useSparse) {
        Vectors.dense((0 until numFeatures).map { i =>
          if (rng.nextDouble() <= density) {
            if (!chiSq) rng.nextGaussian() + (y * scaleFactor) else rng.nextInt(6)*1.0
          } else {
            0.0
          }
        }.toArray)
      } else {
        Vectors.dense(Array.fill(numFeatures){
          if (!chiSq) rng.nextGaussian() + (y * scaleFactor) else rng.nextInt(6)*1.0
        })
      }

    LabeledPoint(y, x)
  }

  override def setSeed(seed: Long) {
    rng.setSeed(seed)
  }

  override def copy(): ClassLabelGenerator =
    new ClassLabelGenerator(numFeatures, threshold, scaleFactor, chiSq, useSparse, density)
}
