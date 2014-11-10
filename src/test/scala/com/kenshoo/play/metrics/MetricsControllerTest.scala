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

import org.specs2.mutable.{Before, Specification}
import play.api.mvc.{Controller, Result}
import play.api.test.{WithApplication, FakeApplication, FakeRequest}
import play.api.libs.json.{Json, JsValue}
import play.api.test.Helpers._
import com.codahale.metrics.{SharedMetricRegistries, MetricRegistry}

class MetricsControllerSpec extends Specification {
  "metrics servlet" should {
    "returns json result" in new ControllerRegistry {
      val result = controller.metrics(None)(FakeRequest())
      val jsValue: JsValue = Json.parse(contentAsString(result))
      (jsValue \ "counters" \ "my-counter" \ "count").as[Int] mustEqual(1)
    }

    "returns json result from a custom registry" in new ControllerRegistry {
      val result = controller.metrics(Some("custom"))(FakeRequest())
      val jsValue: JsValue = Json.parse(contentAsString(result))
      (jsValue \ "counters" \ "custom-counter" \ "count").as[Int] mustEqual(1)
    }

    "sets no cache control" in new ControllerRegistry {
      val result = controller.metrics(None)(FakeRequest())
      headers(result) must haveValue("must-revalidate,no-cache,no-store")
    }

  }

  abstract class ControllerRegistry extends WithApplication(
    FakeApplication(additionalPlugins = Seq("com.kenshoo.play.metrics.MetricsPlugin"))
  ) with Before {
    lazy val testRegistry = new MetricRegistry
    lazy val customTestRegistry = new MetricRegistry

    def before {
      testRegistry.counter("my-counter").inc()
      customTestRegistry.counter("custom-counter").inc()
    }

    lazy val controller = new Controller() with MetricsController {
      override val registry = testRegistry
      override def customRegistry(name: String) = customTestRegistry
      override val app = implicitApp
    }
  }

}
