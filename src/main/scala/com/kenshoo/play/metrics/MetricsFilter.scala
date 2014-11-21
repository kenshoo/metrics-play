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
  def label: String = classOf[MetricsFilter].getName
  def healthChecks: Seq[String] = Seq.empty
  def knownStatuses = Seq(Status.OK, Status.BAD_REQUEST, Status.FORBIDDEN, Status.NOT_FOUND,
    Status.CREATED, Status.TEMPORARY_REDIRECT, Status.INTERNAL_SERVER_ERROR)

  def statusCodes: Map[Int, Meter] = knownStatuses.map(s => s -> registry.meter(name(label, s.toString))).toMap
  def healthCheckStatusCodes: Map[Int, Meter] = knownStatuses.map( s => s -> registry.meter(name("HealthCheck_" + label, s.toString))).toMap

  def requestsTimer: Timer = registry.timer(name(label, "requestTimer"))
  def activeRequests: Counter = registry.counter(name(label, "activeRequests"))
  def otherStatuses: Meter = registry.meter(name(label, "other"))

  def apply(next: EssentialAction) = new EssentialAction {
    def apply(rh: RequestHeader) = {
      val context = requestsTimer.time()

      def isHealthCheck(uri: String): Boolean = healthChecks.find(_ == uri).nonEmpty

      def logCompleted(result: Result): Result = {
        activeRequests.dec()
        context.stop()
        if (isHealthCheck(rh.uri)) {
          healthCheckStatusCodes.getOrElse(result.header.status, otherStatuses).mark()
        }
        else {
          statusCodes.getOrElse(result.header.status, otherStatuses).mark()
        }
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