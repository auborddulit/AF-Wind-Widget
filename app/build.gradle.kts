import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    id("com.android.application")
}

fun getKey(key: String): String {
    val fl = rootProject.file("key.properties")
    if (fl.exists()) {
        val properties = Properties()
        properties.load(FileInputStream(fl))
        return properties.getProperty(key)
    } else {
        throw FileNotFoundException()
    }
}

// Build number from GitHub Actions, so every new APK installs as an update
val buildNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "0").toInt()

// Signing key: kept in signing/ so every build is signed the same way (updates install over the old version)
val signingProperties = Properties().apply {
    val f = rootProject.file("signing/keystore.properties")
    if (f.exists()) FileInputStream(f).use { load(it) }
}
val signingKeystoreFile = rootProject.file("signing/afwind-release.keystore")

android {
    namespace = "net.gitsaibot.af"
    compileSdk = 36

    defaultConfig {
        applicationId = "net.auborddulit.afwind"
        minSdk = 29
        targetSdk = 36
        versionCode = 100 + buildNumber
        versionName = "1.0.$buildNumber"
        buildConfigField("String", "USER_AGENT", "\"" + getKey("user_agent") + "\"")
        buildConfigField("String", "API_KEY", "\"" + getKey("apiKey") + "\"")
    }

    signingConfigs {
        create("release") {
            if (signingKeystoreFile.exists()) {
                storeFile = signingKeystoreFile
                storePassword = signingProperties.getProperty("storePassword")
                keyAlias = signingProperties.getProperty("keyAlias")
                keyPassword = signingProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = if (signingKeystoreFile.exists()) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
            // Remove unused library code and resources: much smaller APK
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.txt"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    testOptions {
        // Android classes (Log, Uri...) return default values in plain JVM unit tests
        unitTests.isReturnDefaultValues = true
    }

    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
}

dependencies {
    implementation("androidx.core:core:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata:2.10.0")
    implementation("androidx.activity:activity:1.12.2")
    implementation("androidx.work:work-runtime:2.11.0")

    testImplementation("junit:junit:4.13.2")
}
