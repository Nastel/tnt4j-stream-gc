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
package com.jkoolcloud.tnt4j.stream.java.gc;

import java.lang.management.ManagementFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.jkoolcloud.tnt4j.TrackingLogger;
import com.jkoolcloud.tnt4j.core.OpLevel;
import com.jkoolcloud.tnt4j.core.OpType;
import com.jkoolcloud.tnt4j.utils.Utils;

/**
 * VM shutdown hook implementation that generates tracking events on UncaughtException as well as on JVM shutdown.
 * 
 * @version $Revision: 1 $
 * 
 */
public class VMShutdownHook implements Runnable, Thread.UncaughtExceptionHandler {

	AtomicLong lastError = new AtomicLong(0);
	TrackingLogger logger;

	public VMShutdownHook(TrackingLogger lg) {
		logger = lg;
	}

	@Override
	public void uncaughtException(Thread t, Throwable e) {
		long elapsed = lastError.get() > 0 ? (System.currentTimeMillis() - lastError.get()) : 0;
		logger.tnt(OpLevel.FATAL, OpType.EVENT, "uncaughtException-" + t.getName(), null,
				TimeUnit.MILLISECONDS.toMicros(elapsed), e.getMessage(), e);
	}

	@Override
	public void run() {
		long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
		logger.tnt(OpLevel.DEBUG, OpType.STOP, "vm-shutdown", null, uptime, Utils.getVMName() + " stopped, uptime={0}",
				uptime);
		flush();
	}

	private void flush() {
		try {
			logger.getEventSink().flush();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
