# Phase 2 — 의존성 Enforcement + 검증 게이트

> **목표**: 모든 후속 phase를 지탱할 **안전망**을 구축한다. 모듈 의존 규칙 위반, INTERNET 권한 새어 들어옴, 정적 분석 실패를 자동으로 빌드 실패로 전환한다. 이 phase가 끝나면 이후 모든 phase에서 `./gradlew checkAll`이 단일 진실의 검증 명령이 된다.
>
> **핵심 원칙**: 안전망은 **일부러 깨뜨려 봐야** 작동하는지 확인된다. 이 phase는 위반 케이스를 의도적으로 만들어 빌드 실패가 발생하는지 역검증하는 단계를 포함한다.

---

## 1. 컨텍스트

- **선행**: Phase 1 완료 (13 모듈 셸, `./gradlew :app:assembleDebug` 통과).
- **참조 문서**:
  - `CLAUDE.md` §1 (절대 규칙), §3.2 (모듈 의존성 규칙), §5.2 (통합 검증 게이트)
  - `docs/SSOT_v5.0.md` §3.3 (의존성 규칙), §11 (권한), §15.4 (의존성 분석 태스크)
  - `docs/BUILD.md` §4 (검증), §5 (Convention Plugins), §6 (커스텀 검증 태스크)

---

## 2. 작업 범위

### 수정 허용

**신규 생성**:
```
build-logic/convention/src/main/kotlin/
├── tasks/
│   ├── CheckModuleDependenciesTask.kt
│   ├── CheckNoInternetPermissionTask.kt
│   └── CheckNoHardcodedUserStringsTask.kt
├── ModuleDependencyRules.kt           # 의존성 규칙 정의
├── stack.android.library.gradle.kts   # (Phase 1에서 생성 — ktlint/detekt 추가)
├── stack.android.application.gradle.kts # (업데이트)
└── stack.kotlin.library.gradle.kts    # (업데이트)
```

**수정**:
- `gradle/libs.versions.toml` — ktlint, detekt 플러그인 추가
- `build-logic/convention/build.gradle.kts` — task 등록
- root `build.gradle.kts` — `checkAll` 등록, `checkModuleDependencies` 등록
- `.github/workflows/verify.yml` — CI 파이프라인 신규

**신규 설정 파일**:
- `config/detekt/detekt.yml`
- `.github/workflows/verify.yml`

### 수정 금지

- `docs/`, `CLAUDE.md`, `CHANGELOG.md`, `README.md`
- `app/`, `core/`, `feature/` 모듈 내부 (build.gradle.kts는 **예외적으로** ktlint/detekt 플러그인 추가는 convention plugin에서 일괄 적용하므로 각 모듈의 build.gradle.kts는 건드리지 않음)
- `.gitignore`, `.gitattributes`, `.editorconfig`, `gradle.properties`, `gradle/wrapper/`

---

## 3. 산출물 상세

### 3.1 `ModuleDependencyRules.kt`

모듈별 금지 의존성 정의. SSOT §3.3을 그대로 코드화.

```kotlin
// build-logic/convention/src/main/kotlin/ModuleDependencyRules.kt
package com.stack.player.buildlogic

/**
 * 모듈 간 의존성 규칙.
 * 
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
        
        // :app만이 모든 것에 의존 가능. 예외적으로 :app이 feature/core에 의존하는 것은 OK.
        // 따라서 :app 항목은 없음 (빈 리스트 = 제한 없음).
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
            if (matched != null) {
                Violation(modulePath, dep, matched)
            } else {
                null
            }
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
        fun format(): String =
            "$module depends on $forbiddenDep (forbidden by rule: $rulePattern)"
    }
}
```

### 3.2 `CheckModuleDependenciesTask.kt`

```kotlin
// build-logic/convention/src/main/kotlin/tasks/CheckModuleDependenciesTask.kt
package com.stack.player.buildlogic.tasks

import com.stack.player.buildlogic.ModuleDependencyRules
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.TaskAction

/**
 * 모든 sub-project의 project dependency를 검사하여
 * ModuleDependencyRules를 위반하는 의존성이 있으면 빌드를 실패시킨다.
 *
 * 실행: ./gradlew checkModuleDependencies
 */
abstract class CheckModuleDependenciesTask : DefaultTask() {
    
    init {
        group = "verification"
        description = "모듈 간 의존성 규칙(SSOT §3.3)을 검증한다. 위반 시 빌드 실패."
    }
    
    @TaskAction
    fun check() {
        val allViolations = mutableListOf<ModuleDependencyRules.Violation>()
        
        project.subprojects.forEach { subproject ->
            val modulePath = subproject.path
            // 모든 configuration의 project dependency를 수집
            val projectDeps = subproject.configurations
                .flatMap { config ->
                    config.dependencies.withType(ProjectDependency::class.java)
                }
                .map { it.dependencyProject.path }
                .distinct()
            
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
        
        logger.lifecycle("[OK] Module dependency rules: ${project.subprojects.size} modules passed.")
    }
}
```

### 3.3 `CheckNoInternetPermissionTask.kt`

```kotlin
// build-logic/convention/src/main/kotlin/tasks/CheckNoInternetPermissionTask.kt
package com.stack.player.buildlogic.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

/**
 * APK 매니페스트에 INTERNET 권한이 포함되어 있는지 검사.
 * SSOT §11.2 / CLAUDE.md §1.1 절대 규칙.
 *
 * aapt로 APK 권한을 덤프하여 android.permission.INTERNET 존재 시 빌드 실패.
 */
abstract class CheckNoInternetPermissionTask : DefaultTask() {
    
    init {
        group = "verification"
        description = "APK에 INTERNET 권한이 포함되어 있는지 검사. 제로 네트워크 원칙 강제."
    }
    
    @get:InputFile
    abstract val apkFile: RegularFileProperty
    
    @TaskAction
    fun check() {
        val apkPath = apkFile.get().asFile
        if (!apkPath.exists()) {
            throw GradleException("APK not found: $apkPath. `./gradlew :app:assembleDebug`를 먼저 실행.")
        }
        
        val androidHome = System.getenv("ANDROID_HOME")
            ?: throw GradleException("ANDROID_HOME 환경변수가 설정되지 않았습니다.")
        
        val buildToolsDir = java.io.File("$androidHome/build-tools")
            .listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
            ?: throw GradleException("build-tools를 찾을 수 없습니다: $androidHome/build-tools")
        
        val aapt = java.io.File(buildToolsDir, "aapt")
        if (!aapt.exists()) {
            throw GradleException("aapt를 찾을 수 없습니다: $aapt")
        }
        
        val output = ProcessBuilder(aapt.absolutePath, "dump", "permissions", apkPath.absolutePath)
            .redirectErrorStream(true)
            .start()
            .inputStream
            .bufferedReader()
            .readText()
        
        if (output.contains("android.permission.INTERNET")) {
            throw GradleException(
                """
                =====================================================
                [FAIL] INTERNET permission detected in APK.
                =====================================================
                Stack은 제로 네트워크 원칙을 따릅니다 (SSOT §1.2 / §11.2).
                INTERNET 권한이 manifest merge로 추가되었는지 확인하세요.
                
                APK: ${apkPath.absolutePath}
                
                전체 권한 덤프:
                $output
                """.trimIndent()
            )
        }
        
        logger.lifecycle("[OK] APK does not declare INTERNET permission.")
    }
}
```

### 3.4 `CheckNoHardcodedUserStringsTask.kt`

```kotlin
// build-logic/convention/src/main/kotlin/tasks/CheckNoHardcodedUserStringsTask.kt
package com.stack.player.buildlogic.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

/**
 * Android Lint의 HardcodedText 검사를 기반으로 하되,
 * 이 태스크는 Compose의 Text("...") 같은 Kotlin 소스 내 하드코딩을 추가 검사한다.
 *
 * 간단한 정규식 기반 1차 방어선. Android Lint가 XML을 검사, 이 태스크가 .kt를 검사.
 * 명확한 예외(@Preview용 더미, 주석, 로그)는 휴리스틱으로 스킵.
 *
 * 실제 개발 단계에서는 Compose Lint Rules 라이브러리 도입 검토 (v1.1+).
 */
abstract class CheckNoHardcodedUserStringsTask : DefaultTask() {
    
    init {
        group = "verification"
        description = "Compose 등 Kotlin 코드에 하드코딩된 사용자 노출 문자열을 탐지."
    }
    
    @get:InputFiles
    val sourceFiles = project.objects.fileCollection()
    
    @TaskAction
    fun check() {
        val suspiciousLines = mutableListOf<String>()
        
        // 1차: Compose Text("...") 패턴
        // 예외: @Preview 내부, 주석, logger 호출, stringResource 인자, 공백만 있는 문자열
        val textPattern = Regex("""\bText\s*\(\s*"([^"]{2,})"""")
        val testFilePattern = Regex("""(/test/|/androidTest/|/preview/|Preview\.kt$)""")
        
        sourceFiles.asFileTree.matching {
            include("**/*.kt")
            exclude("**/build/**")
        }.forEach { file ->
            if (testFilePattern.containsMatchIn(file.absolutePath)) return@forEach
            
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("*")) return@forEachIndexed
                
                textPattern.findAll(line).forEach { match ->
                    val text = match.groupValues[1]
                    // 단순 whitespace, ASCII 심볼만, @-prefix (resource ref) 등은 스킵
                    if (text.isBlank()) return@forEach
                    if (text.startsWith("@")) return@forEach
                    if (text.all { !it.isLetter() }) return@forEach
                    
                    suspiciousLines += "${file.relativeTo(project.rootDir)}:${index + 1}  →  Text(\"$text\")"
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
                appendLine("규칙: CLAUDE.md §1.3 / SSOT §10.")
            }
            // v1.0에서는 경고만. v1.1+에서 GradleException으로 승격 검토.
            logger.warn(report)
            // throw GradleException(report)
        } else {
            logger.lifecycle("[OK] No hardcoded user-facing strings detected in Compose Text(...).")
        }
    }
}
```

> **주의**: 이 태스크는 v1.0에서는 **경고만**. Compose Lint Rules를 도입하면 더 정교한 검사 가능. 현재는 단순 1차 방어선.

### 3.5 Convention Plugin 업데이트

#### `stack.android.library.gradle.kts` (ktlint/detekt 추가)

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

configure<LibraryExtension> {
    compileSdk = libs.versions.compileSdk.get().toInt()
    
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    lint {
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

detekt {
    toolVersion = libs.versions.detekt.get()
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    allRules = false
}
```

`stack.android.application.gradle.kts`, `stack.kotlin.library.gradle.kts`도 ktlint/detekt 플러그인 추가 (동일 패턴).

### 3.6 `config/detekt/detekt.yml`

```yaml
# Stack Detekt Configuration
# 기본 규칙 기반, Stack 특화 조정.

build:
  maxIssues: 0
  excludeCorrectable: false

comments:
  CommentOverPrivateFunction:
    active: false
  EndOfSentenceFormat:
    active: false
  UndocumentedPublicClass:
    active: false
  UndocumentedPublicFunction:
    active: false
  UndocumentedPublicProperty:
    active: false

complexity:
  LongMethod:
    active: true
    threshold: 60
  LongParameterList:
    active: true
    functionThreshold: 6
    constructorThreshold: 7
    ignoreDefaultParameters: true
    ignoreAnnotated: ['Composable']
  ComplexCondition:
    active: true
    threshold: 4
  CyclomaticComplexMethod:
    active: true
    threshold: 15

naming:
  FunctionNaming:
    active: true
    ignoreAnnotated: ['Composable', 'Test']
  TopLevelPropertyNaming:
    active: true

style:
  MagicNumber:
    active: true
    ignoreNumbers: ['-1', '0', '1', '2']
    ignoreHashCodeFunction: true
    ignoreEnums: true
    ignoreAnnotation: true
  MaxLineLength:
    active: true
    maxLineLength: 120
  WildcardImport:
    active: true
    excludeImports:
      - 'kotlinx.coroutines.*'
      - 'androidx.compose.runtime.*'
      - 'androidx.compose.foundation.layout.*'
      - 'androidx.compose.material3.*'
  ForbiddenComment:
    active: true
    comments:
      - reason: 'TODO는 phases/*-OPEN.md 또는 GitHub Issue로 관리'
        value: 'TODO:'
      - reason: 'FIXME는 GitHub Issue로 관리'
        value: 'FIXME:'
      - reason: 'XXX는 명확한 이슈로 관리'
        value: 'XXX:'
  ReturnCount:
    active: true
    max: 4
  UnusedImports:
    active: true
  UnusedPrivateMember:
    active: true
```

### 3.7 `gradle/libs.versions.toml` 업데이트

```toml
[versions]
# 기존에 추가
# ...
ktlint = "12.1.2"
detekt = "1.23.7"

[plugins]
# 기존에 추가
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

### 3.8 `build-logic/convention/build.gradle.kts` 업데이트

```kotlin
plugins {
    `kotlin-dsl`
}

group = "com.stack.player.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
    compileOnly(libs.ktlint.gradle.plugin)
    compileOnly(libs.detekt.gradle.plugin)
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
```

`libs.versions.toml`에 추가:
```toml
[libraries]
ktlint-gradle-plugin = { group = "org.jlleitschuh.gradle.ktlint", name = "org.jlleitschuh.gradle.ktlint.gradle.plugin", version.ref = "ktlint" }
detekt-gradle-plugin = { group = "io.gitlab.arturbosch.detekt", name = "detekt-gradle-plugin", version.ref = "detekt" }
```

### 3.9 Root `build.gradle.kts` 업데이트

```kotlin
import com.stack.player.buildlogic.tasks.CheckModuleDependenciesTask
import com.stack.player.buildlogic.tasks.CheckNoInternetPermissionTask
import com.stack.player.buildlogic.tasks.CheckNoHardcodedUserStringsTask

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// ========== 검증 태스크 등록 ==========

tasks.register<CheckModuleDependenciesTask>("checkModuleDependencies")

tasks.register<CheckNoInternetPermissionTask>("checkNoInternetPermission") {
    dependsOn(":app:assembleDebug")
    apkFile.set(
        layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk")
    )
}

tasks.register<CheckNoHardcodedUserStringsTask>("checkNoHardcodedUserStrings") {
    sourceFiles.setFrom(
        subprojects.map { project ->
            project.layout.projectDirectory.dir("src")
        }
    )
}

// ========== 통합 게이트 ==========

tasks.register("checkAll") {
    group = "verification"
    description = "Phase 종료 전 통과 필수. 정적 분석 + 의존성 + 권한 + 테스트 전체."
    
    dependsOn(
        // 정적 분석
        subprojects.map { "${it.path}:ktlintCheck" },
        subprojects.map { "${it.path}:detekt" },
        // Android Lint
        ":app:lintDebug",
        // 모듈 의존성
        "checkModuleDependencies",
        // 단위 테스트
        subprojects.map { "${it.path}:testDebugUnitTest" }
            .filter { path ->
                // JVM 모듈은 testDebugUnitTest 없음 → test
                val moduleName = path.removeSuffix(":testDebugUnitTest")
                !moduleName.endsWith(":core:common") && !moduleName.endsWith(":core:domain")
            },
        // INTERNET 권한
        "checkNoInternetPermission",
        // 하드코딩 문자열
        "checkNoHardcodedUserStrings",
    )
}
```

> 위 `checkAll`의 test task 필터링 로직은 단순화를 위해 Phase 3 이후 개선 가능. Phase 2 시점에는 모든 서브프로젝트에 테스트가 없으므로 실제 실행은 no-op.

### 3.10 `.github/workflows/verify.yml`

```yaml
name: Verify

on:
  push:
    branches: [main]
  pull_request:

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: ${{ github.ref != 'refs/heads/main' }}

jobs:
  verify:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}
      
      - name: Verify
        run: ./gradlew checkAll --stacktrace
      
      - name: Upload lint reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: lint-reports
          path: |
            **/build/reports/lint-results-*.html
            **/build/reports/detekt/
            **/build/reports/ktlint/
      
      - name: Upload test reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/build/reports/tests/'
```

---

## 4. 제약 사항

- **새 의존성 추가**: ktlint, detekt 플러그인만 (사용자 승인 받은 상태).
- **module의 build.gradle.kts 수정 금지** (ktlint/detekt는 convention plugin에서 자동 적용).
- **기존 코드 수정 금지** (Phase 1의 MainActivity 등).
- **detekt.yml 규칙 완화 금지** (CLAUDE.md 절대 규칙 기반).

---

## 5. 역검증 (의도적 위반)

**이 phase는 안전망이 실제로 작동하는지 확인하는 역검증 단계를 반드시 포함합니다.** 

다음을 **임시로** 시도한 뒤 원복:

### 5.1 모듈 의존성 위반 검증

`feature/library/build.gradle.kts`를 임시로 수정:
```kotlin
dependencies {
    implementation(project(":feature:player"))  // ← 금지
}
```

실행:
```bash
./gradlew :checkModuleDependencies
```

기대 출력:
```
[FAIL] Module dependency rule violations (1):
  - :feature:library depends on :feature:player (forbidden by rule: :feature:*)
```

**확인 후 즉시 수정 사항 되돌리기**:
```bash
git checkout feature/library/build.gradle.kts
```

### 5.2 INTERNET 권한 위반 검증

`app/src/main/AndroidManifest.xml`에 임시로 추가:
```xml
<uses-permission android:name="android.permission.INTERNET" />
```

실행:
```bash
./gradlew :app:assembleDebug :checkNoInternetPermission
```

기대 출력:
```
[FAIL] INTERNET permission detected in APK.
```

**확인 후 즉시 되돌리기**:
```bash
git checkout app/src/main/AndroidManifest.xml
```

### 5.3 Detekt 위반 검증

임시 파일 작성:
```kotlin
// app/src/main/kotlin/com/stack/player/_Temp.kt
fun foo() {
    // TODO: this will fail detekt  ← ForbiddenComment 위반
    val x = 12345  // MagicNumber 위반
}
```

실행:
```bash
./gradlew :app:detekt
```

기대: 빌드 실패 + ForbiddenComment/MagicNumber 위반 리포트.

**확인 후 파일 삭제**.

### 5.4 기록

이 역검증 실행 결과를 PR 본문에 기록:

```markdown
## 역검증 (안전망 작동 확인)

- [x] 모듈 의존성 위반 (feature:library → feature:player) 시도 → checkModuleDependencies 실패 확인
- [x] INTERNET 권한 추가 시도 → checkNoInternetPermission 실패 확인
- [x] TODO 주석 + magic number 추가 시도 → detekt 실패 확인
- [x] 위 모든 시도를 되돌린 후 checkAll 통과
```

---

## 6. 종료 조건

### 6.1 자동 검증

```bash
# 통합 게이트 통과
./gradlew checkAll

# 개별 task 존재 확인
./gradlew tasks --group=verification | grep -E "checkAll|checkModuleDependencies|checkNoInternetPermission|checkNoHardcodedUserStrings"

# APK 빌드 + 권한 검사
./gradlew :app:assembleDebug :checkNoInternetPermission
```

### 6.2 수동 검증 (역검증 포함)

- [ ] §5.1 의존성 위반 검증 완료 (원복 확인)
- [ ] §5.2 INTERNET 위반 검증 완료 (원복 확인)
- [ ] §5.3 Detekt 위반 검증 완료 (원복 확인)
- [ ] `./gradlew checkAll`이 clean 상태에서 통과
- [ ] GitHub Actions workflow (`.github/workflows/verify.yml`)가 PR에서 실행됨 (PR 후 확인)

### 6.3 CI 검증

PR을 생성한 뒤 GitHub Actions에서 `verify` 잡이 초록으로 통과하는지 확인.

---

## 7. 실패 시 대응

### 7.1 `checkAll` 의존 task가 존재하지 않음

```
Task 'testDebugUnitTest' not found in project ':core:common'.
```

→ `:core:common`은 JVM 모듈이므로 `test` task만 있음. `checkAll`의 test task 리스트를 JVM/Android 모듈 구분하여 필터링. 또는 각 convention plugin에서 `check` task를 재지정.

### 7.2 detekt가 기존 코드에서 실패

Phase 1의 MainActivity 등에서 `MagicNumber`, `LongMethod` 등 경고.

→ `detekt.yml`에서 해당 규칙의 예외 추가 (예: `ignoreNumbers`에 특정 상수 추가) 또는 Phase 1 코드 리팩터. 단, **규칙을 비활성화하지 말 것** — 예외는 명시적으로.

### 7.3 ktlint가 실패

자동 포맷 적용:
```bash
./gradlew ktlintFormat
```

### 7.4 GitHub Actions에서만 실패

- ANDROID_HOME 경로가 다름 → 직접 설정하지 말고 `actions/setup-java@v4`와 `gradle/actions/setup-gradle@v4`에 의존.
- Build tools 버전이 다름 → `CheckNoInternetPermissionTask`의 aapt 경로 로직이 최대 버전 자동 선택하도록 구현됨 (§3.3).

---

## 8. 커밋 / PR

### 8.1 커밋 메시지

```
chore(build): Phase 2 의존성 enforcement 및 검증 게이트 구축

- ModuleDependencyRules: SSOT §3.3 의존성 규칙을 코드화
- CheckModuleDependenciesTask: 모듈 간 금지 의존성 검출
- CheckNoInternetPermissionTask: APK 매니페스트 INTERNET 권한 차단
- CheckNoHardcodedUserStringsTask: Compose Text(...) 하드코딩 경고
- ktlint / detekt 통합 (convention plugin으로 전 모듈 자동 적용)
- detekt.yml: Stack 특화 규칙 설정
- checkAll: 통합 검증 게이트
- GitHub Actions: verify 워크플로

역검증으로 의존성/권한/detekt 위반 케이스가 실제로 빌드 실패를 트리거함을 확인.

Refs: docs/SSOT_v5.0.md §3.3 §11 §15.4, docs/BUILD.md §6,
      CLAUDE.md §1 §3.2 §5.2, phases/02-dependency-enforcement.md
```

### 8.2 PR 본문

```markdown
## 무엇
모든 후속 phase를 지탱할 안전망 구축. 
- 모듈 의존성 규칙 자동 검증
- APK 매니페스트 INTERNET 권한 차단
- ktlint / detekt / Android Lint 통합
- GitHub Actions CI

## 왜
SSOT §3.3의 의존성 그래프, §11.2의 제로 네트워크 원칙, 
CLAUDE.md §1의 절대 규칙을 코드 수준에서 강제. 
Phase 3~14에서 규칙 회귀를 자동 차단.

## 어떻게
- Convention plugin에서 ktlint/detekt 자동 적용 → 각 모듈의 build.gradle.kts 수정 불필요
- 커스텀 Gradle task 3종을 build-logic에 추가
- `checkAll` 한 명령으로 전체 검증

## 검증

### 정상 케이스
- [x] `./gradlew checkAll` 통과
- [x] `./gradlew :app:assembleDebug` 통과

### 역검증 (안전망 작동 확인)
- [x] `:feature:library`에 `:feature:player` 의존 추가 시도 → checkModuleDependencies 실패 확인
- [x] AndroidManifest에 INTERNET 추가 시도 → checkNoInternetPermission 실패 확인
- [x] TODO 주석 + magic number 추가 → detekt 실패 확인
- [x] 위 모든 시도를 되돌린 후 checkAll 통과

### CI
- [x] GitHub Actions `verify` 잡 통과

## 다음 Phase
Phase 3: `:core:design` 토큰 + `:core:common` 유틸. 첫 실제 코드 작성.
```

### 8.3 CLAUDE.md §10.4 갱신

Phase 2 체크박스 `☑`.

---

## 9. 가정 / 질문

- **Q. `checkNoHardcodedUserStrings`가 경고만 내는 이유?**
  - A. 정규식 기반 1차 방어선은 false positive 가능성이 높음. 엄격 강제는 v1.1+에서 Compose Lint Rules 도입 후. 현재는 경고로 인지만.
- **Q. 왜 `lint.warningsAsErrors = true`?**
  - A. Android Lint의 `MissingTranslation`, `HardcodedText` 등이 경고로만 떠서 간과되기 쉬움. Phase 3부터 i18n 누락 즉시 빌드 실패.
- **Q. `checkAll`에 macrobenchmark가 빠진 이유?**
  - A. macrobenchmark는 실제 기기 필요 → CI에서 실행 어려움. Phase 14에서 별도 workflow로 분리.
- **Q. 역검증 단계가 정말 필요한가?**
  - A. **필수**. 안전망이 "보이지만 실제로 작동 안 하는" 상태로 남는 것이 가장 위험. 1회 역검증으로 확실히.

---

## 10. 이 phase 완료 후 효과

앞으로 phase 3~14에서:
- 의존성 그래프 위반 시도 → 즉시 빌드 실패
- INTERNET 권한 새어 들어옴 → 즉시 빌드 실패
- 코드 스타일 위반 → 즉시 빌드 실패
- i18n 누락 → 즉시 빌드 실패 (Android Lint)
- Detekt 복잡도/금지 주석 → 즉시 빌드 실패
- 하드코딩 문자열 → 경고 (v1.1+에서 빌드 실패 승격)

**Claude Code가 phase 범위를 벗어나거나 절대 규칙을 어기는 코드를 작성해도, 커밋 전에 빌드가 실패하여 걸러집니다.**

---

*Phase 2 끝 — 이 phase가 통과하면 Phase 3로.*
