# metrics-play

This module provides some support for [Micrometer Metrics](https://micrometer.io/docs) library in a Play2 application (
Scala)

[![Build Status](https://travis-ci.org/kenshoo/metrics-play.png)](https://travis-ci.org/kenshoo/metrics-play)

[![codecov.io](https://img.shields.io/codecov/c/gh/kenshoo/metrics-play/master.svg)](https://codecov.io/github/kenshoo/metrics-play/branch/master)

Play Version: 2.8.13, Micrometer Version: 1.8.3, Scala Versions: 2.12.15, 2.13.8

## Features

1. Default Metrics Registry
2. Metrics Servlet
3. Filter to instrument http requests

## Usage

Add metrics-play dependency:

```scala
libraryDependencies ++= Seq(
  "com.kenshoo" %% "metrics-play" % "2.8.13_0.9.0"
)
```

To enable the module:

add to application.conf the following line

     play.modules.enabled+="com.kenshoo.play.metrics.PlayModule"

### Default Registry

To add a custom metrics, you can use `defaultRegistry` which returns an instance
of [MeterRegistry](https://javadoc.io/doc/io.micrometer/micrometer-core/latest/io/micrometer/core/instrument/MeterRegistry.html)
.

```scala
import javax.inject.Inject
import com.kenshoo.play.metrics.Metrics
import io.micrometer.core.instrument.Counter

class SomeController @Inject()(metrics: Metrics) {
  val counter = metrics.defaultRegistry.counter("name")
  counter.increment()
}
````

### Metrics Controller

*metrics-play* includes a play2 controller which exports all registered metrics as a json document.

To enable the controller add a mapping to conf/routes file

     GET     /admin/metrics              com.kenshoo.play.metrics.MetricsController.metrics

#### Configuration

Some configuration is supported through the default configuration file:

    metrics.durationUnit (default is SECONDS)

    metrics.jvm - [true/false] (default is true) controls reporting jvm metrics

    metrics.logback - [true/false] (default is true) controls reporing logback metrics

### Metrics Filter

An implementation of the Metrics' instrumenting filter for Play2. It records requests duration tagged with return code
and number of active requests.

```scala
import com.kenshoo.play.metrics.MetricsFilter
import play.api.http.HttpFilters

import javax.inject.Inject

class Filters @Inject()(metricsFilter: MetricsFilter) extends HttpFilters {
  val filters = Seq(metricsFilter)
}
```

## Advanced usage

By default, metrics are prefixed with "com.kenshoo.play.metrics.MetricsFilter".

```json
"com.kenshoo.play.metrics.MetricsFilter.requestTimer;status=200" : {
  "count": 4,
  "max": 41.255709,
  "mean": 14.190021,
  "duration_units": "milliseconds"
}
```

You can change the prefix by extending `MetricsFilterImpl`.

```scala
package myapp

import javax.inject.Inject

import com.kenshoo.play.metrics.{MetricsImpl, MetricsFilter, Metrics, MetricsFilterImpl}
import play.api.http.Status
import play.api.inject.Module
import play.api.{Configuration, Environment}

class MyMetricsFilter @Inject()(metrics: Metrics) extends MetricsFilterImpl(metrics) {

  // configure metrics prefix
  override def labelPrefix: String = "foobar"

  // configure status codes to be monitored. other status codes are labeled as "other"
  override def knownStatuses = Seq(Status.OK)
}

class MyMetricsModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    Seq(
      bind[MetricsFilter].to[MyMetricsFilter].eagerly,
      bind[Metrics].to[MetricsImpl].eagerly
    )
  }
}
```

and add the following line to application.conf

```
play.modules.enabled+="myapp.MyMetricsModule"
```

## Changes

* 2.8.13_0.9.0 - Migrate the project to Micrometer Metrics backend
* 2.7.3_0.8.2 - Minor compatability fix for Play 2.8
* 2.7.3_0.8.1 - Upgrade to play 2.7.3 and support Scala version 2.12.8 / 2.13.0 with dropwizard 4.0.5
* 2.7.0_0.8.0 - Upgrade to play 2.7.0 and Scala 2.12.8 and dropwizard 4.0.5
* 2.6.19_0.7.0 - Upgrade to play 2.6.19 and Scala 2.12.6 and dropwizard 4.0.3
* 2.6.2_0.6.1 - Upgrade to play 2.6 and Scala 2.12. Migration: If you get errors like "No configuration setting found
  ..." when building fat JARs, check your merge strategy for reference.conf.
* 2.4.0_0.4.0 - Re-implement as Play Module
* 2.4.0_0.3.0 - Upgrade to play 2.4, metrics 3.1.2
* 2.3.0_0.2.1 - Breaking Change! prefix jvm metric names to standardize with dropwizard
* 2.3.0_0.2.0 - Meter uncaught exceptions as 500 Internal Server Error
* 2.3.0_0.1.9 - Add extra http codes, support configurable metrics names for requests filter
* 2.3.0_0.1.8 - Support default registry in play java. Replace MetricsRegistry.default with
  MetricsRegistry.defaultRegistry (to support java where default is a reserved keyword)

## License

This code is released under the Apache Public License 2.0.
