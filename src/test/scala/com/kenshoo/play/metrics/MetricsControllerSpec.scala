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
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._


class MetricsControllerSpec extends Specification  {

  "MetricsController" should {
    "return JSON serialized by Metric's toJson with correct headers" in {
      val controller = new MetricsController(new Metrics {
        def defaultRegistry = throw new NotImplementedError
        def toJson = "{}"
      }, Helpers.stubControllerComponents())

      val result = controller.metrics.apply(FakeRequest())
      contentAsString(result) must equalTo("{}")
      contentType(result) must beSome("application/json")
      headers(result) must haveValue("must-revalidate,no-cache,no-store")
    }

    "return 500 if metrics module is disabled" in {

      val controller = new MetricsController(new DisabledMetrics(), Helpers.stubControllerComponents())

      val result = controller.metrics.apply(FakeRequest())

      status(result) must equalTo(INTERNAL_SERVER_ERROR)
    }
  }
}
