# metrics-play

This module provides some support for @codahale [Metrics](https://dropwizard.github.io/metrics/3.1.0/) library in a Play2 application (Scala)

[![Build Status](https://travis-ci.org/kenshoo/metrics-play.png)](https://travis-ci.org/kenshoo/metrics-play)

Play Version: 2.3.4, Metrics Version: 3.1.0, Scala Versions: 2.10.4, 2.11.2

## Features

1. Default Metrics Registry
2. Metrics Servlet
3. Filter to instrument http requests


## Usage

Add metrics-play dependency:

```scala
    val appDependencies = Seq(
    ...
    "com.kenshoo" %% "metrics-play" % "2.3.0_0.1.9"
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

     val counter = MetricsRegistry.defaultRegistry.counter("name")
     counter.inc()
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

    object Global extends WithFilters(MetricsFilter)
```

 Note - to use the filter in play java, replace MetricsFilter class with JavaMetricsFilter

 ```java
    import com.kenshoo.play.metrics.JavaMetricsFilter;
    import play.GlobalSettings;
    import play.api.mvc.EssentialFilter;
    
    public class Global extends GlobalSettings {
        @Override
        public <T extends EssentialFilter> Class<T>[] filters() {
    
            return new Class[]{JavaMetricsFilter.class};
        }
    }
 ```
#### Configuration
Configuration can optionally be overridden through subclassing MetricsFilter in order to change the prefix label for
created metrics, and to specify which HTTP Status codes should have individual metrics.

```scala
    import com.kenshoo.play.metrics.{MetricsRegistry, MetricsFilter}
    import play.api.mvc._

    class Global(val reg: MetricRegistry) extends WithFilters(new MetricsFilter{
      val registry: MetricRegistry = reg
      override val knownStatuses: Seq[Int] = Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND, Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR)
      override val label: String = classOf[MetricsFilter].getName
    })
```

## Changes

2.3.0_0.1.8 - Support default registry in play java. Replace MetricsRegistry.default with MetricsRegistry.defaultRegistry (to support java where default is a reserved keyword)
2.3.0_0.1.9 - Add extra http codes, support configurable metrics names for requests filter

## License
This code is released under the Apache Public License 2.0.
