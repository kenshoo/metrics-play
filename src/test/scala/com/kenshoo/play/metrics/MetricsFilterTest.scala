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
import play.api.http.Status
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import com.codahale.metrics._
import play.api.test.FakeApplication
import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration
import scala.concurrent.Await


class MetricsFilterSpec extends Specification {
  sequential

  val labelPrefix = classOf[MetricsFilter].getName

  "metrics filter" should {
    "return passed response code" in new ApplicationWithFilter {
      val result = route(FakeRequest("GET", "/")).get
      status(result) must equalTo(OK)
    }

    "increment status code counter" in new ApplicationWithFilter {
      route(FakeRequest("GET", "/")).get
      val meter: Meter = registry.meter(MetricRegistry.name(labelPrefix, "200"))
      val meters: Map[String, Meter] = registry.getMeters.toMap
      meter.getCount must equalTo(1)
    }

    "increment status code counter for uncaught exceptions" in new ApplicationWithFilter {
      Await.ready(route(FakeRequest("GET", "/throws")).get, Duration(2, "seconds"))
      val meter: Meter = registry.meter(MetricRegistry.name(labelPrefix, "500"))
      val meters: Map[String, Meter] = registry.getMeters.toMap
      meter.getCount must equalTo(1)
    }

    "increment request timer" in new ApplicationWithFilter {
      route(FakeRequest("GET", "/")).get
      val timer = registry.timer(MetricRegistry.name(labelPrefix, "requestTimer"))
      timer.getCount must beGreaterThan(0L)
    }
  }

  class MockGlobal(val reg: MetricRegistry) extends WithFilters(new MetricsFilter{
    val registry: MetricRegistry = reg
    override val knownStatuses = Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND,
      Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR, Status.CONFLICT,
      Status.UNAUTHORIZED)
    override val labelPrefix = classOf[MetricsFilter].getName
  }) {
    def handler = Action {
      Results.Ok("ok")
    }
    def throws = Action {
      throw new RuntimeException("")
      Results.Ok("ok")
    }

    override def onRouteRequest(request: RequestHeader): Option[Handler] = request.path match {
      case "/throws" =>
        Some(throws)
      case _ =>
        Some(handler)
    }
  }


  abstract class ApplicationWithFilter(val registry: MetricRegistry = new MetricRegistry) extends WithApplication(FakeApplication(withGlobal = Some(new MockGlobal(registry)),
    additionalPlugins = Seq("com.kenshoo.play.metrics.MetricsPlugin"),
    additionalConfiguration = Map("metrics.jvm" -> false)))

}
