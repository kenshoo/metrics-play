package com.kenshoo.play.metrics

import java.io.StringWriter
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}

import ch.qos.logback.classic
import com.codahale.metrics.json.MetricsModule
import com.codahale.metrics.jvm.{ThreadStatesGaugeSet, MemoryUsageGaugeSet, GarbageCollectorMetricSet}
import com.codahale.metrics.logback.InstrumentedAppender
import com.codahale.metrics.{SharedMetricRegistries, MetricRegistry}
import com.codahale.metrics.jvm.JvmAttributeGaugeSet
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

  private val innerLogger: Logger = Logger(classOf[MetricsImpl])

  val validUnits = Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS")

  val registryName: String = configuration.get[String]("metrics.name")
  val rateUnit: String = configuration.getAndValidate[String]("metrics.rateUnit", validUnits)
  val durationUnit: String = configuration.getAndValidate[String]("metrics.durationUnit", validUnits)
  val showSamples: Boolean = configuration.get[Boolean]("metrics.showSamples")
  val jvmMetricsEnabled: Boolean = configuration.get[Boolean]("metrics.jvm")
  val logbackEnabled: Boolean = configuration.get[Boolean]("metrics.logback")

  val mapper: ObjectMapper = new ObjectMapper()

  def toJson: String = {

    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, defaultRegistry)
    stringWriter.toString
  }

  def defaultRegistry: MetricRegistry = SharedMetricRegistries.getOrCreate(registryName)

  def setupJvmMetrics(registry: MetricRegistry): Unit = {
    if (jvmMetricsEnabled) {
      registry.register("jvm.attribute", new JvmAttributeGaugeSet())
      registry.register("jvm.gc", new GarbageCollectorMetricSet())
      registry.register("jvm.memory", new MemoryUsageGaugeSet())
      registry.register("jvm.threads", new ThreadStatesGaugeSet())
    }
  }

  def setupLogbackMetrics(registry: MetricRegistry): Unit = {
    if (logbackEnabled) {
      val appender: InstrumentedAppender = new InstrumentedAppender(registry)

      val logger: classic.Logger = innerLogger.logger.asInstanceOf[classic.Logger]
      appender.setContext(logger.getLoggerContext)
      appender.start()
      logger.addAppender(appender)
    }
  }

  def onStart(): Unit = {

    setupJvmMetrics(defaultRegistry)
    setupLogbackMetrics(defaultRegistry)

    val module = new MetricsModule(TimeUnit.valueOf(rateUnit), TimeUnit.valueOf(durationUnit), showSamples)
    mapper.registerModule(module)
  }

  def onStop(): Unit = {
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
