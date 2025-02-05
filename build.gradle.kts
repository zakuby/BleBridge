// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}



configurations.all {
    resolutionStrategy {
        // use 0.9.0 to fix crash on Android 11
        force("com.facebook.soloader:soloader:0.9.0")
    }
}