package com.kenshoo.play.metrics

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.TimeUnit

import com.codahale.metrics.{MetricRegistry, MetricFilter}
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import play.api.{Logger, Configuration}

object Reporter {

  def graphite(conf: Configuration, registry: MetricRegistry) = {
    {
      for {
        host <- conf.getString("host")
        port <- conf.getInt("port")
        unit <- conf.getString("unit")
        period <- conf.getInt("period")
        prefix <- conf.getString("prefix")
      } yield () => {
        Logger.info("Enabling graphite logging")
        val myname = InetAddress.getLocalHost.getHostName.replace('.', '_')
        val graphite = new Graphite(new InetSocketAddress(host, port))
        val reporter = GraphiteReporter.forRegistry(registry)
          .prefixedWith(s"app.$myname.$prefix")
          .convertRatesTo(TimeUnit.SECONDS)
          .convertDurationsTo(TimeUnit.MILLISECONDS)
          .filter(MetricFilter.ALL)
          .build(graphite)
        reporter.start(period, TimeUnit.valueOf(unit))
      }
    }.getOrElse(() => Unit)
  }
}
