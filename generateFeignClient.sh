#!/bin/bash
mkdir -p target
if [ ! -f target/swagger-codegen-cli.jar ]; then
    wget http://central.maven.org/maven2/io/swagger/swagger-codegen-cli/2.3.1/swagger-codegen-cli-2.3.1.jar -O target/swagger-codegen-cli.jar
fi
mkdir -p mottak-api-client
rm -rf mottak-api-client/*
java -jar target/swagger-codegen-cli.jar generate -i ~/.m2/repository/no/ks/fiks/svarinn-mottak-service/1.0.0-SNAPSHOT/svarinn-mottak-service-1.0.0-SNAPSHOT-svarinn-mottak-api-v1.json -l java --library feign -c mottak_config.json -o mottak-api-client
