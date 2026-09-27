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
