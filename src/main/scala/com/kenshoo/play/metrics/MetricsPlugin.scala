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

import ch.qos.logback.classic
import com.codahale.metrics.logback.InstrumentedAppender
import play.api.{Logger, Application, Play, Plugin}

import com.codahale.metrics.{MetricRegistry, SharedMetricRegistries}
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ThreadStatesGaugeSet, GarbageCollectorMetricSet, MemoryUsageGaugeSet}

import com.fasterxml.jackson.databind.ObjectMapper


object MetricsRegistry {

  def defaultRegistry = Play.current.plugin[MetricsPlugin] match {
    case Some(plugin) => SharedMetricRegistries.getOrCreate(plugin.registryName)
    case None => throw new Exception("metrics plugin is not configured")
  }
  
  @deprecated(message = "use defualtRegistry")
  def default = defaultRegistry
}


class MetricsPlugin(val app: Application) extends Plugin {
  val validUnits = Some(Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"))

  val mapper: ObjectMapper = new ObjectMapper()

  def registryName = app.configuration.getString("metrics.name").getOrElse("default")

  implicit def stringToTimeUnit(s: String) : TimeUnit = TimeUnit.valueOf(s)

  override def onStart() {
    def setupJvmMetrics(registry: MetricRegistry) {
      val jvmMetricsEnabled = app.configuration.getBoolean("metrics.jvm").getOrElse(true)
      if (jvmMetricsEnabled) {
        registry.registerAll(new GarbageCollectorMetricSet())
        registry.registerAll(new MemoryUsageGaugeSet())
        registry.registerAll(new ThreadStatesGaugeSet())
      }
    }

    def setupLogbackMetrics(registry: MetricRegistry) = {
      val logbackEnabled = app.configuration.getBoolean("metrics.logback").getOrElse(true)
      if (logbackEnabled) {
        val appender: InstrumentedAppender = new InstrumentedAppender(registry)

        val logger: classic.Logger = Logger.logger.asInstanceOf[classic.Logger]
        appender.setContext(logger.getLoggerContext)
        appender.start()
        logger.addAppender(appender)
      }
    }

    if (enabled) {
      val registry: MetricRegistry = SharedMetricRegistries.getOrCreate(registryName)
      val rateUnit     = app.configuration.getString("metrics.rateUnit", validUnits).getOrElse("SECONDS")
      val durationUnit = app.configuration.getString("metrics.durationUnit", validUnits).getOrElse("SECONDS")
      val showSamples  = app.configuration.getBoolean("metrics.showSamples").getOrElse(false)

      setupJvmMetrics(registry)
      setupLogbackMetrics(registry)

      val module = new MetricsModule(rateUnit, durationUnit, showSamples)
      mapper.registerModule(module)
    }
  }


  override def onStop() {
    if (enabled) {
      SharedMetricRegistries.remove(registryName)
    }
  }

  override def enabled = app.configuration.getBoolean("metrics.enabled").getOrElse(true)
}

