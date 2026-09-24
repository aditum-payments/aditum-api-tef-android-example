plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// token do integrador: -Ppaytrial.token, variável PAYTRIAL_TOKEN ou gradle.properties — nunca no código
val partnerToken = providers.gradleProperty("paytrial.token").orNull ?: System.getenv("PAYTRIAL_TOKEN") ?: ""
val gitHash = runCatching {
    providers.exec { commandLine("git", "rev-parse", "--short=9", "HEAD"); isIgnoreExitValue = true }.standardOutput.asText.get().trim()
}.getOrDefault("").ifBlank { "local" }
val appVersion = "3.0.0"

// ---------------------------------------------------------------------------------------------------------------
// Dispositivo e ambiente, como no APK do serviço (SmartPosTef-A2A-<D|P>-<MODELO>-<versão>+<hash>-<tipo>.apk):
//   -Ppaytrial.device=<MODELO ou apelido>   (ou PAYTRIAL_DEVICE)  escolhe a chave de assinatura do fabricante e o nome
//   -Ppaytrial.environment=development|production  (ou ENVIRONMENT)  D ou P no nome
// A tabela abaixo ESPELHA build-matrix/src/main/kotlin/br/com/aditum/buildlogic/Devices.kt do aditum-tef (fonte da
// verdade). Ela é copiada porque este app compila sozinho, fora do repositório; o `scripts/sync-example-aar.sh --check`
// (e o `build-all.sh --example`) acusa quando as duas divergem.
// ---------------------------------------------------------------------------------------------------------------
/** Chave de assinatura → arquivo `.jks` e alias (o alias não é segredo; senhas só por variável/propriedade). */
val signingKeys = mapOf(
    "aditum" to ("AditumApk-Signing.jks" to "aditumapk-signing"),
    "gertec-gpos700" to ("Development_GertecDeveloper_EnhancedAPP-GPOS700.jks" to "developmentgertecdeveloper_enhancedapp"),
    "gertec-gpos720" to ("AndroidDefaultDev_Enhanced-GPOS720.jks" to "androidenhanceddev"),
    "gertec-gpos760" to ("AndroidDefaultDev_Enhanced-GPOS760.jks" to "androidenhanceddev"),
    "positivo-l3" to ("platform.jks" to "platform"),
    "positivo-l3-2024" to ("platform_2024072401.jks" to "platform"),
    "positivo-l400" to ("platform.jks" to "platform"),
)
/** Alvo de build (modelo) → chave de assinatura. */
val deviceKeys = mapOf(
    "GPOS760" to "gertec-gpos760",
    "GPOS720" to "gertec-gpos720",
    "GPOS700X" to "gertec-gpos700",
    "GPOS700MINI" to "gertec-gpos700",
    "GPOS700" to "gertec-gpos700",
    "A910" to "aditum",
    "P2" to "aditum",
    "DX8000" to "aditum",
    "L3" to "positivo-l3",
    "L3_2024" to "positivo-l3-2024",
    "L400" to "positivo-l400",
    "X990" to "aditum",
    "X990UX" to "aditum",
    "TEFSDK" to "aditum",
)
/** Nomes de hardware atendidos por cada alvo (o mesmo APK do fabricante roda neles). */
val deviceAliases = mapOf(
    "GPOS700A" to "GPOS700",
    "A910S" to "A910", "A920" to "A910", "A920PRO" to "A910", "A930" to "A910", "A50" to "A910", "A77" to "A910",
    "P2LITE" to "P2", "P2LITESE" to "P2", "P2MINI" to "P2", "P2PRO" to "P2", "P2SMARTPAD" to "P2",
    "DX4000" to "DX8000", "EX4000" to "DX8000", "APOSA8" to "DX8000", "DX8000C" to "DX8000",
    "L300" to "L3", "X990PRO" to "X990",
)
fun normalize(s: String) = s.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
val requestedDevice = (providers.gradleProperty("paytrial.device").orNull ?: System.getenv("PAYTRIAL_DEVICE")).orEmpty().trim()
val device: String? = requestedDevice.ifBlank { null }?.let { r ->
    val n = normalize(r)
    deviceKeys.keys.firstOrNull { normalize(it) == n } ?: deviceAliases.entries.firstOrNull { normalize(it.key) == n }?.value
        ?: error("paytrial: dispositivo '$r' desconhecido. Alvos: ${deviceKeys.keys.joinToString()} (e apelidos: ${deviceAliases.keys.joinToString()})")
}
/** Token do dispositivo no nome do APK: o modelo em maiúsculas (TefSdk como no serviço); sem dispositivo, ANY. */
val deviceToken = when (device) { null -> "ANY"; "TEFSDK" -> "TefSdk"; else -> device }
val environment = (providers.gradleProperty("paytrial.environment").orNull ?: System.getenv("ENVIRONMENT") ?: "development").trim()
val envToken = if (environment.equals("production", ignoreCase = true)) "P" else "D"
// produção com o token de desenvolvimento não autentica em lugar nenhum: melhor falhar no build que no terminal
if (envToken == "P" && (partnerToken.isBlank() || partnerToken == "paytrial-dev-token"))
    error("paytrial: build de produção exige o token do integrador de produção (-Ppaytrial.token ou PAYTRIAL_TOKEN)")

// Assinatura, com as mesmas regras e os mesmos segredos do APK do serviço (doc 14 §2): release assinado com a chave do
// dispositivo — arquivo em -Ppaytrial.signing.dir (padrão: certs/ do aditum-tef, dois níveis acima), senhas em
// ADITUM_SIGNING_<CHAVE>_STORE / _KEY / _ALIAS ou -Ppaytrial.signing.<chave>.storePassword / .keyPassword / .keyAlias.
// No CI (CI/TF_BUILD) o release falha sem a chave; fora dele sai com a chave de debug e um aviso. Debug usa sempre a
// chave de debug, como o terminal. -Ppaytrial.keystore=<arquivo> + PAYTRIAL_STORE_PASSWORD/_KEY_ALIAS/_KEY_PASSWORD
// (um .jks só, o jeito antigo) vence a tabela.
val signingDir = file(providers.gradleProperty("paytrial.signing.dir").orNull ?: "../../certs")
val signingStrict = (providers.gradleProperty("paytrial.signing.strict").orNull?.toBoolean()) ?: (System.getenv("CI") != null || System.getenv("TF_BUILD") != null)
val explicitKeystore = providers.gradleProperty("paytrial.keystore").orNull
fun secret(env: String, property: String): String? =
    providers.gradleProperty(property).orNull?.takeIf { it.isNotBlank() } ?: System.getenv(env)?.takeIf { it.isNotBlank() }

android {
    namespace = "br.com.aditum.paytrial"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "br.com.aditum.paytrial"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 300
        versionName = appVersion
        buildConfigField("String", "PARTNER_TOKEN", "\"$partnerToken\"")
        buildConfigField("String", "DEVICE", "\"$deviceToken\"")
        buildConfigField("String", "ENVIRONMENT", "\"$environment\"")
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
        vectorDrawables.useSupportLibrary = true
    }
    signingConfigs {
        if (explicitKeystore != null) create("terminal") {
            storeFile = file(explicitKeystore)
            storePassword = System.getenv("PAYTRIAL_STORE_PASSWORD")
            keyAlias = System.getenv("PAYTRIAL_KEY_ALIAS")
            keyPassword = System.getenv("PAYTRIAL_KEY_PASSWORD")
        } else if (device != null) {
            val keyId = deviceKeys.getValue(device)
            val (jks, alias) = signingKeys.getValue(keyId)
            val prefix = "ADITUM_SIGNING_" + keyId.uppercase().replace('-', '_')
            val store = secret("${prefix}_STORE", "paytrial.signing.$keyId.storePassword")
            val keyFile = signingDir.resolve(jks)
            val why = when { !keyFile.isFile -> "arquivo ${keyFile.path} não encontrado"; store == null -> "senha ausente: defina ${prefix}_STORE (e ${prefix}_KEY se diferente)"; else -> null }
            if (why == null) create("terminal") {
                storeFile = keyFile; storePassword = store
                keyAlias = secret("${prefix}_ALIAS", "paytrial.signing.$keyId.keyAlias") ?: alias
                keyPassword = secret("${prefix}_KEY", "paytrial.signing.$keyId.keyPassword") ?: store
                enableV1Signing = true; enableV2Signing = true
            } else if (signingStrict) error("paytrial: o release para $device exige a chave '$keyId' — $why")
            else logger.warn("paytrial: release para $device SEM a chave '$keyId' ($why): sai com a chave de DEBUG — instala no aparelho de desenvolvimento, não serve para o fabricante")
        }
    }
    buildTypes {
        debug { if (explicitKeystore != null) signingConfig = signingConfigs.getByName("terminal") }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("terminal") ?: signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true; buildConfig = true; aidl = false }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { jvmToolchain(17) }

// nome do artefato como o do serviço: AditumPayTrial-A2A-<D|P>-<MODELO>-<versão>+<hash>-<tipo>.apk em build/dist/
if (device == null) logger.lifecycle("paytrial: sem -Ppaytrial.device o APK sai como ANY e o release com a chave de debug")
androidComponents {
    onVariants { v ->
        val name = "AditumPayTrial-A2A-$envToken-$deviceToken-$appVersion+$gitHash-${v.buildType}.apk"
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
