import AssemblyKeys._

name := "MultiModelTrainingBenchmarks"

version := "1.0"

libraryDependencies += "org.apache.spark" %% "spark-mllib" % "1.1.0-SNAPSHOT" % "provided"

assemblySettings

test in assembly := {}

outputPath in assembly := file("target/mm_benchmark.jar")
