package com.stack.player.buildlogic

/**
 * SSOT v5.0 §3.3 / CLAUDE.md §3.2의 의존성 그래프를 표현한다.
 * 이 규칙을 위반하는 project dependency가 발견되면 빌드를 실패시킨다.
 *
 * 매칭 방식:
 * - 정확히 일치 (e.g., ":core:domain")
 * - 와일드카드 prefix (e.g., ":feature:*" → ":feature:"로 시작하는 모든 모듈)
 */
object ModuleDependencyRules {

    /**
     * 각 모듈이 **의존해서는 안 되는** 다른 모듈 경로 목록.
     * key: 검사 대상 모듈. value: 금지된 의존 대상.
     */
    val forbiddenDependencies: Map<String, List<String>> = mapOf(
        // :core:domain은 순수 Kotlin. Android 관련 모듈 일체 금지.
        ":core:domain" to listOf(
            ":app",
            ":core:database",
            ":core:datastore",
            ":core:audio",
            ":core:design",
            ":core:ui",
            ":feature:*",
        ),

        // :core:common은 의존 없음 (순수 Kotlin 유틸).
        ":core:common" to listOf(
            ":app",
            ":core:database", ":core:datastore", ":core:domain",
            ":core:audio", ":core:design", ":core:ui",
            ":feature:*",
        ),

        // :core:design은 순수 디자인 토큰. 다른 core에 의존하지 않음.
        ":core:design" to listOf(
            ":app",
            ":core:database", ":core:datastore", ":core:domain",
            ":core:audio", ":core:ui",
            ":feature:*",
        ),

        // :core:ui는 design과 common만.
        ":core:ui" to listOf(
            ":app",
            ":core:database", ":core:datastore", ":core:domain",
            ":core:audio",
            ":feature:*",
        ),

        // :core:audio는 domain과 common만. UI 및 feature 금지.
        ":core:audio" to listOf(
            ":app",
            ":core:database", ":core:datastore",
            ":core:design", ":core:ui",
            ":feature:*",
        ),

        // :core:database는 domain (repository 구현) + common.
        ":core:database" to listOf(
            ":app",
            ":core:datastore", ":core:audio",
            ":core:design", ":core:ui",
            ":feature:*",
        ),

        // :core:datastore도 마찬가지.
        ":core:datastore" to listOf(
            ":app",
            ":core:database", ":core:audio",
            ":core:design", ":core:ui",
            ":feature:*",
        ),

        // 모든 :feature는 다른 :feature에 직접 의존 금지.
        // 또한 data layer (database, audio, datastore) 직접 의존 금지.
        ":feature:library" to listOf(
            ":app",
            ":feature:player", ":feature:playlist",
            ":feature:search", ":feature:settings",
            ":core:database", ":core:datastore", ":core:audio",
        ),
        ":feature:player" to listOf(
            ":app",
            ":feature:library", ":feature:playlist",
            ":feature:search", ":feature:settings",
            ":core:database", ":core:datastore", ":core:audio",
        ),
        ":feature:playlist" to listOf(
            ":app",
            ":feature:library", ":feature:player",
            ":feature:search", ":feature:settings",
            ":core:database", ":core:datastore", ":core:audio",
        ),
        ":feature:search" to listOf(
            ":app",
            ":feature:library", ":feature:player",
            ":feature:playlist", ":feature:settings",
            ":core:database", ":core:datastore", ":core:audio",
        ),
        ":feature:settings" to listOf(
            ":app",
            ":feature:library", ":feature:player",
            ":feature:playlist", ":feature:search",
            ":core:database", ":core:datastore", ":core:audio",
        ),

        // :app만이 모든 것에 의존 가능. 따라서 :app 항목 없음.
    )

    /**
     * 주어진 모듈의 실제 의존성이 규칙을 위반하는지 검사.
     *
     * @return 위반 목록. 빈 목록이면 규칙 준수.
     */
    fun findViolations(
        modulePath: String,
        actualDependencies: List<String>,
    ): List<Violation> {
        val forbidden = forbiddenDependencies[modulePath] ?: return emptyList()
        return actualDependencies.mapNotNull { dep ->
            val matched = forbidden.firstOrNull { pattern -> matches(dep, pattern) }
            if (matched != null) Violation(modulePath, dep, matched) else null
        }
    }

    private fun matches(dependency: String, pattern: String): Boolean = when {
        pattern.endsWith(":*") -> {
            val prefix = pattern.removeSuffix(":*") + ":"
            dependency.startsWith(prefix)
        }
        else -> dependency == pattern
    }

    data class Violation(
        val module: String,
        val forbiddenDep: String,
        val rulePattern: String,
    ) {
        fun format(): String = "$module depends on $forbiddenDep (forbidden by rule: $rulePattern)"
    }
}
