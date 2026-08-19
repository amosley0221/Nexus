// Launcher -> companion. A thin re-export of the Google overlay interface.
//
// Nexus cannot bind the Google app's overlay service itself: the Google app
// only serves clients it recognises, so the call has to originate from a
// separate companion package. The companion owns that binding and relays these
// calls to it.
package com.nexus.companion;

import com.nexus.companion.INexusOverlayCallback;

interface INexusOverlay {
    /**
     * Hands the launcher's window over to the overlay. [attrs] must be the
     * launcher activity window's attributes, including its window token — the
     * Google app draws the feed into a window parented to that token.
     *
     * Returns true when the attach reached the Google app.
     */
    boolean attachWindow(in android.view.WindowManager.LayoutParams attrs,
            in android.content.res.Configuration configuration,
            int clientOptions,
            INexusOverlayCallback callback);

    oneway void detachWindow(boolean isChangingConfigurations);

    oneway void startScroll();

    oneway void onScroll(float progress);

    oneway void endScroll();

    oneway void openOverlay(int options);

    oneway void closeOverlay(int options);

    /** Bitmask: bit 0 = activity started, bit 1 = activity resumed. */
    oneway void setActivityState(int flags);

    oneway void onLauncherPause();

    oneway void onLauncherResume();

    /** True once the Google app reports a feed ready to render. */
    boolean hasOverlayContent();

    /** True while the companion holds a live binding to the Google app. */
    boolean isConnected();

    /**
     * Human-readable reason the overlay is unavailable, or null when it is
     * working. Surfaced on the Discover page so a failure explains itself
     * instead of showing a blank feed.
     */
    String getUnavailableReason();
}
