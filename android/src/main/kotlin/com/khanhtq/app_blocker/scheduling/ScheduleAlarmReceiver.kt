package com.khanhtq.app_blocker.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.khanhtq.app_blocker.blocking.BlockingServiceManager
import com.khanhtq.app_blocker.debug.DebugEventBuffer
import com.khanhtq.app_blocker.event.BlockEventStreamHandler

class ScheduleAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val scheduleId = intent.getStringExtra(ScheduleManager.EXTRA_SCHEDULE_ID) ?: return
        val appIdentifiers = intent.getStringArrayListExtra(ScheduleManager.EXTRA_APP_IDENTIFIERS)
            ?: return

        val blockingServiceManager = BlockingServiceManager(context)
        val scheduleManager = ScheduleManager(context)

        val timestamp = System.currentTimeMillis()

        when (action) {
            ScheduleManager.ACTION_SCHEDULE_START -> {
                Log.i("ScheduleAlarmReceiver", "ALARM START scheduleId=$scheduleId apps=${appIdentifiers.size}")
                DebugEventBuffer.append(context, "alarmFiredStart", mapOf(
                    "scheduleId" to scheduleId,
                    "appIdentifiers" to appIdentifiers,
                ))
                blockingServiceManager.startBlocking(appIdentifiers)
                BlockEventStreamHandler.sendEvent(
                    mapOf(
                        "type" to "scheduleActivated",
                        "scheduleId" to scheduleId,
                        "appIdentifiers" to appIdentifiers,
                        "timestamp" to timestamp,
                    )
                )
            }

            ScheduleManager.ACTION_SCHEDULE_END -> {
                Log.i("ScheduleAlarmReceiver", "ALARM END scheduleId=$scheduleId")
                DebugEventBuffer.append(context, "alarmFiredEnd", mapOf(
                    "scheduleId" to scheduleId,
                    "appIdentifiers" to appIdentifiers,
                ))
                blockingServiceManager.stopBlockingApps(appIdentifiers.toList())
                BlockEventStreamHandler.sendEvent(
                    mapOf(
                        "type" to "scheduleDeactivated",
                        "scheduleId" to scheduleId,
                        "timestamp" to timestamp,
                    )
                )

                if (scheduleManager.findSchedule(scheduleId)?.scheduleDate != null) {
                    scheduleManager.removeSchedule(scheduleId)
                }
            }
        }
    }
}
