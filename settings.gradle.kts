// Aditum Pay Trial — app de exemplo App2App. Build PRÓPRIO: compila sozinho (clone só desta pasta) e também de dentro
// do aditum-tef (`./gradlew -p sdk/example assembleDebug`). Do app da Aditum vem só o .aar em libs/.
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
plugins {
    // baixa um JDK 17 quando o da máquina for outro (o mesmo resolvedor do aditum-tef)
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
rootProject.name = "aditum-pay-trial"
