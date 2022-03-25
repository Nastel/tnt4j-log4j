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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

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
 * <p>
 * Log4j appender for sending log4j events to TNT4j logging framework.
 * </p>
 *
 * <p>
 * This appender will extract information from the log4j {@code LoggingEvent} and construct the appropriate message for
 * sending to TNT4j.
 * </p>
 *
 * <p>
 * This appender has the following behavior:
 * </p>
 * <ul>
 *
 * <li>This appender does not require a layout.</li>
 * <li>TNT4J hash tags can be passed using log4j messages (using {@code #tag=value} convention) as well as
 * {@code MDC}.</li>
 * <li>All messages logged to this appender will be sent to all defined sinks as configured by tnt4j configuration.</li>
 *
 * </ul>
 *
 * <p>
 * This appender supports the following properties:
 * </p>
 * <table summary="" cellspacing=10>
 * <tr>
 * <td valign=top><b>SourceName</b></td>
 * <td valign=top>source name associated with the appender matching tnt4j configuration</td>
 * </tr>
 * <tr>
 * <td valign=top><b>SourceType</b></td>
 * <td valign=top>source type as defined by {@code SourceType}</td>
 * </tr>
 * <tr>
 * <td valign=top><b>SnapshotCategory</b></td>
 * <td valign=top>snapshot category name (default is Log4J)</td>
 * </tr>
 * <tr>
 * <td valign=top><b>MetricsOnException</b></td>
 * <td valign=top>report jvm metrics on exception (true|false)</td>
 * </tr>
 * <tr>
 * <td valign=top><b>MetricsFrequency</b></td>
 * <td valign=top>report jvm metrics on every specified number of seconds (only on logging activity)</td>
 * </tr>
 * <tr>
 * <td valign=top><b>MaxActivitySize</b></td>
 * <td valign=top>maximum size of any given activity before it gets flushed (default: 100)</td>
 * </tr>
 * </table>
 *
 * <p>
 * This appender by default sets the following TNT4j Activity and Event parameters based on the information in the log4j
 * event, as follows:
 * </p>
 * <table summary="" cellspacing=10>
 * <tr>
 * <td valign=top><b>TNT4j Parameter</b></td>
 * <td valign=top><b>Log4j Event field</b></td>
 * </tr>
 * <tr>
 * <td valign=top>Tag</td>
 * <td valign=top>Thread name</td>
 * </tr>
 * <tr>
 * <td valign=top>Severity</td>
 * <td valign=top>Level</td>
 * </tr>
 * <tr>
 * <td valign=top>Completion Code</td>
 * <td valign=top>Level</td>
 * </tr>
 * <tr>
 * <td valign=top>Message Data</td>
 * <td valign=top>Message</td>
 * </tr>
 * <tr>
 * <td valign=top>Start Time</td>
 * <td valign=top>Timestamp</td>
 * </tr>
 * <tr>
 * <td valign=top>End Time</td>
 * <td valign=top>Timestamp</td>
 * </tr>
 * </table>
 *
 * <p>
 * In addition, it will set other TNT4j Activity and Event parameters based on the local environment. These default
 * parameter values can be overridden by annotating the log event messages or passing them using {@code MDC}.
 *
 * <p>
 * The following '#' hash tag annotations are supported for reporting activities:
 * </p>
 * <table summary="">
 * <tr>
 * <td><b>beg</b></td>
 * <td>Begin an activity (collection of related events/messages)</td>
 * </tr>
 * <tr>
 * <td><b>end</b></td>
 * <td>End an activity (collection of related events/messages)</td>
 * </tr>
 * <tr>
 * <td><b>app</b></td>
 * <td>Application/source name</td>
 * </tr>
 * </table>
 *
 * <p>
 * The following '#' hash tag annotations are supported for reporting events:
 * </p>
 * <table summary="">
 * <tr>
 * <td><b>app</b></td>
 * <td>Application/source name</td>
 * </tr>
 * <tr>
 * <td><b>usr</b></td>
 * <td>User name</td>
 * </tr>
 * <tr>
 * <td><b>cid</b></td>
 * <td>Correlator for relating events across threads, applications, servers</td>
 * </tr>
 * <tr>
 * <td><b>tag</b></td>
 * <td>User defined tag</td>
 * </tr>
 * <tr>
 * <td><b>loc</b></td>
 * <td>Location specifier</td>
 * </tr>
 * <tr>
 * <td><b>opn</b></td>
 * <td>Event/Operation name</td>
 * </tr>
 * <tr>
 * <td><b>opt</b></td>
 * <td>Event/Operation Type - Value must be either a member of {@link OpType} or the equivalent numeric value</td>
 * </tr>
 * <tr>
 * <td><b>rsn</b></td>
 * <td>Resource name on which operation/event took place</td>
 * </tr>
 * <tr>
 * <td><b>msg</b></td>
 * <td>Event message (user data) enclosed in single quotes e.g. {@code #msg='My error message'}</td>
 * </tr>
 * <tr>
 * <td><b>sev</b></td>
 * <td>Event severity - Value can be either a member of {@link OpLevel} or any numeric value</td>
 * </tr>
 * <tr>
 * <td><b>ccd</b></td>
 * <td>Event completion code - Value must be either a member of {@link OpCompCode} or the equivalent numeric value</td>
 * </tr>
 * <tr>
 * <td><b>rcd</b></td>
 * <td>Reason code</td>
 * </tr>
 * <tr>
 * <td><b>exc</b></td>
 * <td>Exception message</td>
 * </tr>
 * <tr>
 * <td><b>elt</b></td>
 * <td>Elapsed time of event, in microseconds</td>
 * </tr>
 * <tr>
 * <td><b>age</b></td>
 * <td>Message/event age in microseconds (useful when receiving messages, designating message age on receipt)</td>
 * </tr>
 * <tr>
 * <td><b>stt</b></td>
 * <td>Start time, as the number of microseconds since epoch</td>
 * </tr>
 * <tr>
 * <td><b>ent</b></td>
 * <td>End time, as the number of microseconds since epoch</td>
 * </tr>
 * <tr>
 * <td><b>%[data-type][:value-type]/user-key</b></td>
 * <td>User defined key/value pair and data-type-[s|i|l|f|n|d|b] are type specifiers (i=Integer, l=Long, d=Double,
 * f=Float, n=Number, s=String, b=Boolean) (e.g #%i/myfield=7634732)</td>
 * </tr>
 * </table>
 *
 * Value types are optional and defined in {@link ValueTypes}. It is highly recommended to annotate user defined
 * properties with data-type and value-type.
 *
 * <p>
 * An example of annotating (TNT4J) a single log message using log4j:
 * </p>
 * <p>
 * {@code logger.error("Operation Failed #app=MyApp #opn=save #rsn=" + filename + "  #rcd="
 *  + errno + " #msg='My error message'");}
 * </p>
 *
 *
 * <p>
 * An example of reporting a TNT4J activity using log4j (activity is a related collection of events):
 * </p>
 * <p>
 * {@code logger.info("Starting order processing #app=MyApp #beg=" + activityName);}
 * </p>
 * <p>
 * {@code logger.debug("Operation processing #app=MyApp #opn=save #rsn=" + filename);}
 * </p>
 * <p>
 * {@code logger.error("Operation Failed #app=MyApp #opn=save #rsn=" + filename + "  #rcd=" + errno);}
 * </p>
 * <p>
 * {@code logger.info("Finished order processing #app=MyApp #end=" + activityName + " #%l/order=" + orderNo + " #%d:currency/amount=" + amount);}
 * </p>
 *
 * @version $Revision: 1 $
 *
 */
public class TNT4JAppender extends AppenderSkeleton implements AppenderConstants {
	public static final String SNAPSHOT_CATEGORY = "Log4J";

	private TrackingLogger logger;
	private String sourceName;
	private String snapCategory = SNAPSHOT_CATEGORY;
	private int maxActivitySize = 100;
	private SourceType sourceType = SourceType.APPL;
	private ConfigFactory cFactory = DefaultConfigFactory.getInstance();
	private Map<String, Properties> cProperties = null;

	private boolean metricsOnException = true;
	private long metricsFrequency = 60, lastSnapshot = 0;

	/**
	 * Create an appender instance without any defaults. Have to call {@code setXXX()} calls to configure the appender
	 * before activation.
	 * 
	 */
	public TNT4JAppender() {
	}

	/**
	 * Create an appender instance with default configuration and {@code SourceType.APPL} type.
	 * 
	 * @param source
	 *            name
	 */
	public TNT4JAppender(String source) {
		this(source, SourceType.APPL);
	}

	/**
	 * Create an appender instance with default configuration factory.
	 * 
	 * @param source
	 *            name
	 * @param type
	 *            source type, see {@code SourceType}
	 * @see SourceType
	 */
	public TNT4JAppender(String source, SourceType type) {
		this(source, type, DefaultConfigFactory.getInstance());
	}

	/**
	 * Create an appender instance
	 * 
	 * @param source
	 *            name
	 * @param type
	 *            source type, see {@code SourceType}
	 * @param cf
	 *            logger configuration factory instance
	 * @see SourceType
	 * @see ConfigFactory
	 */
	public TNT4JAppender(String source, SourceType type, ConfigFactory cf) {
		this(source, type, cf, null);
	}

	/**
	 * Create an appender instance
	 * 
	 * @param source
	 *            name
	 * @param type
	 *            source type, see {@code SourceType}
	 * @param cProps
	 *            user defined TNT4J logger properties (null if none)
	 * @see SourceType
	 * @see ConfigFactory
	 */
	public TNT4JAppender(String source, SourceType type, Map<String, Properties> cProps) {
		this(source, type, DefaultConfigFactory.getInstance(), cProps);
	}

	/**
	 * Create an appender instance
	 * 
	 * @param source
	 *            name
	 * @param type
	 *            source type, see {@code SourceType}
	 * @param cf
	 *            logger configuration factory instance
	 * @param cProps
	 *            user defined TNT4J logger properties (null if none)
	 * @see SourceType
	 * @see ConfigFactory
	 */
	public TNT4JAppender(String source, SourceType type, ConfigFactory cf, Map<String, Properties> cProps) {
		this.setSourceName(source);
		this.setSourceType(type);
		this.setConfigFactory(cf);
		this.setConfigProperties(cProps);
	}

	/**
	 * Associate a logger configuration factory with this appender
	 * 
	 * @param cf
	 *            logger configuration factory instances
	 * @see ConfigFactory
	 */
	public void setConfigFactory(ConfigFactory cf) {
		cFactory = cf;
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

	@Override
	public void activateOptions() {
		try {
			if (sourceName == null) {
				setSourceName(getName());
			}
			TrackerConfig config = ((cProperties == null) ? cFactory.getConfig(sourceName, sourceType)
					: cFactory.getConfig(sourceName, sourceType, cProperties));
			logger = TrackingLogger.getInstance(config.build());
			logger.open();
		} catch (Throwable e) {
			LogLog.error(
					"Unable to create tracker instance=" + getName() + ", config.factory=" + cFactory + ", source.name="
							+ sourceName + ", source.type=" + sourceType + ", snapshot.category=" + snapCategory,
					e);
		}
	}

	@Override
	protected void append(LoggingEvent event) {
		long lastReport = System.currentTimeMillis();
		String eventMsg = event.getRenderedMessage();

		ThrowableInformation ti = event.getThrowableInformation();
		Throwable ex = ti != null ? ti.getThrowable() : null;

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
	 * Process a given log4j event into a TNT4J event object {@link TrackingEvent}.
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
	private TrackingEvent processEventMessage(Map<String, String> attrs, TrackingActivity activity, LoggingEvent jev,
			String eventMsg, Throwable ex) {
		int rcode = 0;
		long elapsedTimeUsec = getUsecsSinceLastEvent();
		long evTime = jev.getTimeStamp() * 1000; // convert to usec
		long startTime = 0, endTime = 0;
		Snapshot snapshot = null;

		OpCompCode ccode = getOpCompCode(jev);
		OpLevel level = getOpLevel(jev);

		TrackingEvent event = logger.newEvent(jev.getLocationInformation().getMethodName(), eventMsg);
		event.getOperation().setSeverity(level);
		event.setTag(jev.getThreadName());
		event.getOperation().setResource(jev.getLoggerName());
		event.setLocation(
				jev.getLocationInformation().getFileName() + ":" + jev.getLocationInformation().getLineNumber());
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
	 * Map log4j logging event level to TNT4J {@link OpLevel}.
	 *
	 * @param event
	 *            log4j logging event object
	 * @return TNT4J {@link OpLevel}.
	 */
	private OpLevel getOpLevel(LoggingEvent event) {
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
	 * Map log4j logging event level to TNT4J {@link OpCompCode}.
	 *
	 * @param event
	 *            log4j logging event object
	 * @return TNT4J {@link OpCompCode}.
	 */
	private OpCompCode getOpCompCode(LoggingEvent event) {
		Level lvl = event.getLevel();
		if (lvl == Level.INFO) {
			return OpCompCode.SUCCESS;
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

	@Override
	public void close() {
		if (logger != null) {
			logger.close();
		}
	}

	@Override
	public boolean requiresLayout() {
		return false;
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
