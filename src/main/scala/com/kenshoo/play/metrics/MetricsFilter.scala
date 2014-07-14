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

  lazy val statusLevelMeters: Map[Int, Meter] = {
    if (MetricsFilter.showLevels) {
      val buckets = 1 to 5
      newMeters(buckets, buckets.map(x => statusLevelName(x)))
    } else Map()
  }


  def requestsTimer:  Timer   = registry.timer(name(classOf[MetricsFilter], "requestTimer"))
  def activeRequests: Counter = registry.counter(name(classOf[MetricsFilter], "activeRequests"))
  def otherStatuses:  Meter   = registry.meter(name(classOf[MetricsFilter], "other"))

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(rh: RequestHeader) = {
      val context = requestsTimer.time()

      def logCompleted(result: Result): Result = {
        activeRequests.dec()
        context.stop()
        statusCodes.getOrElse(result.header.status, otherStatuses).mark()
        statusLevelMeters.get(statusLevel(result.header.status)).map(_.mark)
        result
      }

      activeRequests.inc()
      next(rh).map(logCompleted)
    }
  }


  /** The name of the status level of an HTTP status code (e.g., "2xx", "5xx") */
  private def statusLevelName(s: Int): String = {
    s + "xx"
  }

  private def statusLevel(s: Int) = s / 100

  private def newMeters(keys: Seq[Int], names: Seq[String]): Map[Int, Meter] = {
    keys.zip(names.map(name => newMeter(name))).toMap
  }

  /** Creates a new meter with the specified name */
  private def newMeter(meterName: String): Meter = {
    registry.meter(name(classOf[MetricsFilter], meterName))
  }
}

object MetricsFilter extends MetricsFilter {
  def registry = MetricsRegistry.default

  var showLevels = false
}
