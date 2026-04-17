# Stack — Build Guide

> 본 문서는 Stack 프로젝트의 빌드, 테스트, 배포 절차를 다룹니다.
> 명령은 모두 저장소 루트에서 실행하는 것을 가정합니다.

---

## 1. Prerequisites

### 1.1 필수 도구

| 도구 | 버전 | 비고 |
|---|---|---|
| JDK | 17 (LTS) | Temurin 권장 |
| Android Studio | Ladybug 2024.2.1+ | AGP 8.7+ 호환 |
| Android SDK | API 35 | compileSdk |
| Android SDK Build-Tools | 35.0.0+ | |
| Android Emulator | API 26+ | minSdk 검증용 |
| Git | 2.40+ | |

### 1.2 환경 변수

```bash
# ~/.zshrc 또는 ~/.bashrc
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"  # macOS 예
export ANDROID_HOME="$HOME/Library/Android/sdk"                                    # macOS 예
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/35.0.0:$PATH"
```

### 1.3 검증

```bash
java -version          # 17.x
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --list_installed
adb --version
```

---

## 2. 첫 빌드 (Onboarding)

### 2.1 클론 및 brand-new 빌드

```bash
git clone {저장소}
cd stack
./gradlew --version    # Gradle wrapper가 자동 설치
./gradlew :app:assembleDebug
```

첫 빌드는 의존성 다운로드로 5~15분 소요. 이후는 캐시되어 1~2분.

### 2.2 local.properties

자동 생성되지만 누락 시:

```properties
# local.properties (gitignore됨)
sdk.dir=/Users/{user}/Library/Android/sdk
```

### 2.3 IDE 설정

- Android Studio에서 "Open" → 저장소 루트 선택.
- Gradle JDK: File → Settings → Build → Gradle → Gradle JDK = 17.
- Kotlin Compiler: K2 활성 확인 (`kotlin.experimental.tryK2=true`는 Kotlin 2.0에서 기본).

### 2.4 .editorconfig 적용 확인

- ktlint 규칙이 IDE에 자동 적용되는지 확인.
- "Reformat Code" (Cmd+Alt+L) 실행 시 4-space 들여쓰기, trailing comma 등이 적용되어야 함.

---

## 3. 빌드 변형

### 3.1 Build types

| Build type | applicationId 접미사 | minify | shrinkResources | signing |
|---|---|---|---|---|
| `debug` | `.debug` | false | false | debug.keystore (자동) |
| `release` | (없음) | true | true | upload key (수동 구성) |

`debug`와 `release`를 같은 기기에 동시 설치 가능.

### 3.2 Gradle 명령

```bash
# 디버그 APK
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# 디버그 설치 + 실행
./gradlew :app:installDebug
adb shell am start -n com.stack.player.debug/com.stack.player.MainActivity

# 릴리스 APK (서명 필요, §9 참조)
./gradlew :app:assembleRelease

# 릴리스 AAB (Play Store 업로드용)
./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab

# 모두 정리
./gradlew clean
```

### 3.3 빠른 반복

```bash
# 한 모듈만 빌드 (의존 모듈은 캐시 사용)
./gradlew :feature:library:assembleDebug

# 변경된 것만
./gradlew :app:assembleDebug --build-cache

# 병렬 빌드 활성 (gradle.properties)
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
```

---

## 4. 검증 (Verification)

### 4.1 통합 게이트

```bash
./gradlew checkAll
```

내부 실행 순서 (의존성 그래프 기반):

```
checkAll
  ├─ ktlintCheck
  ├─ detekt
  ├─ lintDebug                       (모든 모듈)
  ├─ checkModuleDependencies          (커스텀 task)
  ├─ testDebugUnitTest                (모든 모듈)
  ├─ checkNoInternetPermission        (커스텀 task, assembleDebug 의존)
  └─ checkNoHardcodedUserStrings      (커스텀 lint)
```

매 phase 종료 전 통과 필수. CI도 동일 명령.

### 4.2 정적 분석

```bash
./gradlew ktlintCheck                 # 코드 스타일
./gradlew ktlintFormat                # 자동 수정
./gradlew detekt                      # 코드 스멜
./gradlew lintDebug                   # Android Lint
./gradlew :app:lintRelease            # release 변형 lint
```

리포트 위치:
- ktlint: `build/reports/ktlint/`
- detekt: `build/reports/detekt/`
- lint: `app/build/reports/lint-results-debug.html`

### 4.3 단위 테스트

```bash
./gradlew testDebugUnitTest                              # 모든 모듈
./gradlew :core:audio:testDebugUnitTest                  # 한 모듈
./gradlew :core:audio:testDebugUnitTest --tests "*Cross*" # 패턴 매칭
./gradlew :core:audio:testDebugUnitTest --info           # 자세히
```

리포트: `{module}/build/reports/tests/testDebugUnitTest/index.html`

### 4.4 instrumentation 테스트

```bash
# 에뮬레이터 또는 기기 연결 필요
./gradlew :core:database:connectedDebugAndroidTest
./gradlew :app:connectedDebugAndroidTest
```

리포트: `{module}/build/reports/androidTests/connected/index.html`

### 4.5 Macrobenchmark

```bash
./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest \
  -P android.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

> Baseline profile / macrobenchmark는 실제 기기에서만 신뢰할 수 있는 수치.

### 4.6 모듈 의존성 검증

```bash
./gradlew :checkModuleDependencies
```

위반 발견 시 빌드 실패. 위반 케이스를 일부러 만들어 검증하려면:

```kotlin
// feature/library/build.gradle.kts
dependencies {
    implementation(project(":feature:player"))  // ← 위반: feature → feature 금지
}
```

→ `./gradlew :checkModuleDependencies` 실패.

### 4.7 권한 검증

```bash
./gradlew :app:assembleDebug
$ANDROID_HOME/build-tools/35.0.0/aapt dump permissions \
  app/build/outputs/apk/debug/app-debug.apk
```

기대 출력 (예시):
```
package: com.stack.player.debug
uses-permission: name='android.permission.READ_MEDIA_AUDIO'
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
uses-permission: name='android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK'
uses-permission: name='android.permission.WAKE_LOCK'
```

`INTERNET`이 출력되면 즉시 작업 중단 + 마지막 변경 추적.

자동화는 `:checkNoInternetPermission` 태스크 (다음 §6).

---

## 5. Convention Plugins (`build-logic/`)

### 5.1 구조

```
build-logic/
├── settings.gradle.kts               # build-logic을 별도 included build로
├── convention/
│   ├── build.gradle.kts              # convention 모듈 자체 빌드
│   └── src/main/kotlin/
│       ├── stack.android.application.gradle.kts
│       ├── stack.android.library.gradle.kts
│       ├── stack.android.library.compose.gradle.kts
│       ├── stack.android.feature.gradle.kts
│       ├── stack.kotlin.library.gradle.kts
│       ├── stack.hilt.gradle.kts
│       ├── stack.room.gradle.kts
│       └── tasks/
│           ├── CheckModuleDependenciesTask.kt
│           ├── CheckNoInternetPermissionTask.kt
│           └── CheckAllTask.kt
```

### 5.2 settings.gradle.kts (root)

```kotlin
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
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

include(
    ":app",
    ":core:common", ":core:design", ":core:ui",
    ":core:database", ":core:datastore",
    ":core:domain", ":core:audio",
    ":feature:library", ":feature:player",
    ":feature:playlist", ":feature:search", ":feature:settings",
)
```

### 5.3 모듈에서 사용

```kotlin
// feature/library/build.gradle.kts
plugins {
    id("stack.android.feature")
}

android {
    namespace = "com.stack.player.feature.library"
}

dependencies {
    implementation(projects.core.ui)
    implementation(projects.core.design)
    implementation(projects.core.domain)
    implementation(projects.core.common)
}
```

`stack.android.feature` 안에 다음이 자동 적용됨:
- compose 활성
- Hilt
- 표준 dependency (kotlinx.coroutines, lifecycle, navigation 등)
- ktlint, detekt
- `:checkModuleDependencies`에 등록

---

## 6. 커스텀 검증 태스크

### 6.1 `:checkModuleDependencies`

목적: 모듈 의존 그래프가 CLAUDE.md §3.2 / SSOT §3.3 규칙을 준수하는지 검증.

```kotlin
// build-logic/convention/src/main/kotlin/tasks/CheckModuleDependenciesTask.kt
abstract class CheckModuleDependenciesTask : DefaultTask() {
    @TaskAction
    fun check() {
        val rules = ModuleRules(
            forbidden = mapOf(
                ":feature:library" to listOf(":feature:player", ":feature:playlist", ":feature:search", ":feature:settings", ":core:database", ":core:audio", ":core:datastore"),
                ":core:domain" to listOf(":core:database", ":core:audio", ":core:datastore", ":app"),
                ":core:audio" to listOf(":feature:*", ":core:ui", ":core:design"),
                // ...
            ),
        )
        val violations = project.subprojects.flatMap { module ->
            val deps = module.configurations
                .flatMap { it.dependencies }
                .filterIsInstance<ProjectDependency>()
                .map { it.dependencyProject.path }
            rules.violationsFor(module.path, deps)
        }
        if (violations.isNotEmpty()) {
            throw GradleException(violations.joinToString("\n") { "Violation: $it" })
        }
    }
}
```

루트 `build.gradle.kts`에 등록:
```kotlin
tasks.register<CheckModuleDependenciesTask>("checkModuleDependencies")
```

### 6.2 `:checkNoInternetPermission`

```kotlin
abstract class CheckNoInternetPermissionTask : DefaultTask() {
    @get:InputFile
    abstract val apkFile: RegularFileProperty
    
    @TaskAction
    fun check() {
        val aapt = "${System.getenv("ANDROID_HOME")}/build-tools/35.0.0/aapt"
        val output = ProcessBuilder(aapt, "dump", "permissions", apkFile.get().asFile.absolutePath)
            .redirectErrorStream(true)
            .start()
            .inputStream.bufferedReader().readText()
        if (output.contains("android.permission.INTERNET")) {
            throw GradleException("FORBIDDEN: INTERNET permission detected in APK\n$output")
        }
    }
}
```

`assembleDebug` 의존:
```kotlin
tasks.register<CheckNoInternetPermissionTask>("checkNoInternetPermission") {
    dependsOn(":app:assembleDebug")
    apkFile.set(layout.projectDirectory.file("app/build/outputs/apk/debug/app-debug.apk"))
}
```

### 6.3 `:checkAll`

```kotlin
tasks.register("checkAll") {
    group = "verification"
    description = "전체 검증 게이트. phase 종료 전 통과 필수."
    dependsOn(
        "ktlintCheck",
        "detekt",
        ":app:lintDebug",
        "checkModuleDependencies",
        "testDebugUnitTest",
        "checkNoInternetPermission",
    )
}
```

---

## 7. Version Catalog

### 7.1 위치

`gradle/libs.versions.toml`

### 7.2 구조

```toml
[versions]
kotlin = "2.0.21"
agp = "8.7.2"
hilt = "2.52"
room = "2.7.0"
media3 = "1.5.0"
compose-bom = "2024.10.01"
coroutines = "1.9.0"
lifecycle = "2.8.7"
navigation = "2.8.4"
coil = "3.0.4"
kotlinx-serialization = "1.7.3"
kotlinx-collections-immutable = "0.3.8"
datastore = "1.1.1"
work = "2.10.0"

junit5 = "5.11.3"
mockk = "1.13.13"
turbine = "1.2.0"
truth = "1.4.4"
robolectric = "4.14.1"

ktlint = "1.4.1"
detekt = "1.23.7"

[libraries]
kotlinx-coroutines-core = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-collections-immutable = { group = "org.jetbrains.kotlinx", name = "kotlinx-collections-immutable", version.ref = "kotlinx-collections-immutable" }
hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.2.0" }
room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-paging = { group = "androidx.room", name = "room-paging", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
coil-compose = { group = "io.coil-kt.coil3", name = "coil-compose", version.ref = "coil" }
# ... (전체 의존성)

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version = "2.0.21-1.0.28" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version = "12.1.2" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

### 7.3 사용

```kotlin
dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
```

### 7.4 새 의존성 추가 절차

1. **사용자 승인 받기** (CLAUDE.md §1.2).
2. `libs.versions.toml`의 `[versions]`에 버전 추가.
3. `[libraries]`에 좌표 추가.
4. (필요 시) `[plugins]`에 추가.
5. 모듈의 `build.gradle.kts`에서 `libs.xxx` 사용.
6. `./gradlew :app:assembleDebug` 통과 확인.
7. `:checkNoInternetPermission` 통과 확인.
8. PR 본문에 라이선스 명시 + APK 사이즈 변화 보고.

---

## 8. ProGuard / R8

### 8.1 모듈별 keep 규칙

각 모듈의 `consumer-rules.pro`에 모듈 사용자(다운스트림)가 알아야 할 keep 규칙을 작성. `proguard-rules.pro`는 모듈 자체 빌드용.

### 8.2 기본 규칙 (자동 포함)

- Compose, Hilt, Room: 공식 keep 규칙 자동.

### 8.3 명시 필요한 규칙

- **kotlinx.serialization**:
  ```proguard
  -keepattributes *Annotation*, InnerClasses
  -dontnote kotlinx.serialization.AnnotationsKt
  -keep,includedescriptorclasses class com.stack.player.**$$serializer { *; }
  -keepclassmembers class com.stack.player.** {
      *** Companion;
  }
  -keepclasseswithmembers class com.stack.player.** {
      kotlinx.serialization.KSerializer serializer(...);
  }
  ```

- **Reflective access** (예: 백업 manifest 역직렬화):
  ```proguard
  -keep class com.stack.player.core.backup.model.** { *; }
  ```

### 8.4 검증

```bash
./gradlew :app:assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
# 핵심 시나리오 수동 청취 (재생, 백업, 복원, EQ 등)
```

R8 매핑 파일은 `app/build/outputs/mapping/release/mapping.txt`. 크래시 stacktrace 디코딩에 보관 필수.

---

## 9. 서명 (Signing)

### 9.1 키스토어 생성 (1회)

```bash
keytool -genkey -v \
  -keystore ~/keys/stack-upload.keystore \
  -keyalg RSA -keysize 4096 -validity 25000 \
  -alias stack-upload
```

### 9.2 환경 변수 (절대 커밋 금지)

```bash
# ~/.zshrc 또는 CI secret
export STACK_KEYSTORE_PATH="$HOME/keys/stack-upload.keystore"
export STACK_KEYSTORE_PASSWORD="..."
export STACK_KEY_ALIAS="stack-upload"
export STACK_KEY_PASSWORD="..."
```

### 9.3 app/build.gradle.kts

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = System.getenv("STACK_KEYSTORE_PATH")?.let { file(it) }
            storePassword = System.getenv("STACK_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("STACK_KEY_ALIAS")
            keyPassword = System.getenv("STACK_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

서명 정보가 없으면 release 빌드는 unsigned APK를 생성 (배포 불가, 로컬 검증만 가능).

### 9.4 Play App Signing

업로드 키와 앱 서명 키를 분리. Google Play Console에서 앱 서명 키는 Google이 보관, 업로드 키만 개발자 보유.

---

## 10. CI 파이프라인

### 10.1 GitHub Actions 예시

```yaml
# .github/workflows/verify.yml
name: Verify

on:
  push:
    branches: [main]
  pull_request:

jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v4
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}
      - run: ./gradlew checkAll
      - name: Upload lint reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: lint-reports
          path: '**/build/reports/lint-results-*.html'
      - name: Upload test reports
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: test-reports
          path: '**/build/reports/tests/'

  build-release:
    runs-on: ubuntu-latest
    needs: verify
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - run: ./gradlew :app:bundleRelease
        env:
          STACK_KEYSTORE_PATH: ${{ secrets.STACK_KEYSTORE_PATH }}
          STACK_KEYSTORE_PASSWORD: ${{ secrets.STACK_KEYSTORE_PASSWORD }}
          STACK_KEY_ALIAS: ${{ secrets.STACK_KEY_ALIAS }}
          STACK_KEY_PASSWORD: ${{ secrets.STACK_KEY_PASSWORD }}
      - uses: actions/upload-artifact@v4
        with:
          name: app-release.aab
          path: app/build/outputs/bundle/release/app-release.aab
```

### 10.2 PR 보호

main 브랜치 보호 규칙:
- `verify` 잡 통과 필수
- 1인 이상 리뷰 승인
- 브랜치 최신 상태 요구
- force push 금지

---

## 11. APK 분석

### 11.1 사이즈 측정

```bash
./gradlew :app:assembleRelease
ls -lh app/build/outputs/apk/release/app-release.apk
```

### 11.2 APK Analyzer (Android Studio)

Build → Analyze APK → 선택. 다음을 확인:
- DEX 메서드 수 (64K 한계 — 멀티덱스로 회피)
- 리소스 크기 (이미지가 비대하면 압축)
- 라이브러리별 기여도

### 11.3 명령줄 분석

```bash
# 매니페스트
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump xmltree app-release.apk --file AndroidManifest.xml

# 리소스
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump resources app-release.apk

# 사이즈 분해
$ANDROID_HOME/build-tools/35.0.0/aapt2 dump badging app-release.apk
```

### 11.4 사이즈 회귀 방지

CI에서 PR마다 APK 사이즈 비교 후 +5% 초과 시 경고. (단순 GitHub Action으로 구현 가능.)

---

## 12. Baseline Profile

### 12.1 목적

- 콜드 스타트 시 핵심 코드 경로를 AOT 컴파일하여 첫 진입을 빠르게.

### 12.2 모듈

```kotlin
// baselineprofile/build.gradle.kts
plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    targetProjectPath = ":app"
    ...
}
```

### 12.3 생성

```bash
./gradlew :app:generateBaselineProfile \
  -P android.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

생성된 프로파일은 `app/src/main/baseline-prof.txt`에 자동 복사. APK에 포함되어 첫 설치 시 활용.

### 12.4 검증

```bash
./gradlew :app:assembleBenchmarkRelease
adb install app/build/outputs/apk/benchmarkRelease/app-benchmarkRelease.apk
# 콜드 스타트 측정
```

---

## 13. 디버깅

### 13.1 logcat 필터

```bash
adb logcat -s "Stack:*" "ExoPlayer:*" "MediaSession:*"
```

### 13.2 Hilt 디버깅

컴파일 에러:
```
[Dagger/MissingBinding] X cannot be provided without an @Inject constructor or an @Provides-annotated method.
```
→ `@Module` 누락, `@InstallIn` 누락, 또는 `@Binds`/`@Provides` 누락.

런타임 누락:
```
java.lang.IllegalStateException: Hilt component not found
```
→ Activity/Service에 `@AndroidEntryPoint` 누락.

### 13.3 Room 디버깅

```kotlin
Room.databaseBuilder(...)
    .setQueryCallback({ sql, args -> Log.d("Room", "$sql | $args") }, Executors.newSingleThreadExecutor())
    .build()
```

(release 빌드에서는 비활성).

### 13.4 Compose 디버깅

- Layout Inspector → Recomposition Counts: 폭증 부분 식별.
- `androidx.compose.runtime.tracing` 추가 시 Perfetto에서 합성 추적.

### 13.5 ANR 분석

```bash
adb pull /data/anr/traces.txt
```

메인 스레드 stack에서 차단 지점 확인. `Dispatchers.Main`에서 IO 작업이 가장 흔한 원인.

---

## 14. 트러블슈팅

| 증상 | 원인 후보 | 해결 |
|---|---|---|
| `Could not resolve all artifacts` | 인터넷 차단 / proxy / 의존성 좌표 오타 | 네트워크 / `~/.gradle/init.d/` proxy / `libs.versions.toml` 검토 |
| `Out of memory` 컴파일 중 | Gradle JVM heap 부족 | `gradle.properties`: `org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC` |
| KSP 에러 (Hilt/Room) | annotation processor 버전 불일치 | KSP 버전이 Kotlin 버전과 매칭되는지 확인 |
| `INTERNET` 권한 등장 | 새 의존성 manifest merge | `aapt dump permissions` + 의존성 추적 |
| Compose preview 렌더 실패 | preview용 더미 데이터 누락, theme 미적용 | `StackTheme { ... }` 감싸기, 더미 데이터 inline |
| ExoPlayer "wrong thread" | CommandDispatcher 우회 | 모든 player 호출을 `dispatcher.run { }`로 |
| 잠금화면 컨트롤 미표시 | MediaSession publish 실패 | `MediaSessionService` 등록, 알림 채널, `POST_NOTIFICATIONS` 권한 |
| release 빌드 크래시 | R8 strip된 클래스 | `mapping.txt`로 stack 디코딩, keep 규칙 추가 |
| `kotlinx.serialization` 런타임 에러 | keep 규칙 누락 | §8.3 참조 |
| Configuration cache 실패 | task 안 `Project` 직접 참조 | task input/output을 명시적으로 노출 |

---

## 15. gradle.properties 권장 설정

```properties
# JVM
org.gradle.jvmargs=-Xmx4g -Xss2m -XX:+UseG1GC -Dfile.encoding=UTF-8

# 빌드 가속
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
org.gradle.configuration-cache.problems=warn

# Android
android.useAndroidX=true
android.nonTransitiveRClass=true
android.nonFinalResIds=true
android.enableJetifier=false

# Kotlin
kotlin.code.style=official
kotlin.incremental=true
kapt.use.k2=true

# KSP
ksp.useKSP2=true

# Compose
android.experimental.enableScreenshotTest=true
```

---

## 16. 빠른 참조

```bash
# 첫 빌드
./gradlew :app:assembleDebug

# 검증 게이트
./gradlew checkAll

# 핵심 모듈 빠른 빌드
./gradlew :core:audio:assemble :core:audio:testDebugUnitTest

# 릴리스
./gradlew :app:bundleRelease

# APK 권한 확인
$ANDROID_HOME/build-tools/35.0.0/aapt dump permissions \
  app/build/outputs/apk/debug/app-debug.apk

# 의존성 그래프
./gradlew :feature:library:dependencies --configuration debugRuntimeClasspath

# 릴리스 매핑 보존
cp app/build/outputs/mapping/release/mapping.txt mapping/v1.0.0.txt
```

---

*마지막 갱신: 2026-04-17 / SSOT v5.0 §14, §15 기준*
