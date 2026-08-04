package com.rekluzlabs.temporatic.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object ShareHelper {
    fun shareUri(context: Context, uri: Uri) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(intent, "Share Screenshot").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

}
