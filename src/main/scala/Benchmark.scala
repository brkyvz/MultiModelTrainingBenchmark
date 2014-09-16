import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.optimization._
import org.apache.spark.SparkContext
import util.DataGenerator

object Benchmark {

  def main(args: Array[String]) {

    if (args.length != 10) {
      println("Usage: MultiModelBenchmark " +
        "<iteration-count> <step_size-count> <regParam-count> <num_examples> <num_features> " +
        "<num_partitions> <seed> <useSparsity> <sparsity> <linear>")
      System.exit(1)
    }
    val iterCount: Int = args(0).toInt
    val ssCount: Int = args(1).toInt
    val regCount: Int = args(2).toInt
    val nexamples: Int = args(3).toInt
    val nfeatures: Int = args(4).toInt
    val nparts: Int = args(5).toInt
    val seed: Int = args(6).toInt
    val isSparse: Boolean = args(7).toBoolean
    val howSparse: Double = args(8).toDouble
    val linearModel: Boolean = args(9).toBoolean
    val eps = 3

    val sc = new SparkContext()
    val dataRDD =
      if (linearModel) {
        DataGenerator.generateLabeledPoints(sc, nexamples, nfeatures, 0.0, eps, nparts, seed,
          useSparse = true, density = howSparse).map( e => (e.label, e.features))
      } else {
        DataGenerator.generateClassificationLabeledPoints(sc, nexamples, nfeatures, 0.0, eps, nparts, seed,
          useSparse = true, density = howSparse).map( e => (e.label, e.features))
      }

    dataRDD.cache()
    val numExamps = dataRDD.count()

    val initialWeights = Array.fill(nfeatures)(0.0)

    val numModels = ssCount * regCount

    val gradient = if (linearModel) new LeastSquaresGradient else new LogisticGradient
    val mmgradient = if (linearModel) new MultiModelLeastSquaresGradient else new MultiModelLogisticGradient

    val updaters = Array(new MultiModelSquaredL2Updater, new MultiModelL1Updater)

    val stepSize = Array.tabulate(ssCount)(i => 0.01 + 0.01 * i)
    val regParam = Array.tabulate(regCount)(i => 0.01 + 0.01 * i)
    val numIterations = Array.tabulate(iterCount)(i => 10 + 10 * i)
    val miniBatchFrac = 1.0

    val start = System.currentTimeMillis()
    (0 until iterCount).flatMap { j =>
      (0 until numModels).map { i =>
        GradientDescent.runMiniBatchSGD(
          dataRDD,
          gradient,
          new SquaredL2Updater(),
          stepSize(math.round(i * 1.0 / numModels).toInt),
          numIterations(j),
          regParam(i % regParam.length),
          miniBatchFrac,
          Vectors.dense(initialWeights.clone()))
      }
    }
    (0 until iterCount).flatMap { j =>
      (0 until numModels).map { i =>
        GradientDescent.runMiniBatchSGD(
          dataRDD,
          gradient,
          new L1Updater(),
          stepSize(math.round(i * 1.0 / numModels).toInt),
          numIterations(j),
          regParam(i % regParam.length),
          miniBatchFrac,
          Vectors.dense(initialWeights.clone()))
      }
    }
    val durGD = System.currentTimeMillis() - start

    val startMM =  System.currentTimeMillis()
    MultiModelGradientDescent.runMiniBatchMMSGD(
      dataRDD,
      mmgradient,
      updaters,
      stepSize,
      numIterations,
      regParam,
      miniBatchFrac,
      Vectors.dense(initialWeights))
    val durMM = System.currentTimeMillis() - startMM

    println(s"$iterCount\t$ssCount\t$regCount\t$nexamples\t$nfeatures\t$nparts\t$seed\t$isSparse\t$howSparse\t" +
      s"$linearModel\t$durGD\t$durMM")
    sc.stop()
  }

}
