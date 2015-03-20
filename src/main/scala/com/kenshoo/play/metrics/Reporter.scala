package com.kenshoo.play.metrics

import java.io.File
import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent.TimeUnit

import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.codahale.metrics.{ConsoleReporter, CsvReporter, MetricFilter, MetricRegistry}
import play.api.{Configuration, Logger}

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
        Logger.info("Enabling GraphiteReporter")
        val myname = InetAddress.getLocalHost.getHostName
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

  def console(conf: Configuration, registry: MetricRegistry) = {
    for {
      unit <- conf.getString("unit")
      period <- conf.getInt("period")
      prefix <- conf.getString("prefix")
    } yield () => {
      Logger.info("Enabling ConsoleReporter")

      ConsoleReporter.forRegistry(registry)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .convertRatesTo(TimeUnit.SECONDS)
        .build().start(period, TimeUnit.valueOf(unit))
    }
  }.getOrElse(() => Unit)


  def csv(conf: Configuration, registry: MetricRegistry) = {
    for {
      outputDir <- conf.getString("output")
      unit <- conf.getString("unit")
      period <- conf.getInt("period")
      prefix <- conf.getString("prefix")
    } yield () => {
      Logger.info("Enabling CsvReporter")

      CsvReporter.forRegistry(registry)
        .convertDurationsTo(TimeUnit.MILLISECONDS)
        .convertRatesTo(TimeUnit.SECONDS)
        .build(new File(outputDir)).start(period, TimeUnit.valueOf(unit))
    }
  }.getOrElse(() => Unit)

}
