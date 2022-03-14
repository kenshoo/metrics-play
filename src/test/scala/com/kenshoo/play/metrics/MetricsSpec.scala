package com.kenshoo.play.metrics

import io.micrometer.core.instrument.{Clock, Gauge, Meter}
import io.micrometer.jmx.{JmxConfig, JmxMeterRegistry}
import org.specs2.mutable.Specification
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsObject, JsResultException, JsValue, Json}
import play.api.test.Helpers._

import scala.collection.mutable

class MetricsSpec extends Specification {

  def withApplication[T](conf: Map[String, Any])(block: Application => T): T = {

    lazy val application = new GuiceApplicationBuilder()
      .configure(conf)
      .overrides(
        bind[Metrics].to[MetricsImpl]
      ).build()

    running(application) {
      val defaultRegistry = metrics(application).defaultRegistry
      val jmxRegistry = new JmxMeterRegistry(JmxConfig.DEFAULT, Clock.SYSTEM)
      defaultRegistry.add(jmxRegistry)
      try {
        block(application)
      } finally {
        defaultRegistry.remove(jmxRegistry)
        defaultRegistry.clear()
        jmxRegistry.stop()
      }
    }
  }

  def metrics(implicit app: Application) = app.injector.instanceOf[Metrics]

  sequential

  "Metrics" should {

    "serialize to JSON" in withApplication(Map.empty) { implicit app =>
      val jsValue: JsValue = Json.parse(metrics.toJson)
      (jsValue \ "gauges").as[JsObject] must not(throwA[JsResultException])
    }

    "be able to add custom counter" in withApplication(Map("metrics.jvm" -> false)) { implicit app =>
      metrics.defaultRegistry.counter("my-counter").increment()

      val jsValue: JsValue = Json.parse(metrics.toJson)
      println(jsValue)

      (jsValue \ "counters" \ "my-counter" \ "count").as[Int] mustEqual (1)


    }

    "contain JVM metrics" in withApplication(Map("metrics.jvm" -> true)) { implicit app =>
      val gauges = mutable.Map.empty[Meter.Id, Gauge]
      metrics.defaultRegistry.forEachMeter {
        meter => if (meter.isInstanceOf[Gauge]) gauges += (meter.getId -> meter.asInstanceOf[Gauge])
      }
      gauges.collectFirst {
        case (k, v) if k.getName == "jvm.info" => v
      } must beSome[Gauge]
    }
  }


  "contain logback metrics" in withApplication(Map.empty) { implicit app =>
    val meters = mutable.Map.empty[Meter.Id, Meter]
    metrics.defaultRegistry.forEachMeter {
      meter => meters += (meter.getId -> meter)
    }
    meters.collectFirst {
      case (k, v) if k.getName == "logback.events" => v
    } must beSome[Meter]
  }

  "be able to turn off JVM metrics" in withApplication(Map("metrics.jvm" -> false)) { implicit app =>
    val meters = mutable.Map.empty[String, Meter]
    metrics.defaultRegistry.forEachMeter {
      meter => meters += (meter.getId.getName -> meter)
    }
    meters must not haveKey ("jvm.info")
  }

  "be able to turn off logback metrics" in withApplication(Map("metrics.logback" -> false)) { implicit app =>

    val meters = mutable.Map.empty[String, Meter]
    metrics.defaultRegistry.forEachMeter {
      meter => meters += (meter.getId.getName -> meter)
    }
    meters must not haveKey ("logback.events")
  }
}
