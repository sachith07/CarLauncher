plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vezel.launcher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vezel.launcher"
        minSdk = 27
        targetSdk = 34
        versionCode = 7
        versionName = "0.7.0-dashboard-gui"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }

        // drawer flavor default; overridden by product flavors
        buildConfigField("boolean", "HOME_CAPABLE", "false")
    }

    flavorDimensions += "home"
    productFlavors {
        create("drawer") {
            dimension = "home"
            isDefault = true
            buildConfigField("boolean", "HOME_CAPABLE", "false")
        }
        create("homePilot") {
            dimension = "home"
            buildConfigField("boolean", "HOME_CAPABLE", "true")
        }
    }

    signingConfigs {
        // Release signing: copy keystore.properties.example → keystore.properties (gitignored).
        // Without that file, release falls back to the debug keystore.
        val keystorePropsFile = rootProject.file("keystore.properties")
        if (keystorePropsFile.exists()) {
            val props = mutableMapOf<String, String>()
            keystorePropsFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
                .forEach { line ->
                    val idx = line.indexOf('=')
                    props[line.substring(0, idx).trim()] = line.substring(idx + 1).trim()
                }
            create("release") {
                storeFile = rootProject.file(props.getValue("storeFile"))
                storePassword = props.getValue("storePassword")
                keyAlias = props.getValue("keyAlias")
                keyPassword = props.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Prefer release keystore when present; otherwise debug signing (documented OK).
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
}
