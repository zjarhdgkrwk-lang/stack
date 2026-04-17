plugins {
    id("stack.android.application")
}

android {
    namespace = "com.stack.player"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
}
