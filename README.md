# tnt4j-log4j

Log4J Appender for TNT4J.

-----------------------

**NOTE:** `tnt4j-log4j` version `0.4.0` migrated `log4j` API from version `1.x` to `2.x`.

Latest `log4j` `1.x` compliant `tnt4j-log4j` version is `0.3.5`.

-----------------------

### LOG4J Appender

All LOG4J messages can be routed to TNt4J event sinks via `TNT4JAppender` for Log4J, which allows developers to send event messages to
TNT4J.

Developers may also enrich event messages and pass context to TNT4J using hashtag enrichment scheme. Hashtags are used to decorate event
messages with important metadata about each log message. This metadata is used to generate TNT4J tracking events:

```java
logger.info("Starting a tnt4j activity #beg=Test, #app="+Log4JTest.class.getName());
logger.warn("First log message #app="+Log4JTest.class.getName()+", #msg='1 Test warning message'");
logger.error("Second log message #app="+Log4JTest.class.getName()+", #msg='2 Test error message'",new Exception("test exception"));
logger.info("Ending a tnt4j activity #end=Test, #app="+Log4JTest.class.getName()+" #%i/order-no="+orderNo+" #%d:currency/amount="+amount);
```

Above example groups messages between first and last into a related logical collection called `Activity`. Activity is a collection of
logically related events/messages. Hashtags `#beg`, `#end` are used to demarcate activity boundaries. This method also supports nested
activities.

User defined fields can be reported using `#[data-type][:value-type]/your-metric-name=your-value` convention (e.g. `#%i/order-no=62627`
or `#%d:currency/amount=50.45`).
`TNT4JAppender` supports the following optional `data-type` qualifiers:

```
	%i/ -- integer
	%l/ -- long
	%d/ -- double
	%f/ -- float
	%b/ -- boolean
	%n/ -- number
	%s/ -- string
```

All `value-type` qualifiers are defined in `com.jkoolcloud.tnt4j.core.ValueTypes`. Examples:

```
	currency 	-- generic currency
	flag 		-- boolean flag
	age 		-- age in time units
	guid 		-- globally unique identifier
	guage		-- numeric gauge
	counter		-- numeric counter
	percent		-- percent
	timestamp	-- timestamp
	addr 		-- generic address
```

Not specifying a qualifier defaults to auto-detection of type by `TNT4JAppender`. First `number` qualifier is tested and defaults
to `string` if the test fails (e.g. `#order-no=62627`). User defined fields are reported as a TNT4J snapshot with `Log4j` category and
snapshot name set to activity name set by `#beg`, `#end`, `#opn` tags.

Below is a sample log4j appender configuration:

```
### Default TNT4J Appender configuration
log4j.appender.tnt4j=com.jkoolcloud.tnt4j.logger.log4j.TNT4JAppender
log4j.appender.tnt4j.SourceName=com.log4j.Test
log4j.appender.tnt4j.SourceType=APPL
log4j.appender.tnt4j.MetricsOnException=true
log4j.appender.tnt4j.MetricsFrequency=60
log4j.appender.tnt4j.layout=org.apache.log4j.EnhancedPatternLayout
log4j.appender.tnt4j.layout.ConversionPattern=%d{ABSOLUTE} %-5p [%c{1}] %m%n
```

Running Samples
===============================================

* Simple TNT4J Sample application (`com.jkoolcloud.tnt4j.examples.TNT4JTest`):

```java	
java -Dlog4j2.configurationFile=file:config/log4j2.xml -Dtnt4j.config=config/tnt4j.properties -Dtnt4j.token.repository=config/tnt4j-tokens.properties  -Dtnt4j.dump.on.vm.shutdown=true -Dtnt4j.dump.provider.default=true -Dtnt4j.formatter.json.newline=true -classpath "./lib/*" com.jkoolcloud.tnt4j.examples.TNT4JTest com.myco.TestApp MYSERVER "Test log message" correlator1 "TestCommand"  TestLocation
```

**Command line arguments:**

* `-Dtnt4j.dump.on.vm.shutdown=true` java property allows application state dumps generated automatically upon VM shutdown.
* `-Dtnt4j.dump.provider.default=true` java property registers all default dump providers (memory, stack, logging stats).
* `-Dtnt4j.formatter.json.newline=true` java property directs `JSONFormatter` to append new line when formatting log entries.

See `<timestamp>.log` and `<vmid>.dump` file for output produced by `com.jkoolcloud.tnt4j.examples.TNT4JTest`. See `config/tnt4j.properties`
for TNT4J configuration: factories, formatters, listeners, etc. See Wiki for more information.

How to Build tnt4j-log4j
=========================================
Requirements

* JDK 1.8+

TNT4J-LOG4J depends on the following external packages:

* [TNT4J-API](http://nastel.github.io/TNT4J/)
* [Apache Log4J 2.x](https://logging.apache.org/log4j/2.x/)

Please use JCenter or Maven and these dependencies will be downloaded automatically.

tnt4j-log4j requires TNT4J. You will therefore need to point TNT4J to it's property file via the -Dtnt4j.config argument. This property
file is located here in GitHub under the /config directory. If using JCenter or Maven, it can be found in the zip assembly along with the
source code and javadoc.
