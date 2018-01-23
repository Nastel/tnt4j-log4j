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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import com.jkoolcloud.tnt4j.core.ActivityStatus;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.core.Snapshot;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.AbstractEventSink;
import com.jkoolcloud.tnt4j.source.Source;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * <p>
 * {@link com.jkoolcloud.tnt4j.sink.EventSink} implementation that routes log messages to log4j. This implementation is
 * designed to log messages to log4j framework.
 * </p>
 *
 *
 * @see TrackingEvent
 * @see EventFormatter
 * @see OpLevel
 *
 * @version $Revision: 11 $
 *
 */
public class Log4JEventSink extends AbstractEventSink {
	private static final String[] log4JSevMap = { "INFO", "TRACE", "DEBUG", "INFO", "WARN", "WARN", "ERROR", "FATAL",
			"FATAL", "FATAL", "FATAL" };

	private static final String[] log4JStatusMap = { "INFO", "INFO", "INFO", "ERROR" };

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
	 *
	 */
	public Log4JEventSink(String name, Properties props, EventFormatter frmt) {
		super(name, frmt);
		open();
	}

	@Override
	protected void _log(TrackingEvent event) {
		logger.log(getL4JLevel(event), getEventFormatter().format(event), event.getOperation().getThrowable());
	}

	@Override
	protected void _log(TrackingActivity activity) {
		Priority level = getL4JLevel(activity.getSeverity());
		Throwable ex = activity.getThrowable();
		logger.log(level, getEventFormatter().format(activity), ex);
	}

	@Override
	protected void _log(Snapshot snapshot) {
		logger.log(getL4JLevel(snapshot.getSeverity()), getEventFormatter().format(snapshot));
	}

	@Override
	protected void _log(long ttl, Source src, OpLevel sev, String msg, Object... args) {
		logger.log(getL4JLevel(sev), getEventFormatter().format(ttl, src, sev, msg, args), Utils.getThrowable(args));
	}

	@Override
	protected void _write(Object msg, Object... args) throws IOException {
		logger.info(getEventFormatter().format(msg, args));
	}

	/**
	 * Maps {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent} severity to log4j Level.
	 *
	 * @param ev
	 *            application tracking event
	 * @return log4j level
	 * @see OpType
	 */
	public Level getL4JLevel(TrackingEvent ev) {
		return getL4JLevel(ev.getSeverity());
	}

	/**
	 * Maps {@link com.jkoolcloud.tnt4j.core.ActivityStatus} severity to log4j Level.
	 *
	 * @param status
	 *            application activity status
	 * @return log4j level
	 * @see ActivityStatus
	 */
	public Level getL4JLevel(ActivityStatus status) {
		return Level.toLevel(log4JStatusMap[status.ordinal()], Level.INFO);
	}

	/**
	 * Maps {@link com.jkoolcloud.tnt4j.core.OpLevel} severity to log4j Level.
	 *
	 * @param sev
	 *            severity level
	 * @return log4j level
	 * @see OpType
	 */
	public Level getL4JLevel(OpLevel sev) {
		return Level.toLevel(log4JSevMap[sev.ordinal()], Level.INFO);
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
	public synchronized void open() {
		if (logger == null) {
			logger = Logger.getLogger(getName());
		}
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public boolean isSet(OpLevel sev) {
		return logger.isEnabledFor(getL4JLevel(sev));
	}
}
