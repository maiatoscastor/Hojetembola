// build.gradle.kts — nível do projecto
// Declara todos os plugins; "apply false" porque são aplicados no módulo :app
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp)                 apply false
    alias(libs.plugins.hilt.android)        apply false
    alias(libs.plugins.google.services)     apply false
}
