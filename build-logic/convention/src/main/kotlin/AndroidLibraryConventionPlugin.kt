import com.android.build.gradle.LibraryExtension
import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")

            val libs = the<LibrariesForLibs>()

            configure<LibraryExtension> {
                compileSdk = libs.versions.compileSdk.get().toInt()
                defaultConfig {
                    minSdk = libs.versions.minSdk.get().toInt()
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
            }

            configure<KotlinAndroidProjectExtension> {
                compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
            }
        }
    }
}
