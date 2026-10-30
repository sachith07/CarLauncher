package com.vezel.launcher

import android.app.Application
import android.util.Log
import kotlin.system.exitProcess

/**
 * Lightweight crash logging. Does not force HOME role or auto-restart loops.
 * Process death recovery relies on Android relaunch + SharedPreferences restore.
 */
class VezelApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught on ${thread.name}: ${throwable.message}", throwable)
            previous?.uncaughtException(thread, throwable)
                ?: run {
                    // Avoid silent hang if no prior handler.
                    exitProcess(10)
                }
        }
        Log.i(TAG, "VezelApp created — prefs survive process death; see docs/home-fallback.md")
    }

    companion object {
        private const val TAG = "VezelLauncher"
    }
}
