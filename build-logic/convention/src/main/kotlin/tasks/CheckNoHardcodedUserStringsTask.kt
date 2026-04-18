package com.stack.player.buildlogic.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty

import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Compose의 Text("...") 같은 Kotlin 소스 내 하드코딩된 사용자 노출 문자열을 검사한다.
 *
 * Android Lint가 XML을 검사하는 것을 보완하는 1차 방어선.
 * v1.0에서는 경고만 발생시키며, Compose Lint Rules 도입 시 강화 예정.
 *
 * (configuration cache 호환: task action 내 project 접근 없음)
 *
 * 실행: ./gradlew checkNoHardcodedUserStrings
 */
abstract class CheckNoHardcodedUserStringsTask : DefaultTask() {

    init {
        group = "verification"
        description = "Compose 등 Kotlin 코드에 하드코딩된 사용자 노출 문자열을 탐지."
    }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    // 에러 메시지의 상대 경로 표시용. task input/output에 영향 없음.
    @get:Internal
    abstract val rootDir: DirectoryProperty

    @TaskAction
    fun check() {
        val suspiciousLines = mutableListOf<String>()
        val root = rootDir.orNull?.asFile

        val textPattern = Regex("""\bText\s*\(\s*"([^"]{2,})"""")
        val testFilePattern = Regex("""(/test/|/androidTest/|/preview/|Preview\.kt${'$'})""")

        sourceFiles.asFileTree.matching {
            include("**/*.kt")
            exclude("**/build/**")
        }.forEach fileLoop@{ file ->
            if (testFilePattern.containsMatchIn(file.absolutePath)) return@fileLoop

            val relativePath = if (root != null) file.relativeTo(root).path else file.name

            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed

                textPattern.findAll(line).forEach matchLoop@{ match ->
                    val text = match.groupValues[1]
                    if (text.isBlank()) return@matchLoop
                    if (text.startsWith("@")) return@matchLoop
                    if (text.all { !it.isLetter() }) return@matchLoop

                    suspiciousLines += "$relativePath:${index + 1}  →  Text(\"$text\")"
                }
            }
        }

        if (suspiciousLines.isNotEmpty()) {
            val report = buildString {
                appendLine("=====================================================")
                appendLine("[WARN] Possibly hardcoded user-facing strings (${suspiciousLines.size}):")
                appendLine("=====================================================")
                suspiciousLines.take(50).forEach { appendLine("  - $it") }
                if (suspiciousLines.size > 50) {
                    appendLine("  ... and ${suspiciousLines.size - 50} more")
                }
                appendLine()
                appendLine("사용자 노출 문자열은 strings.xml + stringResource()를 사용하세요.")
                appendLine("규칙: CLAUDE.md §1.3 / SSOT §10")
            }
            // v1.0에서는 경고만. v1.1+에서 GradleException으로 승격 예정.
            logger.warn(report)
        } else {
            logger.lifecycle("[OK] No hardcoded user-facing strings detected in Compose Text(...).")
        }
    }
}
