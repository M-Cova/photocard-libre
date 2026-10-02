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

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    defaultConfig {
        applicationId = "org.photocardlibre.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "0.1-beta.8"
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

val fdroidOfflineWheelsProperty = providers.gradleProperty("photocardFdroidOfflineWheels").orNull
check(fdroidOfflineWheelsProperty == null || fdroidOfflineWheelsProperty in listOf("true", "false")) {
    "photocardFdroidOfflineWheels must be either true or false"
}
val useFdroidOfflineWheels = fdroidOfflineWheelsProperty == "true"
val fdroidRequirementsFile = rootProject.file("fdroid/requirements.txt")
val fdroidWheelHashesFile = rootProject.file("fdroid/wheel-hashes.sha256")
val fdroidWheelsDirectory = rootProject.file("fdroid/wheels")

if (useFdroidOfflineWheels) {
    check(fdroidRequirementsFile.isFile) {
        "F-Droid offline wheel mode requires ${fdroidRequirementsFile.absolutePath}"
    }
    check(fdroidWheelHashesFile.isFile) {
        "F-Droid offline wheel mode requires ${fdroidWheelHashesFile.absolutePath}"
    }
    check(fdroidWheelsDirectory.isDirectory) {
        "F-Droid offline wheel mode requires ${fdroidWheelsDirectory.absolutePath}. " +
            "Run fdroid/fetch-wheels.sh fdroid/wheels first."
    }

    val expectedWheelNames = fdroidWheelHashesFile.readLines()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { line -> line.substringAfter("  ", missingDelimiterValue = "") }
    check(expectedWheelNames.isNotEmpty() && expectedWheelNames.none(String::isEmpty)) {
        "Invalid F-Droid wheel hash manifest: ${fdroidWheelHashesFile.absolutePath}"
    }
    val missingWheelNames = expectedWheelNames.filterNot { fdroidWheelsDirectory.resolve(it).isFile }
    check(missingWheelNames.isEmpty()) {
        "F-Droid offline wheel mode is missing: ${missingWheelNames.joinToString()}. " +
            "Run fdroid/fetch-wheels.sh fdroid/wheels first."
    }
}

chaquopy {
    defaultConfig {
        version = "3.13"
        buildPython("python3.13")
        pip {
            if (useFdroidOfflineWheels) {
                options(
                    "--no-index",
                    "--find-links", fdroidWheelsDirectory.absolutePath,
                    "--require-hashes",
                )
                install("-r", fdroidRequirementsFile.absolutePath)
            } else {
                install("Pillow==11.0.0")
                install("reportlab==5.0.1")
                install("charset-normalizer==3.5.1")
                install("chaquopy-freetype==2.9.1")
                install("chaquopy-libjpeg==1.5.3")
            }
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
