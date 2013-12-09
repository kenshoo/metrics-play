package com.kenshoo.play.metrics

import play.api.mvc._
import play.api.http.Status
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.codahale.metrics._
import com.codahale.metrics.MetricRegistry.name


abstract class MetricsFilter extends EssentialFilter {

  def registry: MetricRegistry

  val knownStatuses = Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND,
    Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR)

  def statusCodes: Map[Int, Meter] = knownStatuses.map (s => s -> registry.meter(name(classOf[MetricsFilter], s.toString))).toMap

  def requestsTimer:  Timer   = registry.timer(name(classOf[MetricsFilter], "requestTimer"))
  def activeRequests: Counter = registry.counter(name(classOf[MetricsFilter], "activeRequests"))
  def otherStatuses:  Meter   = registry.meter(name(classOf[MetricsFilter], "other"))

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(rh: RequestHeader) = {
      val context = requestsTimer.time()

      def logCompleted(result: SimpleResult): SimpleResult = {
        activeRequests.dec()
        context.stop()
        statusCodes.getOrElse(result.header.status, otherStatuses).mark()
        result
      }

      activeRequests.inc()
      next(rh).map(logCompleted)
    }
  }
}

object MetricsFilter extends MetricsFilter {
  def registry = MetricsRegistry.default
}
