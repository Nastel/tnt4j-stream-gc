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
package org.tnt4j.stream.java.gc;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.GarbageCollectorMXBean;
import java.util.List;

import javax.management.NotificationEmitter;
import javax.management.NotificationListener;

import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.utils.Utils;

/**
 * This class provides java agent implementation for tracking GC invocations
 * using JMX notifications available in JDK1.7.04 or higher.
 * 
 * @version $Revision: 1 $
 * 
 */
public class GCTracker {
	private static final String DEFAULT_SOURCE_NAME = System.getProperty("tnt4j.stream.gc.sourcename", "org.tnt4j.stream.java.gc");
	private static final String DEFAULT_RESOURCE_NAME = System.getProperty("tnt4j.stream.gc.rsname", Utils.getVMName());

	/*
	 * Tracking logger instance where all GC tracking messages are recorded.
	 */
	private static TrackingLogger logger;

	protected static void createTracker(String sourceName) {
		try {
			logger = TrackingLogger.getInstance(sourceName);
			logger.open();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Obtain {@code TrackingLogger} instance associated with this
	 * tracker.
	 * 
	 * @return {@code TrackingLogger} instance
	 */
	public static TrackingLogger getTracker() {
		return logger;
	}
	
	/**
	 * Install tracker with a default resource and source name 
	 * {@code org.tnt4j.stream.java.gc} that must match configuration
	 * in {@code tnt4j.properties}
	 * 
	 */
	public static void installTracker() {
		installTracker(DEFAULT_RESOURCE_NAME, DEFAULT_SOURCE_NAME);
	}


	/**
	 * Install tracker with a default source name {@code org.tnt4j.stream.java.gc}
	 * that must match configuration in {@code tnt4j.properties}
	 * 
	 * @param resourceName used for labeling GC tracking activity
	 */
	public static void installTracker(String resourceName) {
		installTracker(resourceName, DEFAULT_SOURCE_NAME);
	}

	/**
	 * Install tracker with a specified source name that must match
	 * configuration in {@code tnt4j.properties}
	 * 
	 * @param sourceName matching tnt4j configuration
	 * @param resourceName used for labeling GC tracking activity
	 */
	public static void installTracker(String resourceName, String sourceName) {
		if (logger == null) {
			createTracker(sourceName);
			List<GarbageCollectorMXBean> gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
			for (GarbageCollectorMXBean gcbean : gcbeans) {
				NotificationEmitter emitter = (NotificationEmitter) gcbean;
				NotificationListener listener = new GCNotificationListener(logger, resourceName);
				emitter.addNotificationListener(listener, null, null);
			}
		}
	}

	/**
	 * Entry point to be loaded as -javaagent:jarpath[=res-name,source-name]
	 * Example: -javaagent:tnt4j-stream-gc.jar
	 * 
	 * @param options parameters if any
	 * @param inst instrumentation handle
	 */
	public static void premain(String options, Instrumentation inst) throws IOException {
		if (Utils.isEmpty(options)) {
			GCTracker.installTracker();
		} else {
			String [] args = options.split(",");
			if (args.length < 2) {
				GCTracker.installTracker(args[0]);
			} else {
				GCTracker.installTracker(args[0], args[1]);
			}
		}
		System.out.println("GCTracker: options=" + options + ", gc.tracker=" + GCTracker.getTracker().getSource());
	}
}