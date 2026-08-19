package com.nexus.launcher.integration.notifications

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nexus.launcher.domain.NotificationCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Feeds the pull-down-at-top overscroll cards. The service is optional: until the
 * user grants notification access in system settings the flow just stays empty
 * and the overscroll shows an enable prompt instead of cards.
 */
class NexusNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        connected = true
        publish()
    }

    override fun onListenerDisconnected() {
        connected = false
        _cards.value = emptyList()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = publish()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = publish()

    private fun publish() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        _cards.value = active
            .asSequence()
            .filter { it.isClearable }
            .filterNot { it.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0 }
            .sortedByDescending { it.postTime }
            .map { sbn -> sbn.toCard(this) }
            .filter { it.title.isNotBlank() || it.text.isNotBlank() }
            .distinctBy { it.packageName + it.title + it.text }
            .take(12)
            .toList()
    }

    private fun StatusBarNotification.toCard(context: Context): NotificationCard {
        val extras = notification.extras
        val pm = context.packageManager
        val appName = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        val icon = runCatching { notification.smallIcon?.loadDrawable(context) }.getOrNull()
            ?: runCatching { pm.getApplicationIcon(packageName) }.getOrNull()

        return NotificationCard(
            key = key,
            packageName = packageName,
            appName = appName,
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty(),
            whenMillis = if (notification.`when` > 0) notification.`when` else postTime,
            icon = icon,
        )
    }

    /** Dismiss a card from the launcher, mirroring a swipe in the system shade. */
    fun dismiss(key: String) {
        runCatching { cancelNotification(key) }
        publish()
    }

    companion object {
        private val _cards = MutableStateFlow<List<NotificationCard>>(emptyList())
        val cards: StateFlow<List<NotificationCard>> = _cards.asStateFlow()

        @Volatile
        var connected: Boolean = false
            private set

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners",
            ).orEmpty()
            val us = ComponentName(context, NexusNotificationListener::class.java)
            return flat.split(':').any {
                ComponentName.unflattenFromString(it)?.packageName == us.packageName
            }
        }

        fun requestAccess(context: Context) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
