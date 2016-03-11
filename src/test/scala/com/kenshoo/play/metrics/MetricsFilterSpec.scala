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

import javax.inject.Inject
import akka.stream.scaladsl.Source
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Action}
import play.api.routing.Router
import play.api.test._
import play.api.test.Helpers._
import com.codahale.metrics.MetricRegistry
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import play.api.inject.bind
import play.api.mvc.Results._

// inspired by Play's SecurityHeadersFilterSpec.scala
object MetricsFilterSpec extends Specification {
  sequential

  class Filters @Inject() (metricsFilter: MetricsFilter) extends HttpFilters {
    def filters = Seq(metricsFilter)
  }

  def withApplication[T](result: => Result)(block: Application => T): T = {
    lazy val application = new GuiceApplicationBuilder()
      .overrides(
        bind[Router].to(Router.from {
          case _ => Action(result)
        }),
        bind[HttpFilters].to[Filters],
        bind[MetricsFilter].to[MetricsFilterImpl],
        bind[Metrics].to[MetricsImpl]
      ).build()
    running(application){
      block(application)
    }
  }

  def metrics(implicit app: Application): Metrics = app.injector.instanceOf[Metrics]

  val labelPrefix = classOf[MetricsFilter].getName

  "MetricsFilter" should {
    "return passed response code" in withApplication(Ok("")) { app =>
      val result = route(app, FakeRequest()).get
      status(result) must equalTo(OK)
    }
    "increment status code counter" in withApplication(Ok("")) { implicit app =>
      Await.ready(route(app, FakeRequest()).get, Duration(2, "seconds"))
      val meter = metrics.defaultRegistry.meter(MetricRegistry.name(labelPrefix, "200"))
      meter.getCount must equalTo(1)
    }
    "increment status code counter for uncaught exceptions" in withApplication(throw new RuntimeException("")) { implicit app =>
      Await.ready(route(app, FakeRequest()).get, Duration(2, "seconds"))
      val meter = metrics.defaultRegistry.meter(MetricRegistry.name(labelPrefix, "500"))
      meter.getCount must equalTo(1)
    }
    "increment request timer" in withApplication(Ok("")) { implicit app =>
      Await.ready(route(app, FakeRequest()).get, Duration(2, "seconds"))
      val timer = metrics.defaultRegistry.timer(MetricRegistry.name(labelPrefix, "request_timer"))
      timer.getCount must equalTo(1)
    }
    "active requests counter" in withApplication(Ok("")) { implicit app =>
      Await.ready(route(app, FakeRequest()).get, Duration(2, "seconds"))
      val timer = metrics.defaultRegistry.counter(MetricRegistry.name(labelPrefix, "active_requests"))
      timer.getCount must equalTo(0)
    }
    "active requests counter" in withApplication(Ok.chunked(Source.fromIterator(() => List("1", "2", "3").toIterator)
      .delay(Duration(1, "seconds")))) { implicit app =>
      val timer = metrics.defaultRegistry.counter(MetricRegistry.name(labelPrefix, "active_requests"))
      val f = route(app, FakeRequest()).get
      timer.getCount must equalTo(1)
      Await.ready(f, Duration(3, "seconds"))
      timer.getCount must equalTo(0)
    }
  }
}