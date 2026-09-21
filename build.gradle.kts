import br.com.aditum.buildlogic.AditumSigning
import br.com.aditum.buildlogic.ArtifactNames
import br.com.aditum.buildlogic.Devices
import br.com.aditum.buildlogic.buildAxes
import br.com.aditum.buildlogic.requestedModel
import com.android.build.api.artifact.SingleArtifact

plugins {
    id("aditum.android.app")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

// O exemplo é instalado no terminal ao lado do serviço, então é assinado com a chave do modelo pedido em
// -Paditum.model (Positivo e Gertec exigem a do fabricante); sem modelo é o pacote genérico TefSdk (doc 14 §7.2).
val axes = buildAxes()
val spec = requestedModel()?.let { Devices.find(it) ?: error("aditum: ${Devices.PROPERTY}=$it não está na matriz") } ?: Devices.defaultOf("generic")

android {
    namespace = "br.com.aditum.tef.sdk.example"
    defaultConfig { applicationId = "br.com.aditum.paymentexample" }
    buildFeatures { compose = true }
    buildTypes { release { signingConfig = signingConfigs.getByName("debug") } }
    androidComponents {
        val signing = AditumSigning(project, this@android)
        onVariants { v ->
            if (v.buildType == "release") v.signingConfig?.setConfig(signing.forRelease(spec.signing, v.name))
            val fileName = ArtifactNames.exampleApk(axes, spec.token, v.buildType ?: "debug")
            val dist = tasks.register<Copy>("dist${v.name.replaceFirstChar { it.uppercase() }}") {
                group = "distribution"; description = "Copia o APK ${v.name} para build/dist/$fileName"
                from(v.artifacts.get(SingleArtifact.APK)) { include("*.apk") }; rename { fileName }; into(layout.buildDirectory.dir("dist"))
            }
            tasks.matching { it.name == "assemble${v.name.replaceFirstChar { c -> c.uppercase() }}" }.configureEach { finalizedBy(dist) }
        }
    }
}
dependencies {
    implementation(project(":sdk:integration"))
    implementation(libs.androidx.core.ktx); implementation(libs.androidx.activity.compose)
    implementation(compose.runtime); implementation(compose.foundation); implementation(compose.material3); implementation(compose.ui)
    androidTestImplementation(libs.androidx.test.runner); androidTestImplementation(libs.androidx.test.junit)
}
