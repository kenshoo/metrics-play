package com.kenshoo.play.metrics

import com.codahale.metrics.SharedMetricRegistries
import org.specs2.specification.BeforeAfterEach


trait BeforaAfterTest extends BeforeAfterEach {
  override protected def before: Any = {}

  override protected def after: Any = SharedMetricRegistries.clear()
}
