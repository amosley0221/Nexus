package com.nexus.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * A launcher entry that opens Nexus Settings.
 *
 * It exists purely so "Nexus Settings" appears in the app list and in search;
 * it draws nothing, forwards to the single HOME activity, and finishes.
 */
class NexusSettingsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivity(
            Intent(this, NexusLauncherActivity::class.java)
                .setAction(NexusLauncherActivity.ACTION_OPEN_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
        overridePendingTransition(0, 0)
    }
}
