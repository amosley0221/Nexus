package com.nexus.launcher.integration.apps

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Biometric gate for locked apps. This only guards launches that go through the
 * Nexus UI — Android gives a third-party launcher no way to intercept a launch
 * from the recents list or another launcher, which the settings screen says out loud.
 */
object AppLock {

    private const val AUTHENTICATORS =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun canAuthenticate(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    /**
     * Prompts for fingerprint with a PIN/pattern fallback. If the device has
     * neither enrolled, [onSuccess] runs directly so a locked app never becomes
     * permanently unreachable.
     */
    fun authenticate(
        activity: FragmentActivity,
        appLabel: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit = {},
    ) {
        if (!canAuthenticate(activity)) {
            onSuccess()
            return
        }

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) =
                    onSuccess()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) =
                    onFailure()
            },
        )

        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock $appLabel")
                .setSubtitle("Locked by Nexus")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
        )
    }
}
