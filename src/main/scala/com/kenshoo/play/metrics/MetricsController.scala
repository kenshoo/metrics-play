package com.kenshoo.play.metrics

import java.io.StringWriter

import play.api.{Application, Play}
import play.api.mvc.{Action, Controller}

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ObjectWriter, ObjectMapper}


trait MetricsController {
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

object MetricsController extends Controller with MetricsController {
  def registry = MetricsRegistry.default
  def app = Play.current
}
