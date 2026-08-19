plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.nexus.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexus.launcher"
        minSdk = 30
        targetSdk = 35
        // CI passes its run number so successive builds are distinguishable on
        // the phone and never look like a downgrade.
        versionCode = (System.getenv("NEXUS_VERSION_CODE") ?: "1").toInt()
        versionName = "0.1.0"
        vectorDrawables.useSupportLibrary = true
    }


    /**
     * One stable key for every build. Without this each machine — and every
     * ephemeral CI runner — would auto-generate its own debug keystore, so each
     * APK would carry a different signature and Android would refuse to install
     * it over the last one. It also keeps the launcher and the companion
     * signature-matched, which the overlay bridge requires.
     *
     * Set the NEXUS_KEYSTORE_* environment variables to sign with a private key
     * instead of the committed one.
     */
    signingConfigs {
        create("nexus") {
            val keystorePath = System.getenv("NEXUS_KEYSTORE_PATH")
                ?: providers.gradleProperty("nexusKeystoreFile").get()
            storeFile = rootProject.file(keystorePath)
            storePassword = System.getenv("NEXUS_KEYSTORE_PASSWORD")
                ?: providers.gradleProperty("nexusKeystorePassword").get()
            keyAlias = System.getenv("NEXUS_KEY_ALIAS")
                ?: providers.gradleProperty("nexusKeyAlias").get()
            keyPassword = System.getenv("NEXUS_KEY_PASSWORD")
                ?: providers.gradleProperty("nexusKeyPassword").get()
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("nexus")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("nexus")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        aidl = true
    }

    sourceSets {
        getByName("main") {
            // Shared with :companion so both sides compile the identical
            // interface definition.
            aidl.srcDirs("src/main/aidl", "../shared/aidl")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.window)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.palette)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.serialization.json)
}
