package com.hxb.restart
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent


class MyGitPushListener(val project: Project) : NotificationListener {
    val logger = com.intellij.openapi.diagnostic.Logger.getInstance(MyGitPushListener::class.java)
    override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
        logger.error("MyGitPushListener"+notification.javaClass.name)
    }
}