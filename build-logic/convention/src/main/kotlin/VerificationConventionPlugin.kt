import com.stack.player.buildlogic.tasks.CheckModuleDependenciesTask
import com.stack.player.buildlogic.tasks.CheckNoHardcodedUserStringsTask
import com.stack.player.buildlogic.tasks.CheckNoInternetPermissionTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.kotlin.dsl.register

/**
 * 루트 프로젝트에 적용하는 검증 게이트 플러그인.
 * checkModuleDependencies, checkNoInternetPermission, checkNoHardcodedUserStrings, checkAll 태스크를 등록한다.
 *
 * 적용: root build.gradle.kts의 plugins { id("stack.verification") }
 */
class VerificationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "stack.verification 플러그인은 루트 프로젝트에만 적용할 수 있습니다."
        }

        // 설정 단계에서 모든 모듈의 project dependency를 수집한다.
        // (configuration cache 호환: task action 내 project 접근을 피하기 위해 여기서 수집)
        val depsProvider = target.provider {
            target.subprojects.associate { subproject ->
                subproject.path to subproject.configurations
                    .flatMap { config ->
                        runCatching {
                            config.dependencies.withType(ProjectDependency::class.java).toList()
                        }.getOrDefault(emptyList())
                    }
                    .map { it.dependencyProject.path }
                    .distinct()
            }
        }

        target.tasks.register<CheckModuleDependenciesTask>("checkModuleDependencies") {
            moduleDependencies.set(depsProvider)
        }

        target.tasks.register<CheckNoInternetPermissionTask>("checkNoInternetPermission") {
            dependsOn(":app:assembleDebug")
            apkFile.set(
                target.layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk")
            )
            mergedManifestFile.set(
                target.layout.projectDirectory.file(
                    "app/build/intermediates/merged_manifests/debug/AndroidManifest.xml"
                )
            )
        }

        target.tasks.register<CheckNoHardcodedUserStringsTask>("checkNoHardcodedUserStrings") {
            rootDir.set(target.rootProject.layout.projectDirectory)
            sourceFiles.setFrom(
                target.subprojects.map { it.layout.projectDirectory.dir("src") }
            )
        }

        target.tasks.register("checkAll") {
            group = "verification"
            description = "Phase 종료 전 통과 필수. 정적 분석 + 의존성 + 권한 + 테스트 전체."

            val androidTestTasks = target.subprojects
                .filter { it.path != ":core:common" && it.path != ":core:domain" }
                .map { "${it.path}:testDebugUnitTest" }

            val jvmTestTasks = listOf(":core:common:test", ":core:domain:test")

            dependsOn(
                target.subprojects.map { "${it.path}:ktlintCheck" },
                target.subprojects.map { "${it.path}:detekt" },
                ":app:lintDebug",
                "checkModuleDependencies",
                androidTestTasks,
                jvmTestTasks,
                "checkNoInternetPermission",
                "checkNoHardcodedUserStrings",
            )
        }
    }
}
