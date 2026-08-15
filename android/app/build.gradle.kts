import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.prontuario.glasses"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.prontuario.glasses"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        // EM DISPUTA (MEMORY.md §4.1): vídeo de segurança desligado por padrão até decisão do time.
        buildConfigField("boolean", "SECURITY_VIDEO_DEFAULT", "false")
    }

    buildFeatures {
        buildConfig = true
    }

    flavorDimensions += "device"
    productFlavors {
        // sim: sem dependência do DAT; compila e roda sem token/óculos (AND-07, PRG-01).
        create("sim") {
            dimension = "device"
            applicationIdSuffix = ".sim"
            versionNameSuffix = "-sim"
        }
        // dat: integração real DAT 0.9.0; requer github_token e credenciais do Wearables Developer Center.
        create("dat") {
            dimension = "device"
            manifestPlaceholders["mwdat_application_id"] =
                (project.findProperty("mwdatAppId") as String?) ?: "SET_ME"
            manifestPlaceholders["mwdat_client_token"] =
                (project.findProperty("mwdatClientToken") as String?) ?: "SET_ME"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.coroutines.android)
    implementation(libs.vosk.android)

    "datImplementation"(libs.mwdat.core)
    "datImplementation"(libs.mwdat.camera)
    "datImplementation"(libs.mwdat.mockdevice)

    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.json.jvm)
}
