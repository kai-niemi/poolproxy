#!/bin/bash

args="--profiles default --spring.datasource.url=jdbc:postgresql://localhost:26257/sandbox?sslmode=disable"

cd pool-proxy-sandbox && java -jar target/sandbox.jar $args $*