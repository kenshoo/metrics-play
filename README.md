# metrics-play

This module provides some support for @codahale [Metrics](https://dropwizard.github.io/metrics/3.1.0/) library in a Play2 application (Scala)

[![Build Status](https://travis-ci.org/kenshoo/metrics-play.png)](https://travis-ci.org/kenshoo/metrics-play)

Play Version: 2.4.0, Metrics Version: 3.1.1, Scala Versions: 2.11.6

## Features

1. Default Metrics Registry
2. Metrics Servlet
3. Filter to instrument http requests


## Usage

Add metrics-play dependency:

```scala
    val appDependencies = Seq(
    ...
    "com.kenshoo" %% "metrics-play" % "2.4.0_0.4.0"
    )
```

To enable the module:

add to application.conf the following line

     play.modules.enabled+="com.kenshoo.play.metrics.PlayModule"

### Default Registry

To add a custom metrics, you can use `defaultRegistry` which returns an instance of [MetricRegistry](http://metrics.dropwizard.io/3.1.0/manual/core/).

```scala
     import com.kenshoo.play.metrics.Metrics
     import com.codahale.metrics.Counter

     class SomeController @Inject() (metrics: Metrics) {
         val counter = metrics.defaultRegistry.counter("name")
         counter.inc()
     }
````

### Metrics Controller

An implementation of the [metrics-servlet](http://metrics.codahale.com/manual/servlets/) as a play2 controller.

It exports all registered metrics as a json document.

To enable the controller add a mapping to conf/routes file

     GET     /admin/metrics              com.kenshoo.play.metrics.MetricsController.metrics
     
#### Configuration
Some configuration is supported through the default configuration file:

    metrics.rateUnit - (default is SECONDS) 

    metrics.durationUnit (default is SECONDS)

    metrics.showSamples [true/false] (default is false)

    metrics.jvm - [true/false] (default is true) controls reporting jvm metrics
  
    metrics.logback - [true/false] (default is true) controls reporing logback metrics

### Metrics Filter

An implementation of the Metrics' instrumenting filter for Play2. It records requests duration, number of active requests and counts each return code


```scala
    import com.kenshoo.play.metrics.MetricsFilter
    import play.api.mvc._

    class Filters @Inject() (metricsFilter: MetricsFilter) extends HttpFilters {
        val filters = Seq(metricsFilter)
    }
```

## Changes

* 2.4.0_0.4.0 - Re-implement as Play Module
* 2.4.0_0.3.0 - Upgrade to play 2.4, metrics 3.1.2
* 2.3.0_0.2.1 - Breaking Change! prefix jvm metric names to standardize with dropwizard
* 2.3.0_0.2.0 - Meter uncaught exceptions as 500 Internal Server Error
* 2.3.0_0.1.9 - Add extra http codes, support configurable metrics names for requests filter
* 2.3.0_0.1.8 - Support default registry in play java. Replace MetricsRegistry.default with MetricsRegistry.defaultRegistry (to support java where default is a reserved keyword)


## License
This code is released under the Apache Public License 2.0.
