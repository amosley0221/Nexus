// Companion -> launcher. Mirrors the Google callback, forwarded across the
// companion process boundary.
package com.nexus.companion;

oneway interface INexusOverlayCallback {
    /** Overlay drag progress, 0f (hidden) to 1f (fully open). */
    void overlayScrollChanged(float progress);

    /**
     * Overlay state bitmask, forwarded verbatim from the Google app.
     * Bit 0 = attached and holding content.
     */
    void overlayStatusChanged(int status);

    /**
     * The companion's own connection state, which the Google callback has no
     * equivalent for: true once the companion is bound to the Google app and
     * the overlay is usable, false when that binding drops.
     */
    void companionStateChanged(boolean connected, String detail);
}
