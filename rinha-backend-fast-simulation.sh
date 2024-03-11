#!/usr/bin/env bash
cd $(dirname $0)

readonly GATLING_BIN_DIR=$HOME/gatling/bin
readonly GATLING_WORKSPACE="$(pwd)/load-test/user-files"
readonly RESULTS_WORKSPACE="$(pwd)/load-test/results"

[ ! -d "${GATLING_WORKSPACE}" ] && mkdir -p "${GATLING_WORKSPACE}"
[ ! -d "${RESULTS_WORKSPACE}" ] && mkdir -p "${RESULTS_WORKSPACE}"

for i in {1..20}; do
    # 2 requests to wake the 2 api instances up :)
    curl --fail http://localhost:9999/clientes/1/extrato && \
    echo "" && \
    curl --fail http://localhost:9999/clientes/1/extrato && \
    echo "" && \
    sh $GATLING_BIN_DIR/gatling.sh -rm local \
            --simulation RinhaBackendFastSimulation \
            --run-description "Rinha de Backend - 2024/Q1: Crébito" \
            --results-folder $RESULTS_WORKSPACE \
            --simulations-folder "$GATLING_WORKSPACE/simulations" && \
    break || sleep 2;
done
