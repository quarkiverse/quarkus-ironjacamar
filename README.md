# Quarkus Jakarta Connectors

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.jca/quarkus-jca?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.jca/quarkus-jca)

## Overview

This extension provides very limited support for Jakarta Connectors in Quarkus. It is based on
the [Jakarta Connector Architecture](https://projects.eclipse.org/projects/ee4j.jca) 2.1.0 specification.

## Features

- Manages the resource adapter lifecycle using the Quarkus container
- Flexible configuration of resource adapters
- WorkManager implementation uses Vert.x worker threads
- Integrates with the Quarkus transaction manager
- Native mode is supported (but not every the Resource Adapter, which demands a specific extension if necessary)

## Limitations

- JNDI is not supported
- Security work context propagation is not supported
- Tested with the Artemis JCA resource adapter only

## Documentation

The documentation is available [here](https://docs.quarkiverse.io/quarkus-ironjacamar/dev/index.html).
