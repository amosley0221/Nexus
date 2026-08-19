// Binder callback the Google app uses to report overlay state back to the client.
//
// The package and interface name below are part of the binder descriptor string
// that the Google app matches against, so neither may be renamed.
package com.google.android.libraries.launcherclient;

oneway interface ILauncherOverlayCallback {
    /**
     * Reports how far the overlay has been dragged in, 0f (hidden) to 1f (fully
     * open). Fired while the user drags and while the overlay settles.
     */
    void overlayScrollChanged(float progress);

    /**
     * Bitmask of overlay state. Bit 0 set means the overlay is attached and has
     * content to show; see NexusOverlayService for the full breakdown.
     */
    void overlayStatusChanged(int status);
}
