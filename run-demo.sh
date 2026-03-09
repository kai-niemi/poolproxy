#!/bin/bash

args="--profiles default --spring.datasource.url=jdbc:postgresql://localhost:26257/demo?sslmode=disable&allow_unsafe_internals=true"

cd pool-proxy-demo && java -jar target/demo.jar $args $*