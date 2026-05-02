plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.myschool"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.myschool"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val atlasApiUrl = (project.findProperty("ATLAS_DATA_API_URL") as String?) ?: ""
        val atlasApiKey = (project.findProperty("ATLAS_DATA_API_KEY") as String?) ?: ""
        val atlasDataSource = (project.findProperty("ATLAS_DATA_SOURCE") as String?) ?: ""
        val atlasDb = (project.findProperty("ATLAS_DB") as String?) ?: ""
        val atlasCollection = (project.findProperty("ATLAS_COLLECTION") as String?) ?: ""
        buildConfigField("String", "ATLAS_DATA_API_URL", "\"$atlasApiUrl\"")
        buildConfigField("String", "ATLAS_DATA_API_KEY", "\"$atlasApiKey\"")
        buildConfigField("String", "ATLAS_DATA_SOURCE", "\"$atlasDataSource\"")
        buildConfigField("String", "ATLAS_DB", "\"$atlasDb\"")
        buildConfigField("String", "ATLAS_COLLECTION", "\"$atlasCollection\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/LICENSE"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation(libs.recyclerview)
    implementation(libs.coordinatorlayout)
    implementation("androidx.fragment:fragment:1.8.8")

    // Firebase
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // CameraX
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)

    // ML Kit OCR
    implementation(libs.mlkit.text.recognition)

    // Room
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // iText PDF
    implementation(libs.itextpdf)

    // Lifecycle
    implementation(libs.lifecycle.runtime)
    implementation(libs.lifecycle.viewmodel)

    // Glide
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.guava)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
