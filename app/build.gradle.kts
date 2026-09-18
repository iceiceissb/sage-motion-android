import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.android.compose.screenshot")
}

val sageLocalProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use(::load)
}

fun sageConfigValue(name: String): String =
    providers.environmentVariable(name).orNull
        ?: sageLocalProperties.getProperty(name, "")

fun quotedBuildConfig(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val amapApiKey = sageConfigValue("AMAP_API_KEY")
val amapStyleId = sageConfigValue("AMAP_STYLE_ID")
val sageAgentBackendUrl = sageConfigValue("SAGE_AGENT_BACKEND_URL")
val sageAgentClientToken = sageConfigValue("SAGE_AGENT_CLIENT_TOKEN")

android {
    namespace = "cn.tsinghua.sagemotion"
    compileSdk = 35

    flavorDimensions += "speech"

    defaultConfig {
        applicationId = "cn.tsinghua.sagemotion"
        minSdk = 26
        targetSdk = 35
        versionCode = 29
        versionName = "1.19.0-agent-deployment"

        manifestPlaceholders["AMAP_API_KEY"] = amapApiKey
        buildConfigField("boolean", "AMAP_API_KEY_CONFIGURED", amapApiKey.isNotBlank().toString())
        buildConfigField("String", "AMAP_STYLE_ID", quotedBuildConfig(amapStyleId))
        buildConfigField("String", "SAGE_AGENT_BACKEND_URL", quotedBuildConfig(sageAgentBackendUrl))
        buildConfigField("String", "SAGE_AGENT_CLIENT_TOKEN", quotedBuildConfig(sageAgentClientToken))

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    productFlavors {
        create("slim") {
            dimension = "speech"
            versionNameSuffix = "-slim"
            buildConfigField("boolean", "BUNDLED_OFFLINE_SPEECH", "false")
            ndk {
                // The recommended downloadable APK targets current 64-bit Android phones.
                // Build the full flavor when 32-bit device support and bundled Vosk are required.
                abiFilters += "arm64-v8a"
            }
        }
        create("full") {
            dimension = "speech"
            buildConfigField("boolean", "BUNDLED_OFFLINE_SPEECH", "true")
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a")
            }
        }
    }

    buildTypes {
        release {
            // This is an installable research build, signed with the same local debug key as
            // earlier APKs so it can upgrade them. Use a private release key for public stores.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    experimentalProperties["android.experimental.enableScreenshotTest"] = true
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.mlkit:image-labeling:17.0.9")
    implementation("com.amap.api:3dmap-location-search:10.1.200_loc6.4.9_sea9.7.4")

    // Only the full flavor carries the Vosk runtime and its 65 MiB Chinese model assets.
    add("fullImplementation", "net.java.dev.jna:jna:5.18.1@aar")
    add("fullImplementation", "com.alphacephei:vosk-android:0.3.75@aar")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    screenshotTestImplementation("androidx.compose.ui:ui-tooling")
}
