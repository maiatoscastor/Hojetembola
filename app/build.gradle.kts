plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
}

android {
    namespace  = "com.hojetembola.app"
    compileSdk = 36

    defaultConfig {
        applicationId         = "com.hojetembola.app"
        minSdk                = 26
        targetSdk             = 36
        versionCode           = 1
        versionName           = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

// ── Kotlin JVM target (AGP 9 built-in Kotlin API — sem toolchain) ─────────────
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // ── AndroidX core ────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ── Supabase (BOM gerencia versões dos módulos) ───────────────────────────
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // ── Ktor — cliente HTTP usado pelo Supabase SDK ───────────────────────────
    implementation("io.ktor:ktor-client-android:3.0.0")

    // ── Room — cache offline ──────────────────────────────────────────────────
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ── WorkManager — sincronização offline ──────────────────────────────────
    implementation(libs.work.runtime.ktx)

    // ── Lifecycle (ViewModel + LiveData) ─────────────────────────────────────
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)

    // ── Navigation Component ──────────────────────────────────────────────────
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // ── Hilt — injecção de dependências ──────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ── Hilt WorkManager extension ────────────────────────────────────────────
    implementation(libs.hilt.work)
    ksp(libs.hilt.androidx.compiler)

    // ── Firebase (BOM) — FCM ──────────────────────────────────────────────────
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")

    // ── Glide — carregamento de imagens ──────────────────────────────────────
    implementation(libs.glide)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.coroutines.android)

    // ── Testes ────────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
