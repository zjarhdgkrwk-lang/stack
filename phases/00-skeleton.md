# Phase 0 — 저장소 스켈레톤

> **목표**: Gradle 멀티모듈 빌드의 최소 골격을 만든다. 코드는 아직 없다. 이 phase의 성공은 `./gradlew help`가 에러 없이 실행되고, 프로젝트를 Android Studio에서 열었을 때 sync가 깨끗하게 끝나는 것.

---

## 1. 컨텍스트

- **선행**: 없음 (첫 phase).
- **참조 문서**:
  - `CLAUDE.md` 전체
  - `docs/SSOT_v5.0.md` §2 (기술 스택), §14 (빌드 & CI)
  - `docs/BUILD.md` §5 (Convention Plugins 구조), §7 (Version Catalog), §15 (gradle.properties)

---

## 2. 작업 범위

### 수정 허용 디렉토리 / 파일

저장소 루트에 다음 파일/폴더를 신규 생성:

```
stack/
├── build.gradle.kts                   # root build script
├── settings.gradle.kts                # module 목록은 비어있음 (Phase 1에서 추가)
├── gradle.properties
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       ├── gradle-wrapper.properties
│       └── gradle-wrapper.jar
├── gradlew
├── gradlew.bat
├── build-logic/
│   ├── settings.gradle.kts
│   └── convention/
│       ├── build.gradle.kts
│       └── src/main/kotlin/            # (빈 디렉토리, .gitkeep)
└── .gitkeep들 (필요한 곳)
```

### 수정 금지

- `docs/`, `CLAUDE.md`, `CHANGELOG.md`, `README.md`, `phases/`
- `.gitignore`, `.gitattributes`, `.editorconfig` (이미 배치됨 — Phase 0 전에 수동 배치)
- `app/`, `core/`, `feature/` (Phase 1에서 추가)

---

## 3. 산출물 상세

### 3.1 `gradle/wrapper/gradle-wrapper.properties`

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

### 3.2 `gradle/wrapper/gradle-wrapper.jar` 및 `gradlew`, `gradlew.bat`

다음 명령으로 생성 (Gradle이 로컬에 없으면 SDKMAN 등으로 설치 후):

```bash
gradle wrapper --gradle-version 8.10.2 --distribution-type bin
```

**주의**: `gradle-wrapper.jar`는 바이너리이므로 반드시 git에 커밋. `.gitignore`에서 `!gradle-wrapper.jar`로 예외 처리 되어 있음.

### 3.3 `gradle.properties`

```properties
# JVM
org.gradle.jvmargs=-Xmx4g -Xss2m -XX:+UseG1GC -Dfile.encoding=UTF-8

# 빌드 가속
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn

# AndroidX
android.useAndroidX=true
android.nonTransitiveRClass=true
android.nonFinalResIds=true
android.enableJetifier=false

# Kotlin
kotlin.code.style=official
kotlin.incremental=true

# KSP
ksp.useKSP2=true
```

### 3.4 `gradle/libs.versions.toml`

`docs/BUILD.md` §7.2의 카탈로그를 그대로 사용하되, Phase 0에서는 **실제로 사용할 항목만** 포함. 이후 phase에서 필요시 추가.

```toml
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

[libraries]
# 빈 상태. Phase 1부터 추가.
```

> 주의: `[libraries]`는 빈 섹션으로 두면 TOML 파서가 에러. 주석만 두거나, 첫 library를 placeholder로 추가 후 Phase 1에서 실제 libs 추가.

### 3.5 `settings.gradle.kts` (root)

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "stack"

// 모듈은 Phase 1에서 추가
```

### 3.6 `build.gradle.kts` (root)

```kotlin
// Top-level build file.
// 모듈별 plugin 적용은 각 모듈의 build.gradle.kts에서.

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

### 3.7 `build-logic/settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"

include(":convention")
```

### 3.8 `build-logic/convention/build.gradle.kts`

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
}
```

> 이 파일은 `libs.android.gradle.plugin`, `libs.kotlin.gradle.plugin`, `libs.ksp.gradle.plugin`을 참조하므로 `libs.versions.toml`에 다음을 추가해야 함:

```toml
[libraries]
android-gradle-plugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
kotlin-gradle-plugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
ksp-gradle-plugin = { group = "com.google.devtools.ksp", name = "com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }
```

### 3.9 `build-logic/convention/src/main/kotlin/.gitkeep`

빈 디렉토리를 git에 추적시키기 위해. Phase 1에서 실제 convention plugin 파일들이 추가됨.

### 3.10 `local.properties` 는 **커밋 금지**

`.gitignore`에 이미 포함됨. 로컬에서만 존재.

```properties
sdk.dir=/Users/{username}/Library/Android/sdk
```

Android Studio가 자동 생성. 수동 생성도 가능.

---

## 4. 제약 사항

- **새 의존성 추가 금지** (§3.4의 version catalog 내용만, 사용자 승인 받은 상태).
- **새 권한 선언 금지** (아직 manifest 자체가 없음).
- **Convention plugin 코드 작성 금지** (Phase 2 담당).
- **app 또는 feature/core 모듈 생성 금지** (Phase 1 담당).
- `./gradlew wrapper` 실행 시 네트워크 필요 — 최초 1회 필요.

---

## 5. 작업 순서 (권장)

1. 로컬에서 Gradle CLI로 wrapper 생성: `gradle wrapper --gradle-version 8.10.2 --distribution-type bin`
2. 결과 파일들을 커밋 (`gradlew`, `gradlew.bat`, `gradle/wrapper/*`)
3. `.gitignore` / `.gitattributes` / `.editorconfig` 배치 확인 (이미 있음)
4. `gradle.properties` 작성
5. `gradle/libs.versions.toml` 작성 (§3.4)
6. `build-logic/` 구조 생성 (§3.7, §3.8)
7. root `settings.gradle.kts` 작성 (§3.5)
8. root `build.gradle.kts` 작성 (§3.6)
9. `./gradlew help` 실행 → 통과 확인
10. Android Studio에서 프로젝트 열기 → sync 통과 확인

---

## 6. 종료 조건

다음이 **모두** 통과해야 Phase 0 완료:

### 6.1 자동 검증

```bash
# Gradle wrapper 동작
./gradlew --version
# 출력에 "Gradle 8.10.2", "Kotlin 2.0.x", "JVM 17" 포함

# 기본 task 실행
./gradlew help
# 출력: "BUILD SUCCESSFUL"

# 프로젝트 구조 확인
./gradlew projects
# 출력: "Root project 'stack'" + (No sub-projects)

# build-logic 컴파일 가능 여부
./gradlew :build-logic:convention:compileKotlin
# 에러 없이 통과 (현재는 소스가 없으므로 빈 성공)
```

### 6.2 수동 검증

- [ ] Android Studio에서 프로젝트를 열었을 때 **sync가 에러 없이** 완료된다
- [ ] IDE의 Project 창에서 `gradle/libs.versions.toml`이 **인식 가능한 Version Catalog**로 표시된다
- [ ] `gradle-wrapper.jar`가 git에 추적되어 있다 (`git ls-files | grep wrapper`)
- [ ] `local.properties`는 git에 추적되지 **않는다** (`git status` 클린)
- [ ] `.editorconfig`가 IDE에 적용되어 새 Kotlin 파일이 4-space 들여쓰기로 자동 포맷된다

### 6.3 파일 목록 검증

```bash
# 다음 파일들이 모두 존재해야 함
test -f build.gradle.kts
test -f settings.gradle.kts
test -f gradle.properties
test -f gradle/libs.versions.toml
test -f gradle/wrapper/gradle-wrapper.properties
test -f gradle/wrapper/gradle-wrapper.jar
test -f gradlew
test -f gradlew.bat
test -f build-logic/settings.gradle.kts
test -f build-logic/convention/build.gradle.kts
test -d build-logic/convention/src/main/kotlin
echo "All files present"
```

---

## 7. 실패 시 대응

### 7.1 `./gradlew --version`이 실패

- JDK 17이 `JAVA_HOME`에 설정되어 있는가?
- `gradle-wrapper.jar`가 손상되지 않았는가? (100KB 내외여야 정상)

### 7.2 Android Studio sync 실패

- `local.properties`의 `sdk.dir`이 올바른 경로인가?
- `pluginManagement`에서 google/mavenCentral 저장소가 모두 포함되어 있는가?
- Gradle JDK가 17로 설정되어 있는가? (Preferences → Build → Gradle)

### 7.3 `compileOnly(libs.android.gradle.plugin)` 해결 실패

- `libs.versions.toml`의 `[libraries]` 섹션이 올바른가?
- `build-logic/settings.gradle.kts`가 `versionCatalogs { from(files("../gradle/libs.versions.toml")) }`를 갖고 있는가?

### 7.4 부분 완료 시

`phases/00-skeleton-OPEN.md`에 다음을 기록:

```markdown
## 완료
- [x] gradle wrapper
- [x] settings.gradle.kts
...

## 미완료
- [ ] build-logic convention 컴파일 — compileOnly 의존성 해결 실패
  - 원인 후보: ...
  - 시도한 것: ...
  - 차단 요인: ...
```

---

## 8. 커밋 / PR

### 8.1 커밋 메시지

```
chore(build): Phase 0 저장소 스켈레톤 구성

- Gradle 8.10.2 wrapper
- root build.gradle.kts / settings.gradle.kts
- gradle/libs.versions.toml (버전 카탈로그 초기 구조)
- build-logic/convention/ 모듈 (빈 상태, Phase 2에서 채움)
- gradle.properties (JVM/병렬/캐시/AndroidX 설정)

Refs: docs/SSOT_v5.0.md §14, docs/BUILD.md §5 §7, phases/00-skeleton.md
```

### 8.2 PR 본문 템플릿

```markdown
## 무엇
Gradle 멀티모듈 빌드의 최소 골격을 생성. 이 PR 시점에 `./gradlew help`가 통과.

## 왜
모든 후속 phase가 이 기반 위에서 동작. SSOT §14의 빌드 구성 요건 충족.

## 어떻게
- Gradle 8.10.2 / AGP 8.7.2 / Kotlin 2.0.21 / JDK 17
- build-logic은 별도 included build로 (convention plugin 분리)
- Version catalog 도입

## 검증
- [x] `./gradlew --version` 통과
- [x] `./gradlew help` 통과
- [x] `./gradlew :build-logic:convention:compileKotlin` 통과 (빈 성공)
- [x] Android Studio sync 성공
- [x] `gradle-wrapper.jar` git 추적, `local.properties` 미추적

## 다음 Phase
Phase 1: 13개 모듈 셸 추가.
```

### 8.3 CLAUDE.md §10.4 갱신

PR 머지 후, 별도의 작은 PR로 CLAUDE.md §10.4의 Phase 0 체크박스를 `☑`로 변경.

---

## 9. 가정 / 질문

없음 (Phase 0은 결정이 명확). 예상되는 질문:

- Q. Gradle 버전을 8.10.2로 고정한 이유?
  - A. AGP 8.7.2와 호환되는 최신 안정판 (2024년 11월 기준). 향후 8.11+ 업데이트 시 별도 chore PR.
- Q. `kotlin.experimental.tryK2`를 gradle.properties에 추가해야 하는가?
  - A. Kotlin 2.0부터 K2가 기본이므로 불필요.
- Q. `android.enableJetifier=false`로 둔 이유?
  - A. 모든 의존성이 AndroidX이므로 Jetifier 불필요. 빌드 속도 향상.

---

*Phase 0 프롬프트 끝 — 이 phase가 통과하면 Phase 1로.*
