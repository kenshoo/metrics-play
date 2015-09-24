package com.kenshoo.play.metrics

import play.api.{Environment, Configuration}
import play.api.inject.Module

class PlayModule extends Module {
  def bindings(environment: Environment, configuration: Configuration) = {
    if (configuration.getBoolean("metrics.enabled").getOrElse(true)) {
      Seq(
        bind[MetricsFilter].to[MetricsFilterImpl].eagerly,
        bind[Metrics].to[MetricsImpl].eagerly
      )
    } else {
      Seq(
        bind[MetricsFilter].to[DisabledMetricsFilter].eagerly,
        bind[Metrics].to[DisabledMetrics].eagerly
      )
    }
  }
}
