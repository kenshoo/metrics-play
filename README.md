# metrics-play

This module provides some support for Coda Hale Metrics library in a Play2 application (Scala)

## Features

1. Default Metrics Registry
2. Metrics Servlet
3. Filter to instrument http requests


## Usage

Add metrics-play dependency:

```scala
    val appDependencies = Seq(
    ...
    "com.kenshoo" %% "metrics-play" % "0.1.0"
    )
```

Enable the plugin:
add to conf/play.plugins the following line

     {priority}:com.kenshoo.play.metrics.MetricsPlugin

where priority is the priority of this plugin in respect to other plugins.





