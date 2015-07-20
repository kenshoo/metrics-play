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

import com.codahale.metrics.{Metric, SharedMetricRegistries}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

import scala.collection.JavaConversions._
import scala.collection.mutable.Map

class MetricsPluginSpec extends Specification with Mockito with BeforaAfterTest {
  sequential

  var currentApp: Option[Application] = None

  "metrics plugin" should {

    "be enabled by default" in {
      val plugin = config()
      plugin.enabled must beTrue
    }

    "can be turned off" in {
      val plugin = config(Option(false))
      plugin.enabled must beFalse
    }

    "can be turned on" in {
      val plugin = config(Option(true))
      plugin.enabled must beTrue
    }

    "registers default metric registry" in {
      val plugin = config()
      SharedMetricRegistries.names().contains("default") must beTrue
    }

    "registers metric by name" in  {
      val plugin = config(name = Option("name"))
      SharedMetricRegistries.names().contains("name") must beTrue
    }

    "registers jvm metrics" in {
      val plugin = config()
      val metrics: Map[String, Metric] = SharedMetricRegistries.getOrCreate("default").getMetrics
      metrics must haveKey("heap.usage")
    }

    "registers logback metrics" in {
      val plugin = config(logback = Some(true))
      val metrics: Map[String, Metric] = SharedMetricRegistries.getOrCreate("default").getMetrics
      metrics must haveKey("ch.qos.logback.core.Appender.info")
    }
  }

  def config(enabled: Option[Boolean] = Option.empty,
             name: Option[String] = Option.empty,
             jvm: Option[Boolean] = Option.empty,
             logback: Option[Boolean] = Option.empty): MetricsPlugin = {

    val app = new GuiceApplicationBuilder().configure(
      "metrics.enabled" -> enabled.getOrElse(true),
      "metrics.name" -> name.getOrElse("default"),
      "metrics.jvm" -> jvm.getOrElse(true),
      "metrics.logback" -> logback.getOrElse(false),
      "metrics.showSamples" -> false
    ).build()
    val conf = app.configuration
    currentApp = Some(app)
    app.injector.instanceOf[MetricsPlugin]
  }

}
