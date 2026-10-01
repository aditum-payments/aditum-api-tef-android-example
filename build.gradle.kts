import java.util.Properties
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
    "positivo-l3" to ("platform-L300-2023-ou-anterior.jks" to "platform"),
    "positivo-l3-2024" to ("platform-L300-2024-ou-superior.jks" to "platform"),
    "positivo-l400" to ("platform-L400.jks" to "platform"),
)
/** Alvo de build (modelo) → chave de assinatura. */
val deviceKeys = mapOf(
    "GPOS760" to "gertec-gpos760",
    "GPOS720" to "gertec-gpos720",
    "GPOS700X" to "gertec-gpos700",
    "GPOS700MINI" to "gertec-gpos700",
    "GPOS700" to "gertec-gpos700",
    "PAX" to "aditum",
    "P2" to "aditum",
    "AXIUM" to "aditum",
    "L300" to "positivo-l3",
    "L3_2024" to "positivo-l3-2024",
    "L400" to "positivo-l400",
    "X990" to "aditum",
    "TEFSDK" to "aditum",
)
/** Nomes de hardware atendidos por cada alvo (o mesmo APK do fabricante roda neles). */
val deviceAliases = mapOf(
    "GPOS700A" to "GPOS700",
    "A910" to "PAX", "A910S" to "PAX", "A920" to "PAX", "A920PRO" to "PAX",
    "P2LITE" to "P2", "P2LITESE" to "P2", "P2MINI" to "P2", "P2PRO" to "P2", "P2SMARTPAD" to "P2",
    "AXIUM" to "AXIUM", "DX8000" to "AXIUM", "DX4000" to "AXIUM", "EX4000" to "AXIUM", "EX6000" to "AXIUM", "SX7000" to "AXIUM",
    "L3" to "L300", "X990PRO" to "X990", "X990UX" to "X990",
)
fun normalize(s: String) = s.trim().uppercase().replace(Regex("[^A-Z0-9]"), "")
val requestedDevice = (providers.gradleProperty("paytrial.device").orNull ?: System.getenv("PAYTRIAL_DEVICE")).orEmpty().trim()
val device: String? = requestedDevice.ifBlank { null }?.let { r ->
    val n = normalize(r)
    deviceKeys.keys.firstOrNull { normalize(it) == n } ?: deviceAliases.entries.firstOrNull { normalize(it.key) == n }?.value
        ?: error("paytrial: dispositivo '$r' desconhecido. Alvos: ${deviceKeys.keys.joinToString()} (e apelidos: ${deviceAliases.keys.joinToString()})")
}
/** Token do dispositivo no nome do APK: o modelo em maiúsculas (TefSdk como no serviço); sem dispositivo, nenhum. */
val deviceToken = when (device) { null -> ""; "TEFSDK" -> "TefSdk"; else -> device }
val environment = (providers.gradleProperty("paytrial.environment").orNull ?: System.getenv("ENVIRONMENT") ?: "development").trim()
val envToken = if (environment.equals("production", ignoreCase = true)) "P" else "D"
// produção com o token de desenvolvimento não autentica em lugar nenhum: melhor falhar no build que no terminal
if (envToken == "P" && (partnerToken.isBlank() || partnerToken == "paytrial-dev-token"))
    error("paytrial: build de produção exige o token do integrador de produção (-Ppaytrial.token ou PAYTRIAL_TOKEN)")

// Assinatura, com as mesmas regras e os mesmos segredos do APK do serviço (doc 14 §2; decisão de 24/09): debug e release
// assinados com a chave do fabricante do dispositivo quando existe; sem ela, com a da Aditum; a de debug do Android só
// se nem a da Aditum estiver disponível, com aviso. Arquivos em -Ppaytrial.signing.dir (padrão: certs/ do aditum-tef, dois
// níveis acima); senhas, nesta ordem: -Ppaytrial.signing.<chave>.storePassword / .keyPassword / .keyAlias, ambiente
// ADITUM_SIGNING_<CHAVE>_STORE / _KEY / _ALIAS, e o signing.properties do aditum-tef (aditum.signing.<chave>.*, commitado
// de propósito), em -Ppaytrial.signing.properties. Fora do aditum-tef, sem esses arquivos, sai com a de debug. No CI
// (CI/TF_BUILD) o release falha sem a chave do dispositivo. -Ppaytrial.keystore=<arquivo> + PAYTRIAL_STORE_PASSWORD /
// _KEY_ALIAS / _KEY_PASSWORD (um .jks só, o jeito antigo) vence a tabela.
val signingDir = file(providers.gradleProperty("paytrial.signing.dir").orNull ?: "../../certs")
val signingStrict = (providers.gradleProperty("paytrial.signing.strict").orNull?.toBoolean()) ?: (System.getenv("CI") != null || System.getenv("TF_BUILD") != null)
val explicitKeystore = providers.gradleProperty("paytrial.keystore").orNull
val signingFile = Properties().apply {
    file(providers.gradleProperty("paytrial.signing.properties").orNull ?: "../../signing.properties").takeIf { it.isFile }
        ?.reader(Charsets.UTF_8)?.use { load(it) }
}
fun secret(env: String, property: String, fileKey: String? = null): String? =
    providers.gradleProperty(property).orNull?.takeIf { it.isNotBlank() } ?: System.getenv(env)?.takeIf { it.isNotBlank() }
        ?: fileKey?.let { signingFile.getProperty(it) }?.takeIf { it.isNotBlank() }

/** Chave pronta para assinar (arquivo + senhas), ou o motivo de não estar. */
data class ResolvedKey(val id: String, val file: File, val store: String, val alias: String, val key: String)
fun resolveKey(keyId: String): Pair<ResolvedKey?, String?> {
    val (jks, alias) = signingKeys.getValue(keyId)
    val prefix = "ADITUM_SIGNING_" + keyId.uppercase().replace('-', '_')
    val store = secret("${prefix}_STORE", "paytrial.signing.$keyId.storePassword", "aditum.signing.$keyId.storePassword")
    val keyFile = signingDir.resolve(jks)
    val why = when { !keyFile.isFile -> "arquivo ${keyFile.path} não encontrado"; store == null -> "senha ausente: defina ${prefix}_STORE (ou aditum.signing.$keyId.storePassword no signing.properties)"; else -> null }
    if (why != null) return null to why
    return ResolvedKey(keyId, keyFile, store!!,
        secret("${prefix}_ALIAS", "paytrial.signing.$keyId.keyAlias", "aditum.signing.$keyId.keyAlias") ?: alias,
        secret("${prefix}_KEY", "paytrial.signing.$keyId.keyPassword", "aditum.signing.$keyId.keyPassword") ?: store) to null
}
val deviceKeyId = device?.let { deviceKeys.getValue(it) }
val (deviceKey, deviceKeyWhy) = deviceKeyId?.let { resolveKey(it) } ?: (null to null)
val chosenKey: ResolvedKey? = deviceKey ?: if (deviceKeyId != "aditum") resolveKey("aditum").first else null
if (explicitKeystore == null) when {
    chosenKey == null -> logger.warn("paytrial: sem a chave ${deviceKeyId?.let { "'$it' nem a " } ?: ""}'aditum' (${deviceKeyWhy ?: resolveKey("aditum").second}): sai com a chave de DEBUG do Android — instala no aparelho de desenvolvimento, não serve para o fabricante")
    deviceKeyId != null && chosenKey.id != deviceKeyId -> logger.warn("paytrial: sem a chave '$deviceKeyId' do $device ($deviceKeyWhy): assinado com a da Aditum")
    else -> logger.lifecycle("paytrial: assinatura com a chave '${chosenKey.id}' (${chosenKey.file.name})")
}

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
        // TefSDK não tem as telas da Aditum: o build para ele só oferece "Telas do integrador" (useOnlySdk = true)
        buildConfigField("boolean", "ADITUM_SCREENS", (deviceToken != "TefSdk").toString())
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
        } else if (chosenKey != null) create("terminal") {
            storeFile = chosenKey.file; storePassword = chosenKey.store; keyAlias = chosenKey.alias; keyPassword = chosenKey.key
            enableV1Signing = true; enableV2Signing = true
        }
    }
    buildTypes {
        // debug e release com a mesma chave, como o terminal: o APK de teste instala por cima do de release e passa na
        // lista branca dos terminais que exigem a chave do fabricante (L300/L400, GPOS Enhanced)
        debug { signingConfig = signingConfigs.findByName("terminal") ?: signingConfigs.getByName("debug") }
        release {
            isMinifyEnabled = false
            if (signingStrict && explicitKeystore == null && deviceKeyId != null && deviceKey == null)
                error("paytrial: o release para $device exige a chave '$deviceKeyId' — $deviceKeyWhy")
            signingConfig = signingConfigs.findByName("terminal") ?: signingConfigs.getByName("debug")
        }
    }
    buildFeatures { compose = true; buildConfig = true; aidl = false }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
kotlin { jvmToolchain(17) }

// nome do artefato como o do serviço: AditumPayTrial-A2A-<D|P>-<MODELO>-<versão>+<hash>-<tipo>.apk em build/dist/
// (sem -Ppaytrial.device, sem o modelo: AditumPayTrial-A2A-<D|P>-<versão>+<hash>-<tipo>.apk)
if (device == null) logger.lifecycle("paytrial: sem -Ppaytrial.device o APK sai sem dispositivo no nome")
androidComponents {
    onVariants { v ->
        val name = "AditumPayTrial-A2A-$envToken-${if (deviceToken.isEmpty()) "" else "$deviceToken-"}$appVersion+$gitHash-${v.buildType}.apk"
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
    // codificador ESC/POS da impressora externa (`:lib:escpos` do repositório, copiado por scripts/sync-example-aar.sh)
    implementation(files("libs/aditum-escpos-3.0.0.jar"))
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
