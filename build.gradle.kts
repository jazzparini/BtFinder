// Archivo de build de nivel raíz. Declara los plugins usados por los módulos
// sin aplicarlos aquí (se aplican en app/build.gradle.kts).
plugins {
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
