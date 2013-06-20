package com.kenshoo.play.metrics

import org.specs2.mutable.Specification
import play.api.mvc._
import play.api.test._
import play.api.test.Helpers._
import com.codahale.metrics._
import play.api.test.FakeApplication
import scala.Some


class MetricsFilterSpec extends Specification {
  sequential

  "metrics filter" should {
    "return passed response code" in new ApplicationWithFilter {
      val result = route(FakeRequest("GET", "/")).get
      status(result) must equalTo(OK)
    }

    "increment status code counter" in new ApplicationWithFilter {
      route(FakeRequest("GET", "/")).get
      val meter: Meter = registry.meter(MetricRegistry.name(classOf[MetricsFilter], "200"))
      meter.getCount must equalTo(1)
    }

    "increment request timer" in new ApplicationWithFilter {
      route(FakeRequest("GET", "/")).get
      val timer = registry.timer(MetricRegistry.name(classOf[MetricsFilter], "requestTimer"))
      timer.getCount must beGreaterThan(0l)
    }
  }

  class MockGlobal(val reg: MetricRegistry) extends WithFilters(new MetricsFilter{
    val registry: MetricRegistry = reg
  }) {
    def handler = Action {
      Results.Ok("ok")
    }
    override def onRouteRequest(request: RequestHeader): Option[Handler] = {
      Some(handler)
    }
  }


  abstract class ApplicationWithFilter(val registry: MetricRegistry = new MetricRegistry) extends WithApplication(FakeApplication(withGlobal = Some(new MockGlobal(registry)),
    additionalPlugins = Seq("com.kenshoo.play.metrics.MetricsPlugin"),
    additionalConfiguration = Map("metrics.jvm" -> false)))

}
