Spring Integration Splunk Adapter
=================================================

The SI adapter includes Outbound Channel Adapter and Inbound Channel Adapter.

Inbound channel adapter :
-----------------------------------------------------------------------------
The Inbound channel adapter is used to read data from Splunk and output a message containing the data to a Spring Integration channel. There are 5 ways to get data from Splunk:

* Blocking
* Non blocking
* Saved search
* Realtime
* Export


### Blocking search:

```xml
	<int-splunk:inbound-channel-adapter id="splunkInboundChannelAdapter"
		search="search spring:example"
		splunk-server-ref="splunkServer"
		channel="inputFromSplunk" mode="BLOCKING" earliest-time="-1d" latest-time="now" init-earliest-time="-1d">
		<int:poller fixed-rate="5" time-unit="SECONDS"/>
	</int-splunk:inbound-channel-adapter>
```


### Non blocking search:

```xml
	<int-splunk:inbound-channel-adapter id="splunkInboundChannelAdapter"
		search="search spring:example"
		splunk-server-ref="splunkServer"
		channel="inputFromSplunk" mode="NORMAL" earliest-time="-1d" latest-time="now" init-earliest-time="-1d">
		<int:poller fixed-rate="5" time-unit="SECONDS"/>
	</int-splunk:inbound-channel-adapter>
```


### Saved search:

```xml
	<int-splunk:inbound-channel-adapter id="splunkInboundChannelAdapter"
		savedSearch="test" splunk-server-ref="splunkServer"
		channel="inputFromSplunk" mode="SAVEDSEARCH" earliest-time="-1d" latest-time="now" init-earliest-time="-1d">
		<int:poller fixed-rate="5" time-unit="SECONDS"/>
	</int-splunk:inbound-channel-adapter>
```


### Realtime search:

```xml
	<int-splunk:inbound-channel-adapter id="splunkInboundChannelAdapter"
		search="search spring:example" splunk-server-ref="splunkServer" channel="inputFromSplunk"
		mode="REALTIME" earliest-time="-5s" latest-time="rt" init-earliest-time="-1d">
		<int:poller fixed-rate="5" time-unit="SECONDS"/>
	</int-splunk:inbound-channel-adapter>
```

The Realtime search starts only the single log-lived Splunk Job, so each `poll` asks the same Jobs for current
`ResultsPreview` storing `offset` for the next poll. If Realtime search Job is finished or failed the next `poll`
will start a new fresh Job with the current `offset`.

**Important**. It isn't recommended to use `fixed-rate` on the `<poller>`. Since we have only a single search Job,
concurrent `ResultsPreview`s may return the same events from Splunk.

### Export:

```xml
	<int-splunk:inbound-channel-adapter id="splunkInboundChannelAdapter"
		auto-startup="true" search="search spring:example" splunk-server-ref="splunkServer" channel="inputFromSplunk"
		mode="EXPORT" earliest-time="-5d" latest-time="now" init-earliest-time="-1d">
		<int:poller fixed-rate="5" time-unit="SECONDS"/>
	</int-splunk:inbound-channel-adapter>
```

Outbound channel adapter:
----------------------------------------------------------------------------------------------

The Outbound channel adapter is used to write data to Splunk from a Spring Integration message channel. There are 3 types of data writers provided:

* submit - Use's Splunk's REST API. Appropriate for small or infrequent data loads. Posts data to a named index or the default if not specified.
* index - Streams data to a named index or the default if not specified.
* tcp - Streams data to a tcp port associated with a defined tcp input.

The outbound channel adapter requires a child *-writer element which defines related attributes:

### Submit:

```xml
	<int-splunk:outbound-channel-adapter
		id="splunkOutboundChannelAdapter"
		channel="outputToSplunk"
		splunk-server-ref="splunkServer"
		source-type="spring-integration"
		source="example2">
		<int-splunk:submit-writer index="foo"/>
	</int-splunk:outbound-channel-adapter>
```

### Index:

```xml
	<int-splunk:outbound-channel-adapter
		id="splunkOutboundChannelAdapter"
		channel="outputToSplunk"
		splunk-server-ref="splunkServer"
	 >
		<int-splunk:index-writer index="someIndex"/>
	</int-splunk:outbound-channel-adapter>
```

### TCP

```xml
	<int-splunk:outbound-channel-adapter
		id="splunkOutboundChannelAdapter"
		channel="outputToSplunk"
		splunk-server-ref="splunkServer"
	  >
		<int-splunk:tcp-writer port="9999"/>
	</int-splunk:outbound-channel-adapter>
```

*NOTE: The input must exist and be enabled on the server*

### Configuring The Splunk Server connection

```xml
	<int-splunk:server id="splunkServer" username="admin" password="password" timeout="5000" host="somehost.someplace.com" port="9000" />
```

Alternatively, you can configure a Splunk Server failover mechanism

```xml
  <int-splunk:server id="splunkServer" username="admin" password="password" timeout="5000"
  					 host="somehost.someplace.com" port="9000" />

  <int-splunk:server id="splunkServerBackup" username="admin" password="password" timeout="5000"
   					 host="somehost.someotherplace.com" port="9000" />

  <util:list id="splunkServersList">
    <ref bean="splunkServer" />
    <ref bean="splunkServerBackup" />
  </util:list>

  <bean id="splunkServiceFactory" class="org.springframework.integration.splunk.support.SplunkServiceFactory">
    <constructor-arg ref="splunkServersList"/>
  </bean>
```

Additional server properties include (see [splunk](http://docs.splunk.com/Documentation/Splunk/latest) documentation for details):

* app
* scheme
* scope
* owner

The default host is *localhost* and the default port is *8089*. The *timeout* attribute indicates how long to wait
for a connection in milliseconds.


Development
-----------------
### Build:

	./gradlew build

### Import the project to Eclipse:

To generate Eclipse metadata (e.g., .classpath and .project files), do the following:

	./gradlew eclipse

