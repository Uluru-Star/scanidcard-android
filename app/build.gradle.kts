plugins {
    alias(libs.plugins.android.application)
}

fun readDotEnv(): Map<String, String> {
    val envFile = rootProject.file(".env")
    if (!envFile.exists()) return emptyMap()

    val map = mutableMapOf<String, String>()
    envFile.readLines(Charsets.UTF_8)
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .forEach { line ->
            val idx = line.indexOf("=")
            val k = line.substring(0, idx).trim()
            var v = line.substring(idx + 1).trim()
            // 兼容带引号的值
            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                v = v.substring(1, v.length - 1)
            }
            map[k] = v
        }
    return map
}

android {
    namespace = "com.example.scanidcard"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.scanidcard"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val env = readDotEnv()

        // 同时支持两种写法：SecretId/SecretKey（老师要求） 或 TENCENT_SECRET_ID/KEY
        val sid = env["SecretId"] ?: env["TENCENT_SECRET_ID"] ?: ""
        val skey = env["SecretKey"] ?: env["TENCENT_SECRET_KEY"] ?: ""
        val region = env["Region"] ?: env["TENCENT_REGION"] ?: "ap-guangzhou"

        buildConfigField("String", "TENCENT_SECRET_ID", "\"$sid\"")
        buildConfigField("String", "TENCENT_SECRET_KEY", "\"$skey\"")
        buildConfigField("String", "TENCENT_REGION", "\"$region\"")

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // 你后面 TencentOcrClient 如果用 Gson 解析 JSON，就要加这个
    implementation("com.google.code.gson:gson:2.11.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
