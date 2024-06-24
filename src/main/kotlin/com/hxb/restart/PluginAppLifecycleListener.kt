package com.hxb.restart

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.components.service

class PluginAppLifecycleListener : AppLifecycleListener {

    override fun appStarted() {
        val applicationService = service<StartService>()
        applicationService.start();
    }

    override fun appClosing() {
        val applicationService = service<StartService>()
        applicationService.stop();
    }
}