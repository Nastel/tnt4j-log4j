/*
 * Copyright 2014-2022 JKOOL, LLC.
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

package com.jkoolcloud.tnt4j.logger.log4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.message.Message;

import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.config.ConfigFactory;
import com.jkoolcloud.tnt4j.config.DefaultConfigFactory;
import com.jkoolcloud.tnt4j.config.TrackerConfig;
import com.jkoolcloud.tnt4j.core.*;
import com.jkoolcloud.tnt4j.logger.AppenderConstants;
import com.jkoolcloud.tnt4j.logger.AppenderTools;
import com.jkoolcloud.tnt4j.source.SourceType;
import com.jkoolcloud.tnt4j.tracker.TimeTracker;
import com.jkoolcloud.tnt4j.tracker.TrackingActivity;
import com.jkoolcloud.tnt4j.tracker.TrackingEvent;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * Manages Log4j logged messages to be reported over TNT4J API.
 * 
 * @version $Revision: 1 $
 * 
 * @see com.jkoolcloud.tnt4j.logger.log4j.TNT4JAppender
 */
public class TNT4JManager extends AbstractManager implements AppenderConstants {
	private final Configuration configuration;

	private TrackingLogger logger;

	private String sourceName;
	private SourceType sourceType;
	private String snapCategory;

	private int maxActivitySize;
	private boolean metricsOnException;
	private long metricsFrequency;

	private ConfigFactory cFactory = DefaultConfigFactory.getInstance();
	private Map<String, Properties> cProperties = null;

	private long lastSnapshot = 0;

	/**
	 * Constructs a TNT4J manager instance.
	 * 
	 * @param configuration
	 *            this manager bound appender configuration
	 * @param loggerContext
	 *            the logger context
	 * @param name
	 *            this manager bound appender name
	 * @param sourceName
	 *            tracker source name
	 * @param sourceType
	 *            tracker source type
	 * @param snapCategory
	 *            snapshot category name
	 * @param maxActivitySize
	 *            maximum activity linked items size
	 * @param metricsOnException
	 *            flag indicating whether to report JVM metrics when exception is logged
	 * @param metricsFrequency
	 *            JVM metrics reporting frequency, in seconds
	 */
	protected TNT4JManager(Configuration configuration, LoggerContext loggerContext, String name, String sourceName,
			SourceType sourceType, String snapCategory, int maxActivitySize, boolean metricsOnException,
			long metricsFrequency) {
		super(loggerContext, name);

		this.configuration = Objects.requireNonNull(configuration);

		this.sourceName = sourceName;
		this.sourceType = sourceType;
		this.snapCategory = snapCategory;

		this.maxActivitySize = maxActivitySize;
		this.metricsOnException = metricsOnException;
		this.metricsFrequency = metricsFrequency;
	}

	/**
	 * Returns this manager bound appender configuration.
	 * 
	 * @return appender configuration instance
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	/**
	 * Make the Manager available for use.
	 */
	public void startup() {
		try {
			if (sourceName == null) {
				setSourceName(getName());
			}
			TrackerConfig config = ((cProperties == null) ? cFactory.getConfig(sourceName, sourceType)
					: cFactory.getConfig(sourceName, sourceType, cProperties));
			logger = TrackingLogger.getInstance(config.build());
			logger.open();
		} catch (Throwable e) {
			LOGGER.error(
					"Unable to create tracker instance=" + getName() + ", config.factory=" + cFactory + ", source.name="
							+ sourceName + ", source.type=" + sourceType + ", snapshot.category=" + snapCategory,
					e);
		}
	}

	@Override
	public boolean releaseSub(long timeout, TimeUnit timeUnit) {
		if (logger != null) {
			logger.close();
		}

		return true;
	}

	/**
	 * Report single log event.
	 * 
	 * @param event
	 *            log event to report
	 */
	public void tnt(LogEvent event) {
		long lastReport = System.currentTimeMillis();

		Message msg = event.getMessage();
		String eventMsg = msg == null ? "" : msg.getFormattedMessage();
		Throwable ex = msg == null ? null : msg.getThrowable();

		HashMap<String, String> attrs = new HashMap<>();
		AppenderTools.parseEventMessage(attrs, eventMsg, '#');

		boolean activityMessage = AppenderTools.isActivityInstruction(attrs);
		if (activityMessage) {
			AppenderTools.processActivityAttrs(logger, getName(), attrs, getOpLevel(event), ex);
		} else {
			TrackingActivity activity = logger.getCurrentActivity();
			TrackingEvent tev = processEventMessage(attrs, activity, event, eventMsg, ex);

			boolean reportMetrics = activity.isNoop() && ((ex != null && metricsOnException)
					|| ((lastReport - lastSnapshot) > (metricsFrequency * 1000)));

			if (reportMetrics) {
				// report a single tracking event as part of an activity
				activity = logger.newActivity(tev.getSeverity(), event.getThreadName());
				activity.start();
				activity.setResource(event.getLoggerName());
				activity.setSource(tev.getSource()); // use event's source name for this activity
				activity.setException(ex);
				activity.setStatus(ex != null ? ActivityStatus.EXCEPTION : ActivityStatus.END);
				activity.tnt(tev);
				activity.stop();
				logger.tnt(activity);
				lastSnapshot = lastReport;
			} else if (activity.isNoop()) {
				// report a single tracking event as datagram
				logger.tnt(tev);
			} else {
				activity.tnt(tev);
			}
			if (activity.getIdCount() >= maxActivitySize) {
				activity.setException(ex);
				activity.setStatus(ex != null ? ActivityStatus.EXCEPTION : ActivityStatus.END);
				activity.stop();
				logger.tnt(activity);
			}
		}
	}

	/**
	 * Obtain elapsed nanoseconds since last log4j event
	 *
	 * @return elapsed nanoseconds since last log4j event
	 */
	protected long getUsecsSinceLastEvent() {
		return TimeUnit.NANOSECONDS.toMicros(TimeTracker.hitAndGet());
	}

	/**
	 * Process a given log4j event into a TNT4J event object {@link com.jkoolcloud.tnt4j.tracker.TrackingEvent}.
	 *
	 * @param attrs
	 *            a set of name/value pairs
	 * @param activity
	 *            tnt4j activity associated with current message
	 * @param jev
	 *            log4j logging event object
	 * @param eventMsg
	 *            string message associated with this event
	 * @param ex
	 *            exception associated with this event
	 *
	 * @return tnt4j tracking event object
	 */
	private TrackingEvent processEventMessage(Map<String, String> attrs, TrackingActivity activity, LogEvent jev,
			String eventMsg, Throwable ex) {
		int rcode = 0;
		long elapsedTimeUsec = getUsecsSinceLastEvent();
		long evTime = jev.getTimeMillis() * 1000; // convert to usec
		long startTime = 0, endTime = 0;
		Snapshot snapshot = null;

		OpCompCode ccode = getOpCompCode(jev);
		OpLevel level = getOpLevel(jev);

		StackTraceElement location = jev.getSource();
		TrackingEvent event = logger.newEvent(location.getMethodName(), eventMsg);
		event.getOperation().setSeverity(level);
		event.setTag(jev.getThreadName());
		event.getOperation().setResource(jev.getLoggerName());
		event.setLocation(location.getFileName() + ":" + location.getLineNumber());
		event.setSource(logger.getConfiguration().getSourceFactory().newSource(jev.getLoggerName()));

		for (Map.Entry<String, String> entry : attrs.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if (key.equalsIgnoreCase(PARAM_CORRELATOR_LABEL)) {
				event.setCorrelator(value);
			} else if (key.equalsIgnoreCase(PARAM_TAG_LABEL)) {
				event.setTag(value);
			} else if (key.equalsIgnoreCase(PARAM_LOCATION_LABEL)) {
				event.setLocation(value);
			} else if (key.equalsIgnoreCase(PARAM_RESOURCE_LABEL)) {
				event.getOperation().setResource(value);
			} else if (key.equalsIgnoreCase(PARAM_USER_LABEL)) {
				event.getOperation().setUser(value);
			} else if (key.equalsIgnoreCase(PARAM_ELAPSED_TIME_LABEL)) {
				elapsedTimeUsec = Long.parseLong(value);
			} else if (key.equalsIgnoreCase(PARAM_AGE_TIME_LABEL)) {
				event.setMessageAge(Long.parseLong(value));
			} else if (key.equalsIgnoreCase(PARAM_START_TIME_LABEL)) {
				startTime = Long.parseLong(value);
			} else if (key.equalsIgnoreCase(PARAM_END_TIME_LABEL)) {
				endTime = Long.parseLong(value);
			} else if (key.equalsIgnoreCase(PARAM_REASON_CODE_LABEL)) {
				rcode = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase(PARAM_COMP_CODE_LABEL)) {
				ccode = OpCompCode.valueOf(value);
			} else if (key.equalsIgnoreCase(PARAM_SEVERITY_LABEL)) {
				event.getOperation().setSeverity(OpLevel.valueOf(value));
			} else if (key.equalsIgnoreCase(PARAM_OP_TYPE_LABEL)) {
				event.getOperation().setType(OpType.valueOf(value));
			} else if (key.equalsIgnoreCase(PARAM_OP_NAME_LABEL)) {
				event.getOperation().setName(value);
			} else if (key.equalsIgnoreCase(PARAM_EXCEPTION_LABEL)) {
				event.getOperation().setException(value);
			} else if (key.equalsIgnoreCase(PARAM_MSG_DATA_LABEL)) {
				event.setMessage(value);
			} else if (key.equalsIgnoreCase(PARAM_APPL_LABEL)) {
				event.setSource(logger.getConfiguration().getSourceFactory().newSource(value));
			} else if (!Utils.isEmpty(key) && !Utils.isEmpty(value)) {
				// add unknown attribute into snapshot
				if (snapshot == null) {
					snapshot = logger.newSnapshot(snapCategory, event.getOperation().getName());
					event.getOperation().addSnapshot(snapshot);
				}
				snapshot.add(AppenderTools.toProperty(key, value));
			}
		}
		startTime = startTime <= 0 ? (evTime - elapsedTimeUsec) : evTime;
		endTime = endTime <= 0 ? (startTime + elapsedTimeUsec) : endTime;

		event.start(startTime);
		event.stop(ccode, rcode, ex, endTime);
		return event;
	}

	/**
	 * Map log4j logging event level to TNT4J {@link com.jkoolcloud.tnt4j.core.OpLevel}.
	 *
	 * @param event
	 *            log4j logging event object
	 * @return TNT4J {@link com.jkoolcloud.tnt4j.core.OpLevel}.
	 */
	private OpLevel getOpLevel(LogEvent event) {
		Level lvl = event.getLevel();
		if (lvl == Level.INFO) {
			return OpLevel.INFO;
		} else if (lvl == Level.FATAL) {
			return OpLevel.FATAL;
		} else if (lvl == Level.ERROR) {
			return OpLevel.ERROR;
		} else if (lvl == Level.WARN) {
			return OpLevel.WARNING;
		} else if (lvl == Level.DEBUG) {
			return OpLevel.DEBUG;
		} else if (lvl == Level.TRACE) {
			return OpLevel.TRACE;
		} else if (lvl == Level.OFF) {
			return OpLevel.NONE;
		} else {
			return OpLevel.INFO;
		}
	}

	/**
	 * Map log4j logging event level to TNT4J {@link com.jkoolcloud.tnt4j.core.OpCompCode}.
	 *
	 * @param event
	 *            log4j logging event object
	 * @return TNT4J {@link com.jkoolcloud.tnt4j.core.OpCompCode}.
	 */
	private OpCompCode getOpCompCode(LogEvent event) {
		Level lvl = event.getLevel();
		if (lvl == Level.INFO) {
			return OpCompCode.SUCCESS;
		} else if (lvl == Level.FATAL) {
			return OpCompCode.ERROR;
		} else if (lvl == Level.ERROR) {
			return OpCompCode.ERROR;
		} else if (lvl == Level.WARN) {
			return OpCompCode.WARNING;
		} else if (lvl == Level.DEBUG) {
			return OpCompCode.SUCCESS;
		} else if (lvl == Level.TRACE) {
			return OpCompCode.SUCCESS;
		} else if (lvl == Level.OFF) {
			return OpCompCode.SUCCESS;
		} else {
			return OpCompCode.SUCCESS;
		}
	}

	/**
	 * Associate a logger configuration factory with this appender
	 *
	 * @param cf
	 *            logger configuration factory instances
	 * @see com.jkoolcloud.tnt4j.config.ConfigFactory
	 */
	public void setConfigFactory(ConfigFactory cf) {
		cFactory = cf;
	}

	/**
	 * Assign a set of TNT4J streaming properties. These properties are used to configure underlying TNT4J
	 * {@code TrackingLogger}
	 *
	 * @param cProps
	 *            user defined TNT4J property map
	 */
	public void setConfigProperties(Map<String, Properties> cProps) {
		cProperties = cProps;
	}

	/**
	 * Obtain snapshot category associated with this appender. This name is used for reporting user defined metrics
	 *
	 * @return snapshot category name string that maps to tnt4j snapshot category
	 */
	public String getSnapshotCategory() {
		return snapCategory;
	}

	/**
	 * Set snapshot category associated with this appender. This name is used for reporting user defined metrics
	 *
	 * @param name
	 *            snapshot category name
	 */
	public void setSnapshotCategory(String name) {
		snapCategory = name;
	}

	/**
	 * Obtain source name associated with this appender. This name is used tnt4j source for loading tnt4j configuration.
	 *
	 * @return source name string that maps to tnt4j configuration
	 */
	public String getSourceName() {
		return sourceName;
	}

	/**
	 * Set source name associated with this appender. This name is used tnt4j source for loading tnt4j configuration.
	 *
	 * @param name
	 *            source name
	 */
	public void setSourceName(String name) {
		sourceName = name;
	}

	/**
	 * Obtain source type associated with this appender see {@code SourceType}
	 *
	 * @return source type string representation
	 * @see SourceType
	 */
	public String getSourceType() {
		return sourceType.toString();
	}

	/**
	 * Assign default source type string see {@code SourceType}
	 *
	 * @param type
	 *            source type string representation, see {@code SourceType}
	 * @see SourceType
	 */
	public void setSourceType(String type) {
		sourceType = SourceType.valueOf(type);
	}

	/**
	 * Assign default source type, see {@code SourceType}
	 *
	 * @param type
	 *            source type, see {@code SourceType}
	 * @see SourceType
	 */
	public void setSourceType(SourceType type) {
		sourceType = type;
	}

	/**
	 * Obtain maximum size of any activity
	 *
	 * @return source name string that maps to tnt4j configuration
	 */
	public int getMaxActivitySize() {
		return maxActivitySize;
	}

	/**
	 * Set maximum size of any activity
	 *
	 * @param size
	 *            maximum size must be greater than 0
	 */
	public void setMaxActivitySize(int size) {
		maxActivitySize = size;
	}

	/**
	 * Return whether appender generates metrics log entries with exception
	 *
	 * @return true to publish default jvm metrics when exception is logged
	 */
	public boolean getMetricsOnException() {
		return metricsOnException;
	}

	/**
	 * Direct appender to generate metrics log entries with exception when set to true, false otherwise.
	 *
	 * @param flag
	 *            true to append metrics on exception, false otherwise
	 */
	public void setMetricsOnException(boolean flag) {
		metricsOnException = flag;
	}

	/**
	 * Appender generates metrics based on a given frequency in seconds.
	 *
	 * @return metrics frequency, in seconds
	 */
	public long getMetricsFrequency() {
		return metricsFrequency;
	}

	/**
	 * Set metric collection frequency seconds.
	 *
	 * @param freq
	 *            number of seconds
	 */
	public void setMetricsFrequency(long freq) {
		metricsFrequency = freq;
	}
}
