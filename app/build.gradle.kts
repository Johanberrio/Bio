plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.alvia.health"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.alvia.health"
        minSdk = 26
        targetSdk = 36
        versionCode = 23
        versionName = "0.6.8"
        testInstrumentationRunner = if (project.hasProperty("bioAuthTest")) "com.alvia.health.AuthFlowTests" else if (project.hasProperty("bioLiveTest")) "com.alvia.health.DocumentFlowTests" else "com.alvia.health.SmokeTests"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildTypes { release { isMinifyEnabled = false } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
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
