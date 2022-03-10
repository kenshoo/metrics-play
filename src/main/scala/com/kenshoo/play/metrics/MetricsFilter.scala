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

import akka.stream.Materializer
import io.micrometer.core.instrument.{Counter, MeterRegistry, Timer}
import play.api.http.Status
import play.api.mvc._

import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait MetricsFilter extends Filter

class DisabledMetricsFilter @Inject()(implicit val mat: Materializer) extends MetricsFilter {
  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {
    nextFilter(rh)
  }
}

class MetricsFilterImpl @Inject()(metrics: Metrics)(implicit val mat: Materializer, val ec: ExecutionContext) extends MetricsFilter {

  def registry: MeterRegistry = metrics.defaultRegistry

  /** Specify a meaningful prefix for metrics
    *
    * Defaults to classOf[MetricsFilter].getName for backward compatibility as
    * this was the original set value.
    *
    */
  def labelPrefix: String = classOf[MetricsFilter].getName

  def requestsTimer(status: Int): Timer = registry.timer(s"$labelPrefix.requestTimer","status",status.toString)

  def activeRequests: AtomicInteger = registry.gauge[AtomicInteger](s"$labelPrefix.activeRequests", new AtomicInteger(0), (count: AtomicInteger) => count.get().doubleValue())

  def apply(nextFilter: (RequestHeader) => Future[Result])(rh: RequestHeader): Future[Result] = {

    val sample = Timer.start(registry)

    def logCompleted(result: Result): Unit = {
      activeRequests.decrementAndGet()
      sample.stop(requestsTimer(result.header.status))
    }

    activeRequests.incrementAndGet()
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