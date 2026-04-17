# Phase 1 — 13개 모듈 셸

> **목표**: SSOT §3.2의 13개 모듈을 모두 생성한다. 각 모듈은 비어있되 독립적으로 컴파일된다. 이 phase의 성공은 `./gradlew :app:assembleDebug`가 통과하고, APK를 기기에 설치하면 "Stack" 텍스트만 있는 빈 화면이 뜨는 것.

---

## 1. 컨텍스트

- **선행**: Phase 0 완료 (`./gradlew help` 통과, build-logic 빈 구조 존재).
- **참조 문서**:
  - `CLAUDE.md` 전체 (특히 §3 모듈 지도)
  - `docs/SSOT_v5.0.md` §3.1 (멀티모듈), §3.2 (모듈 목록), §3.3 (의존성 규칙), §7.1 (MVI)
  - `docs/BUILD.md` §5 (Convention Plugins), §7 (Version Catalog)
  - `docs/STYLE_GUIDE.md` §2 (파일/패키지)

---

## 2. 작업 범위

### 수정 허용

**신규 생성**:
```
app/
core/
  common/  design/  ui/  database/  datastore/  domain/  audio/
feature/
  library/  player/  playlist/  search/  settings/
```

각 모듈에는:
- `build.gradle.kts`
- `src/main/AndroidManifest.xml` (Android 모듈만)
- `src/main/kotlin/{package 경로}/.gitkeep` (빈 kotlin 디렉토리 유지)

**수정**:
- `settings.gradle.kts` (루트): 13개 모듈 `include()` 추가
- `gradle/libs.versions.toml`: 이 phase에서 사용하는 라이브러리 추가
- `build-logic/convention/src/main/kotlin/` 하위에 **최소한의** convention plugin만:
  - `stack.android.application.gradle.kts`
  - `stack.android.library.gradle.kts`
  - `stack.kotlin.library.gradle.kts`

### 수정 금지

- `docs/`, `CLAUDE.md`, `CHANGELOG.md`, `README.md`, `phases/`
- `.gitignore`, `.gitattributes`, `.editorconfig`, `gradle.properties`
- `gradle/wrapper/`, `gradlew`, `gradlew.bat`
- **Compose 설정 관련 convention plugin은 다음 phase 이후** (Phase 3에서 Compose 도입 시)
- **Hilt 설정은 이후 phase에서** (Repository 구현 phase까지 Hilt 불필요)

---

## 3. 모듈 목록 (13 + app)

| # | 경로 | Gradle 플러그인 | namespace | 비고 |
|---|---|---|---|---|
| 1 | `:app` | `stack.android.application` | `com.stack.player` | MainActivity, Application |
| 2 | `:core:common` | `stack.kotlin.library` | — (JVM) | 순수 Kotlin |
| 3 | `:core:design` | `stack.android.library` | `com.stack.player.core.design` | Compose는 Phase 3에서 |
| 4 | `:core:ui` | `stack.android.library` | `com.stack.player.core.ui` | Compose는 Phase 4에서 |
| 5 | `:core:database` | `stack.android.library` | `com.stack.player.core.database` | Room은 Phase 5에서 |
| 6 | `:core:datastore` | `stack.android.library` | `com.stack.player.core.datastore` | |
| 7 | `:core:domain` | `stack.kotlin.library` | — (JVM) | 순수 Kotlin |
| 8 | `:core:audio` | `stack.android.library` | `com.stack.player.core.audio` | ExoPlayer는 Phase 8에서 |
| 9 | `:feature:library` | `stack.android.library` | `com.stack.player.feature.library` | |
| 10 | `:feature:player` | `stack.android.library` | `com.stack.player.feature.player` | |
| 11 | `:feature:playlist` | `stack.android.library` | `com.stack.player.feature.playlist` | |
| 12 | `:feature:search` | `stack.android.library` | `com.stack.player.feature.search` | |
| 13 | `:feature:settings` | `stack.android.library` | `com.stack.player.feature.settings` | |

Phase 1에서는 **최소한의 의존성만** (Phase 2에서 전체 enforcement, Phase 3부터 각 모듈 내용 채움).

---

## 4. 산출물 상세

### 4.1 `settings.gradle.kts` (업데이트)

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

include(
    ":app",
    ":core:common",
    ":core:design",
    ":core:ui",
    ":core:database",
    ":core:datastore",
    ":core:domain",
    ":core:audio",
    ":feature:library",
    ":feature:player",
    ":feature:playlist",
    ":feature:search",
    ":feature:settings",
)
```

### 4.2 `gradle/libs.versions.toml` 업데이트

Phase 1에서 추가되는 항목:

```toml
[versions]
agp = "8.7.2"
kotlin = "2.0.21"
ksp = "2.0.21-1.0.28"

# 추가
minSdk = "26"
targetSdk = "35"
compileSdk = "35"
androidx-core = "1.13.1"
androidx-appcompat = "1.7.0"
androidx-activity = "1.9.3"

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

[libraries]
# build-logic 전용 (Phase 0에서 추가됨)
android-gradle-plugin = { group = "com.android.tools.build", name = "gradle", version.ref = "agp" }
kotlin-gradle-plugin = { group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version.ref = "kotlin" }
ksp-gradle-plugin = { group = "com.google.devtools.ksp", name = "com.google.devtools.ksp.gradle.plugin", version.ref = "ksp" }

# 최소 AndroidX (app 진입점 용)
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidx-core" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "androidx-appcompat" }
androidx-activity = { group = "androidx.activity", name = "activity", version.ref = "androidx-activity" }
```

### 4.3 Convention Plugins

#### `build-logic/convention/src/main/kotlin/stack.android.library.gradle.kts`

```kotlin
import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// libs version catalog 접근
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
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}
```

> 주의: `LibrariesForLibs` 자동 생성 accessor를 사용하려면 `build-logic/convention/build.gradle.kts`에 `implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))` 같은 workaround 또는 Gradle 공식 방법을 적용해야 함. 아래 §4.3.4 참조.

#### `build-logic/convention/src/main/kotlin/stack.android.application.gradle.kts`

```kotlin
import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val libs = the<org.gradle.accessors.dm.LibrariesForLibs>()

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
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false   // Phase 14에서 true로
            isShrinkResources = false // Phase 14에서 true로
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}
```

#### `build-logic/convention/src/main/kotlin/stack.kotlin.library.gradle.kts`

```kotlin
import org.gradle.api.JavaVersion

plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}
```

#### `build-logic/convention/build.gradle.kts` (업데이트)

convention plugin들이 version catalog accessor를 쓰려면 다음 설정 필요:

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
    // version catalog accessor generation
    compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "stack.android.application"
            implementationClass = "StackAndroidApplicationPlugin"  // 또는 스크립트 플러그인 사용
        }
        register("androidLibrary") {
            id = "stack.android.library"
            implementationClass = "StackAndroidLibraryPlugin"
        }
        register("kotlinLibrary") {
            id = "stack.kotlin.library"
            implementationClass = "StackKotlinLibraryPlugin"
        }
    }
}
```

> **대안**: `.gradle.kts` 스크립트 플러그인을 쓰면 `gradlePlugin { plugins { register(...) } }` 블록이 불필요 (Gradle이 자동 인식). 위 §4.3의 `*.gradle.kts` 파일을 그대로 쓴다면 implementationClass 대신 precompiled script plugin으로 동작. 이 경우 `build.gradle.kts`에서 `gradlePlugin` 블록을 생략 가능.
>
> 단, **precompiled script plugin에서 version catalog accessor 접근**에는 `the<LibrariesForLibs>()` 호출이 필요하며, 이를 위해 `build.gradle.kts`에 `compileOnly(files(...))`가 필요.

### 4.4 `:app` 모듈

#### `app/build.gradle.kts`

```kotlin
plugins {
    id("stack.android.application")
}

android {
    namespace = "com.stack.player"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    
    // Phase 1에서는 feature 모듈 의존 없음 (다음 phase에서 추가)
}
```

#### `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- Phase 1: 권한 선언 없음 -->

    <application
        android:label="Stack"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:theme="@style/Theme.Stack.Starter"
        android:supportsRtl="false"
        tools:targetApi="35">

        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>
```

#### `app/src/main/kotlin/com/stack/player/MainActivity.kt`

```kotlin
package com.stack.player

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// Phase 1: 최소 진입점. Phase 3에서 Compose로 교체.
internal class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val textView = TextView(this).apply {
            text = "Stack"
            textSize = 32f
            setPadding(64, 128, 64, 64)
        }
        setContentView(textView)
    }
}
```

#### `app/src/main/res/values/themes.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Stack.Starter" parent="Theme.AppCompat.DayNight.NoActionBar">
        <!-- Phase 3에서 Compose Theme으로 교체 -->
    </style>
</resources>
```

#### `app/src/main/res/values/strings.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name" translatable="false">Stack</string>
</resources>
```

#### `app/src/main/res/mipmap-*`, `app/src/main/res/values-night/themes.xml`

런처 아이콘은 Android Studio의 Image Asset Studio로 기본 아이콘 생성 (Phase 13에서 최종 아이콘으로 교체).

`values-night/themes.xml`는 `values/themes.xml`과 동일 내용으로 일단 둠.

### 4.5 각 `core:*` 모듈

#### 공통 구조 (예: `core/common/`)

**`core/common/build.gradle.kts`** (JVM 모듈):
```kotlin
plugins {
    id("stack.kotlin.library")
}
```

**`core/common/src/main/kotlin/com/stack/player/core/common/.gitkeep`**: 빈 파일.

#### Android 모듈 (예: `core/design/`)

**`core/design/build.gradle.kts`**:
```kotlin
plugins {
    id("stack.android.library")
}

android {
    namespace = "com.stack.player.core.design"
}
```

**`core/design/src/main/AndroidManifest.xml`**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

**`core/design/src/main/kotlin/com/stack/player/core/design/.gitkeep`**: 빈.

### 4.6 각 `feature:*` 모듈

`feature/library/build.gradle.kts`:
```kotlin
plugins {
    id("stack.android.library")
}

android {
    namespace = "com.stack.player.feature.library"
}
```

(나머지 4개 feature 모듈 동일 구조, namespace만 다름.)

### 4.7 Kotlin 모듈 (JVM)

`:core:common`과 `:core:domain`은 `stack.kotlin.library` 사용, Android 의존 없음.

```kotlin
// core/common/build.gradle.kts
plugins {
    id("stack.kotlin.library")
}
```

---

## 5. 제약 사항

- **Hilt, Compose, Room, ExoPlayer, Coil 등 의존성 추가 금지.** 이 phase는 순수 골격만.
- **`:feature:*` 간 의존성 추가 금지** (Phase 2에서 의존성 enforcement로 자동 차단될 예정, 그 전까지는 Claude가 자발적 준수).
- **`:core:audio` / `:core:database`를 `:feature:*`에 직접 의존시키지 말 것.**
- **새 권한 선언 금지.**
- **새 사용자 노출 문자열 추가 금지** (`app_name`만).
- **`app` 모듈에서 `feature` 모듈 의존성 추가 금지** (이 phase에서는 feature가 비어있으므로 불필요).

---

## 6. 작업 순서 (권장)

1. `gradle/libs.versions.toml`에 최소 의존성 추가 (§4.2)
2. 3개의 convention plugin 작성 (§4.3)
3. `build-logic/convention/build.gradle.kts` 업데이트 (version catalog accessor)
4. `./gradlew :build-logic:convention:compileKotlin` 통과 확인
5. `settings.gradle.kts`에 13 모듈 include 추가
6. 각 모듈 디렉토리 생성 + `build.gradle.kts` + (Android 모듈) manifest
7. `.gitkeep` 파일들 추가 (빈 kotlin src 디렉토리 유지용)
8. `:app` 모듈에 MainActivity, themes.xml, strings.xml
9. `./gradlew projects` — 모든 모듈이 인식되는지 확인
10. `./gradlew :app:assembleDebug` 통과 확인
11. 기기에 설치 후 빈 화면 확인: `./gradlew :app:installDebug`

---

## 7. 종료 조건

### 7.1 자동 검증

```bash
# 모든 모듈이 인식되는가
./gradlew projects
# 출력에 13개 sub-project 모두 포함되어야 함

# 각 모듈이 독립 컴파일되는가
./gradlew :core:common:assemble
./gradlew :core:design:assembleDebug
./gradlew :core:ui:assembleDebug
./gradlew :core:database:assembleDebug
./gradlew :core:datastore:assembleDebug
./gradlew :core:domain:assemble
./gradlew :core:audio:assembleDebug
./gradlew :feature:library:assembleDebug
./gradlew :feature:player:assembleDebug
./gradlew :feature:playlist:assembleDebug
./gradlew :feature:search:assembleDebug
./gradlew :feature:settings:assembleDebug

# app이 빌드되는가
./gradlew :app:assembleDebug

# INTERNET 권한 미포함 확인
$ANDROID_HOME/build-tools/35.0.0/aapt dump permissions \
  app/build/outputs/apk/debug/app-debug.apk | grep -i internet
# 출력 없어야 함 (grep 실패가 정답)
```

### 7.2 수동 검증

- [ ] Android Studio에서 프로젝트 열었을 때 13개 모듈이 모두 트리에 표시됨
- [ ] `./gradlew :app:installDebug` 후 기기에서 "Stack" 텍스트만 있는 화면이 뜸
- [ ] 앱 실행 시 크래시 없음
- [ ] APK 매니페스트에 권한이 선언되지 않음

### 7.3 구조 검증

```bash
# 모든 모듈에 build.gradle.kts가 있는가
for m in core/common core/design core/ui core/database core/datastore \
         core/domain core/audio \
         feature/library feature/player feature/playlist \
         feature/search feature/settings app; do
    test -f "$m/build.gradle.kts" || echo "MISSING: $m/build.gradle.kts"
done

# Android 모듈에 manifest가 있는가
for m in app core/design core/ui core/database core/datastore \
         core/audio feature/library feature/player feature/playlist \
         feature/search feature/settings; do
    test -f "$m/src/main/AndroidManifest.xml" || echo "MISSING: $m manifest"
done
```

---

## 8. 실패 시 대응

### 8.1 Convention plugin 해석 실패

```
Plugin [id: 'stack.android.library'] was not found in any of the following sources:
```

→ `build-logic/settings.gradle.kts`가 root `settings.gradle.kts`의 `pluginManagement { includeBuild("build-logic") }`에서 올바르게 포함되는지 확인.

### 8.2 Version catalog accessor 접근 실패

```
Unresolved reference: libs
```
in convention plugin.

→ `build-logic/convention/build.gradle.kts`에 `compileOnly(files(libs.javaClass.superclass.protectionDomain.codeSource.location))` 추가 확인. 또는 Gradle 8.5+에서는 자동 지원되므로 확인.

### 8.3 `:app:assembleDebug` 실패

- AndroidX 의존성 버전 호환성 확인
- `compileSdk=35` / `minSdk=26` 설정 확인
- `JAVA_HOME`이 JDK 17인지 확인

### 8.4 부분 완료

`phases/01-modules-OPEN.md`에 완료/미완료 기록.

---

## 9. 커밋 / PR

### 9.1 커밋 메시지

```
chore(build): Phase 1 13개 모듈 셸 생성

- :app (MainActivity 최소 진입점, AppCompat 테마)
- :core (common, design, ui, database, datastore, domain, audio)
- :feature (library, player, playlist, search, settings)
- convention plugin 3종 (android.application, android.library, kotlin.library)
- libs.versions.toml에 AndroidX 최소 의존성 추가

각 모듈은 독립 컴파일. `./gradlew :app:assembleDebug` 통과.
INTERNET 권한 미포함 확인.

Refs: docs/SSOT_v5.0.md §3.2, phases/01-modules.md
```

### 9.2 PR 본문

```markdown
## 무엇
SSOT §3.2의 13개 모듈 셸을 생성. 각 모듈은 비어있지만 독립 컴파일 가능.

## 검증
- [x] 13개 모듈 모두 `./gradlew :{모듈}:assembleDebug` 통과
- [x] `./gradlew :app:assembleDebug` 통과 및 기기 설치 → "Stack" 화면 표시
- [x] APK 매니페스트에 INTERNET 포함되지 않음
- [x] Android Studio에서 모든 모듈 인식

## 남은 작업
Phase 2에서 의존성 enforcement (`:checkModuleDependencies`) 및 `:checkAll` 통합 게이트 구성.
Phase 3부터 각 모듈 내용 채움.
```

### 9.3 CLAUDE.md §10.4 갱신

Phase 1 체크박스 `☑`.

---

## 10. 가정 / 질문

- **Q. 왜 Phase 1에서 Hilt를 포함하지 않는가?**
  - A. Hilt는 실제 DI가 필요한 시점(Phase 5+ Repository 구현)에 도입. 지금 도입하면 빈 컴파일 시간만 늘어남.
- **Q. 왜 Phase 1에서 Compose를 포함하지 않는가?**
  - A. UI 작업은 Phase 3(디자인 토큰)부터. `:app`은 임시 AppCompat + TextView로 동작 확인만.
- **Q. `android:supportsRtl="false"` 이유?**
  - A. SSOT §10.5 — v1.0은 RTL 미지원.
- **Q. `applicationIdSuffix=".debug"` 이유?**
  - A. CLAUDE.md §5.1 / BUILD.md §3.1 — debug/release 동시 설치 가능.
- **Q. 런처 아이콘은?**
  - A. Phase 1은 Android Studio 기본 아이콘 사용. Phase 13 (i18n 완성 + UI 마감)에서 최종 아이콘.

---

*Phase 1 끝 — 이 phase가 통과하면 Phase 2로.*
