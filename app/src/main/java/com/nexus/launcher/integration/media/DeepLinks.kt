package com.nexus.launcher.integration.media

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Deep links into the media apps the hubs shortcut to. Every call falls back to
 * the app's launcher intent, then to a web URL, so a tap always does something
 * even when the target app is not installed.
 */
object DeepLinks {

    fun twitchStream(context: Context, channel: String) = open(
        context,
        primary = "twitch://stream/$channel",
        packageName = "tv.twitch.android.app",
        web = "https://twitch.tv/$channel",
    )

    fun appleMusic(context: Context, path: String = "") = open(
        context,
        primary = "musics://music.apple.com/$path",
        packageName = "com.apple.android.music",
        web = "https://music.apple.com/$path",
    )

    fun plex(context: Context, key: String? = null) = open(
        context,
        primary = key?.let { "plex://$it" } ?: "plex://",
        packageName = "com.plexapp.android",
        web = "https://app.plex.tv",
    )

    fun netflix(context: Context) = open(
        context,
        primary = "nflx://",
        packageName = "com.netflix.mediaclient",
        web = "https://netflix.com",
    )

    fun disneyPlus(context: Context) = open(
        context,
        primary = "disneyplus://",
        packageName = "com.disney.disneyplus",
        web = "https://disneyplus.com",
    )

    fun primeVideo(context: Context) = open(
        context,
        primary = "primevideo://",
        packageName = "com.amazon.avod.thirdpartyclient",
        web = "https://primevideo.com",
    )

    fun web(context: Context, url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun isInstalled(context: Context, packageName: String): Boolean =
        runCatching { context.packageManager.getPackageInfo(packageName, 0) }.isSuccess

    private fun open(context: Context, primary: String, packageName: String, web: String) {
        val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse(primary))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (runCatching { context.startActivity(deepLink); true }.getOrDefault(false)) return

        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(launch); true }.getOrDefault(false)) return
        }

        web(context, web)
    }
}
