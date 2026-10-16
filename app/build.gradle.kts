import java.security.KeyStore
import java.security.PrivateKey

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Never fall back to the debug key. Only release tasks require these inputs.
val releaseSigningInputs = listOf(
    "AFM_RELEASE_KEYSTORE_PATH", "AFM_RELEASE_STORE_PASSWORD",
    "AFM_RELEASE_KEY_ALIAS", "AFM_RELEASE_KEY_PASSWORD",
).associateWith { providers.environmentVariable(it).orNull }

val validateReleaseSigning by tasks.registering {
    notCompatibleWithConfigurationCache("Signing credentials must not be serialized")
    doLast {
        val missing = releaseSigningInputs.filterValues { it.isNullOrBlank() }.keys
        check(missing.isEmpty()) { "Release signing requires: ${missing.joinToString()}. See docs/RELEASING.md" }
        val path = file(releaseSigningInputs.getValue("AFM_RELEASE_KEYSTORE_PATH")!!)
        check(path.isFile) { "Release keystore file does not exist. See docs/RELEASING.md" }
        try {
            val store = KeyStore.getInstance("PKCS12")
            path.inputStream().use {
                store.load(it, releaseSigningInputs.getValue("AFM_RELEASE_STORE_PASSWORD")!!.toCharArray())
            }
            val key = store.getKey(
                releaseSigningInputs.getValue("AFM_RELEASE_KEY_ALIAS"),
                releaseSigningInputs.getValue("AFM_RELEASE_KEY_PASSWORD")!!.toCharArray(),
            )
            check(key is PrivateKey)
        } catch (_: Exception) {
            // Do not propagate provider exceptions containing credential details.
            error("Cannot unlock Release private key; check PKCS12 file, alias and passwords. See docs/RELEASING.md")
        }
    }
}

android {
    namespace = "dev.milkei.audiofocusmixer"
    compileSdk = 36
    defaultConfig {
        applicationId = "dev.milkei.audiofocusmixer"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("release") {
            storeFile = releaseSigningInputs["AFM_RELEASE_KEYSTORE_PATH"]?.let { file(it) }
            storeType = "PKCS12"
            storePassword = releaseSigningInputs["AFM_RELEASE_STORE_PASSWORD"]
            keyAlias = releaseSigningInputs["AFM_RELEASE_KEY_ALIAS"]
            keyPassword = releaseSigningInputs["AFM_RELEASE_KEY_PASSWORD"]
            // API 26+ supports v2; v3 also supports later Android versions.
            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = true
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures {
        compose = true
        aidl = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(validateReleaseSigning)
}
dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
}
