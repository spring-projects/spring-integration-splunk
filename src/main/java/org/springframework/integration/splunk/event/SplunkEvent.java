/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.splunk.event;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.util.Assert;

/**
 * Splunk data entity
 *
 * @author Jarred Li
 * @author Damien Dallimore
 * @author David Turanski
 * @since 1.0
 *
 */

@SuppressWarnings("serial")
public class SplunkEvent implements Serializable {

	/**
	 * Contents of the event message
	 */
	private StringBuffer eventMessage;

	/**
	 * Whether or not to add a date to the event string
	 */
	protected boolean useInternalDate = false;

	/**
	 * default date format is using internal generated date
	 */
	protected static final String DATEFORMATPATTERN = "yyyy-MM-dd\tHH:mm:ss:SSSZ";
	/**
	 * Date Formatter
	 */
	protected static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern(DATEFORMATPATTERN);

	/**
	 * Event prefix fields
	 */
	protected static final String PREFIX_NAME = "name";
	protected static final String PREFIX_EVENT_ID = "event_id";

	/**
	 * Java Throwable type fields
	 */
	protected static final String THROWABLE_CLASS = "throwable_class";
	protected static final String THROWABLE_MESSAGE = "throwable_message";
	protected static final String THROWABLE_STACKTRACE_ELEMENTS = "stacktrace_elements";

	protected static final String LINEBREAK = "\n";


	/**
	 * A Constructor to load data from a Map
	 * @param data the map
	 */
	public SplunkEvent(Map<String, String> data) {
		this.eventMessage = new StringBuffer();
		for (String key : data.keySet()) {
			this.addPair(key, data.get(key));
		}
	}

	/**
	 * A Copy constructor
	 * @param splunkEvent to copy
	 */
	public SplunkEvent(SplunkEvent splunkEvent) {
		this.eventMessage = splunkEvent.eventMessage;
		this.useInternalDate = splunkEvent.useInternalDate;
	}

	/**
	 * Constructor to create a generic event
	 * @param eventName the event name
	 * @param eventID the event id
	 * @param useInternalDate whether or not to add a date to the event string
	 */
	public SplunkEvent(String eventName, String eventID, boolean useInternalDate) {

		this.eventMessage = new StringBuffer();
		this.useInternalDate = useInternalDate;

		addPair(PREFIX_NAME, eventName);
		addPair(PREFIX_EVENT_ID, eventID);
	}

	/**
	 * Constructor to create a generic event with the default format
	 *
	 * @param eventName the event name
	 * @param eventID the event ID
	 */
	public SplunkEvent(String eventName, String eventID) {

		this(eventName, eventID, false);
	}

	/**
	 * Default constructor
	 */
	public SplunkEvent() {
		this.eventMessage = new StringBuffer();
	}

	/**
	 * Add a key value pair to json
	 *
	 * @param key with which the specified value is to be associated
	 * @param value to be associated
	 */
	public void addPair(String key, char value) {
		addPair(key, String.valueOf(value));
	}

	/**
	 * Add a key value pair to json
	 *
	 * @param key with which the specified value is to be associated
	 * @param value to be associated
	 */
	public void addPair(String key, boolean value) {
		addPair(key, String.valueOf(value));
	}

	/**
	 * Add a key value pair to json
	 *
	 * @param key with which the specified value is to be associated
	 * @param value to be associated
	 */
	public void addPair(String key, double value) {
		addPair(key, String.valueOf(value));
	}

	/**
	 * Add a key value pair to json
	 *
	 * @param key with which the specified value is to be associated
	 * @param value to be associated
	 */
	public void addPair(String key, long value) {
		addPair(key, String.valueOf(value));
	}

	/**
	 * Add a key value pair to json
	 *
	 * @param key with which the specified value is to be associated
	 * @param value to be associated
	 */
	public void addPair(String key, int value) {
		addPair(key, String.valueOf(value));
	}

	/**
	 * Add a key value pair to json
	 *
	 * @param key with which the specified value is to be associated
	 * @param value to be associated
	 */
	public void addPair(String key, Object value) {
		addPair(key, value.toString());
	}

	/**
	 * Utility method for formatting Throwable,Error,Exception objects in a more
	 * linear and Splunk friendly manner than printStackTrace
	 *
	 * @param throwable
	 *            the Throwable object to add to the event
	 */
	public void addThrowable(Throwable throwable) {

		addThrowableObject(throwable, -1);
	}

	/**
	 * Utility method for formatting Throwable,Error,Exception objects in a more
	 * linear and Splunk friendly manner than printStackTrace
	 *
	 * @param throwable
	 *            the Throwable object to add to the event
	 * @param stackTraceDepth
	 *            maximum number of stacktrace elements to log
	 */
	public void addThrowable(Throwable throwable, int stackTraceDepth) {

		addThrowableObject(throwable, stackTraceDepth);
	}

	/**
	 * Internal private method for formatting Throwable,Error,Exception objects
	 * in a more linear and Splunk friendly manner than printStackTrace
	 *
	 * @param throwable
	 *            the Throwable object to add to the event
	 * @param stackTraceDepth
	 *            maximum number of stacktrace elements to log, -1 for all
	 */

	private void addThrowableObject(Throwable throwable, int stackTraceDepth) {

		addPair(THROWABLE_CLASS, throwable.getClass().getCanonicalName());
		addPair(THROWABLE_MESSAGE, throwable.getMessage());
		StackTraceElement[] elements = throwable.getStackTrace();
		StringBuffer sb = new StringBuffer();
		int depth = 0;
		for (StackTraceElement element : elements) {
			depth++;
			if (stackTraceDepth == -1 || stackTraceDepth >= depth)
				sb.append(element.toString()).append(",");
			else
				break;

		}
		addPair(THROWABLE_STACKTRACE_ELEMENTS, sb.toString());
	}

	/**
	 * Add a key value pair to json
	 *
	 * @param key with which the specified value is to be associated
	 * @param value to be associated
	 */
	public void addPair(String key, String value) {
		Assert.notNull(key, "key cannot be null");

		//start json
		if (this.eventMessage.length() == 0){
			this.eventMessage.append("{");
		}
		//another json param
		else{
			this.eventMessage.append(",");
		}
		//add the json param
		this.eventMessage.append("\"").append(key).append("\":\"").append(value).append("\"");

	}

	@Override
	/**
	 * return the completed event message
	 */
	public String toString() {

		String event = "";

		if (useInternalDate) {
			addPair("timestamp",DATE_FORMATTER.print(new Date().getTime()));
		}

		//end json
		if (this.eventMessage.length() != 0){
		  eventMessage.append("}");
		}
		event = eventMessage.toString();

		return event;
	}

}
