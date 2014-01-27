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
import play.Play

import com.codahale.metrics._
import com.codahale.metrics.MetricRegistry.name

import scala.collection.JavaConverters._

abstract class MetricsFilter extends EssentialFilter {

  def registry: MetricRegistry
  lazy val knownStatuses: Iterable[Int] = {
    val knownStatusPref = Play.application().configuration().getList("metrics.knownStatuses")
    if (knownStatusPref != null && !knownStatusPref.isEmpty()) {
      knownStatusPref.asScala.asInstanceOf[Seq[Int]]
    } else {
      Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND,
        Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR)
    }
  }
  val knownStatusLevels = Seq(100, 200, 300, 400, 500)

  def statusCodes: Map[Int, Meter] = knownStatuses.map (s => s -> registry.meter(name(classOf[MetricsFilter], s.toString))).toMap

  def statusLevels: Option[Map[Int, Meter]] = {
    val showStatusLevelsEnabled = Play.application().configuration().getBoolean("metrics.showHttpStatusLevels")
    if (showStatusLevelsEnabled != null && showStatusLevelsEnabled) {
      Some(knownStatusLevels.map (s => s -> registry.meter(name(classOf[MetricsFilter], statusLevelName(s)))).toMap)
    } else None
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
        statusCodes.getOrElse(status, otherStatuses).mark()
        statusLevels.map(s => s.get(statusLevel(status)).map({
          s => s.mark
        }))
        result
      }

      activeRequests.inc()
      next(rh).map(logCompleted)
    }
  }

  /** The status level of the HTTP status code (e.g., 200, 500) */
  private def statusLevel(status: Int): Int = {
    status / 100 * 100
  }

  /** The name of the status level of an HTTP status code (e.g., "2xx", "5xx") */
  private def statusLevelName(s: Int): String = {
    (s / 100).toString + "xx"
  }
}

object MetricsFilter extends MetricsFilter {
  def registry = MetricsRegistry.default
}
