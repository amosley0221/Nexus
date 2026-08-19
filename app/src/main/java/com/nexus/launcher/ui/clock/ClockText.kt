package com.nexus.launcher.ui.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The clock split into its parts rather than one formatted string: Home stacks
 * the hour over the minutes in different colours, so it needs them separately.
 * [meridiem] is empty when the device is on 24-hour time.
 */
@Immutable
data class ClockText(
    val hour: String,
    val minute: String,
    val meridiem: String,
    val date: String,
)

/**
 * Live clock and date line. Driven by the system's ACTION_TIME_TICK broadcast
 * rather than a polling loop, so it updates exactly on the minute and costs
 * nothing while the screen is off.
 */
@Composable
fun rememberClockText(): ClockText {
    val context = LocalContext.current
    var state by remember { mutableStateOf(buildClockText(context)) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                state = buildClockText(context)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    return state
}

private fun buildClockText(context: Context): ClockText {
    val now = Date()
    val locale = Locale.getDefault()
    val is24Hour = DateFormat.is24HourFormat(context)

    val hour = SimpleDateFormat(if (is24Hour) "HH" else "h", locale).format(now)
    val minute = SimpleDateFormat("mm", locale).format(now)
    val meridiem = if (is24Hour) "" else SimpleDateFormat("a", locale).format(now)
    val day = SimpleDateFormat("EEE, MMM d", locale).format(now)

    val battery = batteryPercent(context)
    val date = buildString {
        append(day)
        if (battery != null) {
            append("  ")
            append("$battery%")
        }
    }
    return ClockText(hour = hour, minute = minute, meridiem = meridiem, date = date)
}

private fun batteryPercent(context: Context): Int? {
    val manager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
    val level = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    return level.takeIf { it in 0..100 }
}
