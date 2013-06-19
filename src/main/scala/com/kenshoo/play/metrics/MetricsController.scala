package com.kenshoo.play.metrics

import play.api.mvc.{Action, Controller}
import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ObjectWriter, ObjectMapper}
import java.io.StringWriter
import play.api.Play.current
import play.api.Application



trait MetricsController {
  self: Controller =>

  val registry: MetricRegistry

  val app: Application

  def serialize(mapper: ObjectMapper) = {
    val writer: ObjectWriter = mapper.writerWithDefaultPrettyPrinter()
    val stringWriter = new StringWriter()
    writer.writeValue(stringWriter, registry)
    Ok(stringWriter.toString).as("application/json").withHeaders("Cache-Control" -> "must-revalidate,no-cache,no-store")
  }

  def metrics = Action {
    app.plugin[MetricsPlugin] match {
      case Some(plugin)  => plugin.enabled match {
        case true => serialize(plugin.mapper)
        case false => InternalServerError("metrics plugin not enabled")
      }
      case None => InternalServerError("metrics plugin is not found")
    }
  }

}

object MetricsController extends Controller with MetricsController {
  lazy val registry = MetricsRegistry.default
  lazy val app = current
}
