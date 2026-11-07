import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.alvia.health"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.alvia.health"
        minSdk = 26
        targetSdk = 36
        versionCode = 34
        versionName = "0.6.19-preview"
        testInstrumentationRunner = if (project.hasProperty("bioAccountMenuTest")) "com.alvia.health.AccountMenuTests" else if (project.hasProperty("bioDeletionTest")) "com.alvia.health.DeletionTests" else if (project.hasProperty("bioWeightTest")) "com.alvia.health.WeightTests" else if (project.hasProperty("bioLayoutTest")) "com.alvia.health.LayoutTests" else if (project.hasProperty("bioAuthTest")) "com.alvia.health.AuthFlowTests" else if (project.hasProperty("bioLiveTest")) "com.alvia.health.DocumentFlowTests" else "com.alvia.health.SmokeTests"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    val uploadPropertiesFile = rootProject.file(".secrets/upload-signing.properties")
    if (uploadPropertiesFile.exists()) {
        val uploadProperties = Properties().apply { uploadPropertiesFile.inputStream().use { load(it) } }
        signingConfigs.create("upload") {
            storeFile = rootProject.file(uploadProperties.getProperty("storeFile"))
            storePassword = uploadProperties.getProperty("storePassword")
            keyAlias = uploadProperties.getProperty("keyAlias")
            keyPassword = uploadProperties.getProperty("keyPassword")
        }
    }
    buildTypes { release {
        isMinifyEnabled = false
        if (uploadPropertiesFile.exists()) signingConfig = signingConfigs.getByName("upload")
    } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("com.android.billingclient:billing:8.3.0")
    implementation("androidx.fragment:fragment:1.8.9")
    implementation("com.google.guava:guava:33.4.8-android")
    implementation("androidx.health.connect:connect-client:1.1.0")
    implementation("androidx.work:work-runtime-ktx:2.10.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")
}
