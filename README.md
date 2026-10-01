# Nova API Standard

Every REST service ends up inventing its own envelope, its own error
shape and its own pagination fields. This library settles all three once,
in plain Java, so that every Nova service answers with the same contract.

No Spring, no Quarkus, no Jackson annotations on the public types — the
framework adapters live in separate repositories and only wire this in.

## What's inside

| Package | Type | Purpose |
|---|---|---|
| `response` | `ApiResponse<T>`, `ResponseBuilder` | The envelope every endpoint returns |
| `error` | `ApiError` | A single machine-readable failure |
| `error` | `DomainError`, `ApplicationError`, `InfrastructureError`, `PlatformError` on `NovaError`; `ErrorStatusMapper`, `ErrorCatalog`, `ErrorSerializer`; `TraceIdSource` | Errors classified by layer instead of by HTTP status (ADR-031), and the three ports that turn them into a response |
| `page` | `PageInfo` | Page number, size, totals |
| `query` | `FilterCriteria`, `FilterOperator`, `SortCriteria`, `SortDirection` | Filtering and sorting read off the query string |
| `link` | `ApiLink` | HATEOAS links |
| `http` | `HttpStatusCode`, `HttpCategory` | Status codes as an enum instead of loose ints |
| `ratelimit` | `RateLimitInfo` | Remaining quota and reset window |
| `request` | `RequestContext`, `RequestContextBuilder` | Correlation id, caller, locale |
| `client` | `UserAgentParser`, `ClientInfo`, `Browser`, `DeviceType`, `OperatingSystem` | User-agent parsing without a third-party dependency |
| `metadata` | `ApiMetadata` | Timing and version stamped on the response |
## Install

Published to GitHub Packages, so the repository needs to be declared and
authenticated with a token that has `read:packages`.

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-01-api-standard")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("pe.edu.nova.java.libs:nova-api-standard:0.1.0-SNAPSHOT")
}
```

## Use

`ApiResponse` is a record with static factories for the common outcomes:

```java
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

// 200 with a body
return ApiResponse.ok(customer);

// 201
return ApiResponse.created(customer);

// 204
return ApiResponse.noContent();

// 4xx / 5xx, either with one message or a list of field errors
return ApiResponse.error(404, "Customer not found");
return ApiResponse.error(422, validationErrors);
```

Filtering and sorting are parsed into value objects rather than passed
around as strings:

```java
FilterCriteria filter = new FilterCriteria("status", FilterOperator.EQUALS, "ACTIVE");
SortCriteria sort = new SortCriteria("createdAt", SortDirection.DESC);
```

### Errors by layer

A use case says what went wrong, not which HTTP status that is, so the same
code runs behind HTTP or behind a queue consumer:

```java
throw DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe");
throw ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30));
throw InfrastructureError.timeout("payments", exception);
```

The framework adapter logs the error once, with its upstream and cause, and
then turns it into the envelope and its headers:

```java
SerializedError response = ErrorPorts.defaults().respond(error);
// the timeout above: status 504, an ApiResponse with GATEWAY_TIMEOUT and the traceId;
// the rate limit: status 429 and Retry-After: 30
```

`ErrorStatusMapper` picks the status, `ErrorCatalog` the code and message
the client sees, and `ErrorSerializer` the body and headers. Nova ships a
default for each, and an organization such as UTP replaces or wraps any of
them from its own profile without forking, with
`new ErrorPorts(mapper, catalog, serializer)`.

The catalog and the serializer never see the full error. They get a
`SanitizedFailure`: layer, type, status, code, message, field errors,
`retryAfter` and `traceId`, without the upstream or the cause, and with the
generic message in a 5xx. A port written by an organization cannot leak what
it never receives. A framework exception that already has its status enters
through `SanitizedFailure.ofStatus`: a 4xx is `application`, a 502, 503 or 504
is `infrastructure`, and any other 5xx is `platform`.

What the defaults never show: a 5xx carries only the generic code of its
status and the `traceId`, never the error's own code, its message or the
upstream. That is the tradeoff. The client can tell a 503 worth retrying from
a 500 that is not, but anything more specific means looking the `traceId` up
in the log.

The `traceId` is taken when the error is built, not when the response is
written, because by then the request context may be gone. It comes from a
`TraceIdSource` that the framework adapter registers in
`META-INF/services`; with none registered the error has no `traceId`.

`ApiResponse.error(...)` still answers with the code `ERROR`, so nothing
that uses it changes; the catalog codes reach clients through the adapters.

## Framework adapters

This library stays framework-free on purpose. To wire it into an
application use the adapter for your stack:

- [nova-java-commons-spring-boot-starter](https://github.com/ahincho/nova-java-08-commons-spring-boot-starter)
- [nova-java-api-standard-quarkus-extension](https://github.com/ahincho/nova-java-10-api-standard-quarkus-extension)

## Requirements

Java 25.

## License

Eclipse Public License 2.0 — see [LICENSE](LICENSE).

Copyright © 2026 Angel Hincho.
