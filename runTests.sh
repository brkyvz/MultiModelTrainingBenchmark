#!bin/bash

export NUM_TRIALS=0
export WARM_UP=0
export WAIT=0
export SEED=24
export NUM_ROWS=( 1000000 10000000 100000000 )
export NUM_COLS=( 100 1000 10000 )
export SPARSITY=( 0.01 0.05 0.10 0.20 )
export ITER=2
export STEP=4
export REG=4
export PARTS=64

export MC=1000000
export NC=100
export SP=0.05

if [ $SKIP = 0 ]; then
# Scale Rows Tests
for ROWS in ${NUM_ROWS[@]}; do
#  echo $ENV
  $SPARK/bin/spark-submit --class Benchmark target/mm_benchmark.jar \
    $ITER $STEP $REG $ROWS $NC $PARTS $SEED true $SP true \
    2>&1 | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark target/mm_benchmark.jar \
    $ITER $STEP $REG $ROWS $NC $PARTS $SEED false $SP true \
    2>&1 | tee -a tests.out
done
fi

for COLS in ${NUM_COLS[@]}; do
  $SPARK/bin/spark-submit --class Benchmark target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $COLS $PARTS $SEED true $SP true \
    2>&1 | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $COLS $PARTS $SEED false $SP true \
    2>&1 | tee -a tests.out
done

for RHO in ${SPARSITY[@]}; do
  $SPARK/bin/spark-submit --class Benchmark target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $NC $PARTS $SEED true $RHO true \
    2>&1 | tee -a tests.out

  $SPARK/bin/spark-submit --class Benchmark target/mm_benchmark.jar \
    $ITER $STEP $REG $MC $NC $PARTS $SEED false $RHO true \
    2>&1 | tee -a tests.out

done