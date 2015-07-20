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

import org.specs2.mutable.Specification
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._

class MetricsControllerSpec extends Specification with BeforaAfterTest {
  sequential

  def makeApp =
    new GuiceApplicationBuilder()
    .configure("metrics.name" -> "MetricsControllerSpec_metrics")
    .build()

  "metrics servlet" should {
    "returns json result" in {
      val app = makeApp

      val controller = app.injector.instanceOf[MetricsController]

      val filter = app.injector.instanceOf[JavaMetricsFilter]
      Json.parse(contentAsString(filter.apply(controller.metrics())(FakeRequest()).run))
      val jsValue = Json.parse(contentAsString(filter.apply(controller.metrics())(FakeRequest()).run))

      jsValue.isInstanceOf[JsValue] must_== true
    }


    "sets no cache control" in {
      val app = makeApp

      val controller = app.injector.instanceOf[MetricsController]

      val result = controller.metrics(FakeRequest())
      headers(result) must haveValue("must-revalidate,no-cache,no-store")
    }

  }
}
