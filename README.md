# Quarkus Jakarta Connectors

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.jca/quarkus-jca?logo=apache-maven&style=flat-square)](https://search.maven.org/artifact/io.quarkiverse.jca/quarkus-jca)

## Overview

This extension provides very limited support for Jakarta Connectors in Quarkus. It is based on
the [Jakarta Connector Architecture](https://projects.eclipse.org/projects/ee4j.jca) specification.

## Features

- Manages the resource adapter lifecycle using the Quarkus container
- Flexible configuration of resource adapters

## Needs testing

- Integrates with the Quarkus transaction manager

## Limitations

- JNDI is not supported
- Tested with the Artemis JCA resource adapter only

## Documentation

The documentation is available [here](https://docs.quarkiverse.io/quarkus-jca/dev/index.html).
