package com.kenshoo.play.metrics

import org.specs2.mutable.{Before, Specification}
import play.api.mvc.{Controller, Result}
import play.api.test.{WithApplication, FakeApplication, FakeRequest}
import play.api.libs.json.{Json, JsValue}
import play.api.test.Helpers._
import com.codahale.metrics.MetricRegistry

class MetricsControllerSpec extends Specification {
  "metrics servlet" should {
    "returns json result" in new ControllerRegistry {
      val result: Result = controller.metrics(FakeRequest())
      val jsValue: JsValue = Json.parse(contentAsString(result))
      (jsValue \ "counters" \ "my-counter" \ "count").as[Int] mustEqual(1)
    }

    "sets no cache control" in new ControllerRegistry {
      val result: Result = controller.metrics(FakeRequest())
      headers(result) must haveValue("must-revalidate,no-cache,no-store")
    }

  }

  abstract class ControllerRegistry extends WithApplication(
    FakeApplication(additionalPlugins = Seq("com.kenshoo.play.metrics.MetricsPlugin"))
  ) with Before {
    lazy val testRegistry = new MetricRegistry

    def before {
      testRegistry.counter("my-counter").inc()
    }

    lazy val controller = new Controller() with MetricsController {
      override val registry = testRegistry
      override val app = implicitApp
    }
  }

}
