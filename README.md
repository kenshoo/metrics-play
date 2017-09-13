# metrics-play

This module provides some support for @codahale [Metrics](https://dropwizard.github.io/metrics/3.1.0/) library in a Play2 application (Scala)

[![Build Status](https://travis-ci.org/kenshoo/metrics-play.png)](https://travis-ci.org/kenshoo/metrics-play)

[![codecov.io](https://img.shields.io/codecov/c/gh/kenshoo/metrics-play/master.svg)](https://codecov.io/github/kenshoo/metrics-play/branch/master)

Play Version: 2.6.2, Metrics Version: 3.2.2, Scala Versions: 2.12.2

## Features

1. Default Metrics Registry
2. Metrics Servlet
3. Filter to instrument http requests


## Usage

Add metrics-play dependency:

```scala
    val appDependencies = Seq(
    ...
    "com.kenshoo" %% "metrics-play" % "2.6.2_0.6.1"
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

An implementation of the [metrics-servlet](http://metrics.dropwizard.io/3.1.0/manual/servlets/#metricsservlet) as a play2 controller.

It exports all registered metrics as a json document.

To enable the controller add a mapping to conf/routes file

     GET     /admin/metrics              com.kenshoo.play.metrics.MetricsController.metrics
     
#### Configuration
Some configuration is supported through the default configuration file:

    metrics.enabled - [true/false] (default is true)
    
    metrics.name - (default is "default") Name of the dropwizard metrics registry
    
    metrics.labelPrefix - (defaults to classname ("com.kenshoo.play.metrics.MetricsFilter"))

    metrics.rateUnit - (default is "SECONDS")

    metrics.durationUnit (default is "SECONDS")

    metrics.showSamples [true/false] (default is false)

    metrics.jvm - [true/false] (default is true) controls reporting jvm metrics
  
    metrics.logback - [true/false] (default is true) controls reporing logback metrics


#### Name prefix
By default, metrics are prefixed with the name of the class: "com.kenshoo.play.metrics.MetricsFilter":

```
"com.kenshoo.play.metrics.MetricsFilter.200" : {
   "count" : 1584456,
   "m15_rate" : 1.6800220918042639,
   "m1_rate" : 1.9015104460758263,
   "m5_rate" : 1.8138545372237085,
   "mean_rate" : 3.20162010446889,
   "units" : "events/second"
},
```

You can change the prefix with configuration key `metrics.labelPrefix` 

### Metrics Filter

An implementation of the Metrics' instrumenting filter for Play2. It records requests duration, number of active requests and counts each return code


```scala
    import com.kenshoo.play.metrics.MetricsFilter
    import play.api.mvc._

    class Filters @Inject() (metricsFilter: MetricsFilter) extends HttpFilters {
        val filters = Seq(metricsFilter)
    }
```

## Advanced usage

Inherit your own custom filter to implement modifications not available through configuration:

```scala
package myapp

import javax.inject.Inject

import com.kenshoo.play.metrics.{MetricsImpl, MetricsFilter, Metrics, MetricsFilterImpl}
import play.api.http.Status
import play.api.inject.Module
import play.api.{Configuration, Environment}

class MyMetricsFilter @Inject() (metrics: Metrics) extends MetricsFilterImpl(metrics) {

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

and add a line like this to application.conf

```
play.modules.enabled+="myapp.MyMetricsModule"
```

instead of `com.kenshoo.play.metrics.PlayModule`

## Changes

* 2.6.2_0.6.1 - Upgrade to play 2.6 and Scala 2.12. Migration: If you get errors like "No configuration setting found ..." when building fat JARs, check your merge strategy for reference.conf. 
* 2.4.0_0.4.0 - Re-implement as Play Module
* 2.4.0_0.3.0 - Upgrade to play 2.4, metrics 3.1.2
* 2.3.0_0.2.1 - Breaking Change! prefix jvm metric names to standardize with dropwizard
* 2.3.0_0.2.0 - Meter uncaught exceptions as 500 Internal Server Error
* 2.3.0_0.1.9 - Add extra http codes, support configurable metrics names for requests filter
* 2.3.0_0.1.8 - Support default registry in play java. Replace MetricsRegistry.default with MetricsRegistry.defaultRegistry (to support java where default is a reserved keyword)


## License
This code is released under the Apache Public License 2.0.
