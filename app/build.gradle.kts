plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.anush.vibora"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.anush.vibora"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    /**Firebase Dependencies*/
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.appcheck.debug)

    /** Volley Dependency*/
    implementation(libs.volley)

    /** Gson*/
    implementation(libs.gson)
    implementation (libs.com.auth0.java.jwt)
    implementation (libs.integrity)
    implementation (libs.google.firebase.appcheck.debug)

    /** Google login*/
    implementation(libs.play.services.auth)

    /** Http3*/
    implementation(libs.okhttp)

    /** MVVM Architecture*/
    // ViewModel and LiveData dependencies
    implementation (libs.androidx.lifecycle.viewmodel.ktx)    // ViewModel
    implementation (libs.androidx.lifecycle.livedata.ktx)     // LiveData
    implementation (libs.androidx.lifecycle.runtime.ktx)      // Lifecycle

    /** Coroutine dependencies for ViewModel */
    implementation (libs.kotlinx.coroutines.android) // Coroutine support

    /** AV Loading indicator*/
    implementation(libs.avloadingindicatorview)

    configurations.all {
        resolutionStrategy {
            force("org.apache.httpcomponents.client5:httpclient5:5.2")
        }
    }

}