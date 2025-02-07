#!/bin/bash

set -e

MAIN_DIR="$( cd "$( dirname "$0" )" && pwd )/.."

TESTS_CONF_DIR=$MAIN_DIR/tests/config
rm -Rf $MAIN_DIR/spark-warehouse $MAIN_DIR/metastore_db

TEST_NAME=$1

export JDK_JAVA_OPTIONS='
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.lang.invoke=ALL-UNNAMED
--add-opens=java.base/java.lang.reflect=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.nio=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent=ALL-UNNAMED
--add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
--add-opens=java.base/sun.nio.ch=ALL-UNNAMED
--add-opens=java.base/sun.nio.cs=ALL-UNNAMED
--add-opens=java.base/sun.security.action=ALL-UNNAMED
--add-opens=java.base/sun.util.calendar=ALL-UNNAMED
--add-opens=java.security.jgss/sun.security.krb5=ALL-UNNAMED
'

if [[ -z "$TEST_NAME" ]]; then
    echo "################################"
    echo "Executing unit tests"
    echo "################################"
    sbt +test

    echo "################################"
    echo "Executing basic tests"
    echo "################################"

    spark-submit --class com.yotpo.metorikku.MetorikkuTester target/service-java-data-pipelines-metorikku_${SCALA_BINARY_VERSION}-*.jar --test-settings examples/movies_test.yaml

    (cd examples/udf && sbt +package)
    spark-submit --class com.yotpo.metorikku.MetorikkuTester --jars examples/udf/target/scala-${SCALA_BINARY_VERSION}/udf-example_${SCALA_BINARY_VERSION}-1.0.jar target/service-java-data-pipelines-metorikku_${SCALA_BINARY_VERSION}-*.jar --test-settings examples/udf/udf_test.yaml
fi

for d in $(ls $TESTS_CONF_DIR); do
    if [[ -z "$TEST_NAME" ]] || [[ "$d" == "$TEST_NAME" ]]; then
        echo "################################"
        echo "Executing Integration test $d"
        echo "################################"

        spark-submit --class com.yotpo.metorikku.Metorikku target/service-java-data-pipelines-metorikku_${SCALA_BINARY_VERSION}-*.jar -c $TESTS_CONF_DIR/$d/main.yaml
    fi
done
