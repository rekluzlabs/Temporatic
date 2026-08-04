package com.rekluzlabs.temporatic.processing

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppNameResolver @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun getAppLabel(packageName: String?): String? {
        if (packageName == null) return null
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w("AppNameResolver", "Package not found: $packageName")
            packageName.substringAfterLast('.')
        }
    }
}
