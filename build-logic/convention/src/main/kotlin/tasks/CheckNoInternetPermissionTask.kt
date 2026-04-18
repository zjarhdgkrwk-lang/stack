package com.stack.player.buildlogic.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * APK 매니페스트에 INTERNET 권한이 포함되어 있는지 검사.
 * SSOT §11.2 / CLAUDE.md §1.1 절대 규칙.
 *
 * 1차: aapt로 APK 권한 덤프.
 * 2차 (fallback): merged manifest XML 직접 검사.
 *
 * (configuration cache 호환: task action 내 project 접근 없음)
 */
abstract class CheckNoInternetPermissionTask : DefaultTask() {

    init {
        group = "verification"
        description = "APK에 INTERNET 권한이 포함되어 있는지 검사. 제로 네트워크 원칙 강제."
    }

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apkFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifestFile: RegularFileProperty

    @TaskAction
    fun check() {
        val apk = apkFile.get().asFile
        if (!apk.exists()) {
            throw GradleException(
                "APK not found: $apk\n`./gradlew :app:assembleDebug`를 먼저 실행하세요."
            )
        }

        if (checkViaAapt(apk)) return
        checkViaMergedManifest()
    }

    private fun checkViaAapt(apk: java.io.File): Boolean {
        val androidHome = System.getenv("ANDROID_HOME") ?: return false
        val buildToolsDir = java.io.File("$androidHome/build-tools")
            .listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
            ?: return false
        val aapt = java.io.File(buildToolsDir, "aapt")
        if (!aapt.exists() || !aapt.canExecute()) return false

        val output = ProcessBuilder(aapt.absolutePath, "dump", "permissions", apk.absolutePath)
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()

        if (output.contains("android.permission.INTERNET")) {
            throw GradleException(
                """
                =====================================================
                [FAIL] INTERNET permission detected in APK (via aapt).
                =====================================================
                Stack은 제로 네트워크 원칙을 따릅니다 (SSOT §1.2 / §11.2).
                INTERNET 권한이 manifest merge로 추가되었는지 확인하세요.

                APK: ${apk.absolutePath}

                전체 권한 덤프:
                $output
                """.trimIndent()
            )
        }
        logger.lifecycle("[OK] APK does not declare INTERNET permission (checked via aapt).")
        return true
    }

    private fun checkViaMergedManifest() {
        val manifest = mergedManifestFile.orNull?.asFile
        if (manifest == null || !manifest.exists()) {
            throw GradleException(
                "aapt를 찾을 수 없고, merged manifest도 없습니다.\n" +
                    "`./gradlew :app:assembleDebug`를 먼저 실행하세요."
            )
        }

        val content = manifest.readText()
        if (content.contains("android.permission.INTERNET")) {
            throw GradleException(
                """
                =====================================================
                [FAIL] INTERNET permission detected in merged manifest.
                =====================================================
                Stack은 제로 네트워크 원칙을 따릅니다 (SSOT §1.2 / §11.2).
                Manifest: ${manifest.absolutePath}
                """.trimIndent()
            )
        }
        logger.lifecycle("[OK] APK does not declare INTERNET permission (checked via merged manifest).")
    }
}
