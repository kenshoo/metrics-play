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

import com.codahale.metrics._
import org.specs2.mutable.Specification
import play.api.Logger
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test._

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.duration._

class MetricsFilterSpec extends Specification with BeforaAfterTest {
  sequential

  def venv = play.api.Environment.simple(mode = play.api.Mode.Test)

  def config = play.api.Configuration.load(venv)

  def modules = config.getStringList("play.modules.enabled").fold(
    List.empty[String])(_.toList)


  def app = new GuiceApplicationBuilder()
    .configure("metrics.name" -> "MetricsFilterSpec_metrics")
    .configure("play.http.filters" -> "com.kenshoo.play.metrics.TestingFilters")
    .configure("play.modules.enabled" -> (modules :+
    "com.kenshoo.play.metrics.PlayMetricsModule")).build()


  def registry = app.injector.instanceOf[MetricRegistries].getOrCreate

  "metrics filter" should {
    "return passed response code" in new WithApplication(app) {

      val controller = app.injector.instanceOf[MetricsController]
      val filter = app.injector.instanceOf[JavaMetricsFilter]
      val result = filter.apply(controller.metrics())(FakeRequest()).run
      status(result) must equalTo(OK)
    }

    "increment status code counter" in new WithApplication(app) {
      Await.ready(route(FakeRequest("GET", "/")).get, 1 second)

      val controller = app.injector.instanceOf[MetricsController]
      val filter = app.injector.instanceOf[JavaMetricsFilter]
      val result = Await.result(filter.apply(controller.metrics())(FakeRequest()).run, 1 second)


      val meters2: Map[String, Meter] = registry.getMeters.toMap
      meters2.foreach(m => println(m._1 +": " + m._2.getCount))

      Await.result(filter.apply(controller.metrics())(FakeRequest()).run, 1 second)

      val meter: Meter = registry.meter(s"status.com.kenshoo.play.metrics.MetricsController.200")
      val meters: Map[String, Meter] = registry.getMeters.toMap
      meters.foreach(m => println(m._1 +": " + m._2.getCount))
      meter.getCount must equalTo(1)
    }

    "increment request timer" in new WithApplication(app) {
      Await.ready(route(FakeRequest("GET", "/")).get, 1 second)
      val timer = registry.timer(s"latency.${MetricRegistry.name(classOf[MetricsFilter])}")
      timer.getCount must beGreaterThan(0l)
    }
  }

}
