package com.rekluzlabs.temporatic.processing

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundAppTracker @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun getForegroundApp(): String? {
        if (!hasUsageStatsPermission()) {
            Log.d("ForegroundAppTracker", "Usage stats permission not granted")
            return null
        }

        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val endTime = System.currentTimeMillis()
            val beginTime = endTime - 5000

            val stats = usm.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                beginTime,
                endTime
            )

            val recent = stats
                .filter { it.packageName != context.packageName }
                .sortedByDescending { it.lastTimeUsed }
                .firstOrNull()

            recent?.packageName
        } catch (e: Exception) {
            Log.e("ForegroundAppTracker", "Failed to get foreground app", e)
            null
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }
}
