package com.kenshoo.play.metrics

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}

class PlayModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    if (configuration.get[Boolean]("metrics.enabled")) {
      Seq(
        bind[MetricsFilter].to[MetricsFilterImpl].eagerly(),
        bind[Metrics].to[MetricsImpl].eagerly()
      )
    } else {
      Seq(
        bind[MetricsFilter].to[DisabledMetricsFilter].eagerly(),
        bind[Metrics].to[DisabledMetrics].eagerly()
      )
    }
  }
}
