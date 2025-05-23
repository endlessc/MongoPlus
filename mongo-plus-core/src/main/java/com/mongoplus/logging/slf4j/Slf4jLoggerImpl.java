/*
 *    Copyright 2009-2022 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.mongoplus.logging.slf4j;

import com.mongoplus.logging.Log;
import org.slf4j.Logger;

/**
 * @author Eduardo Macarron
 */
class Slf4jLoggerImpl implements Log {

  private final Logger log;

  public Slf4jLoggerImpl(Logger logger) {
    log = logger;
  }

  @Override
  public boolean isDebugEnabled() {
    return log.isDebugEnabled();
  }

  @Override
  public boolean isTraceEnabled() {
    return log.isTraceEnabled();
  }

  @Override
  public void info(String s) {
    log.info(s);
  }

  @Override
  public void error(String s, Throwable e) {
    log.error(s, e);
  }

  @Override
  public void error(String s, Object arg) {
    log.error(s,arg);
  }

  @Override
  public void error(String s, Object arg1, Object arg2) {
    log.error(s,arg1,arg2);
  }

  @Override
  public void error(String s) {
    log.error(s);
  }

  @Override
  public void debug(String s) {
    log.debug(s);
  }

  @Override
  public void debug(String format, Object arg) {
    log.debug(format,arg);
  }

  @Override
  public void debug(String format, Object arg1, Object arg2) {
    log.debug(format,arg1,arg2);
  }

  @Override
  public void trace(String s) {
    log.trace(s);
  }

  @Override
  public void warn(String s) {
    log.warn(s);
  }

  @Override
  public void warn(String s, Object arg) {
    log.warn(s,arg);
  }

  @Override
  public void warn(String s, Object arg1, Object arg2) {
    log.warn(s,arg1,arg2);
  }

}
