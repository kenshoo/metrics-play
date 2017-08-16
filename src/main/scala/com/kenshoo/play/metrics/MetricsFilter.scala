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

import akka.stream.Materializer
import play.api.mvc._
import play.api.http.Status
import com.codahale.metrics._
import com.codahale.metrics.MetricRegistry.name

import scala.concurrent.{ExecutionContext, Future}

trait MetricsFilter extends Filter

class DisabledMetricsFilter @Inject()(implicit val mat: Materializer) extends MetricsFilter {
  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    nextFilter(rh)
  }
}

class MetricsFilterImpl @Inject() (metrics: Metrics)(implicit val mat: Materializer, val ec: ExecutionContext) extends MetricsFilter {

  def registry: MetricRegistry = metrics.defaultRegistry

  /** Specify a meaningful prefix for metrics
    *
    * Defaults to classOf[MetricsFilter].getName for backward compatibility as
    * this was the original set value.
    *
    */
  def labelPrefix: String = classOf[MetricsFilter].getName

  /** Specify which HTTP status codes have individual metrics
    *
    * Statuses not specified here are grouped together under otherStatuses
    *
    * Defaults to 200, 400, 401, 403, 404, 409, 201, 304, 307, 500, which is compatible
    * with prior releases.
    */
  def knownStatuses = Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND,
    Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR, Status.CONFLICT,
    Status.UNAUTHORIZED, Status.NOT_MODIFIED)


  def statusCodes: Map[Int, Meter] = knownStatuses.map(s => s -> registry.meter(name(labelPrefix, s.toString))).toMap

  def requestsTimer: Timer = registry.timer(name(labelPrefix, "requestTimer"))
  def activeRequests: Counter = registry.counter(name(labelPrefix, "activeRequests"))
  def otherStatuses: Meter = registry.meter(name(labelPrefix, "other"))

  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val context = requestsTimer.time()

    def logCompleted(result: Result): Unit = {
      activeRequests.dec()
      context.stop()
      statusCodes.getOrElse(result.header.status, otherStatuses).mark()
    }

    activeRequests.inc()
    nextFilter(rh).transform(
      result => {
        logCompleted(result)
        result
      },
      exception => {
        logCompleted(Results.InternalServerError)
        exception
      }
    )
  }
}