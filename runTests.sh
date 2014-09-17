#!bin/bash

export NUM_TRIALS=0
export WARM_UP=0
export WAIT=0
export SEED=24
export NUM_ROWS=( 1000000 10000000 )
export NUM_COLS=( 500 1000 5000 10000 )
export SPARSITY=( 0.01 0.05 0.10 0.15 0.20 )
export BLOCK_SIZE=( 32 64 128 256 512 1024 2048 4096 )
export ITER=2
export STEP=4
export REG=4
export PARTS=64

export DRIVER_MEM="200g"

export MC=1000000
export NC=500
export SP=0.05
export BL=64
export exit=1
if [ $exit = 0 ]; then
for ROWS in ${NUM_ROWS[@]}; do
  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $ROWS $NC $BL $PARTS $SEED true $SP true \
    2>>log.txt | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $ROWS $NC $BL $PARTS $SEED false $SP true \
    2>>log.txt | tee -a tests.out
done

for COLS in ${NUM_COLS[@]}; do
  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $COLS $BL $PARTS $SEED true $SP true \
    2>>log.txt | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $COLS $BL $PARTS $SEED false $SP true \
    2>>log.txt | tee -a tests.out
done

for RHO in ${SPARSITY[@]}; do
  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $NC $BL $PARTS $SEED true $RHO true \
    2>>log.txt | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $NC $BL $PARTS $SEED false $RHO true \
    2>>log.txt | tee -a tests.out

done
fi

for BLOCKS in ${BLOCK_SIZE[@]}; do
  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $NC $BLOCKS $PARTS $SEED true $SP true \
    2>>log.txt | tee -a tests.out
done

export NUM_COLS=( 10000 100000 1000000 )
export NUM_ROWS=( 500 1000 5000 10000 50000 )

export NC=100000
export MC=500

export exit=1
if [ $exit = 0 ]; then
for ROWS in ${NUM_ROWS[@]}; do
  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $ROWS $NC $BL $PARTS $SEED true $SP true \
    2>>log.txt | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $ROWS $NC $BL $PARTS $SEED false $SP true \
    2>>log.txt | tee -a tests.out
done

for COLS in ${NUM_COLS[@]}; do
  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $COLS $BL $PARTS $SEED true $SP true \
    2>>log.txt | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $COLS $BL $PARTS $SEED false $SP true \
    2>>log.txt | tee -a tests.out
done

for RHO in ${SPARSITY[@]}; do
  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $NC $BL $PARTS $SEED true $RHO true \
    2>>log.txt | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark --driver-memory $DRIVER_MEM target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $NC $BL $PARTS $SEED false $RHO true \
    2>>log.txt | tee -a tests.out

done
fi
