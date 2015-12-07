/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.splunk.support;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.SchemeIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.splunk.core.DataWriter;
import org.springframework.integration.splunk.event.SplunkEvent;

import com.splunk.Args;

/**
 *
 * A {@code org.springframework.integration.splunk.core.DataWriter} that creates
 * a HTTP Event Collector connection to Splunk
 *
 * @author Damien Dallimore
 *
 */
public class SplunkHECWriter implements DataWriter, SmartLifecycle {

	protected final Log logger = LogFactory.getLog(getClass());

	private boolean autoStartup = true;
	protected Args args;
	private int phase;
	private boolean running;

	// configurable parameters
	private String token;
	private String host = "localhost";
	private int port = 8088;
	private boolean https = false;
	private int poolsize = 1;

	private String index = "main";
	private String source = "spring_integration";
	private String sourcetype = "spring_integration_hec";

	private boolean batchMode = false;
	private long maxBatchSizeBytes = 1048576; // 1MB
	private long maxBatchSizeEvents = 100; // 100 events
	private long maxInactiveTimeBeforeBatchFlush = 5000;// 5 secs

	// batch buffer
	private List<String> batchBuffer;
	private long currentBatchSizeBytes = 0;
	private long lastEventReceivedTime;

	private CloseableHttpAsyncClient httpClient;
	private URI uri;

	private static final HostnameVerifier HOSTNAME_VERIFIER = new HostnameVerifier() {
		public boolean verify(String s, SSLSession sslSession) {
			return true;
		}
	};

	public SplunkHECWriter(Args args) {
		this.args = args;
	}

	// getters and setters for various configurable parameters

	public String getToken() {
		return token;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public boolean isHttps() {
		return https;
	}

	public int getPoolsize() {
		return poolsize;
	}

	public String getIndex() {
		return index;
	}

	public String getSource() {
		return source;
	}

	public String getSourcetype() {
		return sourcetype;
	}

	public boolean isBatchMode() {
		return batchMode;
	}

	public long getMaxBatchSizeBytes() {
		return maxBatchSizeBytes;
	}

	public long getMaxBatchSizeEvents() {
		return maxBatchSizeEvents;
	}

	public long getMaxInactiveTimeBeforeBatchFlush() {
		return maxInactiveTimeBeforeBatchFlush;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setHttps(boolean https) {
		this.https = https;
	}

	public void setPoolsize(int poolsize) {
		this.poolsize = poolsize;
	}

	public void setIndex(String index) {
		this.index = index;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public void setSourcetype(String sourcetype) {
		this.sourcetype = sourcetype;
	}

	public void setBatchMode(boolean batchMode) {
		this.batchMode = batchMode;
	}

	public void setMaxBatchSizeBytes(long maxBatchSizeBytes) {
		this.maxBatchSizeBytes = maxBatchSizeBytes;
	}

	public void setMaxBatchSizeEvents(long maxBatchSizeEvents) {
		this.maxBatchSizeEvents = maxBatchSizeEvents;
	}

	public void setMaxInactiveTimeBeforeBatchFlush(
			long maxInactiveTimeBeforeBatchFlush) {
		this.maxInactiveTimeBeforeBatchFlush = maxInactiveTimeBeforeBatchFlush;
	}

	@Override
	public void write(SplunkEvent data) throws Exception {

		String message = data.toString();

		String currentMessage = "";
		try {

			if (!(message.startsWith("{") && message.endsWith("}"))
					&& !(message.startsWith("\"") && message.endsWith("\"")))
				message = wrapMessageInQuotes(message);

			// could use a JSON Object , but the JSON is so trivial , just
			// building it with a StringBuilder
			StringBuilder json = new StringBuilder();
			json.append("{\"").append("event\":").append(message).append(",\"").append("index\":\"").append(getIndex())
					.append("\",\"").append("source\":\"").append(getSource()).append("\",\"").append("sourcetype\":\"")
					.append(getSourcetype()).append("\"").append("}");

			currentMessage = json.toString();

			if (isBatchMode()) {
				lastEventReceivedTime = System.currentTimeMillis();
				currentBatchSizeBytes += currentMessage.length();
				batchBuffer.add(currentMessage);
				if (flushBuffer()) {
					currentMessage = rollOutBatchBuffer();
					batchBuffer.clear();
					currentBatchSizeBytes = 0;
					hecPost(currentMessage);
				}
			} else {
				hecPost(currentMessage);
			}

		} catch (Exception e) {
			try {
				stop();
			} catch (Exception e1) {
			}

			try {
				start();
			} catch (Exception e2) {
			}
		}

	}

	@Override
	public void start() {

		try {
			this.batchBuffer = Collections
					.synchronizedList(new LinkedList<String>());
			this.lastEventReceivedTime = System.currentTimeMillis();

			Registry<SchemeIOSessionStrategy> sslSessionStrategy = RegistryBuilder
					.<SchemeIOSessionStrategy> create()
					.register("http", NoopIOSessionStrategy.INSTANCE)
					.register(
							"https",
							new SSLIOSessionStrategy(getSSLContext(),
									HOSTNAME_VERIFIER)).build();

			ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor();
			PoolingNHttpClientConnectionManager cm = new PoolingNHttpClientConnectionManager(
					ioReactor, sslSessionStrategy);
			cm.setMaxTotal(getPoolsize());

			HttpHost splunk = new HttpHost(getHost(), getPort());
			cm.setMaxPerRoute(new HttpRoute(splunk), getPoolsize());

			httpClient = HttpAsyncClients.custom().setConnectionManager(cm)
					.build();

			uri = new URIBuilder().setScheme(isHttps() ? "https" : "http")
					.setHost(getHost()).setPort(getPort())
					.setPath("/services/collector").build();

			httpClient.start();

			if (isBatchMode()) {
				new BatchBufferActivityCheckerThread(this).start();
			}
		} catch (Exception e) {
		}

		this.running = true;

	}

	@Override
	public void stop() {
		if (!running) {
			return;
		}

		try {
			httpClient.close();

		} catch (Exception e) {
		}

		this.running = false;

	}

	private String wrapMessageInQuotes(String message) {

		return "\"" + message + "\"";
	}

	class BatchBufferActivityCheckerThread extends Thread {

		SplunkHECWriter parent;

		BatchBufferActivityCheckerThread(SplunkHECWriter parent) {

			this.parent = parent;
		}

		public void run() {

			while (true) {
				String currentMessage = "";
				try {
					long currentTime = System.currentTimeMillis();
					if ((currentTime - lastEventReceivedTime) >= getMaxInactiveTimeBeforeBatchFlush()) {
						if (batchBuffer.size() > 0) {
							currentMessage = rollOutBatchBuffer();
							batchBuffer.clear();
							currentBatchSizeBytes = 0;
							hecPost(currentMessage);
						}
					}

					Thread.sleep(1000);
				} catch (Exception e) {
					try {
						parent.stop();
					} catch (Exception e1) {
					}

					try {
						parent.start();
					} catch (Exception e2) {
					}
				}

			}
		}
	}

	private SSLContext getSSLContext() {
		TrustStrategy acceptingTrustStrategy = new TrustStrategy() {
			public boolean isTrusted(X509Certificate[] certificate,
					String authType) {
				return true;
			}
		};
		SSLContext sslContext = null;
		try {
			sslContext = SSLContexts.custom()
					.loadTrustMaterial(null, acceptingTrustStrategy).build();
		} catch (Exception e) {
			// Handle error
		}
		return sslContext;

	}

	private boolean flushBuffer() {

		return (currentBatchSizeBytes >= getMaxBatchSizeBytes())
				|| (batchBuffer.size() >= getMaxBatchSizeEvents());

	}

	private String rollOutBatchBuffer() {

		StringBuilder sb = new StringBuilder();

		for (String event : batchBuffer) {
			sb.append(event);
		}

		return sb.toString();
	}

	private void hecPost(String currentMessage) throws Exception {
	
		HttpPost post = new HttpPost(uri);
		post.addHeader("Authorization", "Splunk " + getToken());

		StringEntity requestEntity = new StringEntity(currentMessage,
				ContentType.create("application/json", "UTF-8"));

		post.setEntity(requestEntity);
		Future<HttpResponse> future = httpClient.execute(post, null);
		HttpResponse response = future.get();
		// do nothing with the response

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.context.Lifecycle#isRunning()
	 */
	public boolean isRunning() {
		return this.running;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.context.Phased#getPhase()
	 */
	public int getPhase() {
		return this.phase;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.context.SmartLifecycle#isAutoStartup()
	 */
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.context.SmartLifecycle#stop(java.lang.Runnable)
	 */
	public synchronized void stop(Runnable callback) {
		this.stop();
		callback.run();
	}

}
