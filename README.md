# Healthful
[![Maven](https://img.shields.io/maven-central/v/ca.dvgi/healthful_2.13?color=blue)](https://search.maven.org/search?q=g:ca.dvgi%20healthful) [![CI](https://img.shields.io/github/actions/workflow/status/dvgica/healthful/ci.yml?branch=main)](https://github.com/dvgica/healthful/actions)

Healthful is a small, low-dependency Scala library providing `HealthServer`, an HTTP server for health checks.

- [Motivation](#motivation)
- [Installation](#installation)
- [Usage](#usage)
- [Contributing](#contributing)

## Motivation

Many container orchestration systems (e.g. Kubernetes) can be configured to perform health checks on a service. These health checks are used to restart the service when it is no longer live, or to send it traffic while it's ready.

While your service may already have an HTTP library you can use to implement these health checks, it's preferable to decouple the health checks from other service capabilities. It's also nice to not have to write the health checks in the first place!

This library implements liveness and readiness endpoints using `com.sun.net.httpserver.HttpServer`, an [officially supported](https://openjdk.org/jeps/408) part of the OpenJDK standard. Therefore, no additional HTTP dependencies are included or required. Logging is accomplished through [SLF4J](https://slf4j.org/) and configuration through [Lightbend Config](https://github.com/lightbend/config), both Java libraries. There are no Scala dependencies required beyond the stdlib.

## Installation

Healthful is available on Maven Central for Scala 2.12, 2.13, and 3.

Add the following dependency description to your build.sbt:

`"ca.dvgi" %% "healthful" % "<latest>"`

## Usage

``` scala
import ca.dvgi.healthful.HealthServer

// synchronously starts the HealthServer, which will be live but not ready
val hs = HealthServer("MyAwesomeService")

// setup service functionailty, e.g. run an HTTP server

hs.markReady() // readiness endpoint changes to 200

sys.addShutdownHook {
  hs.markUnready()  // readiness endpoint changes back to 503

  // shutdown service functionality, e.g. drain HTTP requests

  hs.close()
}
```

## Configuration

By default, the liveness check is available at `/healthz` and the liveness check at `/ready`, on port 9990.

To modify these defaults, either configure in `application.conf`:

```
healthful {
  port = 9991
  liveness-path = "/my-health"
  readiness-path = "/my-ready"
}
```

or pass extra arguments to `HealthServer.apply`:

``` scala
val hs = HealthServer("MyAwesomeService", 9991, "/my-health", "/my-ready")
```

## Contributing 

Contributions in the form of Issues and PRs are welcome.
