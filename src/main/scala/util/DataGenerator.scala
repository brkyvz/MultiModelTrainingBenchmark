package util

import org.apache.spark.SparkContext
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.random.{RandomRDDs, RandomDataGenerator}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD

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
