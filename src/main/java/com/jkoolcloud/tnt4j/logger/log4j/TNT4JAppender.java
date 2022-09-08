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

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;

import com.jkoolcloud.tnt4j.core.OpCompCode;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.core.ValueTypes;
import com.jkoolcloud.tnt4j.source.SourceType;

/**
 * <p>
 * Log4j appender for sending log4j events to TNT4J logging framework.
 * </p>
 *
 * <p>
 * This appender will extract information from the log4j {@code LoggingEvent} and construct the appropriate message for
 * sending to TNT4J.
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
 * <li>All messages logged to this appender will be sent to all defined sinks as configured by TNT4J configuration.</li>
 *
 * </ul>
 *
 * <p>
 * This appender supports the following properties:
 * </p>
 * <table summary="" cellspacing=10>
 * <tr>
 * <td valign=top><b>SourceName</b></td>
 * <td valign=top>source name associated with the appender matching TNT4J configuration</td>
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
 * This appender by default sets the following TNT4J Activity and Event parameters based on the information in the log4j
 * event, as follows:
 * </p>
 * <table summary="" cellspacing=10>
 * <tr>
 * <td valign=top><b>TNT4J Parameter</b></td>
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
 * In addition, it will set other TNT4J Activity and Event parameters based on the local environment. These default
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
 * @version $Revision: 2 $
 *
 * @see com.jkoolcloud.tnt4j.logger.log4j.TNT4JManager
 */
@Plugin(name = "Tnt4j", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class TNT4JAppender extends AbstractAppender {
	public static final String SNAPSHOT_CATEGORY = "Log4J";

	private final TNT4JManager manager;

	public static class Builder<B extends TNT4JAppender.Builder<B>> extends AbstractAppender.Builder<B>
			implements org.apache.logging.log4j.core.util.Builder<TNT4JAppender> {

		@PluginBuilderAttribute
		private String sourceName;

		@PluginBuilderAttribute
		private SourceType sourceType = SourceType.APPL;

		@PluginBuilderAttribute
		private String snapCategory = SNAPSHOT_CATEGORY;

		@PluginBuilderAttribute
		private int connectTimeoutMillis = 0;

		@PluginBuilderAttribute
		private int maxActivitySize = 100;

		@PluginBuilderAttribute
		private boolean metricsOnException = true;

		@PluginBuilderAttribute
		private long metricsFrequency = 60;

		@Override
		public TNT4JAppender build() {
			TNT4JManager trackerManager = new TNT4JManager(getConfiguration(), getConfiguration().getLoggerContext(),
					getName(), sourceName, sourceType, snapCategory, maxActivitySize, metricsOnException,
					metricsFrequency);
			return new TNT4JAppender(getName(), getFilter(), getLayout(), isIgnoreExceptions(), trackerManager,
					getPropertyArray());
		}

		public String getSourceName() {
			return sourceName;
		}

		public SourceType getSourceType() {
			return sourceType;
		}

		public String getSnapCategory() {
			return snapCategory;
		}

		public int getConnectTimeoutMillis() {
			return connectTimeoutMillis;
		}

		public int getMaxActivitySize() {
			return maxActivitySize;
		}

		public boolean isMetricsOnException() {
			return metricsOnException;
		}

		public long getMetricsFrequency() {
			return metricsFrequency;
		}

		public B setSourceName(String sourceName) {
			this.sourceName = sourceName;
			return asBuilder();
		}

		public B setSourceType(SourceType sourceType) {
			this.sourceType = sourceType;
			return asBuilder();
		}

		public B setSnapCategory(String snapCategory) {
			this.snapCategory = snapCategory;
			return asBuilder();
		}

		public B setMaxActivitySize(int maxActivitySize) {
			this.maxActivitySize = maxActivitySize;
			return asBuilder();
		}

		public B setMetricsOnException(boolean metricsOnException) {
			this.metricsOnException = metricsOnException;
			return asBuilder();
		}

		public B setMetricsFrequency(long metricsFrequency) {
			this.metricsFrequency = metricsFrequency;
			return asBuilder();
		}
	}

	/**
	 * @param <B>
	 *            type of builder instance
	 * 
	 * @return a builder for a TNT4JAppender.
	 */
	@PluginBuilderFactory
	public static <B extends TNT4JAppender.Builder<B>> B newBuilder() {
		return new TNT4JAppender.Builder<B>().asBuilder();
	}

	/**
	 * Create an appender instance.
	 *
	 * @param name
	 *            The Appender name.
	 * @param filter
	 *            The Filter to associate with the Appender.
	 * @param layout
	 *            The layout to use to format the event.
	 * @param ignoreExceptions
	 *            If {@code true}, exceptions will be logged and suppressed. If {@code false} errors will be logged and
	 *            then passed to the application.
	 * @param manager
	 *            tracker manager instance
	 * @param properties
	 *            array of appender properties
	 */
	public TNT4JAppender(String name, Filter filter, Layout<? extends Serializable> layout, boolean ignoreExceptions,
			TNT4JManager manager, Property[] properties) {
		super(name, filter, layout, ignoreExceptions, properties);

		this.manager = Objects.requireNonNull(manager, "manager");
	}

	@Override
	public final void start() {
		super.start();
		manager.startup();
	}

	@Override
	public void append(LogEvent event) {
		try {
			manager.tnt(event);
		} catch (Exception exc) {
			error("Failed to log event in appender [" + getName() + "]", event, exc);
		}
	}

	@Override
	public boolean stop(long timeout, TimeUnit timeUnit) {
		setStopping();
		boolean stopped = super.stop(timeout, timeUnit, false);
		stopped &= manager.stop(timeout, timeUnit);
		setStopped();

		return stopped;
	}
}
