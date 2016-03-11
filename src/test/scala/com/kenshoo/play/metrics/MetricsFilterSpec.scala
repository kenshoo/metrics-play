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
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.util.ByteString
import org.specs2.matcher.Matchers
import org.specs2.mutable.Specification
import play.api.Application
import play.api.http.HttpFilters
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{Result, Action}
import play.api.routing.Router
import play.api.test._
import play.api.test.Helpers._
import com.codahale.metrics.MetricRegistry
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import play.api.inject.bind
import play.api.mvc.Results._

// inspired by Play's SecurityHeadersFilterSpec.scala
object MetricsFilterSpec extends Specification with Matchers {
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

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def source = Source(List("1", "2", "3")).delay(Duration(1, "seconds"), OverflowStrategy.backpressure)

  def metrics(implicit app: Application): Metrics = app.injector.instanceOf[Metrics]

  val labelPrefix = classOf[MetricsFilter].getName

  "MetricsFilter" should {
    "return passed response code" in withApplication(Ok("")) { app =>
      val result = route(app, FakeRequest()).get
      status(result) must equalTo(OK)
    }
    "increment status code counter" in withApplication(Ok("")) { implicit app =>
      val meter = metrics.defaultRegistry.meter(MetricRegistry.name(labelPrefix, "200"))
      status(route(app, FakeRequest()).get) must equalTo(OK)
      meter.getCount must equalTo(1)
    }
    "increment status code counter for uncaught exceptions" in withApplication(throw new RuntimeException("test")) { implicit app =>
      val meter = metrics.defaultRegistry.meter(MetricRegistry.name(labelPrefix, "500"))
      status(route(app, FakeRequest()).get) must throwAn("test")
      meter.getCount must equalTo(1)
    }
    "increment request timer" in withApplication(Ok("")) { implicit app =>
      val timer = metrics.defaultRegistry.timer(MetricRegistry.name(labelPrefix, "request_timer"))
      status(route(app, FakeRequest()).get) must equalTo(OK)
      timer.getCount must equalTo(1)
    }
    "active requests counter (chunked)" in withApplication(Ok.chunked(source)) { implicit app =>
      val counter = metrics.defaultRegistry.counter(MetricRegistry.name(labelPrefix, "active_requests"))
      val result = route(app, FakeRequest()).get
      counter.getCount must equalTo(1)
      val result2 = Await.result(result, Duration(1, "seconds"))
      var counts = List[Long]()
      Await.result(result2.body.dataStream.runFold(ByteString.empty) {(x, y) =>
        counts = counts :+ counter.getCount
        x ++ y
      }, Duration(5, "seconds")).decodeString("utf-8") must equalTo("123")
      counts must equalTo(List(1, 1, 0))
      counter.getCount must equalTo(0)
    }
    "active requests counter (streamed)" in withApplication(Ok.sendResource("LICENSE-2.0.txt")) { implicit app =>
      val counter = metrics.defaultRegistry.counter(MetricRegistry.name(labelPrefix, "active_requests"))
      val result = route(app, FakeRequest()).get
      counter.getCount must equalTo(1)
      val result2 = Await.result(result, Duration(1, "seconds"))
      var counts = List[Long]()
      Await.result(result2.body.dataStream.runFold(ByteString.empty) {(x, y) =>
        counts = counts :+ counter.getCount
        x ++ y
      }, Duration(5, "seconds")).decodeString("utf-8").length must equalTo(11359)
      counts must equalTo(List(1, 1))
      counter.getCount must equalTo(0)
    }
  }
}