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

public class GCTracker {
	/*
	 * Tracking logger instance where all GC tracking messages are recorded.
	 */
	private static TrackingLogger logger;

	protected static void createTracker() {
		try {
			logger = TrackingLogger.getInstance("org.tnt4j.stream.java.gc");
			logger.open();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static TrackingLogger getTracker() {
		return logger;
	}
	
	public static void installTracker() {
		if (logger == null) {
			createTracker();
			List<GarbageCollectorMXBean> gcbeans = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans();
			for (GarbageCollectorMXBean gcbean : gcbeans) {
				System.out.println("Tracking gc=" + gcbean.getName());
				NotificationEmitter emitter = (NotificationEmitter) gcbean;
				NotificationListener listener = new GCNotificationListener(logger);
				emitter.addNotificationListener(listener, null, null);
			}
		}
	}

	/**
	 * Entry point to be loaded as -javaagent:jarpath
	 * Example: -javaagent:tnt4j-stream-gc.jar
	 * 
	 * @param options parameters if any
	 * @param inst instrumentation handle
	 */
	public static void premain(String options, Instrumentation inst) throws IOException {
		GCTracker.installTracker();
		System.out.println("GCTracker: gc.tracker=" + GCTracker.getTracker().getSource());
	}
}