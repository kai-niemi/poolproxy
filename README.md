[![Java CI](https://github.com/kai-niemi/pool-proxy/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/kai-niemi/pool-proxy/actions/workflows/maven.yml)

<!-- TOC -->
* [About](#about)
  * [Main features](#main-features)
  * [Compatibility](#compatibility)
* [Building and Running](#building-and-running)
  * [Install the JDK](#install-the-jdk)
  * [Clone the project](#clone-the-project)
  * [Build the artifact](#build-the-artifact)
* [Terms of Use](#terms-of-use)
<!-- TOC -->

# About

A simple database connection pool proxy library for CockroachDB. It configures the pool size 
and timeouts based on CockroachDB production guidelines relative to cluster provisioning
and node availability. 

In addition, it periodically adjusts the pool size based on 
current node availability and aggregated pool metrics. Thus, the goal is to provide
a fair distribution of pool quotas based on active application (pool) instances and
total cluster vCPU count.

The following diagram illustrates the concept. A pool proxy wraps a target connection 
pool datasource like HikariCP. Connection acquisition and all other mechanics are passed 
through directly to the target datasource. The proxy only adjusts the `minIdle` and 
`maxPoolSize` settings and `maxLifetime` that should be lower than any database imposed 
connection timeout.

![img](pool-proxy.jpg)

## Main features

- Supports HikariCP
- Supports C3P0
- Dynamic and fixed size pool sizing
- Database driven configuration and stats

## Compatibility

- JDK17+
- MacOS / Linux
- CockroachDB
- Spring Boot 3.x

# Building and Running

## Install the JDK

MacOS (using sdkman):

    curl -s "https://get.sdkman.io" | bash
    sdk list java
    sdk install java 17.0 (pick version)  

Ubuntu:

    sudo apt-get install openjdk-17-jdk

## Clone the project

    git clone git@github.com:kai-niemi/pool-proxy && cd pool-proxy

## Build the artifact

    chmod +x mvnw
    ./mvnw clean install

# Terms of Use

This tool is not supported by Cockroach Labs. Use of this tool is entirely at your
own risk and Cockroach Labs makes no guarantees or warranties about its operation.

See [MIT](LICENSE.txt) for terms and conditions.
