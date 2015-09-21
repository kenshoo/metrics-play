package com.kenshoo.play.metrics

import java.util.concurrent.{ConcurrentHashMap, TimeUnit}

import play.api.Routes
import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.codahale.metrics._

import scala.concurrent.Future

/**
 * similar to MetricsFilter but
 * provide requestTime and other metrics for each route of app individually.
 * For simplicity, it does not support filtering response codes yet.
 */
trait RoutesMetricsFilter extends Filter {
  def registry: MetricRegistry

  /** Specify a meaningful prefix for metrics
    *
    * Defaults to classOf[MetricsFilter].getName for backward compatibility as
    * this was the original set value.
    *
    */
  def labelPrefix: String = classOf[MetricsFilter].getName

  lazy val mainMetrics = new RequestMetrics(registry, labelPrefix,  "All")
  lazy val sitesMetrics = new MetricsRoutes(registry, labelPrefix)

  def apply(nextFilter: (RequestHeader) => Future[Result])
           (rh: RequestHeader): Future[Result] = {

    //create metrics collections
    val site      = sitesMetrics.forHeader(rh)
    val context   = mainMetrics.requestTimer.time()

    def logCompleted(result: Result): Unit = {

      mainMetrics.activeRequests.dec()
      site.activeRequests.dec()

      val rtime = context.stop()

      //we dont need to mark rT on mainMetrics
      //context.stop() handle it for us
      site.requestTimer.update(rtime,TimeUnit.NANOSECONDS)

      mainMetrics.statusCodes(result.header.status).mark()
      site.statusCodes(result.header.status).mark()
    }

    //mark active request
    mainMetrics.activeRequests.inc()
    site.activeRequests.inc()

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

object RoutesMetricsFilter extends RoutesMetricsFilter {
  override def registry = MetricsRegistry.defaultRegistry
}

/**
 * Holds few diffrent metrics with same prefixes. 
 */
class RequestMetrics(registry:MetricRegistry, prefix:String, systemName:String) {
  def requestTimer:Timer =
    registry.timer(MetricRegistry.name(prefix, systemName, "requestTimer"))

  def activeRequests:Counter =
    registry.counter(MetricRegistry.name(prefix, systemName, "activeRequests"))

  def statusCodes(status:Int):Meter =
    registry.meter(
      MetricRegistry.name(prefix, systemName, status.toString)
    )
}

/**
 * Helps in mapping RequestHeader -> RequestMetrics. 
 */
class MetricsRoutes(val registry:MetricRegistry, val prefix:String) {
  val accurateSites   = new ConcurrentHashMap[String,RequestMetrics]()

  def forHeader(requestHeader: RequestHeader):RequestMetrics = {
    val tag: String = requestHeader.tags.getOrElse(Routes.ROUTE_PATTERN, "unkown")
    var rm = accurateSites.get(tag)
    if (rm == null) {
      rm = new RequestMetrics(registry, prefix, tag)
    }

    rm
  }
}
