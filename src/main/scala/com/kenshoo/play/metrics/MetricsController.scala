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

import java.io.StringWriter

import play.api.{Application, Play}
import play.api.mvc.{Action, Controller}

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ObjectWriter, ObjectMapper}

trait Metrics {
  self: Controller =>

  def registry: MetricRegistry

  def app: Application

  def serialize(mapper: ObjectMapper) = {
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, registry)
    Ok(stringWriter.toString).as("application/json").withHeaders("Cache-Control" -> "must-revalidate,no-cache,no-store")
  }

  def metrics = Action {
    app.plugin[MetricsPlugin] match {
      case Some(plugin) =>
        if (plugin.enabled)
          serialize(plugin.mapper)
        else
          InternalServerError("metrics plugin not enabled")
      case None => InternalServerError("metrics plugin is not found")
    }
  }
}

class MetricsController extends Controller with Metrics {
  def registry = MetricsRegistry.defaultRegistry
  def app = Play.current
}
