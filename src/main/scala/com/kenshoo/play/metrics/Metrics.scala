package com.kenshoo.play.metrics

import com.fasterxml.jackson.databind.{ObjectMapper, ObjectWriter}
import io.micrometer.core.instrument.binder.jvm.{JvmGcMetrics, JvmInfoMetrics, JvmMemoryMetrics, JvmThreadMetrics}
import io.micrometer.core.instrument.binder.logging.LogbackMetrics
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.micrometer.core.instrument.{Counter, DistributionSummary, FunctionCounter, FunctionTimer, Gauge, LongTaskTimer, Meter, MeterRegistry, Tag, TimeGauge, Timer, Metrics => MicrometerMetrics}
import play.api.inject.ApplicationLifecycle
import play.api.{Configuration, Logger}

import java.io.StringWriter
import java.util
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


trait Metrics {

  def defaultRegistry: CompositeMeterRegistry

  def toJson: String
}

@Singleton
class MetricsImpl @Inject()(lifecycle: ApplicationLifecycle, configuration: Configuration) extends Metrics {

  private val innerLogger: Logger = Logger(classOf[MetricsImpl])

  val validUnits = Set("NANOSECONDS", "MICROSECONDS", "MILLISECONDS", "SECONDS", "MINUTES", "HOURS", "DAYS")

  val registryName: String = configuration.get[String]("metrics.name")
  val rateUnit: TimeUnit = TimeUnit.valueOf(configuration.getAndValidate[String]("metrics.rateUnit", validUnits))
  val durationUnit: TimeUnit = TimeUnit.valueOf(configuration.getAndValidate[String]("metrics.durationUnit", validUnits))
  val showSamples: Boolean = configuration.get[Boolean]("metrics.showSamples")
  val jvmMetricsEnabled: Boolean = configuration.get[Boolean]("metrics.jvm")
  val logbackEnabled: Boolean = configuration.get[Boolean]("metrics.logback")

  val mapper: ObjectMapper = new ObjectMapper()

  def toJson: String = {
    val rootNode = mapper.createObjectNode()
    val gauges = ListBuffer.empty[Gauge]
    val counters = ListBuffer.empty[Counter]
    val timers = ListBuffer.empty[Timer]
    val summaries = ListBuffer.empty[DistributionSummary]
    val longTaskTimers = ListBuffer.empty[LongTaskTimer]
    val timeGauges = ListBuffer.empty[TimeGauge]
    val functionCounters = ListBuffer.empty[FunctionCounter]
    val functionTimers = ListBuffer.empty[FunctionTimer]
    val meters = ListBuffer.empty[Meter]

    defaultRegistry.forEachMeter(meter => meter.use(
      gauge => gauges += gauge,
      counter => counters += counter,
      timer => timers += timer,
      summary => summaries += summary,
      longTaskTimer => longTaskTimers += longTaskTimer,
      timeGauge => timeGauges += timeGauge,
      functionCounter => functionCounters += functionCounter,
      functionTimer => functionTimers += functionTimer,
      meter => meters += meter
    ))

    val gaugesNode = rootNode.putObject("gauges")
    gauges.foreach {
      gauge =>
        val gaugeNode = gaugesNode.putObject(meterName(gauge.getId))
        gaugeNode.put("value", gauge.value())
    }

    val countersNode = rootNode.putObject("counters")
    counters.foreach {
      counter =>
        val counterNode = countersNode.putObject(meterName(counter.getId))
        counterNode.put("count", counter.count())
    }

    val timersNode = rootNode.putObject("timers")
    timers.foreach {
      timer =>
        val timerNode = timersNode.putObject(meterName(timer.getId))
        timerNode.put("count", timer.count())
        timerNode.put("max", timer.max(durationUnit))
        timerNode.put("mean", timer.mean(durationUnit))
        timerNode.put("duration_units", durationUnit.toString.toLowerCase())
    }

    val summariesNode = rootNode.putObject("summaries")
    summaries.foreach {
      summary =>
        val summaryNode = summariesNode.putObject(meterName(summary.getId))
        val summarySnapshot = summary.takeSnapshot()
        summaryNode.put("count", summarySnapshot.count())
        summaryNode.put("max", summarySnapshot.max())
        summaryNode.put("mean", summarySnapshot.mean())

        summarySnapshot.percentileValues().foreach {
          percentileValue =>
            val percentile = percentileValue.percentile() * 100
            val percentileName =
              if (percentile % 1.0 != 0) f"p${percentile}%s"
              else f"p${percentile}%.0f"
            summaryNode.put(percentileName, percentileValue.value())
        }
    }

    val longTaskTimersNode = rootNode.putObject("longTaskTimers")
    longTaskTimers.foreach {
      longTaskTimer =>
        val longTaskTimerNode = longTaskTimersNode.putObject(meterName(longTaskTimer.getId))
        longTaskTimerNode.put("max", longTaskTimer.max(durationUnit))
        longTaskTimerNode.put("mean", longTaskTimer.mean(durationUnit))
        longTaskTimerNode.put("duration_units", durationUnit.toString.toLowerCase())
        longTaskTimerNode.put("active_tasks", longTaskTimer.activeTasks())
    }

    val timeGaugesNode = rootNode.putObject("timeGauges")
    timeGauges.foreach {
      timeGauge =>
        val timeGaugeNode = timeGaugesNode.putObject(meterName(timeGauge.getId))
        timeGaugeNode.put("value", timeGauge.value(durationUnit))
    }

    val functionCountersNode = rootNode.putObject("functionCounters")
    functionCounters.foreach {
      functionCounter =>
        val functionCounterNode = functionCountersNode.putObject(meterName(functionCounter.getId))
        functionCounterNode.put("count", functionCounter.count())
    }

    val functionTimersNode = rootNode.putObject("functionTimers")
    functionTimers.foreach {
      functionTimer =>
        val functionTimerNode = functionTimersNode.putObject(meterName(functionTimer.getId))
        functionTimerNode.put("count", functionTimer.count())
        functionTimerNode.put("mean", functionTimer.mean(durationUnit))
    }

    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, rootNode)
    stringWriter.toString

  }

  def meterName(id: Meter.Id): String = {
    val tags = Option(id.getTags)
      .filterNot(_.isEmpty)
      .map(tagList)
      .getOrElse("")

    id.getName + tags
  }

  def tagList(tags: util.List[Tag]): String = {
    tags.asScala.map {
      tag =>
        val tagValue = tag.getValue.replaceAll(";", "_")
        s"${tag.getKey}=${tagValue}"
    }.mkString(";", ";", "")
  }

  def defaultRegistry: CompositeMeterRegistry = MicrometerMetrics.globalRegistry

  def setupJvmMetrics(registry: MeterRegistry) = {
    if (jvmMetricsEnabled) {
      new JvmInfoMetrics().bindTo(registry)
      new JvmGcMetrics().bindTo(registry)
      new JvmMemoryMetrics().bindTo(registry)
      new JvmThreadMetrics().bindTo(registry)
    }
  }

  def setupLogbackMetrics(registry: MeterRegistry): Unit = {
    if (logbackEnabled) {
      new LogbackMetrics().bindTo(registry)
    }
  }

  def onStart(): Unit = {
    setupJvmMetrics(defaultRegistry)
    setupLogbackMetrics(defaultRegistry)
  }

  def onStop(): Unit = {
    // We do not close the registry because it is a global instance
    defaultRegistry.clear()
  }

  onStart()
  lifecycle.addStopHook(() => Future.successful {
    onStop()
  })
}

@Singleton
class DisabledMetrics @Inject() extends Metrics {
  def defaultRegistry: CompositeMeterRegistry = throw new MetricsDisabledException

  def toJson: String = throw new MetricsDisabledException
}

class MetricsDisabledException extends Throwable
