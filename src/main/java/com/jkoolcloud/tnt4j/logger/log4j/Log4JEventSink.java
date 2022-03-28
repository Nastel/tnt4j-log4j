/*
 * Copyright 2014-2018 JKOOL, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jkoolcloud.tnt4j.logger.log4j;

import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.impl.LoggerEventSink;

/**
 * <p>
 * {@link com.jkoolcloud.tnt4j.sink.EventSink} implementation that routes log messages to log4j. This implementation is
 * designed to log messages to log4j framework.
 * </p>
 *
 * @version $Revision: 12 $
 *
 * @see com.jkoolcloud.tnt4j.format.EventFormatter
 * @see com.jkoolcloud.tnt4j.core.OpLevel
 * @see com.jkoolcloud.tnt4j.logger.log4j.Log4JEventSinkFactory
 */
public class Log4JEventSink extends LoggerEventSink {
	private static final String[] log4JSevMap = { "INFO", "TRACE", "DEBUG", "INFO", "WARN", "WARN", "ERROR", "FATAL",
			"FATAL", "FATAL", "FATAL" };

	private Logger logger = null;

	/**
	 * Create a new log4j backed event sink
	 *
	 * @param name
	 *            log4j event category/application name
	 * @param props
	 *            java properties used by the event sink
	 * @param frmt
	 *            event formatter used to format event entries
	 */
	public Log4JEventSink(String name, Properties props, EventFormatter frmt) {
		super(name, frmt);
		_open();
	}

	@Override
	public boolean isSet(OpLevel sev) {
		_checkState();

		return logger.isEnabled(getLevel(sev));
	}

	@Override
	public Object getSinkHandle() {
		return logger;
	}

	@Override
	public boolean isOpen() {
		return logger != null;
	}

	@Override
	protected synchronized void _open() {
		if (logger == null) {
			logger = LogManager.getLogger(getName());
		}
	}

	@Override
	protected void _close() throws IOException {
	}

	@Override
	protected void writeLine(OpLevel sev, LogEntry entry, Throwable t) {
		if (!isSet(sev)) {
			return;
		}

		Level level = getLevel(sev);
		if (!logger.isEnabled(level)) {
			return;
		}

		String msg = entry.getString();
		incrementBytesSent(msg.length());
		logger.log(level, msg, t);
	}

	/**
	 * Maps {@link com.jkoolcloud.tnt4j.core.OpLevel} severity to log4j Level.
	 *
	 * @param sev
	 *            severity level
	 * @return log4j level
	 * @see OpType
	 */
	public Level getLevel(OpLevel sev) {
		return Level.toLevel(log4JSevMap[sev.ordinal()], Level.INFO);
	}
}
