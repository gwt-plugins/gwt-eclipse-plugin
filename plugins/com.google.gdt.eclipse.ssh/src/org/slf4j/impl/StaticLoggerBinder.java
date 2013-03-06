/*******************************************************************************
 * Copyright 2013 Google Inc. All Rights Reserved.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

/**
 * This will bind our eclipse proxy logger to the slf4j system
 */
public class StaticLoggerBinder implements LoggerFactoryBinder {

  private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();

  // The SLF4J API this implementation is compiled against. Make sure to modify
  // this if the api version is updated.
  // The convention is to not set to final to allow subclasses to modify. We
  // aren't a full slf4j library, so it doesn't matter, but lets follow the
  // convention.
  public static String REQUESTED_API_VERSION = "1.6";

  private static final String LOGGER_FACTORY_CLASS_STR = GPELoggerFactory.class.getName();

  public static final StaticLoggerBinder getSingleton() {
    return SINGLETON;
  }

  private final ILoggerFactory loggerFactory;

  private StaticLoggerBinder() {
    loggerFactory = new GPELoggerFactory();
  }

  public ILoggerFactory getLoggerFactory() {
    return loggerFactory;
  }

  public String getLoggerFactoryClassStr() {
    return LOGGER_FACTORY_CLASS_STR;
  }
}