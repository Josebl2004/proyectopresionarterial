plugins {
    alias(libs.plugins.android.application)
}

val apiBaseUrl = providers.gradleProperty("API_BASE_URL").orNull ?: "https://api.example.com/"
val apiKey = providers.gradleProperty("API_KEY").orNull ?: ""
// URL de webhook n8n configurable desde gradle.properties
val n8nWebhookUrl = providers.gradleProperty("N8N_WEBHOOK_URL").orNull ?: ""

android {
    namespace = "com.example.proyectopresionarterial"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.proyectopresionarterial"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"${apiBaseUrl}\"")
        buildConfigField("String", "API_KEY", "\"${apiKey}\"")
        buildConfigField("long", "REC_CACHE_TTL_MS", "7200000L") // 2 horas configurable
        // Nuevo: URL del webhook n8n (nombre de campo válido)
        buildConfigField("String", "N8N_WEBHOOK_URL", "\"${n8nWebhookUrl}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Room (runtime + annotation processor for Java)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // Lifecycle (ViewModel + LiveData) para MVVM
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // MPAndroidChart
    implementation(libs.mpandroidchart)

    // RecyclerView
    implementation(libs.recyclerview)

    // Glide para carga de imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Interceptor de logging de red (opcional) -> ya no usado en RetrofitProvider pero disponible
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}

// Mostrar advertencias detalladas de APIs deprecadas y "unchecked" durante la compilación Java

tasks.withType<org.gradle.api.tasks.compile.JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
