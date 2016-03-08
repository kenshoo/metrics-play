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

import java.util.concurrent.Executor
import javax.inject.Inject
import akka.util.ByteString
import play.api.Configuration
import play.api.libs.streams.Accumulator
import play.api.mvc._
import play.api.http.Status
import com.codahale.metrics.MetricRegistry.name
import scala.concurrent.ExecutionContext

trait MetricsFilter extends EssentialFilter

class DisabledMetricsFilter @Inject() extends MetricsFilter {
  def apply(nextFilter: EssentialAction) = new EssentialAction {
    override def apply(rh: RequestHeader): Accumulator[ByteString, Result] = {
      nextFilter(rh)
    }
  }
}

class MetricsFilterImpl @Inject() (metrics: Metrics, configuration: Configuration) extends MetricsFilter {
  val registry = metrics.defaultRegistry

  /** Specify a meaningful prefix for metrics
    *
    * Defaults to classOf[MetricsFilter].getName for backward compatibility as
    * this was the original set value.
    *
    */
  val labelPrefix = configuration.getString("metrics.naming.http").getOrElse(classOf[MetricsFilter].getName)

  /** Specify which HTTP status codes have individual metrics
    *
    * Statuses not specified here are grouped together under otherStatuses
    *
    * Defaults to 200, 400, 401, 403, 404, 409, 201, 304, 307, 500, which is compatible
    * with prior releases.
    */
  val knownStatuses = Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND,
    Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR, Status.CONFLICT,
    Status.UNAUTHORIZED, Status.NOT_MODIFIED)
  val statusCodes = knownStatuses.map(s => s -> registry.meter(name(labelPrefix, s.toString))).toMap
  val requestsTimer = registry.timer(name(labelPrefix, "request_timer"))
  val activeRequests = registry.counter(name(labelPrefix, "active_requests"))
  val otherStatuses = registry.meter(name(labelPrefix, "other"))
  val onSameThreadExecutionContext = ExecutionContext.fromExecutor(new Executor {
    override def execute(command: Runnable): Unit = command.run()
  })

  def apply(nextFilter: EssentialAction) = new EssentialAction {
    override def apply(rh: RequestHeader): Accumulator[ByteString, Result] = {
      val context = requestsTimer.time()

      def logCompleted(result: Result): Unit = {
        activeRequests.dec()
        statusCodes.getOrElse(result.header.status, otherStatuses).mark()
        context.stop()
      }

      activeRequests.inc()
      nextFilter(rh).map {
        result =>
          logCompleted(result)
          result
      }(onSameThreadExecutionContext).recover {
        case ex: Throwable =>
          logCompleted(Results.InternalServerError)
          throw ex
      }(onSameThreadExecutionContext)
    }
  }
}