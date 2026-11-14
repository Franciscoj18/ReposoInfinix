plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.reposo.infinix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.reposo.infinix"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        create("reposo") {
            storeFile = file("reposo.keystore")
            storePassword = "reposo123"
            keyAlias = "reposo"
            keyPassword = "reposo123"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("reposo")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("reposo")
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
        buildConfig = false
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

// Nombre del archivo final: ReposoInfinix.apk
android.applicationVariants.all {
    outputs.all {
        (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
            if (buildType.name == "release") "ReposoInfinix.apk" else "ReposoInfinix-debug.apk"
    }
}
