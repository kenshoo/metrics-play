package com.kenshoo.play.metrics

import java.util.concurrent.TimeUnit

import play.api.{Application, Play, Plugin}

import com.codahale.metrics.{MetricRegistry, SharedMetricRegistries}
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ThreadStatesGaugeSet, GarbageCollectorMetricSet, MemoryUsageGaugeSet}

import com.fasterxml.jackson.databind.ObjectMapper


object MetricsRegistry {
  def default = Play.current.plugin[MetricsPlugin] match {
      case Some(plugin) => SharedMetricRegistries.getOrCreate(plugin.registryName)
      case None => throw new Exception("metrics plugin is not configured")
  }
}


class MetricsPlugin(val app: Application) extends Plugin {
  val validUnits = Some(Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"))

  val mapper: ObjectMapper = new ObjectMapper()

  def registryName = app.configuration.getString("metrics.name").getOrElse("default")
  def rateUnit     = app.configuration.getString("metrics.rateUnit", validUnits).getOrElse("SECONDS")
  def durationUnit = app.configuration.getString("metrics.durationUnit", validUnits).getOrElse("SECONDS")
  def showSamples  = app.configuration.getBoolean("metrics.showSamples").getOrElse(false)

  implicit def stringToTimeUnit(s: String) : TimeUnit = TimeUnit.valueOf(s)

  override def onStart() {
    if (enabled) {
      val registry: MetricRegistry = SharedMetricRegistries.getOrCreate(registryName)
      val jvmMetricsEnabled = app.configuration.getBoolean("metrics.jvm").getOrElse(true)
      if (jvmMetricsEnabled) {
        registry.registerAll(new GarbageCollectorMetricSet())
        registry.registerAll(new MemoryUsageGaugeSet())
        registry.registerAll(new ThreadStatesGaugeSet())
      }
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

