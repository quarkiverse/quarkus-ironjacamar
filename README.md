# Quarkus IronJacamar
<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-5-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

[![Version](https://img.shields.io/maven-central/v/io.quarkiverse.ironjacamar/quarkus-ironjacamar?logo=apache-maven&style=for-the-badge)](https://search.maven.org/artifact/io.quarkiverse.ironjacamar/quarkus-ironjacamar)

## Overview

This extension provides support for running Jakarta Connectors in Quarkus. It is based on
the [Jakarta Connectors Architecture 2.1](https://jakarta.ee/specifications/connectors/2.1/) specification.

## Features

- Manages the resource adapter lifecycle using the Quarkus container
- Flexible configuration of resource adapters
- `WorkManager` implementation uses a [ManagedExecutor](https://download.eclipse.org/microprofile/microprofile-context-propagation-1.0/apidocs/org/eclipse/microprofile/context/ManagedExecutor.html)
- Integrates with the Quarkus transaction manager
- Native mode is supported (Resource Adapter implementations may require the creation of a separate extension)
- Pool metrics are exposed as Prometheus metrics
- Message Endpoints work under a duplicated Vert.x Context

## Limitations

- JNDI is not supported
- Security work context propagation is not supported
- Tested with the Artemis JCA resource adapter only (see [quarkus-integration-test-artemis-jms](integration-tests/artemis-jms))

## Documentation

The full documentation is available [here](https://docs.quarkiverse.io/quarkus-ironjacamar/dev/index.html).

## Contributors ✨

Thanks go to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="http://gastaldi.wordpress.com"><img src="https://avatars.githubusercontent.com/u/54133?v=4?s=100" width="100px;" alt="George Gastaldi"/><br /><sub><b>George Gastaldi</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-ironjacamar/commits?author=gastaldi" title="Code">💻</a> <a href="#maintenance-gastaldi" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/vsevel"><img src="https://avatars.githubusercontent.com/u/6041620?v=4?s=100" width="100px;" alt="Vincent Sevel"/><br /><sub><b>Vincent Sevel</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-ironjacamar/issues?q=author%3Avsevel" title="Bug reports">🐛</a> <a href="#userTesting-vsevel" title="User Testing">📓</a> <a href="https://github.com/quarkiverse/quarkus-ironjacamar/commits?author=vsevel" title="Code">💻</a> <a href="#maintenance-vsevel" title="Maintenance">🚧</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://zhfeng.github.io/"><img src="https://avatars.githubusercontent.com/u/1246139?v=4?s=100" width="100px;" alt="Zheng Feng"/><br /><sub><b>Zheng Feng</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-ironjacamar/commits?author=zhfeng" title="Code">💻</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/mkomadel"><img src="https://avatars.githubusercontent.com/u/38321228?v=4?s=100" width="100px;" alt="mkomadel"/><br /><sub><b>mkomadel</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-ironjacamar/issues?q=author%3Amkomadel" title="Bug reports">🐛</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ozangunalp"><img src="https://avatars.githubusercontent.com/u/294765?v=4?s=100" width="100px;" alt="Ozan Gunalp"/><br /><sub><b>Ozan Gunalp</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-ironjacamar/commits?author=ozangunalp" title="Code">💻</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
