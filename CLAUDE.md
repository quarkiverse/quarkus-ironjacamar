# Quarkus IronJacamar

Quarkiverse extension integrating [IronJacamar](https://www.ironjacamar.org/) (JCA reference implementation) into Quarkus, providing Jakarta Connectors Architecture (JCA) 2.1 support. Primary use case: connecting to messaging brokers like Apache Artemis via JCA resource adapters.

## Project Structure

Standard Quarkus extension layout:

- **`runtime/`** — Runtime module with the JCA container logic
- **`deployment/`** — Build-time processing (Jandex scanning, synthetic bean registration, endpoint activation)
- **`integration-tests/`** — Artemis-based integration tests
  - `artemis-jms` — Single RA with message endpoints, transactions, metrics
  - `multiple-artemis-jms` — Multiple named resource adapters in one app
  - `artemis-dev-classloader` — Dev mode classloader behavior
  - `artemis-common` — Shared `ArtemisResourceAdapterFactory`
- **`docs/`** — Quarkiverse documentation site sources

## Key Classes

### Runtime

- `ResourceAdapterFactory` — SPI interface vendors implement to provide their RA, connection factory, and activation spec
- `ResourceEndpoint` — Annotation marking message endpoint classes (inbound consumers)
- `ResourceAdapterKind` — Annotation on `ResourceAdapterFactory` implementations identifying the RA kind
- `ResourceAdapterTypes` — Annotation declaring connection factory types provided by the RA
- `IronJacamarContainer` — Core bean holding ResourceAdapter, ManagedConnectionFactory, ConnectionManager lifecycle
- `IronJacamarRecorder` — Build-time recorder wiring the container, pool services, and endpoint activation
- `IronJacamarSupport` — Runtime helper creating containers and activating endpoints
- `IronJacamarRuntimeConfig` — SmallRye Config mapping (`quarkus.ironjacamar.*`)
- `IronJacamarBuildtimeConfig` — Build-time configuration
- `ConnectionManagerFactory` — Creates IronJacamar ConnectionManager instances
- `TransactionRecoveryManager` — XA transaction recovery integration
- `DefaultMessageEndpointFactory` — Creates message endpoints with Vert.x context duplication

### Deployment

- `IronJacamarProcessor` — Main build step processor: scans for `@ResourceAdapterKind` factories and `@ResourceEndpoint` classes, registers synthetic CDI beans, activates endpoints, starts pool services
- `IronJacamarMetricsProcessor` — Registers pool metrics when micrometer is available

## Architecture Decisions

- Resource adapters start as **Vert.x worker verticles** (one per RA)
- Message endpoints run under a **duplicated Vert.x context** for thread safety
- Pool services (IdleRemover, ConnectionValidator) use **build-time recording** with conditional startup based on config
- Uses Quarkus's `ManagedExecutor` for the JCA WorkManager thread pools
- Multiple named RAs distinguished via `@Identifier` qualifier
- GraalVM native image compatible

## Configuration

Prefix: `quarkus.ironjacamar`

- `quarkus.ironjacamar.<name>.ra.config.*` — Resource adapter properties
- `quarkus.ironjacamar.<name>.ra.cm.*` — Connection manager (transaction support, pool, XA, recovery)
- `quarkus.ironjacamar.activation-spec.<name>.config.*` — Activation spec properties

## Build & Test

```sh
# Build runtime + deployment
./mvnw install -DskipTests

# Run unit tests (deployment module)
./mvnw test -pl deployment

# Run integration tests (requires Docker for Artemis)
./mvnw verify -pl integration-tests/artemis-jms

# Full build with integration tests
./mvnw verify
```