// The Google app's launcher-overlay interface — the service behind the Discover
// feed that sits to the left of the home screen.
//
// This is a client-side declaration of an interface implemented by the Google
// app. The package, interface name, method order, and signatures all form the
// binder contract, so nothing here may be reordered or renamed. Method slots
// that exist in the contract but that Nexus never calls are kept as no-ops so
// the transaction codes of everything after them stay correct.
package com.google.android.libraries.launcherclient;

import com.google.android.libraries.launcherclient.ILauncherOverlayCallback;

interface ILauncherOverlay {
    /** Called once when the user starts dragging toward the overlay. */
    oneway void startScroll();

    /** Drag progress, 0f (hidden) to 1f (fully open). */
    oneway void onScroll(float progress);

    /** Called when the drag ends; the overlay settles open or closed itself. */
    oneway void endScroll();

    /**
     * Attaches the overlay to the launcher's window. Superseded by
     * windowAttached2 on newer Google app builds.
     */
    oneway void windowAttached(in android.view.WindowManager.LayoutParams attrs,
            ILauncherOverlayCallback callback, int options);

    oneway void windowDetached(boolean isChangingConfigurations);

    oneway void closeOverlay(int options);

    oneway void onPause();

    oneway void onResume();

    oneway void openOverlay(int options);

    oneway void requestVoiceDetection(boolean start);

    String getVoiceSearchLanguage();

    boolean isVoiceDetectionRunning();

    /** True once the Google app has a feed ready to render. */
    boolean hasOverlayContent();

    /**
     * Current attach call. The bundle carries the launcher's window attributes,
     * its configuration, and the client option bits.
     */
    oneway void windowAttached2(in android.os.Bundle bundle, ILauncherOverlayCallback callback);

    /** Unused contract slot; kept so later transaction codes stay aligned. */
    oneway void unusedMethod();

    /** Bitmask of the launcher activity's started/resumed state. */
    oneway void setActivityState(int flags);

    boolean startSearch(in byte[] data, in android.os.Bundle bundle);
}
