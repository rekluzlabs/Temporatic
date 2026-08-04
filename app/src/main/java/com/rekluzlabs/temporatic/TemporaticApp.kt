package com.rekluzlabs.temporatic

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TemporaticApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i("TemporaticApp", "Application onCreate")
    }
}
