# Changelog

## [1.0.1](https://github.com/ahincho/nova-java-api-standard/compare/v1.0.0...v1.0.1) (2026-07-13)


### Bug Fixes

* **ci:** add component + skip-snapshot + manifest-file (mask-utils pattern) ([abe2ab1](https://github.com/ahincho/nova-java-api-standard/commit/abe2ab1dddea7a3558a460665fbf83d886fa358b))
* **ci:** add last-release-sha, include-component-in-tag: false, release-type: java to top-level config; pass manifest-file in wrapper ([98b4ff2](https://github.com/ahincho/nova-java-api-standard/commit/98b4ff2cd2fc42f79752d3ca134473b130a01a8f))


### Documentation

* fix stale version in install instructions (0.1.0 -&gt; 1.0.0) ([a17a351](https://github.com/ahincho/nova-java-api-standard/commit/a17a351401b3246446e5937e572b6337a227524f))

## 1.0.0 (2026-07-10)


### Features

* **api:** trigger release-please with explicit workflow permissions ([3a91e5b](https://github.com/ahincho/nova-java-api-standard/commit/3a91e5b076068832074cc32d1e13e6133bd84df2))
* **ci:** inline publish-on-tag + enable Build Cache and Configuration Cache ([00e8942](https://github.com/ahincho/nova-java-api-standard/commit/00e89427f798e6ea93302c6aabe6a6afe95d111e))
* **ci:** migrate to release-please + tag-based publish flow (NOVA-SEMVER-13) ([4e5a362](https://github.com/ahincho/nova-java-api-standard/commit/4e5a3623eae3f08f829565db5f9561e1a67eab00))
* **docs:** add CONTRIBUTING.md with contribution guidelines ([931a4ff](https://github.com/ahincho/nova-java-api-standard/commit/931a4ff053d1ad6e5437868f157af433cb8fc907))
* **gradle:** add GPG signing plugin for Maven Central publishing (NOVA-SEMVER-10) ([1e79845](https://github.com/ahincho/nova-java-api-standard/commit/1e79845a3f774569ca971a7b3419a1010446daad))
* initial commit - API response/error standards, HATEOAS links, page info, filter criteria, request context, user-agent parser ([e1a9b4b](https://github.com/ahincho/nova-java-api-standard/commit/e1a9b4bd4efcf5ae2439f14ff7937b8483dffe22))


### Bug Fixes

* **ci:** add chmod +x gradlew to fix permission denied on Linux runner ([5e66628](https://github.com/ahincho/nova-java-api-standard/commit/5e6662885f074043e8e61a79b6f730934c2a5a3d))
* **ci:** add permissions and explicit inputs to release-please workflow ([14feaef](https://github.com/ahincho/nova-java-api-standard/commit/14feaefe4b683a4ca3e81a72da0b39be7dd6dfc7))
* **ci:** add workflow_dispatch and explicit GITHUB_TOKEN secret to publish-on-tag ([730a0bd](https://github.com/ahincho/nova-java-api-standard/commit/730a0bd33e3d35953a99760c7560f839ba06c960))
* **ci:** inline publish logic to avoid reusable workflow issue with tag push ([0d49a4e](https://github.com/ahincho/nova-java-api-standard/commit/0d49a4e12d47d2a9a6ecb960b3fa4739e0e156e5))
* **ci:** restore original publish-on-tag format (no with, secrets: inherit) ([b97646a](https://github.com/ahincho/nova-java-api-standard/commit/b97646af7d73a11d23c9e321266514a49ccec04d))
* **ci:** restore publish-on-tag with reusable workflow + explicit secrets ([c117662](https://github.com/ahincho/nova-java-api-standard/commit/c11766275388c30c454cbc68a81da22c6f23d3f5))
* **ci:** restore release-please workflow with explicit permissions and inputs ([2b094a0](https://github.com/ahincho/nova-java-api-standard/commit/2b094a0280c607a3363a0594a3698f8ceabfb6d0))
* **ci:** update reusable workflow refs to ahincho/nova-devops ([ffa5791](https://github.com/ahincho/nova-java-api-standard/commit/ffa5791558c4887f3505c0ce7b6e5e21ca70ab35))
* **ci:** use PAT fallback for release-please to enable tag-triggered workflows ([17385e7](https://github.com/ahincho/nova-java-api-standard/commit/17385e763b71ebdab6c00f179a333d7fbe765a3e))
* **gradle:** remove dirty closure (incompatible with Kotlin DSL in Gradle 9.6.1) ([e938f73](https://github.com/ahincho/nova-java-api-standard/commit/e938f73c157e6dc42713d337cf971acb8b20756a))
* **gradle:** use explicit type in dirty closure for Gradle 9.6.1 compatibility ([56cc93e](https://github.com/ahincho/nova-java-api-standard/commit/56cc93ed25dd2633ae659a7e0ac4bab0cc17669e))


### Documentation

* add README with installation and usage instructions ([bb6d108](https://github.com/ahincho/nova-java-api-standard/commit/bb6d10838a9665f9faba09c97fc12e827ce316ff))

## 1.0.0 (2026-07-09)


### Features

* **api:** trigger release-please with explicit workflow permissions ([3a91e5b](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/3a91e5b076068832074cc32d1e13e6133bd84df2))
* **ci:** migrate to release-please + tag-based publish flow (NOVA-SEMVER-13) ([4e5a362](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/4e5a3623eae3f08f829565db5f9561e1a67eab00))
* **docs:** add CONTRIBUTING.md with contribution guidelines ([931a4ff](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/931a4ff053d1ad6e5437868f157af433cb8fc907))
* **gradle:** add GPG signing plugin for Maven Central publishing (NOVA-SEMVER-10) ([1e79845](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/1e79845a3f774569ca971a7b3419a1010446daad))
* initial commit - API response/error standards, HATEOAS links, page info, filter criteria, request context, user-agent parser ([e1a9b4b](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/e1a9b4bd4efcf5ae2439f14ff7937b8483dffe22))


### Bug Fixes

* **ci:** add permissions and explicit inputs to release-please workflow ([14feaef](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/14feaefe4b683a4ca3e81a72da0b39be7dd6dfc7))
* **ci:** restore release-please workflow with explicit permissions and inputs ([2b094a0](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/2b094a0280c607a3363a0594a3698f8ceabfb6d0))
* **ci:** update reusable workflow refs to ahincho/nova-devops ([ffa5791](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/ffa5791558c4887f3505c0ce7b6e5e21ca70ab35))


### Documentation

* add README with installation and usage instructions ([bb6d108](https://github.com/ahincho/nova-java-spring-boot-api-standard/commit/bb6d10838a9665f9faba09c97fc12e827ce316ff))
