# Stack — Claude Code Operating Manual

> 본 파일은 Claude Code가 모든 세션에서 자동 로드합니다.
> 세션 시작 시 이 파일을 다시 읽고, 그 다음 해당 phase 프롬프트(`phases/NN-*.md`)를 읽고, 그 다음 SSOT의 인용 섹션만 읽은 뒤 작업을 시작하세요.

---

## 0. 프로젝트 한 줄

**Stack** — Android용 로컬 음악 재생/관리 앱.
패키지 `com.stack.player`, Kotlin 2.0, Jetpack Compose, multi-module (13개), 제로 네트워크.

전체 명세는 **`docs/SSOT_v5.0.md`** (Single Source of Truth).
본 매뉴얼과 SSOT가 충돌하면 SSOT를 따릅니다.

---

## 1. 절대 규칙 (위반 시 즉시 작업 중단 + 사용자 보고)

### 1.1 권한
- **`INTERNET` 권한 추가 금지.** 어떤 모듈의 manifest에도, 어떤 의존성을 통한 manifest merge로도 안 됩니다.
- 새 권한이 필요해 보이면 **추가하지 말고** 보고하세요.
- 권한 변경 후 반드시 `aapt dump permissions` 검증 (§5.4).

### 1.2 의존성
- 새 외부 라이브러리는 **반드시 `gradle/libs.versions.toml`을 통해서만** 추가합니다.
- `implementation("com.foo:bar:1.0")` 같은 직접 좌표 작성 금지.
- 새 의존성 추가 전 **사용자 승인 필수**. 추가 사유 + 대안 + 라이선스를 함께 제시.
- 영구 금지 카테고리: 분석 SDK (Firebase Analytics, Mixpanel, Amplitude 등), 크래시 리포터 (Crashlytics, Sentry 등), 광고 SDK, 네트워크 클라이언트 (Retrofit, OkHttp, Ktor client 등).

### 1.3 사용자 노출 문자열
- 사용자가 보는 문자열을 코드에 하드코딩 금지 (KO/EN/JA 무관).
- 모든 문자열은 `app/src/main/res/values{,-en,-ja}/strings.xml`에 둡니다.
- 키 명명: `{화면}_{컴포넌트}_{역할}` — 예: `library_tracks_title`, `player_action_play`.
- 새 문자열 추가 시 **3개 언어 모두** 함께 추가 (미번역 lint가 실패시킵니다).
- 포맷 인자는 위치 인덱스 사용: `%1$s`, `%2$d` (i18n 안전).

### 1.4 모듈 의존성
- §3의 의존성 그래프를 위반하면 안 됩니다.
- `./gradlew :checkModuleDependencies`가 위반을 빌드 실패로 보고합니다.
- 특히: `:core:domain`은 순수 Kotlin/JVM. Android, Room, ExoPlayer, Compose 일체 import 금지.

### 1.5 작업 범위
- 각 phase 프롬프트의 **수정 허용 디렉토리** 외 파일은 건드리지 않습니다.
- Refactor 충동이 들어도 **현재 phase 범위 외는 보고만** 하고 진행하지 않습니다.
- 의심 시 작업 중단 후 질문.

### 1.6 에러 처리
- 빌드 에러를 우회 금지: `@Suppress`, `// TODO: fix later`, `try { ... } catch { }`로 무음 처리, 테스트 비활성화 모두 금지.
- 에러는 **수정**하거나 **보고**합니다. 그 외 선택지 없음.
- "일단 통과시키고 나중에 고치기" 패턴 금지.

---

## 2. 진실의 원천 (Source of Truth)

| 무엇 | 어디 |
|---|---|
| 전체 기술 명세 | `docs/SSOT_v5.0.md` |
| 코드 스타일 상세 | `docs/STYLE_GUIDE.md` |
| 스레드/코루틴 정책 | `docs/THREADING_MODEL.md` |
| 빌드/CI 가이드 | `docs/BUILD.md` |
| 디자인 토큰 카탈로그 | `docs/DESIGN_TOKENS.md` |
| 변경 로그 | `CHANGELOG.md` |
| 현재 phase 명세 | `phases/{NN-name}.md` |
| 미완료 항목 | `phases/{NN-name}-OPEN.md` (있는 경우) |

각 phase 시작 시 읽는 순서:
1. 본 파일 (CLAUDE.md) 전체
2. `phases/{현재 phase}.md`
3. SSOT 중 phase가 인용한 § 만 (전체 X — 컨텍스트 절약)

---

## 3. 모듈 지도

### 3.1 디렉토리 구조

```
stack/
├── app/                            # Application, MainActivity, NavHost, DI root
├── core/
│   ├── common/                     # 의존 없음. 순수 Kotlin
│   ├── design/                     # 디자인 토큰, MaterialTheme
│   ├── ui/                         # 재사용 Composable
│   ├── database/                   # Room, DAO, FTS, Migration
│   ├── datastore/                  # DataStore Preferences
│   ├── domain/                     # UseCase, Model, Repository interface
│   └── audio/                      # ExoPlayer, MediaSession, AudioEngine
├── feature/
│   ├── library/
│   ├── player/
│   ├── playlist/
│   ├── search/
│   └── settings/
├── build-logic/                    # Convention plugins
├── gradle/libs.versions.toml       # 모든 외부 의존성 단일 관리
├── docs/                           # SSOT 및 가이드
└── phases/                         # Phase별 작업 명세
```

### 3.2 의존성 규칙 (단방향, 강제)

```
:app
  ↓ 모든 :feature, :core:design, :core:audio (Service 등록), 
    :core:database & :core:datastore (Hilt 모듈만)

:feature:*
  ↓ :core:ui, :core:design, :core:domain, :core:common
  ✗ 다른 :feature 직접 의존 금지
  ✗ :core:database, :core:audio, :core:datastore 직접 의존 금지
  
:core:audio
  ↓ :core:domain, :core:common
  ✗ Compose, :core:ui, :feature 의존 금지

:core:database, :core:datastore
  ↓ :core:domain (Repository 구현용), :core:common

:core:ui
  ↓ :core:design, :core:common
  ✗ :core:domain 의존 금지 (도메인 → UI 매핑은 feature가 담당)

:core:domain
  ↓ :core:common 만
  ✗ Android, Room, ExoPlayer, Compose 일체 금지

:core:design, :core:common
  ↓ 의존 없음
```

위반 시 `:checkModuleDependencies` 태스크가 빌드를 실패시킵니다.

### 3.3 패키지 루트

| 모듈 | package |
|---|---|
| `:app` | `com.stack.player` |
| `:core:common` | `com.stack.player.core.common` |
| `:core:design` | `com.stack.player.core.design` |
| `:core:ui` | `com.stack.player.core.ui` |
| `:core:database` | `com.stack.player.core.database` |
| `:core:datastore` | `com.stack.player.core.datastore` |
| `:core:domain` | `com.stack.player.core.domain` |
| `:core:audio` | `com.stack.player.core.audio` |
| `:feature:library` | `com.stack.player.feature.library` |
| (기타 feature 동일 패턴) | |

---

## 4. 작업 흐름

### 4.1 단위
- **1 phase = 1 git 브랜치 = 1 PR.**
- 브랜치명: `phase/NN-short-name` (예: `phase/08c-crossfade`).
- 한 세션에서 두 phase를 연속 진행 금지 (컨텍스트 오염 방지).
- 한 PR이 1500줄 (생성 코드 + 테스트) 넘으면 phase 분할 신호 — 사용자에게 보고.

### 4.2 Phase 시작 절차 (필수)

매 phase 첫 응답에서 다음 5개를 명시적으로 출력하고 **사용자 승인 후** 코드 작성을 시작합니다.

1. **읽은 문서**: SSOT의 어느 §, 어느 phase 파일
2. **작업 범위**: 수정 허용 디렉토리 / 수정 금지 디렉토리
3. **산출물**: 새로 만들 파일과 수정할 파일의 목록
4. **종료 조건**: 어떤 명령이 통과해야 phase 완료인가
5. **가정 / 질문**: 명세가 모호한 부분, 결정 필요 사항

### 4.3 Phase 종료 조건

다음이 **모두** 통과해야 phase 완료:

```bash
./gradlew checkAll                  # 통합 게이트
./gradlew :app:assembleDebug        # APK 빌드 통과
```

추가로 phase 프롬프트에 명시된 **수동 검증 체크리스트**를 본인이 수행하고 결과를 PR 본문에 기록합니다.

### 4.4 부분 완료 처리

phase가 100% 완료되지 않으면 **`phases/NN-name-OPEN.md`**에 다음을 기록:
- 완료 항목 (체크박스)
- 미완료 항목 + 이유
- 차단 요인 / 외부 의존성
- 다음 세션이 알아야 할 컨텍스트

미완료 항목을 임의로 우회하거나 stub으로 채우지 마세요.

### 4.5 Git / 커밋 규약

- 커밋 메시지: Conventional Commits 형식 (`feat:`, `fix:`, `chore:`, `refactor:`, `test:`, `docs:`).
- 한 커밋 = 한 논리적 변경. WIP 커밋은 push 전 squash.
- 커밋 메시지는 한국어 또는 영어 (혼용 금지, phase 단위 일관성).
- 본인이 push/PR 생성을 수행하기 전에는 사용자에게 변경 요약 보고.

---

## 5. 빌드 / 테스트 / 검증

### 5.1 빌드

```bash
./gradlew :app:assembleDebug          # 디버그 APK
./gradlew :app:assembleRelease        # 릴리스 APK (R8 적용)
./gradlew :app:installDebug           # 연결 기기에 설치
./gradlew :app:bundleRelease          # AAB
```

### 5.2 통합 검증 게이트 (매 phase 종료 전 필수)

```bash
./gradlew checkAll
```

내부적으로 다음을 실행합니다:
- `ktlintCheck` — 코드 스타일
- `detekt` — 복잡도, 코드 스멜
- `lintDebug` — Android Lint (vital + 커스텀 규칙)
- `checkModuleDependencies` — 모듈 의존 그래프 검증
- `testDebugUnitTest` — 모든 모듈 단위 테스트
- `checkNoInternetPermission` — 매니페스트 INTERNET 검사
- `checkNoHardcodedUserStrings` — 사용자 노출 문자열 하드코딩 검사

### 5.3 개별 실행

```bash
./gradlew :core:audio:testDebugUnitTest
./gradlew :feature:library:lintDebug
./gradlew :checkModuleDependencies
./gradlew :core:audio:testDebugUnitTest --tests "*CrossfadeControllerTest*"
```

### 5.4 권한 덤프 (의존성/매니페스트 변경 시 필수)

```bash
./gradlew :app:assembleDebug
$ANDROID_HOME/build-tools/35.0.0/aapt dump permissions \
  app/build/outputs/apk/debug/app-debug.apk
```

`INTERNET`이 출력되면 **즉시 작업 중단** + 마지막에 추가/변경한 의존성 보고.

### 5.5 Compose 메트릭 (성능 작업 시)

```bash
./gradlew :feature:library:assembleRelease \
  -P enableComposeCompilerReports=true
cat feature/library/build/compose_compiler/*-classes.txt
cat feature/library/build/compose_compiler/*-composables.txt
```

---

## 6. 어디에 무엇을 두는가

### 6.1 새 UseCase
**경로**: `core/domain/src/main/kotlin/com/stack/player/core/domain/usecase/{도메인}/{Verb}{Noun}UseCase.kt`
**규칙**: 파일 1개 = 클래스 1개, `operator fun invoke(...)` 단일 메서드.

```kotlin
class PlayTrackUseCase @Inject constructor(
    private val playback: PlaybackController,
) {
    suspend operator fun invoke(track: Track, queue: List<Track>) { ... }
}
```

### 6.2 새 Composable 화면
**경로**: `feature/{name}/src/main/kotlin/com/stack/player/feature/{name}/ui/{Screen}Screen.kt`

같은 파일 또는 같은 디렉토리에 다음을 함께 둡니다:
- `{Screen}Screen` — Composable, ViewModel 주입, Hilt 진입점
- `{Screen}Content` — 순수 Composable, state를 파라미터로 받음, 프리뷰 가능
- `{Screen}State` / `{Screen}Intent` / `{Screen}Event` — sealed class
- `{Screen}ViewModel` — `BaseViewModel<State, Intent, Event>` 상속

프리뷰는 `@PreviewLightDark` 양쪽 필수.

### 6.3 새 DAO
**경로**: `core/database/src/main/kotlin/com/stack/player/core/database/dao/{Entity}Dao.kt`

- 모든 read는 `Flow<T>` 또는 `suspend` 반환. 동기 read 금지.
- `@Query`는 `EXPLAIN QUERY PLAN`으로 검증한 뒤 추가.
- 메서드명 일관성: `observeAll`, `observeById`, `getById`(suspend), `insert`, `update`, `delete`, `count`.

### 6.4 새 Repository 구현
- 인터페이스: `core/domain/.../repository/{X}Repository.kt`
- 구현: `core/{database|audio|datastore}/.../repository/{X}RepositoryImpl.kt`
- Hilt 바인딩: 같은 모듈의 `di/{X}Module.kt`에서 `@Binds`

### 6.5 새 외부 의존성
1. **사용자 승인 받기** (사유, 대안 라이브러리, 라이선스).
2. `gradle/libs.versions.toml`에 version + library 추가.
3. 모듈의 `build.gradle.kts`에서 `implementation(libs.xxx)` 사용.
4. PR 본문에 라이선스 명시 + APK 사이즈 영향 보고.
5. `aapt dump permissions`로 권한 변화 확인.

### 6.6 새 사용자 노출 문자열
1. `app/src/main/res/values/strings.xml`에 한국어
2. `app/src/main/res/values-en/strings.xml`에 영어
3. `app/src/main/res/values-ja/strings.xml`에 일본어
4. Composable에서 `stringResource(R.string.{key})`로 사용

세 파일을 같은 PR에 함께 커밋. lint가 미번역 누락을 실패시킵니다.

### 6.7 새 디자인 토큰
디자인 토큰은 `:core:design`에만. Composable에 직접 hex/dp 값 작성 금지.
새 토큰이 필요하면 사용자에게 먼저 제안 (이름, 값, 라이트/다크).

### 6.8 새 권한
**금지** — 11.1 화이트리스트 외 권한 추가 시 즉시 사용자에게 보고.

---

## 7. 코드 컨벤션 핵심

상세는 `docs/STYLE_GUIDE.md`. 자주 위반되는 항목만:

### 7.1 Kotlin
- `data class`는 도메인/DTO에. 동등성이 필요 없으면 일반 `class`.
- nullable은 도메인 모델에서 가능한 한 제거. `?:` 와 `?.let { }` 남용 금지.
- 결과형은 `kotlin.Result` 대신 `:core:common`의 `Either<L, R>` 또는 sealed result.
- 코루틴 launch는 항상 명시된 scope (`viewModelScope`, `lifecycleScope`, `serviceScope` 등). `GlobalScope` 절대 금지.
- 디스패처는 항상 명시: `withContext(Dispatchers.IO) { ... }`. 메인 스레드에서 IO 작업 시 ANR.
- `runBlocking`은 테스트에서만. 프로덕션 코드 금지.

### 7.2 Compose
- 모든 Composable은 `Modifier`를 첫 옵셔널 파라미터로.
- 상태 수집은 `collectAsStateWithLifecycle()`. `collectAsState` 금지.
- `remember` 키 명시.
- `LazyColumn`/`LazyRow`의 `items`에 `key` 필수.
- `Modifier.clickable`은 `onClickLabel` 함께 (접근성).
- 프리뷰는 `@PreviewLightDark` (두 테마 자동 생성).
- `Modifier.animateItemPlacement()`는 비싸므로 1만+ 항목 리스트에서는 측정 후 결정.

### 7.3 Hilt
- ViewModel은 `@HiltViewModel`.
- Service는 `@AndroidEntryPoint`.
- 인터페이스 바인딩은 `@Binds` (Provides 아님).
- `@Singleton`은 정말 단일 인스턴스가 필요할 때만.
- 모듈은 `@InstallIn(SingletonComponent::class)` 명시.

### 7.4 Room
- `@Entity` 와 도메인 모델은 분리. `Mapper`로 변환.
- Migration은 명시적으로 작성. destructive migration 금지.
- DAO 시그니처: `Flow<T>` 와 `suspend fun`은 함께 쓸 수 없음 (KSP 에러). 주의.
- `@Transaction` 필요한 메서드 (M:N join 포함) 누락 주의.

### 7.5 ExoPlayer
- 모든 player 호출은 메인 스레드. 항상 `:core:audio`의 `CommandDispatcher`를 통해서.
- `Player.Listener`는 메인 스레드에서 콜백. 무거운 작업은 별도 dispatcher로 디스패치.
- `release()`는 `MediaSessionService.onDestroy()`에서 반드시.

### 7.6 로깅
- `println`, `Log.d`, `Log.i` 등 직접 호출 금지.
- `:core:common`의 `Logger` 인터페이스 사용. release 빌드에서 자동 no-op.

### 7.7 주석
- "무엇"이 아니라 "왜"를 적습니다.
- 자명한 코드에 주석 금지.
- `TODO`, `FIXME`, `XXX` 주석 금지 — `phases/NN-OPEN.md` 또는 GitHub Issue로.
- KDoc은 public API에만.

---

## 8. 흔한 함정과 회피

### 8.1 Manifest 의도치 않은 권한
라이브러리가 manifest merge로 `INTERNET` 등 권한을 끌어옴. **새 의존성 추가 후 매번** `aapt dump permissions` 검증.

### 8.2 Hilt 컴파일 에러
- `@AndroidEntryPoint` 누락 (Service, Activity, Fragment).
- `@Module @InstallIn(SingletonComponent::class)` 누락.
- `@Inject constructor`에 `@ApplicationContext` 누락.
- `@AssistedInject` + `@AssistedFactory` 짝.

### 8.3 Room KSP 에러
- DAO에서 `suspend fun foo(): Flow<T>` — 동시 사용 불가. `fun foo(): Flow<T>` 또는 `suspend fun foo(): T`.
- `@Entity`에 PK 누락.
- foreign key 선언 시 인덱스 누락 (lint 경고).

### 8.4 Compose recomposition 폭증
- 람다 안에서 ViewModel 함수를 매번 새로 생성 → `remember(viewModel) { viewModel::onIntent }` 또는 stable 콜백.
- `Modifier`를 매 호출마다 새로 만들지 않기 (같은 객체 재사용).
- 디버깅: Layout Inspector → Recomposition Counts.

### 8.5 strings.xml 파싱
- 작은따옴표 `'` → `\'` 이스케이프 (한국어 문장에서 자주 발생).
- 앰퍼샌드 `&` → `&amp;`.
- 포맷 인자는 `%1$s`, `%2$d` (위치 인덱스).

### 8.6 ExoPlayer "called from wrong thread"
모든 player 호출은 메인. `CommandDispatcher.run { ... }` 우회 코드가 있는지 확인.

### 8.7 ProGuard/R8 release crash
- `kotlinx.serialization` 사용 시 keep 규칙 명시 필요.
- Reflective access는 `consumer-rules.pro`에 명시.
- Room, Compose, Hilt는 공식 keep 규칙이 자동 포함됨 (별도 작업 불필요).

---

## 9. 질문 / 보고 정책

### 9.1 즉시 질문해야 하는 경우
- SSOT나 phase 프롬프트에 답이 없는 결정이 필요할 때
- 새 의존성, 새 권한, 새 매니페스트 항목
- 작업 범위를 벗어난 수정이 필요할 때
- 빌드 에러를 합리적 시간 내에 해결 못 했을 때 (지표: 같은 에러로 3회 시도 실패)
- 기존 코드와 SSOT 사이에 충돌 발견 시

### 9.2 보고 형식

```
[사실] 무엇이 일어났나
[시도] 무엇을 해봤나 (3회 이상이면 모두 나열)
[가설] 원인 후보 1~3개
[옵션] 가능한 해결책 2~3개 + 각각의 trade-off
[권장] 그 중 하나의 추천 + 근거
```

### 9.3 절대 하지 말 것
- 명세에 없는 동작을 "당연히 이래야 한다"며 추측 구현
- 빌드 통과를 위해 테스트 비활성화 / @Ignore 추가
- 빌드 통과를 위해 임의로 의존성 추가
- 에러를 try-catch로 삼키기
- "일단 이렇게 해두고 나중에" 패턴
- 사용자 승인 없이 새 파일/디렉토리 대량 생성

---

## 10. 빠른 참조

### 10.1 자주 쓰는 명령

```bash
# Phase 종료 게이트
./gradlew checkAll

# 한 모듈만 빠르게
./gradlew :core:audio:assemble :core:audio:testDebugUnitTest

# 의존성 그래프 확인
./gradlew :feature:library:dependencies --configuration debugRuntimeClasspath

# 매니페스트 머지 결과 확인
cat app/build/intermediates/merged_manifests/debug/AndroidManifest.xml

# 권한 덤프 (의존성 변경 후 필수)
$ANDROID_HOME/build-tools/35.0.0/aapt dump permissions \
  app/build/outputs/apk/debug/app-debug.apk

# Compose compiler 메트릭
./gradlew :feature:library:assembleRelease \
  -P enableComposeCompilerReports=true

# Baseline profile 생성
./gradlew :baselineprofile:pixel6Api34BenchmarkAndroidTest

# 단일 테스트 실행
./gradlew :core:audio:testDebugUnitTest --tests "*Crossfade*"
```

### 10.2 SSOT 빠른 인덱스

| 주제 | SSOT 섹션 |
|---|---|
| 비목표 / 영구 미지원 | §1.3 |
| 디자인 원칙 | §1.4 |
| 모듈 의존성 규칙 | §3.3 |
| Room 13 테이블 스키마 | §4.1 |
| FTS4 설정 | §4.1.3 |
| DataStore 키 정의 | §4.3 |
| 도메인 모델 | §5.1 |
| Repository 인터페이스 | §5.2 |
| UseCase 목록 | §5.3 |
| Dual ExoPlayer 구조 | §6.2 |
| CommandDispatcher (Mutex) | §6.3 |
| Crossfade 3-branch | §6.2.3 |
| Equal-power 페이드 곡선 | §6.2.4 |
| Gapless 정책 | §6.4 |
| A-B 반복 | §6.5 |
| ReplayGain 합성 | §6.6 |
| 이퀄라이저 | §6.7 |
| 오디오 포커스 | §6.8 |
| MediaSession | §6.9 |
| 큐 관리 정책 | §6.11 |
| 라이브러리 스캔 | §6.12 |
| MVI BaseViewModel | §7.1 |
| 디자인 토큰 (색/타이포/간격) | §8.1, §8.2, §8.3 |
| 컴포넌트 카탈로그 | §8.4 |
| 접근성 | §8.5 |
| i18n 규약 | §10 |
| 권한 화이트리스트 | §11.1 |
| 성능 예산 | §12 |
| 테스트 시나리오 | §13.2 |
| 결정 보류 사항 | §17 |

### 10.3 트러블슈팅 1차 진단

| 증상 | 1차 확인 |
|---|---|
| `INTERNET` 권한 등장 | 마지막 추가한 의존성, manifest merge 로그 |
| Hilt 컴파일 에러 | `@Module @InstallIn` / scope / `@AndroidEntryPoint` 누락 |
| Room KSP 에러 | DAO 시그니처 (`suspend` vs `Flow`), Entity PK |
| Compose recomposition 폭증 | Layout Inspector recomposition counts |
| ExoPlayer "wrong thread" | `CommandDispatcher` 우회 |
| 잠금화면 컨트롤 누락 | MediaSession publish + notification channel |
| 갑작스런 무음 | AudioFocus 이벤트 로그 |
| `.lrc` 파싱 실패 | BOM, 비표준 헤더, 비표준 시간 포맷 (관대 파싱 필요) |
| 백업 복원 트랙 매칭 실패 | `media_uri` 변경 → `(title, album, duration)` 폴백 |

### 10.4 Phase 진행 현황 (이 섹션은 phase 완료마다 갱신)

| # | Phase | 상태 |
|---|---|---|
| 0 | 저장소 스켈레톤 | ☑ |
| 1 | 13개 모듈 셸 | ☑ |
| 2 | 의존성 enforcement | ☐ |
| 3 | core:design + core:common | ☐ |
| 4 | core:ui 컴포넌트 | ☐ |
| 5 | core:database 스키마 | ☐ |
| 6 | core:datastore + core:domain | ☐ |
| 7 | Repository 구현 | ☐ |
| 8a | core:audio — 단일 ExoPlayer | ☐ |
| 8b | core:audio — Dual + Dispatcher | ☐ |
| 8c | core:audio — Crossfade + Gapless | ☐ |
| 8d | core:audio — EQ + ReplayGain + A-B | ☐ |
| 9 | feature:library | ☐ |
| 10 | feature:player + 가사 | ☐ |
| 11 | feature:playlist + search | ☐ |
| 12 | feature:settings + 백업 | ☐ |
| 13 | i18n 완성 | ☐ |
| 14 | 테스트/벤치/CI 마감 | ☐ |

---

## 11. 본 문서 유지보수

- 본 문서는 SSOT v5.x와 함께 진화합니다.
- 모듈 추가/제거, 빌드 명령 변경, 새 게이트 추가, 흔한 함정 발견 시 갱신.
- 모든 갱신은 commit message에 `docs(claude): ...` prefix.
- 본 문서가 길어 300줄을 크게 넘으면 일부를 별도 docs로 분리 검토.

*마지막 갱신: 2026-04-17 / SSOT v5.0과 함께 운용*
