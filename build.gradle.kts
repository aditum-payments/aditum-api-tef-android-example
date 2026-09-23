import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// token do integrador: -Ppaytrial.token, variável PAYTRIAL_TOKEN ou gradle.properties — nunca no código
val partnerToken = providers.gradleProperty("paytrial.token").orNull ?: System.getenv("PAYTRIAL_TOKEN") ?: ""
// keystore opcional (a chave do fabricante do terminal): -Ppaytrial.keystore=/caminho.jks + senhas por variável
val keystorePath = providers.gradleProperty("paytrial.keystore").orNull
val gitHash = runCatching {
    providers.exec { commandLine("git", "rev-parse", "--short=9", "HEAD"); isIgnoreExitValue = true }.standardOutput.asText.get().trim()
}.getOrDefault("").ifBlank { "local" }

android {
    namespace = "br.com.aditum.paytrial"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "br.com.aditum.paytrial"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 300
        versionName = "3.0.0"
        buildConfigField("String", "PARTNER_TOKEN", "\"$partnerToken\"")
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        vectorDrawables.useSupportLibrary = true
    }
    signingConfigs {
        if (keystorePath != null) create("terminal") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("PAYTRIAL_STORE_PASSWORD")
            keyAlias = System.getenv("PAYTRIAL_KEY_ALIAS")
            keyPassword = System.getenv("PAYTRIAL_KEY_PASSWORD")
        }
    }
    buildTypes {
        debug { if (keystorePath != null) signingConfig = signingConfigs.getByName("terminal") }
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePath != null) signingConfigs.getByName("terminal") else signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true; buildConfig = true; aidl = false }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { jvmToolchain(17) }

// nome do artefato como nos demais APKs da Aditum: AditumPayTrial-<versão>+<hash>-<tipo>.apk em build/dist/
androidComponents {
    onVariants { v ->
        val name = "AditumPayTrial-3.0.0+$gitHash-${v.buildType}.apk"
        val dist = tasks.register<Copy>("dist${v.name.replaceFirstChar { it.uppercase() }}") {
            from(v.artifacts.get(com.android.build.api.artifact.SingleArtifact.APK)) { include("*.apk"); rename { name } }
            into(layout.buildDirectory.dir("dist"))
        }
        tasks.matching { it.name == "assemble${v.name.replaceFirstChar { c -> c.uppercase() }}" }.configureEach { finalizedBy(dist) }
    }
}

dependencies {
    // o contrato App2App, como um integrador recebe: o .aar da Aditum dentro do app
    implementation(files("libs/AditumSdkIntegration-A2A-3.0.0.aar"))
    implementation(libs.kotlin.parcelize.runtime)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.json)
}
