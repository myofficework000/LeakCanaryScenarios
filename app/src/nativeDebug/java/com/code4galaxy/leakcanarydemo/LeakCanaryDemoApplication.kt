package com.code4galaxy.leakcanarydemo

import android.app.Application
import leakcanary.LeakCanary

/**
 * Native LeakCanary viewer configuration for the teaching build only.
 *
 * LeakCanary's production-friendly default waits for five retained objects while an app is
 * visible. This demo deliberately lowers that threshold so a single leaking Activity gives a
 * repeatable result after it is destroyed and the dashboard is visible again.
 */
class LeakCanaryDemoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LeakCanary.config = LeakCanary.config.copy(retainedVisibleThreshold = 1)
    }
}
