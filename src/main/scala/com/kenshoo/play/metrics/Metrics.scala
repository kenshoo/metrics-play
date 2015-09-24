package com.kenshoo.play.metrics

import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import ch.qos.logback.classic
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ThreadStatesGaugeSet, MemoryUsageGaugeSet, GarbageCollectorMetricSet}
import com.codahale.metrics.logback.InstrumentedAppender
import com.codahale.metrics.{JvmAttributeGaugeSet, SharedMetricRegistries, MetricRegistry}
import com.fasterxml.jackson.databind.{ObjectWriter, ObjectMapper}
import play.api.{Logger, Configuration}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future


trait Metrics {

  def defaultRegistry: MetricRegistry

  def toJson: String
}

@Singleton
class MetricsImpl @Inject() (lifecycle: ApplicationLifecycle, configuration: Configuration) extends Metrics {

  val validUnits = Some(Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS"))

  val registryName = configuration.getString("metrics.name").getOrElse("default")
  val rateUnit = configuration.getString("metrics.rateUnit", validUnits).getOrElse("SECONDS")
  val durationUnit = configuration.getString("metrics.durationUnit", validUnits).getOrElse("SECONDS")
  val showSamples  = configuration.getBoolean("metrics.showSamples").getOrElse(false)
  val jvmMetricsEnabled = configuration.getBoolean("metrics.jvm").getOrElse(true)
  val logbackEnabled = configuration.getBoolean("metrics.logback").getOrElse(true)

  val mapper: ObjectMapper = new ObjectMapper()

  def toJson: String = {

    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, defaultRegistry)
    stringWriter.toString
  }

  def defaultRegistry: MetricRegistry = SharedMetricRegistries.getOrCreate(registryName)

  def setupJvmMetrics(registry: MetricRegistry) {
    if (jvmMetricsEnabled) {
      registry.register("jvm.attribute", new JvmAttributeGaugeSet())
      registry.register("jvm.gc", new GarbageCollectorMetricSet())
      registry.register("jvm.memory", new MemoryUsageGaugeSet())
      registry.register("jvm.threads", new ThreadStatesGaugeSet())
    }
  }

  def setupLogbackMetrics(registry: MetricRegistry) = {
    if (logbackEnabled) {
      val appender: InstrumentedAppender = new InstrumentedAppender(registry)

      val logger: classic.Logger = Logger.logger.asInstanceOf[classic.Logger]
      appender.setContext(logger.getLoggerContext)
      appender.start()
      logger.addAppender(appender)
    }
  }

  def onStart() = {

    setupJvmMetrics(defaultRegistry)
    setupLogbackMetrics(defaultRegistry)

    val module = new MetricsModule(TimeUnit.valueOf(rateUnit), TimeUnit.valueOf(durationUnit), showSamples)
    mapper.registerModule(module)
  }

  def onStop() = {
    SharedMetricRegistries.remove(registryName)
  }

  onStart()
  lifecycle.addStopHook(() => Future.successful{ onStop() })
}

@Singleton
class DisabledMetrics @Inject() extends Metrics {
  def defaultRegistry: MetricRegistry = throw new MetricsDisabledException

  def toJson: String = throw new MetricsDisabledException
}

class MetricsDisabledException extends Throwable
