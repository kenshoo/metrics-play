/*
* Copyright 2013 Kenshoo.com
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.kenshoo.play.metrics

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import ch.qos.logback.classic
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{GarbageCollectorMetricSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}
import com.codahale.metrics.logback.InstrumentedAppender
import com.codahale.metrics.{MetricRegistry, SharedMetricRegistries}
import com.fasterxml.jackson.databind.ObjectMapper
import play.api._
import play.api.inject.{ApplicationLifecycle, Module}
import javax.inject._
import scala.concurrent.Future

@Singleton
class MetricsPlugin @Inject()(configuration: Configuration, lifecycle: ApplicationLifecycle, registries: MetricRegistries, app: Application, stoper: RegistryStopper) {

  val validUnits = Some(Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"))

  val mapper: ObjectMapper = new ObjectMapper()

  implicit def stringToTimeUnit(s: String): TimeUnit = TimeUnit.valueOf(s)

  val registry = registries.getOrCreate

  lifecycle.addStopHook(() => Future.successful(stoper.stop()))
  onStart()

  def onStart() {
    def setupJvmMetrics(registry: MetricRegistry) {
      val jvmMetricsEnabled = configuration.getBoolean("metrics.jvm").getOrElse(true)
      if (jvmMetricsEnabled) {
        registry.registerAll(new GarbageCollectorMetricSet())
        registry.registerAll(new MemoryUsageGaugeSet())
        registry.registerAll(new ThreadStatesGaugeSet())
      }
    }

    def setupLogbackMetrics(registry: MetricRegistry) = {
      val logbackEnabled = configuration.getBoolean("metrics.logback").getOrElse(true)
      if (logbackEnabled) {
        val appender: InstrumentedAppender = new InstrumentedAppender(registry)

        val logger: classic.Logger = Logger.logger.asInstanceOf[classic.Logger]
        appender.setContext(logger.getLoggerContext)
        appender.start()
        logger.addAppender(appender)
      }
    }

    def setupReporting(conf: Configuration, registry: MetricRegistry) =
      Map(
        "graphite" -> Reporter.graphite _,
        "console" -> Reporter.console _,
        "csv" -> Reporter.csv _
      ).foreach {
        case (name, fun) =>
          conf.getConfig(name).foreach {
            conf =>
              if (conf.getBoolean("enabled").getOrElse(false))
                fun(conf, registry)()
          }
      }

    if (enabled) {
      val rateUnit = configuration.getString("metrics.rateUnit", validUnits).getOrElse("SECONDS")
      val durationUnit = configuration.getString("metrics.durationUnit", validUnits).getOrElse("SECONDS")
      val showSamples = configuration.getBoolean("metrics.showSamples").getOrElse(false)

      setupJvmMetrics(registry)
      setupLogbackMetrics(registry)
      setupReporting(configuration.getConfig("metrics.reporting").getOrElse(Configuration.empty), registry)

      val module = new MetricsModule(rateUnit, durationUnit, showSamples)
      new MetricsModule(rateUnit, durationUnit, showSamples)
      mapper.registerModule(module)
    }
  }

  def enabled = configuration.getBoolean("metrics.enabled").getOrElse(true)
}

class RegistryStopper @Inject()(configuration: Configuration) {
  def stop() = SharedMetricRegistries.remove(configuration.getString("metrics.name").getOrElse("default"))
}

class MetricRegistries @Inject()(configuration: Configuration) {
  def getOrCreate =
    SharedMetricRegistries.getOrCreate(configuration.getString("metrics.name").getOrElse("default"))
}

class PlayMetricsModule extends Module {
  def bindings(environment: Environment,
               configuration: Configuration) = Seq(
    bind[MetricsPlugin].toSelf,
    bind[RegistryStopper].toSelf,
    bind[MetricRegistries].toSelf
  )
}
