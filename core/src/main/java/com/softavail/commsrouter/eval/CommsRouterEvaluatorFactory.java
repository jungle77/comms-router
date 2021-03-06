/*
 * Copyright 2017 SoftAvail Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.softavail.commsrouter.eval;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Ergyun Syuleyman
 */
public class CommsRouterEvaluatorFactory {

  private static final Logger LOGGER = LogManager.getLogger(CommsRouterEvaluatorFactory.class);

  public CommsRouterEvaluator provide(String predicate) {
    CommsRouterEvaluator evaluator = new CommsRouterEvaluator();
    evaluator.init(predicate);
    LOGGER.debug(" *** Created evaluator and validator: {}", evaluator);

    return evaluator;
  }


}
