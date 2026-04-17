import com.android.build.api.dsl.ApplicationExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.android")

            val libs = the<LibrariesForLibs>()

            configure<ApplicationExtension> {
                compileSdk = libs.versions.compileSdk.get().toInt()
                defaultConfig {
                    minSdk = libs.versions.minSdk.get().toInt()
                    targetSdk = libs.versions.targetSdk.get().toInt()
                    versionCode = 1
                    versionName = "0.1.0"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                buildTypes {
                    getByName("debug") { applicationIdSuffix = ".debug" }
                    getByName("release") {
                        isMinifyEnabled = false
                        isShrinkResources = false
                    }
                }
            }

            configure<KotlinAndroidProjectExtension> {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
        }
    }
}
