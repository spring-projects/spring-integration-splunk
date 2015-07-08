/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.splunk.inbound;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.splunk.event.SplunkEvent;
import org.springframework.integration.splunk.rule.SplunkRunning;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.splunk.Job;

/**
 * @author Artem Bilan
 * @since 1.1
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class RealTimeSearchIntegrationTests {

	@ClassRule
	public static SplunkRunning splunkRunning = new SplunkRunning();

	@ClassRule
	public static LongRunningIntegrationTest longRunning = new LongRunningIntegrationTest();

	@Autowired
	private MessageChannel output;

	@Autowired
	private PollableChannel results;

	@Autowired
	private SourcePollingChannelAdapter inboundChannelAdapter;

	@Test
	public void testRealTimeSearch() throws Exception {
		Thread.sleep(500);

		for (int i = 0; i < 50; i++) {
			this.output.send(new GenericMessage<SplunkEvent>(new SplunkEvent("SPRING", "" + i)));
			if (i % 10 == 0) {
				Thread.sleep(100);
			}
		}

		Thread.sleep(200);

		for (int i = 50; i < 100; i++) {
			this.output.send(new GenericMessage<SplunkEvent>(new SplunkEvent("SPRING_INTEGRATION", "" + i)));
			if (i % 10 == 0) {
				Thread.sleep(100);
			}
		}

		for (int i = 0; i < 100; i++) {
			Message<?> receive = this.results.receive(10000);
			assertNotNull(receive);
			assertThat(receive.getPayload(), instanceOf(SplunkEvent.class));
			SplunkEvent payload = (SplunkEvent) receive.getPayload();
			String event_id = payload.getEventData().get("event_id");
			assertEquals("" + i, event_id);
		}

		this.inboundChannelAdapter.stop();

		Job job = TestUtils.getPropertyValue(this.inboundChannelAdapter,
				"source.splunkExecutor.reader.realTimeSearchJob", Job.class);

		int n = 0;

		while (!job.isFinalized() && n++ < 10) {
			Thread.sleep(100);
		}

		assertTrue(n < 10);

		Thread.sleep(2000);

		this.output.send(new GenericMessage<SplunkEvent>(new SplunkEvent("OUT_OF_SEARCH", "foo")));

		Message<?> receive = this.results.receive(100);
		assertNull(receive);

		Thread.sleep(2000);

		this.inboundChannelAdapter.start();

		receive = this.results.receive(100);
		assertNull(receive);

		this.output.send(new GenericMessage<SplunkEvent>(new SplunkEvent("IN_SEARCH", "bar")));

		receive = this.results.receive(10000);
		assertNotNull(receive);
		assertThat(receive.getPayload(), instanceOf(SplunkEvent.class));
		SplunkEvent payload = (SplunkEvent) receive.getPayload();

		assertEquals("IN_SEARCH", payload.getEventData().get("name"));
		assertEquals("bar", payload.getEventData().get("event_id"));
	}

}
