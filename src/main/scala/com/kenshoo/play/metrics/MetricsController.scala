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

import javax.inject.Inject

import play.api.mvc.{Action, Controller}

class MetricsController @Inject() (met: Metrics) extends Controller {

  def metrics = Action {
    try {
      Ok(met.toJson)
        .as("application/json")
        .withHeaders("Cache-Control" -> "must-revalidate,no-cache,no-store")
    } catch {
      case ex: MetricsDisabledException =>
        InternalServerError("metrics plugin not enabled")
    }
  }
}
