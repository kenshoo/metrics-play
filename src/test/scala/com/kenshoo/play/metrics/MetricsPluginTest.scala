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

import org.specs2.mutable.Specification
import org.specs2.mock.Mockito
import play.api.{Configuration, Application}
import com.codahale.metrics.{Metric, SharedMetricRegistries}
import org.specs2.specification.BeforeAfterExample
import scala.collection.JavaConversions._
import scala.collection.mutable.Map


class MetricsPluginSpec extends Specification with Mockito with BeforeAfterExample{
  sequential
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
      plugin.onStart()
      SharedMetricRegistries.names().contains("default") must beTrue
    }

    "registers metric by name" in  {
      val plugin = config(name = Option("name"))
      plugin.onStart()
      SharedMetricRegistries.names().contains("name") must beTrue
    }

    "registers jvm metrics" in {
      val plugin = config()
      plugin.onStart()
      val metrics: Map[String, Metric] = SharedMetricRegistries.getOrCreate("default").getMetrics
      metrics must haveKey("heap.usage")
    }
  }

  def config(enabled: Option[Boolean] = Option.empty,
             name: Option[String] = Option.empty,
             jvm: Option[Boolean] = Option.empty): MetricsPlugin = {
    val app = mock[Application]
    val config = mock[Configuration]
    app.configuration returns config
    config.getString(anyString, any[Option[Set[String]]]) returns Option.empty
    config.getBoolean("metrics.enabled") returns enabled
    config.getString("metrics.name") returns name
    config.getBoolean("metrics.jvm") returns jvm
    config.getBoolean("metrics.showSamples") returns Option.empty
    new MetricsPlugin(app)
  }

  def after {
    SharedMetricRegistries.clear()
  }

  protected def before: Any = {}
}
