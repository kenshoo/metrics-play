package com.kenshoo.play.metrics

import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import ch.qos.logback.classic
import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import io.dropwizard.metrics5.json.MetricsModule
import io.dropwizard.metrics5.jvm.{GarbageCollectorMetricSet, JvmAttributeGaugeSet, MemoryUsageGaugeSet, ThreadStatesGaugeSet}
import io.dropwizard.metrics5.logback.InstrumentedAppender
import io.dropwizard.metrics5.{MetricName, MetricRegistry, SharedMetricRegistries}
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}

import scala.concurrent.Future

trait Metrics {

  def defaultRegistry: MetricRegistry

  def toJson: String
}

@Singleton
class MetricsImpl @Inject() (lifecycle: ApplicationLifecycle, configuration: Configuration) extends Metrics {

  val validUnits = Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS")

  val registryName = configuration.get[String]("metrics.name")
  val rateUnit = configuration.getAndValidate[String]("metrics.rateUnit", validUnits)
  val durationUnit = configuration.getAndValidate[String]("metrics.durationUnit", validUnits)
  val showSamples  = configuration.get[Boolean]("metrics.showSamples")
  val jvmMetricsEnabled = configuration.get[Boolean]("metrics.jvm")
  val logbackEnabled = configuration.get[Boolean]("metrics.logback")

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
      registry.register(MetricName.build("jvm.attribute"), new JvmAttributeGaugeSet())
      registry.register(MetricName.build("jvm.gc"), new GarbageCollectorMetricSet())
      registry.register(MetricName.build("jvm.memory"), new MemoryUsageGaugeSet())
      registry.register(MetricName.build("jvm.threads"), new ThreadStatesGaugeSet())
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
