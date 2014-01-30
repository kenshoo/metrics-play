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
import play.api.Play
import play.api.Play.current

import com.codahale.metrics._
import com.codahale.metrics.MetricRegistry.name

import scala.collection.JavaConverters._

abstract class MetricsFilter extends EssentialFilter {

  def registry: MetricRegistry

  val DEFAULT_STATUSES =
    Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND,
      Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR)

  lazy val knownStatuses = {
    val knownStatusList = Play.configuration.getIntList("metrics.knownStatuses")
    if (knownStatusList.size != 0)
      knownStatusList.get.asScala
    else
      DEFAULT_STATUSES
  }

  lazy val statusCodes = newMeters(knownStatuses)

  lazy val statusLevelMeters: Option[Map[String, Meter]] = {
    {
      val showStatusLevelsEnabled = 
        Play.configuration.getBoolean("metrics.showHttpStatusLevels")
        .getOrElse(false)
      if (showStatusLevelsEnabled) {
        newMeters[String]((1 to 5).map(_ + "xx"))
      } else None
    }
  }

  def requestsTimer:  Timer   = registry.timer(name(classOf[MetricsFilter], "requestTimer"))
  def activeRequests: Counter = registry.counter(name(classOf[MetricsFilter], "activeRequests"))
  def otherStatuses:  Meter   = registry.meter(name(classOf[MetricsFilter], "other"))

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(rh: RequestHeader) = {
      val context = requestsTimer.time()

      def logCompleted(result: SimpleResult): SimpleResult = {
        activeRequests.dec()
        context.stop()
        val status = result.header.status
        statusCodes.map(_.getOrElse(status, otherStatuses).mark())
        statusLevelMeters.map(
          _.get(statusLevelName(status)).map(_.mark))
        result
      }

      activeRequests.inc()
      next(rh).map(logCompleted)
    }
  }

  /** The name of the status level of an HTTP status code (e.g., "2xx", "5xx") */
  private def statusLevelName(s: Int): String = {
    (s / 100) + "xx"
  }

  /** Creates individual meters for all the names in the given sequence */
  private def newMeters[B](names: Seq[B]): Option[Map[B, Meter]] = {
    Some(names.map(code => code -> newMeter(code.toString)).toMap)
  }

  /** Creates a new meter with the specified name */
  private def newMeter(meterName: String): Meter = {
    registry.meter(name(classOf[MetricsFilter], meterName))
  }
}

object MetricsFilter extends MetricsFilter {
  def registry = MetricsRegistry.default
}
