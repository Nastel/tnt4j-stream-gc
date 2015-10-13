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

import java.lang.management.MemoryUsage;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import com.nastel.jkool.tnt4j.TrackingLogger;
import com.nastel.jkool.tnt4j.core.OpLevel;
import com.nastel.jkool.tnt4j.core.OpType;
import com.nastel.jkool.tnt4j.core.Snapshot;
import com.nastel.jkool.tnt4j.core.ValueTypes;
import com.nastel.jkool.tnt4j.tracker.TrackingEvent;
import com.sun.management.GarbageCollectionNotificationInfo;

public class GCNotificationListener implements NotificationListener {
	long totalGcDuration = 0;
	TrackingLogger logger;

	public GCNotificationListener(TrackingLogger lg) {
	    this.logger = lg;
    }

	@Override
	public void handleNotification(Notification notification, Object handback) {
		System.out.println("GC Event gc=" + notification.getType() + ", msg=" + notification.getMessage());
		if (notification.getType().equals(GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION)) {
			GarbageCollectionNotificationInfo info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
			String msg = info.getGcAction() + ": id(" + info.getGcInfo().getId() + ") " + info.getGcName() + " cause("
			        + info.getGcCause() + ") duration.ms(" + info.getGcInfo().getDuration() + "); start-end times: "
			        + info.getGcInfo().getStartTime() + "-" + info.getGcInfo().getEndTime();
	
			TrackingEvent gcEvent = logger.newEvent(OpLevel.TRACE, OpType.CLEAR, info.getGcAction(), null, info.getGcName(), msg);
			gcEvent.stop(TimeUnit.MILLISECONDS.toMicros(info.getGcInfo().getDuration()));
			gcEvent.getOperation().setException(info.getGcCause());

			// Get the information about each memory space, and pretty print it
			Map<String, MemoryUsage> membefore = info.getGcInfo().getMemoryUsageBeforeGc();
			Map<String, MemoryUsage> mem = info.getGcInfo().getMemoryUsageAfterGc();
			for (Entry<String, MemoryUsage> entry : mem.entrySet()) {
				String name = entry.getKey();
				Snapshot memoryAfter = logger.newSnapshot(info.getGcName(), name + "-After");	
				MemoryUsage memAfter = entry.getValue();
				memoryAfter.add("memInit", memAfter.getInit(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
				memoryAfter.add("memCommit", memAfter.getCommitted(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
				memoryAfter.add("memMax", memAfter.getMax(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
				memoryAfter.add("memUsed", memAfter.getUsed(), ValueTypes.VALUE_TYPE_SIZE_BYTE);

				Snapshot memoryBefore = logger.newSnapshot(info.getGcName(), name + "-Before");
				MemoryUsage memBefore = membefore.get(name);
				memoryBefore.add("memInit", memBefore.getInit(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
				memoryBefore.add("memCommit", memBefore.getCommitted(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
				memoryBefore.add("memMax", memBefore.getMax(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
				memoryBefore.add("memUsed", memBefore.getUsed(), ValueTypes.VALUE_TYPE_SIZE_BYTE);
				long memUsage = ((memBefore.getUsed() * 100L)/ memBefore.getCommitted());
				long percent = ((memAfter.getUsed()  * 100L)/ memBefore.getCommitted()); // >100% when it gets expanded
				memoryBefore.add("memBeforeUsage", memUsage, ValueTypes.VALUE_TYPE_PERCENT);
				memoryAfter.add("memAfterUsage", percent, ValueTypes.VALUE_TYPE_PERCENT);
				gcEvent.getOperation().addSnapshot(memoryBefore);
				gcEvent.getOperation().addSnapshot(memoryAfter);
			}
			totalGcDuration += info.getGcInfo().getDuration();
			long percent = (totalGcDuration * 100L) / info.getGcInfo().getEndTime();
			gcEvent.getOperation().addProperty(logger.newProperty("gcId", info.getGcInfo().getId(), ValueTypes.VALUE_TYPE_COUNTER));
			gcEvent.getOperation().addProperty(logger.newProperty("totalGCDuration", totalGcDuration, ValueTypes.VALUE_TYPE_AGE_MSEC));
			gcEvent.getOperation().addProperty(logger.newProperty("totalGCOverhead", percent, ValueTypes.VALUE_TYPE_PERCENT));
			logger.tnt(gcEvent);
		}
	}
}
