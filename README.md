# metrics-play

This module provides some support for @codahale [Metrics](http://metrics.codahale.com/) library in a Play2 application (Scala)

[![Build Status](https://travis-ci.org/kenshoo/metrics-play.png)](https://travis-ci.org/kenshoo/metrics-play)

Play Version: 2.2.0, Metrics Version: 3.0.0, Scala Version: 2.10.0

## Features

1. Default Metrics Registry
2. Metrics Servlet
3. Filter to instrument http requests


## Usage

Add metrics-play dependency:

```scala
    val appDependencies = Seq(
    ...
    "com.kenshoo" %% "metrics-play" % "0.1.3"
    )
```

To enable the plugin:

add to conf/play.plugins the following line

     {priority}:com.kenshoo.play.metrics.MetricsPlugin

where priority is the priority of this plugin with respect to other plugins.

### Default Registry

```scala
     import com.kenshoo.play.metrics.MetricsRegistry
     import com.codahale.metrics.Counter

     val counter = MetricsRegistry.default.counter("name")
     counter.inc()
````

### Metrics Controller

An implementation of the [metrics-servlet](http://metrics.codahale.com/manual/servlets/) as a play2 controller.

It xports all registered metrics as a json document.

To enable the controller add a mapping to conf/routes file

     GET     /admin/metrics              com.kenshoo.play.metrics.MetricsController.metrics
     
#### Configuration
Some configuration is supported through the default configuration file:

    metrics.rateUnit - (default is SECONDS) 

    metrics.durationUnit (default is SECONDS)

    metrics.showSamples [true/false] (default is false)

    metrics.jvm - [true/false] (default is true)

    metrics.showHttpStatusLevels [true/false] (default is false)

    metrics.knownStatuses [list of Ints] (default is a hard-coded list)

### Metrics Filter

An implementation of the Metrics' instrumenting filter for Play2. It records requests duration, number of active requests and counts each return code


```scala
    import com.kenshoo.play.metrics.MetricsFilter
    import play.api.mvc._

    object Global extends WithFilters(MetricsFilter)
```

## License
This code is released under the Apache Public License 2.0.
