import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
}

val releaseSigningProperties = Properties()
val releaseSigningPropertiesFile = rootProject.file("keystore.properties")
if (releaseSigningPropertiesFile.isFile) {
    releaseSigningPropertiesFile.inputStream().use(releaseSigningProperties::load)
}

fun releaseSigningValue(propertyName: String, environmentName: String): String? =
    System.getenv(environmentName)?.takeIf(String::isNotBlank)
        ?: releaseSigningProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)

val releaseSigningValues = mapOf(
    "storeFile" to releaseSigningValue("storeFile", "PHOTOCARD_RELEASE_KEYSTORE"),
    "storePassword" to releaseSigningValue("storePassword", "PHOTOCARD_RELEASE_STORE_PASSWORD"),
    "keyAlias" to releaseSigningValue("keyAlias", "PHOTOCARD_RELEASE_KEY_ALIAS"),
    "keyPassword" to releaseSigningValue("keyPassword", "PHOTOCARD_RELEASE_KEY_PASSWORD"),
)
val releaseSigningIsConfigured = releaseSigningValues.values.all { it != null }
check(releaseSigningValues.values.none { it != null } || releaseSigningIsConfigured) {
    "Release signing is only partially configured. Provide all four values via " +
        "keystore.properties or PHOTOCARD_RELEASE_* environment variables."
}

base {
    archivesName.set("PhotoCard-Libre")
}

android {
    namespace = "org.photocardlibre.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.photocardlibre.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
        versionName = "0.1-beta.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        if (releaseSigningIsConfigured) {
            create("release") {
                storeFile = rootProject.file(releaseSigningValues.getValue("storeFile")!!)
                storePassword = releaseSigningValues.getValue("storePassword")
                keyAlias = releaseSigningValues.getValue("keyAlias")
                keyPassword = releaseSigningValues.getValue("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isDebuggable = false
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
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
        buildConfig = true
    }
    bundle {
        language {
            enableSplit = false
        }
    }
}

chaquopy {
    defaultConfig {
        version = "3.12"
        buildPython("python3.12")
        pip {
            install("Pillow==11.0.0")
            install("reportlab==5.0.1")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.01.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
