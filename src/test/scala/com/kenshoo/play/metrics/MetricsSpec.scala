package com.kenshoo.play.metrics

import org.specs2.mutable.Specification
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{Json, JsValue}
import play.api.test.Helpers._
import scala.collection.JavaConverters._

class MetricsSpec extends Specification {

  def withApplication[T](conf: Map[String, Any])(block: Application => T): T = {

    lazy val application = new GuiceApplicationBuilder()
      .configure(conf)
      .overrides(
        bind[Metrics].to[MetricsImpl]
      ).build()

    running(application){block(application)}
  }

  def metrics(implicit app: Application) = app.injector.instanceOf[Metrics]

  "Metrics" should {

    "serialize to JSON" in withApplication(Map.empty) { implicit app =>
      val jsValue: JsValue = Json.parse(metrics.toJson)
      (jsValue \ "version").as[String] mustEqual "3.0.0"
    }

    "be able to add custom counter" in withApplication(Map.empty) { implicit app =>
      metrics.defaultRegistry.counter("my-counter").inc()

      val jsValue: JsValue = Json.parse(metrics.toJson)
      (jsValue \ "counters" \ "my-counter" \ "count").as[Int] mustEqual(1)
    }

    "contain JVM metrics" in withApplication(Map.empty) { implicit app =>
      metrics.defaultRegistry.getGauges.asScala must haveKey("jvm.attribute.name")
    }

    "contain logback metrics" in withApplication(Map.empty) { implicit app =>
      metrics.defaultRegistry.getMeters.asScala must haveKey("ch.qos.logback.core.Appender.all")
    }

    "be able to turn off JVM metrics" in withApplication(Map("metrics.jvm" -> false)) { implicit app =>
      metrics.defaultRegistry.getGauges.asScala must not haveKey("jvm.attribute.name")
    }

    "be able to turn off logback metrics" in withApplication(Map("metrics.logback" -> false)) { implicit app =>
      metrics.defaultRegistry.getMeters.asScala must not haveKey("ch.qos.logback.core.Appender.all")
    }
  }
}
