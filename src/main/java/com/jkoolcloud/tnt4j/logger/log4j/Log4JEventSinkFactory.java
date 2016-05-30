/*
 * Copyright 2014-2015 JKOOL, LLC.
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

import java.util.Properties;

import com.jkoolcloud.tnt4j.format.DefaultFormatter;
import com.jkoolcloud.tnt4j.format.EventFormatter;
import com.jkoolcloud.tnt4j.sink.AbstractEventSinkFactory;
import com.jkoolcloud.tnt4j.sink.EventSink;

/**
 * <p>Concrete implementation of <code>EventSinkFactory</code> interface over log4j, which
 * creates instances of <code>EventSink</code>. This factory uses <code>Log4jEventSink</code>
 * as the underlying logger provider.</p>
 *
 *
 * @see EventSink
 * @see Log4JEventSink
 *
 * @version $Revision: 1 $
 *
 */
public class Log4JEventSinkFactory extends AbstractEventSinkFactory {

	@Override
	public EventSink getEventSink(String name) {
		return configureSink(new Log4JEventSink(name, System.getProperties(), new DefaultFormatter()));
	}

	@Override
	public EventSink getEventSink(String name, Properties props) {
		return configureSink(new Log4JEventSink(name, props, new DefaultFormatter()));
	}

	@Override
    public EventSink getEventSink(String name, Properties props, EventFormatter frmt) {
		return configureSink(new Log4JEventSink(name, props, frmt));
   }

	/**
	 * Static method to obtain default event sink
	 *
	 * @param name name of the application/event sink to get
	 * @return event sink
	 */
	public static EventSink defaultEventSink(String name) {
		return new Log4JEventSink(name, System.getProperties(), new DefaultFormatter());
	}

	/**
	 * Static method to obtain default event sink
	 *
	 * @param clazz class for which to get the event sink
	 * @return event sink
	 */
	public static EventSink defaultEventSink(Class<?> clazz) {
	    return defaultEventSink(clazz.getName());
    }
}
