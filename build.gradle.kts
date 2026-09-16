plugins {
    id("aditum.android.app")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}
android {
    namespace = "br.com.aditum.tef.sdk.example"
    defaultConfig { applicationId = "br.com.aditum.paymentexample" }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":sdk:integration"))
    implementation(libs.androidx.core.ktx); implementation(libs.androidx.activity.compose)
    implementation(compose.runtime); implementation(compose.foundation); implementation(compose.material3); implementation(compose.ui)
    androidTestImplementation(libs.androidx.test.runner); androidTestImplementation(libs.androidx.test.junit)
}
