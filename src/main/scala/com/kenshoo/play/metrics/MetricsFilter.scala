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
import play.api.Logger
import play.Play
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
  val knownStatusGroups = Seq(100, 200, 300, 400, 500)

  def statusCodes: Map[Int, Meter] = knownStatuses.map (s => s -> registry.meter(name(classOf[MetricsFilter], s.toString))).toMap

  def statusGroups: Option[Map[Int, Meter]] = {
    val showHttpGroupsEnabled = Play.application().configuration().getBoolean("metrics.showHttpGroups")
    if (showHttpGroupsEnabled != null && showHttpGroupsEnabled) {
      Some(knownStatusGroups.map (s => s -> registry.meter(name(classOf[MetricsFilter], statusGroupName(s)))).toMap)
    } else None
  }

  def 	statusesAndPathMeters: Option[collection.mutable.Map[String, Meter]] = {
    val showStatusAndPathsEnabled = Play.application().configuration().getBoolean("metrics.showStatusAndPath")
    if (showStatusAndPathsEnabled != null && showStatusAndPathsEnabled) {
      Some(collection.mutable.Map[String, Meter]())
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
        statusGroups.map(s => s.get(statusGroup(status)).map({
          s => s.mark
        }))
        statusesAndPathMeters.map(m => {
          val statusAndPathString = statusAndPath(status, rh.path)
          m.getOrElse(statusAndPathString, makeNewMeter(m, statusAndPathString)).mark
        })
        result
      }

      activeRequests.inc()
      next(rh).map(logCompleted)
    }
  }

  /** The "group" of the HTTP status code (e.g., 200, 500) */
  private def statusGroup(status: Int): Int = {
    status / 100 * 100
  }

  /** The "group" of an HTTP code (e.g., "2xx", "5xx") */
  private def statusGroupName(s: Int): String = {
    (s / 100).toString + "xx"
  }

  /** A string suitable for passing to the name() function for
   *  the given status and path
   */
  private def statusAndPath(status: Int, path: String): String = {
    path + "." + status.toString
  }

  /** Makes a new meter with the given path, and adds it to the given map */
  private def makeNewMeter(m: collection.mutable.Map[String, Meter], path: String): Meter = {
    val newMeter = registry.meter(name(classOf[MetricsFilter], path))
    m.put(path, newMeter)
    newMeter
  }

}

object MetricsFilter extends MetricsFilter {
  def registry = MetricsRegistry.default
}
