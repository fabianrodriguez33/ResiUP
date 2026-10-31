import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// URL de la API por entorno. Se define en local.properties (no versionado):
//   resi.apiUrl=http://192.168.1.50:8080/          (teléfono físico: IP de la PC en la red local)
//   resi.apiUrlRelease=https://api.ejemplo.pe/      (producción: HTTPS obligatorio)
val propiedadesLocales = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
val apiUrlDebug = propiedadesLocales.getProperty("resi.apiUrl", "http://10.0.2.2:8080/") // 10.0.2.2 = PC anfitriona desde el emulador
val apiUrlRelease = propiedadesLocales.getProperty("resi.apiUrlRelease", "https://CONFIGURAR-URL-DE-PRODUCCION/")

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.koin.android)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "pe.upeu.resi.resiup"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "pe.upeu.resi.resiup"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "API_URL", "\"$apiUrlDebug\"")
        }
        release {
            buildConfigField("String", "API_URL", "\"$apiUrlRelease\"")
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
        compose = true
        buildConfig = true
    }
}