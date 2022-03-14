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

import io.micrometer.core.instrument.Clock
import io.micrometer.jmx.{JmxConfig, JmxMeterRegistry}
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.api.test.Helpers._
import play.api.test._

import javax.inject.Inject
import scala.concurrent.Await
import scala.concurrent.duration.Duration

// inspired by Play's SecurityHeadersFilterSpec.scala

class Filters @Inject()(metricsFilter: MetricsFilter) extends HttpFilters {
  def filters = Seq(metricsFilter)
}

class MetricsFilterSpec extends Specification {
  sequential

  val ec = scala.concurrent.ExecutionContext.Implicits.global

  def withApplication[T](result: => Result)(block: Application => T): T = {

    lazy val application = new GuiceApplicationBuilder()
      .overrides(
        bind[Router].to(Router.from {
          case _ => DefaultActionBuilder(BodyParsers.utils.ignore(AnyContentAsEmpty: AnyContent))(ec) {
            result
          }
        }),
        bind[HttpFilters].to[Filters],
        bind[MetricsFilter].to[MetricsFilterImpl],
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

  val labelPrefix = classOf[MetricsFilter].getName

  "MetricsFilter" should {

    "return passed response code" in withApplication(Ok("")) { app =>
      val result = route(app, FakeRequest()).get
      status(result) must equalTo(OK)
    }

    "increment request timer for successful status code" in withApplication(Ok("")) { implicit app =>
      Await.ready(route(app, FakeRequest()).get, Duration(2, "seconds"))
      val meter = metrics.defaultRegistry.timer(s"$labelPrefix.requestTimer", "status", "200")
      meter.count() must equalTo(1)
    }

    "increment request timer for uncaught exceptions" in withApplication(throw new RuntimeException("")) { implicit app =>
      Await.ready(route(app, FakeRequest()).get, Duration(2, "seconds"))
      val meter = metrics.defaultRegistry.timer(s"$labelPrefix.requestTimer", "status", "500")
      meter.count() must equalTo(1)
    }
  }
}
