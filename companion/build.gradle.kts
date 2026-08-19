plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.nexus.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nexus.companion"
        minSdk = 30
        targetSdk = 35
        versionCode = (System.getenv("NEXUS_VERSION_CODE") ?: "1").toInt()
        versionName = "0.1.0"
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
            signingConfig = signingConfigs.getByName("nexus")
        }
        release {
            isMinifyEnabled = false
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
            // The launcher-facing interface lives in one place and is compiled
            // into both APKs, so the two can never drift out of contract.
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
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
}
