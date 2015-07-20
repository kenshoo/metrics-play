package com.kenshoo.play.metrics

import javax.inject.Inject

import play.api.http.HttpFilters

class TestingFilters @Inject() (metricsFilter: JavaMetricsFilter) extends HttpFilters {
  val filters = Seq(metricsFilter)
}