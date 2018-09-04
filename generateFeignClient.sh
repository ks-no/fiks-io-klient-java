#!/bin/bash

# Config
swagger_jar=target/swagger-codegen-cli.jar
svarinn2_api_spec=target/svarinn2-api-v1.json
svarinn2_api_config=svarinn2-api-client-config.json
svarinn2_api_mvn_project=svarinn2-api-client

# Download dependencies
mkdir -p target
if [ ! -f $swagger_jar ]; then
    wget http://central.maven.org/maven2/io/swagger/swagger-codegen-cli/2.3.1/swagger-codegen-cli-2.3.1.jar -O $swagger_jar
fi
if [ ! -f target/svarinn2-api-v1.json ]; then
    wget https://ks-no.github.io/api/svarinn2-api-v1.json -O $svarinn2_api_spec
fi

# Clean
mkdir -p $svarinn2_api_mvn_project
rm -rf $svarinn2_api_mvn_project/*

# Build
java -jar $swagger_jar generate -i $svarinn2_api_spec -l java --library feign -c $svarinn2_api_config -o $svarinn2_api_mvn_project
