/*
 * Copyright 2014-2023 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkoolcloud.tnt4j.examples;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

public class Log4JTest {
	private static final Logger logger = LogManager.getLogger(Log4JTest.class);

	public static void main(String[] args) {
		ThreadContext.put("app", Log4JTest.class.getName());
		logger.info("Starting a TNT4J activity #beg=Test");
		logger.warn("First log message #app=" + Log4JTest.class.getName() + " #msg='1 Test warning message'");
		logger.error("Second log message #app=" + Log4JTest.class.getName() + " #msg='2 Test error message'",
				new Exception("test exception"));
		logger.info("Ending a TNT4J activity #end= #app=" + Log4JTest.class.getName());

		logger.debug("First datagram message #app=" + Log4JTest.class.getName() + " #msg='Test datagram message'");
		logger.trace("Second datagram message #app=" + Log4JTest.class.getName() + " #msg='Test datagram message'");
		logger.trace("Whole datagram message #rcd=" + 37128 + " #rsn=" + logger.getName());
	}
}
