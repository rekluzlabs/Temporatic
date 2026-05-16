package com.rekluzlabs.temporatic.utils

import android.os.Build
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    fun getCurrentTimestamp(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        return format.format(Date())
    }

    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} | ${Build.FINGERPRINT.take(20)}..."
    }
    
    fun getFilenameTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
    }
}
