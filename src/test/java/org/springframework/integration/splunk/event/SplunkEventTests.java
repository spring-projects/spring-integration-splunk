/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.splunk.event;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author David Turanski
 *
 */
public class SplunkEventTests {
	@Test
	public void testGetEventData() {
		Map<String,String> data = new HashMap<String,String>();
		data.put("foo", "foo");
		data.put("bar", "1");
		SplunkEvent event = new SplunkEvent(data);
		
		assertEquals(23,event.toString().length());
		
	}
	public void testGetEventDataEmpty() {
		SplunkEvent event = new SplunkEvent( );
		assertEquals(0,event.toString().length());
	}

	@Test(expected=RuntimeException.class)
	public void testKeyCannotBeNull() {
		SplunkEvent event = new SplunkEvent();
		event.addPair(null, "foo");
	}

}
