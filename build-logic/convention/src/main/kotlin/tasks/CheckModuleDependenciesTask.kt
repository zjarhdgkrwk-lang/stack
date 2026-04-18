package com.stack.player.buildlogic.tasks

import com.stack.player.buildlogic.ModuleDependencyRules
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * 모든 sub-project의 project dependency를 검사하여
 * ModuleDependencyRules를 위반하는 의존성이 있으면 빌드를 실패시킨다.
 *
 * moduleDependencies는 VerificationConventionPlugin에서 설정 단계에 수집된다.
 * (configuration cache 호환: task action 내 project 접근 없음)
 *
 * 실행: ./gradlew checkModuleDependencies
 */
abstract class CheckModuleDependenciesTask : DefaultTask() {

    init {
        group = "verification"
        description = "모듈 간 의존성 규칙(SSOT §3.3)을 검증한다. 위반 시 빌드 실패."
    }

    @get:Input
    abstract val moduleDependencies: MapProperty<String, List<String>>

    @TaskAction
    fun check() {
        val allViolations = mutableListOf<ModuleDependencyRules.Violation>()
        val deps = moduleDependencies.get()

        deps.forEach { (modulePath, projectDeps) ->
            val violations = ModuleDependencyRules.findViolations(modulePath, projectDeps)
            allViolations += violations
        }

        if (allViolations.isNotEmpty()) {
            val report = buildString {
                appendLine("=====================================================")
                appendLine("[FAIL] Module dependency rule violations (${allViolations.size}):")
                appendLine("=====================================================")
                allViolations.forEach { violation ->
                    appendLine("  - ${violation.format()}")
                }
                appendLine()
                appendLine("규칙 정의: SSOT v5.0 §3.3 / CLAUDE.md §3.2")
                appendLine("규칙 코드: build-logic/convention/.../ModuleDependencyRules.kt")
            }
            throw GradleException(report)
        }

        logger.lifecycle("[OK] Module dependency rules: ${deps.size} modules passed.")
    }
}
