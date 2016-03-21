package com.kenshoo.play.metrics

import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import ch.qos.logback.classic
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ThreadStatesGaugeSet, MemoryUsageGaugeSet, GarbageCollectorMetricSet}
import com.codahale.metrics.logback.InstrumentedAppender
import com.codahale.metrics.{JvmAttributeGaugeSet, SharedMetricRegistries, MetricRegistry}
import com.fasterxml.jackson.databind.ObjectMapper
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
  val mapper = new ObjectMapper()

  setupJvmMetrics(defaultRegistry)
  setupLogbackMetrics(defaultRegistry)
  mapper.registerModule(new MetricsModule(
    TimeUnit.valueOf(configuration.getString("metrics.rateUnit", validUnits).getOrElse("SECONDS")),
    TimeUnit.valueOf(configuration.getString("metrics.durationUnit", validUnits).getOrElse("SECONDS")),
    configuration.getBoolean("metrics.showSamples").getOrElse(false)))
  lifecycle.addStopHook(() => Future.successful(SharedMetricRegistries.remove(registryName)))

  def toJson: String = {
    val writer = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, defaultRegistry)
    stringWriter.toString
  }

  def defaultRegistry: MetricRegistry = SharedMetricRegistries.getOrCreate(registryName)

  private def setupJvmMetrics(registry: MetricRegistry): Unit =
    if (configuration.getBoolean("metrics.jvm").getOrElse(true)) {
      registry.register("jvm.attribute", new JvmAttributeGaugeSet())
      registry.register("jvm.gc", new GarbageCollectorMetricSet())
      registry.register("jvm.memory", new MemoryUsageGaugeSet())
      registry.register("jvm.threads", new ThreadStatesGaugeSet())
    }

  private def setupLogbackMetrics(registry: MetricRegistry): Unit =
    if (configuration.getBoolean("metrics.logback").getOrElse(true)) {
      val appender = new InstrumentedAppender(registry)
      configuration.getString("metrics.naming.logback").foreach(appender.setName)
      val logger = Logger.logger.asInstanceOf[classic.Logger]
      appender.setContext(logger.getLoggerContext)
      appender.start()
      logger.addAppender(appender)
    }
}

@Singleton
class DisabledMetrics @Inject() extends Metrics {
  def defaultRegistry: MetricRegistry = throw new MetricsDisabledException

  def toJson: String = throw new MetricsDisabledException
}

class MetricsDisabledException extends Throwable