package com.vitalsense.app

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.multidex.MultiDex
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VitalSenseApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
            Log.d("VitalSenseFirebase", "FirebaseApp initialized successfully in VitalSenseApp")
        } catch (e: Exception) {
            Log.e("VitalSenseFirebase", "FirebaseApp initialization warning: ${e.message}", e)
        }

        try {
            com.vitalsense.app.core.sync.SyncManager(this).schedulePeriodicSync()
        } catch (e: Exception) {
            Log.e("VitalSenseApp", "SyncManager periodic sync scheduling error: ${e.message}", e)
        }
    }
}
