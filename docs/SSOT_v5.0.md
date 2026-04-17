# Stack — 기술 명세서 (SSOT)

> **버전**: 5.0
> **상태**: 초안 (Approved for v1.0 implementation)
> **최종 수정일**: 2026-04-17
> **패키지**: `com.stack.player`
> **타깃**: Android 26 (min) ~ 35 (target)
> **언어**: Kotlin 2.0
> **저작자 검토**: 준영 / Claude Opus 4.7

---

## 0. 메타

### 0.1 문서의 위치

본 문서는 Stack 프로젝트의 **유일한 진실의 원천(Single Source of Truth)**이다. 코드, 디자인, 마케팅 문안, 향후 기능 결정은 모두 본 문서를 우선한다. 본 문서와 코드가 충돌할 경우, 우선 본 문서를 검토하여 결함이 있으면 문서를 갱신하고, 결함이 없으면 코드를 본 문서에 맞춘다.

### 0.2 버전 정책

| 종류 | 형식 | 예시 |
|---|---|---|
| 본 문서 | `MAJOR.MINOR` | `5.0`, `5.1` |
| 앱 versionName | `MAJOR.MINOR.PATCH` | `1.0.0` |
| 앱 versionCode | 정수, 단조증가 | `100`, `101` |

본 문서의 MAJOR 변경은 아키텍처 또는 데이터 스키마의 비호환 변경을 의미한다. MINOR 변경은 호환되는 기능 추가/수정이다.

### 0.3 용어집

| 용어 | 정의 |
|---|---|
| **Track** | 단일 음원 파일. 데이터베이스의 최소 재생 단위 |
| **Album** | Track의 논리적 묶음. 메타데이터 또는 폴더 기반 추론 |
| **Artist** | 곡/앨범의 제작자. Track:Artist는 N:M |
| **Playlist** | 사용자가 정의한 순서가 있는 Track 모음 |
| **Tag** | 사용자가 정의한 임의 라벨 (e.g., "야간", "운동", "JPOP") |
| **Queue** | 현재 재생 대기열. 영속화됨 |
| **Now Playing** | 현재 재생 중인 Track 단일 객체 |
| **Crossfade** | 두 Track 사이의 중첩 페이드 전환 |
| **Gapless** | 두 Track 사이의 무음 없는 연속 재생 |
| **LRC** | 동기화 가사 텍스트 포맷 (`.lrc`) |
| **MVI** | Model-View-Intent UDF 패턴 |
| **SSOT** | Single Source of Truth |
| **FTS** | Full-Text Search (SQLite FTS4) |

### 0.4 본 문서 규약

- **MUST / SHOULD / MAY**: RFC 2119 의미를 따른다.
- **TODO**: 본 버전에서 결정 보류된 사항. 코드 동결 전 모두 결정해야 한다.
- 코드 블록의 패키지 경로는 모두 `com.stack.player` 하위로 가정한다.

---

## 1. 비전과 원칙

### 1.1 제품 비전

> **로컬 음원을 가장 정중하게 다루는 Android 음악 앱.**
>
> 사용자의 라이브러리는 사용자의 것이다. Stack은 그것을 **소유, 정렬, 분류, 음미**하기 위한 도구이며, 추천하지도, 학습시키지도, 외부로 보내지도 않는다.

### 1.2 핵심 가치 (우선순위 순)

1. **로컬 우선 (Local-first)** — 모든 데이터, 메타데이터, 가사, 통계는 사용자 기기 안에서 닫혀 있는다.
2. **제로 네트워크 (Zero-Network)** — `INTERNET` 권한을 선언하지 않는다. 코드 어디에서도 소켓을 열지 않는다.
3. **고요한 UI** — 정보 위계가 명확하고, 색은 절제되며, 모션은 의미가 있을 때만 발생한다.
4. **상용 수준의 안정성** — 1만 트랙 라이브러리에서도 즉시 응답한다. 백그라운드에서 끊기지 않는다. 데이터를 잃지 않는다.
5. **국제화** — KO / EN / JA를 동등한 1급 언어로 취급한다.

### 1.3 비목표 (Non-Goals)

본 버전(v1.0)은 다음을 **하지 않는다**. 이후 버전에서도 1.2를 위배하면 영구히 하지 않는다.

- 스트리밍 서비스 연동 (Spotify/Apple Music/YouTube Music)
- 클라우드 동기화 (자체 서버 또는 Google Drive 직접 통합)
- 곡 추천, 자동 플레이리스트 생성을 위한 외부 ML 호출
- 광고, 분석 SDK, 크래시 리포트 SDK (단, 사용자 명시적 동의 후 수동 제출 가능한 로컬 로그 익스포트는 허용)
- 가사 자동 다운로드 (사용자가 직접 가져와 임포트하는 것은 지원)
- 앨범 아트 자동 다운로드 (메타데이터 임베디드 / 폴더 내 이미지만 사용)
- 라디오, 팟캐스트, 오디오북 전용 기능
- 소셜 기능 (공유, 좋아요 외부 노출, 친구 라이브러리 보기)

### 1.4 미학 원칙 — Notion + Soft-Archiving

- **Notion** — 흰 여백, 가는 디바이더, 회색 위계, 산세리프 텍스트의 권위.
- **Soft-Archiving** — 도서관/서고의 정중한 침묵. 카드보다 목록, 그림자보다 선, 채도보다 명도.

| 항목 | 채택 | 거부 |
|---|---|---|
| 색 | 무채색 + 1개 액센트 | 그라디언트, 네온 |
| 모서리 | `4dp` (목록), `12dp` (시트), `16dp` (이미지) | `28dp+` 둥근 카드 |
| 그림자 | 거의 사용하지 않음 (`elevation 0~1dp`) | Material 3 `tonalElevation` 강조 |
| 모션 | 200~300ms 이즈드 단순 전환 | 스프링 바운스, 회전 |
| 아이콘 | 1.5px stroke, outline | filled, multicolor |
| 타이포 | 본문 sans, 제목 sans-display, 가사 serif (옵션) | script, display 강조 폰트 |

---

## 2. 기술 스택

### 2.1 언어 / 빌드

| 항목 | 버전 | 비고 |
|---|---|---|
| Kotlin | 2.0.x | K2 컴파일러 |
| Java toolchain | 17 | |
| Gradle | 8.10+ | |
| AGP | 8.7+ | |
| compileSdk | 35 | |
| minSdk | 26 | Android 8.0 |
| targetSdk | 35 | |

### 2.2 핵심 의존성

| 카테고리 | 라이브러리 | 비고 |
|---|---|---|
| UI | Jetpack Compose (BoM 2024.10+) | Material3, Compose Navigation |
| 상태 관리 | Kotlin Coroutines / Flow | StateFlow, SharedFlow |
| DI | Hilt | KSP 기반 |
| 영속화 | Room 2.7+ | KSP, FTS4 |
| 키-값 | DataStore Preferences | |
| 미디어 | Media3 ExoPlayer 1.4+ | `media3-exoplayer`, `media3-session`, `media3-ui` (선택) |
| 이미지 | Coil 3 | 메모리/디스크 캐시 |
| 직렬화 | kotlinx.serialization | JSON 백업 |
| 압축 | java.util.zip | 백업 ZIP |
| 테스트 | JUnit5, Turbine, MockK, Robolectric | |
| 정적 분석 | ktlint, detekt, Android Lint | |

### 2.3 명시적으로 제외하는 라이브러리

- 모든 분석 SDK (Firebase Analytics, Mixpanel, Amplitude 등)
- 모든 크래시 리포터 (Firebase Crashlytics, Sentry 등)
- 모든 광고 SDK
- Retrofit, OkHttp 등 네트워크 라이브러리 (제로 네트워크 원칙)
- LeakCanary 등 디버그 전용은 `debugImplementation`으로만 허용

---

## 3. 아키텍처 개요

### 3.1 레이어드 + 멀티모듈

세 개의 논리 레이어를 13개 Gradle 모듈로 구현한다.

```
[Presentation Layer]   feature:library, feature:player, feature:playlist,
                       feature:search, feature:settings
                              │
[Domain Layer]         core:domain  (use case, model, repository interface)
                              │
[Data Layer]           core:database, core:datastore, core:audio
                              │
[Foundation]           core:design, core:ui, core:common
                              │
[Entry]                app
```

### 3.2 모듈 목록 (총 13)

| # | 모듈 | 역할 |
|---|---|---|
| 1 | `:app` | Application, Hilt root, Navigation host, MainActivity |
| 2 | `:core:common` | Result/Either, 시간/포맷 유틸, 코루틴 디스패처 제공 |
| 3 | `:core:design` | 색/타이포/모양/간격 토큰, MaterialTheme 정의 |
| 4 | `:core:ui` | 재사용 가능한 Composable (TrackRow, MiniPlayer, Sheet 등) |
| 5 | `:core:database` | Room DB, DAO, Entity, Migration, FTS |
| 6 | `:core:datastore` | DataStore Preferences (UserPrefs, PlaybackPrefs) |
| 7 | `:core:domain` | UseCase, Domain Model, Repository **interface** |
| 8 | `:core:audio` | ExoPlayer 래퍼, AudioEngine, MediaSession, EQ |
| 9 | `:feature:library` | 트랙/앨범/아티스트/태그 브라우즈 |
| 10 | `:feature:player` | Now Playing 화면, MiniPlayer, 가사, A-B 반복 UI |
| 11 | `:feature:playlist` | 플레이리스트 CRUD, 정렬, 가져오기/내보내기 |
| 12 | `:feature:search` | 통합 검색 화면, FTS 결과 표시 |
| 13 | `:feature:settings` | 설정, EQ, 백업/복원, 정보, 라이선스 |

### 3.3 의존성 규칙 (MUST)

```
:app          → 모든 :feature:*, :core:design, :core:audio, :core:database, :core:datastore
:feature:*    → :core:ui, :core:design, :core:domain, :core:common
              ✗ :feature:* 끼리 직접 의존 금지
              ✗ :core:database, :core:audio 직접 의존 금지 (Repository를 통해서만)
:core:domain  → :core:common
              ✗ Android, Room, ExoPlayer 등 일체 의존 금지 (순수 Kotlin/JVM)
:core:database → :core:domain (Repository 구현), :core:common
:core:audio   → :core:domain, :core:common
:core:datastore → :core:domain, :core:common
:core:ui      → :core:design, :core:common
:core:design  → (의존 없음)
:core:common  → (의존 없음)
```

위반 검출은 **Gradle convention plugin + 커스텀 dependency analysis 태스크**로 강제한다 (15.4 참조).

### 3.4 단방향 데이터 흐름

```
User Intent
    ↓
ViewModel (MVI Reducer)
    ↓ (UseCase 호출)
Domain Layer
    ↓
Repository (interface)
    ↓
Data Source (Room / DataStore / AudioEngine)
    ↑
Flow<DomainModel>
    ↑
ViewModel.state: StateFlow<UiState>
    ↑
Composable (collectAsStateWithLifecycle)
```

---

## 4. 데이터 레이어

### 4.1 Room 스키마 — 13 테이블 + 1 FTS

> 모든 시각 컬럼(`*_at`)은 epoch milliseconds (Long) 저장. 모든 ID는 String UUID(v4)를 기본으로 하되, `tracks.id`만 미디어 경로 해시로 결정적 생성한다 (재스캔 시 동일성 유지).

#### 4.1.1 테이블 목록

| # | 테이블 | 설명 |
|---|---|---|
| 1 | `tracks` | 단일 음원 파일 메타 |
| 2 | `albums` | 앨범 |
| 3 | `artists` | 아티스트 |
| 4 | `album_artists` | 앨범↔아티스트 N:M |
| 5 | `track_artists` | 트랙↔아티스트 N:M (역할 포함) |
| 6 | `playlists` | 사용자 플레이리스트 |
| 7 | `playlist_tracks` | 플레이리스트 멤버십 (위치 포함) |
| 8 | `tags` | 사용자 정의 태그 |
| 9 | `track_tags` | 트랙↔태그 N:M |
| 10 | `lyrics` | LRC 가사 (트랙 1:1, 옵셔널) |
| 11 | `play_history` | 재생 이벤트 로그 |
| 12 | `track_stats` | 트랙별 집계 통계 (재생수/스킵수/마지막재생시각/평점) |
| 13 | `queue` | 현재 재생 대기열 영속 (단일 행 또는 위치 기반) |

#### 4.1.2 주요 테이블 상세

##### `tracks`

| 컬럼 | 타입 | 제약 | 비고 |
|---|---|---|---|
| `id` | TEXT | PK | sha-1(`mediaUri`)의 앞 16바이트 hex |
| `media_uri` | TEXT | NOT NULL, UNIQUE | content:// 또는 file:// |
| `display_name` | TEXT | NOT NULL | 파일명 (확장자 제외) |
| `title` | TEXT | NOT NULL | 메타데이터 우선, 없으면 display_name |
| `album_id` | TEXT | FK→albums.id, INDEX | 앨범 미상이면 "Unknown Album" 앨범에 연결 |
| `track_number` | INTEGER | | |
| `disc_number` | INTEGER | | |
| `duration_ms` | INTEGER | NOT NULL | |
| `mime_type` | TEXT | NOT NULL | |
| `bitrate` | INTEGER | | bps |
| `sample_rate` | INTEGER | | Hz |
| `channels` | INTEGER | | |
| `bit_depth` | INTEGER | | nullable, FLAC 등에서만 |
| `codec` | TEXT | | "MP3", "FLAC", "AAC", "OPUS" 등 |
| `size_bytes` | INTEGER | NOT NULL | |
| `year` | INTEGER | | |
| `genre` | TEXT | | |
| `composer` | TEXT | | |
| `replaygain_track_db` | REAL | | nullable |
| `replaygain_album_db` | REAL | | nullable |
| `embedded_art_uri` | TEXT | | content:// (mediastore art id) 또는 캐시 경로 |
| `lyric_state` | INTEGER | NOT NULL DEFAULT 0 | 0:없음, 1:비동기(평문), 2:동기(LRC) |
| `added_at` | INTEGER | NOT NULL | 라이브러리 등록 시각 |
| `modified_at` | INTEGER | NOT NULL | 파일 수정 시각 |
| `is_present` | INTEGER | NOT NULL DEFAULT 1 | 파일 존재 여부 (스캔 시 갱신) |

인덱스: `(album_id)`, `(title COLLATE NOCASE)`, `(added_at DESC)`, `(is_present)`.

##### `albums`

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `id` | TEXT | PK |
| `title` | TEXT | NOT NULL |
| `normalized_title` | TEXT | NOT NULL | 정렬용. lower + 공백/구두점 제거 |
| `year` | INTEGER | |
| `art_uri` | TEXT | |
| `track_count` | INTEGER | NOT NULL DEFAULT 0 | 트리거로 갱신 |
| `total_duration_ms` | INTEGER | NOT NULL DEFAULT 0 | 트리거로 갱신 |

##### `artists`

| 컬럼 | 타입 | 제약 |
|---|---|---|
| `id` | TEXT | PK |
| `name` | TEXT | NOT NULL UNIQUE |
| `normalized_name` | TEXT | NOT NULL | |

##### `album_artists` / `track_artists`

```sql
CREATE TABLE album_artists (
  album_id  TEXT NOT NULL,
  artist_id TEXT NOT NULL,
  PRIMARY KEY (album_id, artist_id),
  FOREIGN KEY (album_id)  REFERENCES albums(id)  ON DELETE CASCADE,
  FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE
);

CREATE TABLE track_artists (
  track_id  TEXT NOT NULL,
  artist_id TEXT NOT NULL,
  role      INTEGER NOT NULL DEFAULT 0,  -- 0:main, 1:feat, 2:remixer, 3:composer
  position  INTEGER NOT NULL DEFAULT 0,  -- 표시 순서
  PRIMARY KEY (track_id, artist_id, role),
  FOREIGN KEY (track_id)  REFERENCES tracks(id)  ON DELETE CASCADE,
  FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE
);
```

##### `playlists` / `playlist_tracks`

```sql
CREATE TABLE playlists (
  id            TEXT PRIMARY KEY NOT NULL,
  name          TEXT NOT NULL,
  description   TEXT,
  cover_uri     TEXT,
  is_pinned     INTEGER NOT NULL DEFAULT 0,
  sort_order    INTEGER NOT NULL,        -- 사용자 지정 순서
  track_count   INTEGER NOT NULL DEFAULT 0,
  created_at    INTEGER NOT NULL,
  updated_at    INTEGER NOT NULL
);

CREATE TABLE playlist_tracks (
  playlist_id   TEXT NOT NULL,
  track_id      TEXT NOT NULL,
  position      INTEGER NOT NULL,        -- 0-base
  added_at      INTEGER NOT NULL,
  PRIMARY KEY (playlist_id, position),
  FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
  FOREIGN KEY (track_id)    REFERENCES tracks(id)    ON DELETE CASCADE
);
CREATE INDEX idx_playlist_tracks_track ON playlist_tracks(track_id);
```

> 같은 곡을 같은 플레이리스트에 중복 추가하는 것은 허용한다 (사용자 의도 존중). PK는 `(playlist_id, position)`이다.

##### `tags` / `track_tags`

```sql
CREATE TABLE tags (
  id         TEXT PRIMARY KEY NOT NULL,
  name       TEXT NOT NULL,             -- 표시명. 대소문자 구별
  normalized TEXT NOT NULL UNIQUE,      -- lower + trim. 중복 방지
  color      INTEGER NOT NULL,          -- ARGB. 디자인 토큰 8색 중 하나
  usage_count INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);

CREATE TABLE track_tags (
  track_id  TEXT NOT NULL,
  tag_id    TEXT NOT NULL,
  added_at  INTEGER NOT NULL,
  PRIMARY KEY (track_id, tag_id),
  FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE,
  FOREIGN KEY (tag_id)   REFERENCES tags(id)   ON DELETE CASCADE
);
CREATE INDEX idx_track_tags_tag ON track_tags(tag_id);
```

##### `lyrics`

```sql
CREATE TABLE lyrics (
  track_id     TEXT PRIMARY KEY NOT NULL,
  format       INTEGER NOT NULL,        -- 0: plain, 1: synced (LRC)
  source       INTEGER NOT NULL,        -- 0: embedded tag, 1: sidecar .lrc, 2: user input
  raw          TEXT NOT NULL,           -- 원본 LRC 또는 평문
  offset_ms    INTEGER NOT NULL DEFAULT 0,
  language     TEXT,                    -- BCP-47 (ko, ja, en, ...)
  updated_at   INTEGER NOT NULL,
  FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
);
```

> LRC 파싱 결과는 메모리에서만 다루고, DB에는 원본만 저장한다. 사용자가 에디터에서 수정하면 raw가 갱신된다.

##### `play_history`

```sql
CREATE TABLE play_history (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  track_id     TEXT NOT NULL,
  started_at   INTEGER NOT NULL,
  played_ms    INTEGER NOT NULL,        -- 실제 재생된 시간
  completed    INTEGER NOT NULL,        -- 1: 곡 길이의 90% 이상 재생, 0: 스킵
  source       INTEGER NOT NULL,        -- 0:library, 1:playlist, 2:search, 3:tag, 4:queue, 5:lockscreen
  source_ref   TEXT,                    -- e.g., playlist_id
  FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
);
CREATE INDEX idx_play_history_track   ON play_history(track_id);
CREATE INDEX idx_play_history_started ON play_history(started_at DESC);
```

##### `track_stats`

`play_history`로부터 트리거로 자동 갱신되는 비정규화 테이블.

```sql
CREATE TABLE track_stats (
  track_id      TEXT PRIMARY KEY NOT NULL,
  play_count    INTEGER NOT NULL DEFAULT 0,
  skip_count    INTEGER NOT NULL DEFAULT 0,
  last_played_at INTEGER,
  total_played_ms INTEGER NOT NULL DEFAULT 0,
  rating        INTEGER NOT NULL DEFAULT 0,   -- 0~5, 사용자 직접 부여
  is_starred    INTEGER NOT NULL DEFAULT 0,   -- "별표" 토글
  FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
);
```

##### `queue`

현재 재생 대기열 영속화. 앱 재시작 후 큐 복원에 사용.

```sql
CREATE TABLE queue (
  position    INTEGER PRIMARY KEY NOT NULL,
  track_id    TEXT NOT NULL,
  added_at    INTEGER NOT NULL,
  is_user_added INTEGER NOT NULL DEFAULT 0,    -- "다음에 재생" 여부
  FOREIGN KEY (track_id) REFERENCES tracks(id) ON DELETE CASCADE
);
```

> 큐 자체의 메타(현재 인덱스, 셔플 상태, 반복 모드 등)는 DataStore에 저장한다.

#### 4.1.3 FTS

```sql
CREATE VIRTUAL TABLE tracks_fts USING fts4(
  title, album, artist, tag,
  content='', tokenize=unicode61 'remove_diacritics=2'
);
```

- `tokenize=unicode61`로 한글/일본어 부분 매치를 허용한다.
- 일본어 검색은 가나/한자/로마자 모두 인덱싱한다 (트랙의 ALT_TITLE 메타가 있으면 함께 인덱싱).
- FTS는 view가 아닌 복제 테이블이며, `tracks` 변경 시 트리거로 동기화한다.

### 4.2 마이그레이션 정책

- v1.0 출시 후 모든 스키마 변경은 **destructive migration 금지**.
- `Migration` 객체를 명시적으로 작성하고 마이그레이션 테스트를 작성한다 (Room의 `MigrationTestHelper`).
- 컬럼 추가는 `ALTER TABLE ... ADD COLUMN ... DEFAULT ...`만 허용한다. 컬럼 제거는 임시 테이블 생성 후 복사/교체 패턴.

### 4.3 DataStore — 사용자 설정

**`user_prefs.preferences_pb`** (앱 재설치 전까지 유지)

| 키 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `theme_mode` | INT | 0 (system) | 0:system, 1:light, 2:dark |
| `accent_color` | INT | 디자인 토큰 0번 | |
| `language_override` | STRING | "" | "" = system, "ko", "en", "ja" |
| `lyric_font` | INT | 0 (sans) | 0:sans, 1:serif |
| `lyric_size` | INT | 1 | 0:S, 1:M, 2:L, 3:XL |
| `library_default_sort` | INT | 0 | (5.4 참조) |
| `show_unknown_artists` | BOOL | true | |
| `excluded_folders` | STRING_SET | {} | 라이브러리 스캔 제외 |

**`playback_prefs.preferences_pb`**

| 키 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `crossfade_mode` | INT | 0 | 0:off, 1:auto-only, 2:always |
| `crossfade_duration_ms` | INT | 4000 | 1000~12000 |
| `gapless_enabled` | BOOL | true | |
| `replaygain_mode` | INT | 0 | 0:off, 1:track, 2:album |
| `replaygain_preamp_db` | FLOAT | 0.0 | -15.0~+15.0 |
| `eq_enabled` | BOOL | false | |
| `eq_preset_id` | STRING | "flat" | |
| `eq_bands_db` | STRING | "0,0,0,0,0,0,0,0,0,0" | 10 밴드 콤마 구분 |
| `repeat_mode` | INT | 0 | 0:off, 1:all, 2:one |
| `shuffle_mode` | INT | 0 | 0:off, 1:on |
| `sleep_timer_minutes` | INT | 0 | 0이면 비활성 |
| `sleep_timer_finish_track` | BOOL | true | 종료 시 현재 곡 끝까지 재생 |
| `headset_unplug_pause` | BOOL | true | |
| `bluetooth_disconnect_pause` | BOOL | true | |
| `current_queue_index` | INT | 0 | 큐 복원용 |
| `current_position_ms` | LONG | 0 | 큐 복원용 |

---

## 5. 도메인 레이어

### 5.1 도메인 모델 (요약)

```kotlin
// :core:domain/model/

data class Track(
    val id: String,
    val title: String,
    val album: AlbumRef,           // (id, title, art)
    val artists: List<ArtistRef>,  // ordered, role 포함
    val durationMs: Long,
    val format: AudioFormat,        // codec, sampleRate, bitDepth, bitrate, channels
    val mediaUri: String,
    val lyricState: LyricState,
    val replayGain: ReplayGain?,
    val addedAt: Long,
    val isPresent: Boolean,
)

data class Album(...)
data class Artist(...)
data class Playlist(val id: String, val name: String, val trackCount: Int, ...)
data class Tag(val id: String, val name: String, val color: Int, val usageCount: Int)
data class Lyric(val format: LyricFormat, val lines: List<LyricLine>, val offsetMs: Long)
data class LyricLine(val timeMs: Long, val text: String)
data class TrackStats(val playCount: Int, val rating: Int, val isStarred: Boolean, ...)
data class QueueState(val items: List<Track>, val currentIndex: Int, val shuffle: Boolean, val repeat: RepeatMode)
data class PlaybackState(val isPlaying: Boolean, val positionMs: Long, val track: Track?, val durationMs: Long)
```

### 5.2 Repository 인터페이스

```kotlin
// :core:domain/repository/

interface TrackRepository {
    fun observeAll(sort: TrackSort, filter: TrackFilter): Flow<List<Track>>
    fun observeByIds(ids: List<String>): Flow<List<Track>>
    suspend fun get(id: String): Track?
    suspend fun rescan(scope: ScanScope): ScanResult
}

interface AlbumRepository { ... }
interface ArtistRepository { ... }
interface PlaylistRepository {
    fun observeAll(): Flow<List<Playlist>>
    fun observeTracksOf(playlistId: String): Flow<List<Track>>
    suspend fun create(name: String): Playlist
    suspend fun rename(id: String, newName: String)
    suspend fun reorder(id: String, fromPos: Int, toPos: Int)
    suspend fun addTracks(id: String, trackIds: List<String>, atPos: Int? = null)
    suspend fun removeAt(id: String, position: Int)
    suspend fun delete(id: String)
    suspend fun importM3U(uri: Uri): ImportResult
    suspend fun exportM3U(id: String, dest: Uri): ExportResult
}
interface TagRepository { ... }
interface LyricRepository {
    suspend fun get(trackId: String): Lyric?
    suspend fun put(trackId: String, raw: String, source: LyricSource)
    suspend fun setOffset(trackId: String, offsetMs: Long)
    suspend fun delete(trackId: String)
    suspend fun importLrcFolder(folderUri: Uri): ImportResult
}
interface StatsRepository { ... }
interface SearchRepository {
    suspend fun search(query: String, scopes: Set<SearchScope> = SearchScope.ALL): SearchResult
}
interface BackupRepository {
    suspend fun export(dest: Uri, options: BackupOptions): BackupResult
    suspend fun import(src: Uri, mode: ImportMode): RestoreResult
}
interface PlaybackController {
    val state: StateFlow<PlaybackState>
    val queue: StateFlow<QueueState>
    suspend fun play(items: List<Track>, startIndex: Int = 0)
    suspend fun playAfterCurrent(items: List<Track>)
    suspend fun playLast(items: List<Track>)
    suspend fun togglePlayPause()
    suspend fun next()
    suspend fun previous()
    suspend fun seekTo(positionMs: Long)
    suspend fun setShuffle(enabled: Boolean)
    suspend fun setRepeatMode(mode: RepeatMode)
    suspend fun setAbRepeat(start: Long?, end: Long?)
    suspend fun moveQueueItem(from: Int, to: Int)
    suspend fun removeFromQueue(position: Int)
    suspend fun clearQueue()
}
```

### 5.3 UseCase

UseCase는 **단일 책임의 invoke()** 함수 또는 클래스로 작성한다. 파라미터가 4개 이상이면 입력 데이터 클래스를 만든다.

| UseCase | 의존 |
|---|---|
| `ScanLibraryUseCase` | TrackRepository |
| `PlayTrackUseCase` | PlaybackController |
| `ToggleStarUseCase` | StatsRepository |
| `CreatePlaylistUseCase` | PlaylistRepository |
| `AddTagToTracksUseCase` | TagRepository |
| `ImportLrcFolderUseCase` | LyricRepository |
| `BuildSmartQueueUseCase` | TrackRepository, StatsRepository |
| `ExportBackupUseCase` | BackupRepository |
| `ApplyEqualizerUseCase` | PlaybackController |
| `SearchUseCase` | SearchRepository |
| ... | |

### 5.4 정렬/필터 enum

```kotlin
enum class TrackSort {
    TITLE_ASC, TITLE_DESC,
    ARTIST_ASC, ARTIST_DESC,
    ALBUM_ASC, ALBUM_DESC,
    ADDED_NEWEST, ADDED_OLDEST,
    DURATION_SHORTEST, DURATION_LONGEST,
    PLAY_COUNT_DESC, LAST_PLAYED_DESC,
    RATING_DESC,
}

data class TrackFilter(
    val onlyStarred: Boolean = false,
    val tagIds: Set<String> = emptySet(),
    val albumIds: Set<String> = emptySet(),
    val artistIds: Set<String> = emptySet(),
    val onlyWithLyrics: Boolean = false,
    val onlyPresent: Boolean = true,
)
```

---

## 6. 오디오 엔진 (`:core:audio`)

> 본 절은 본 명세서의 핵심이다. 상용 수준의 재생 안정성을 보장하기 위한 모든 메커니즘이 여기에 있다.

### 6.1 구조 개관

```
┌──────────────────────────────────────────────────────────────────┐
│                    PlaybackService (Foreground)                  │
│  - MediaSessionService 상속                                       │
│  - MediaSession (Media3) 단일 인스턴스                            │
│  - 미디어 알림 자동 발행                                          │
└────────────────────────┬─────────────────────────────────────────┘
                         │
                  ┌──────▼──────┐
                  │ AudioEngine │     (Singleton, Hilt @Singleton)
                  └──────┬──────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
   ┌────▼────┐     ┌────▼────┐      ┌────▼────────────┐
   │ Player  │     │ Player  │      │ CommandDispatcher│
   │   #A    │     │   #B    │      │ (Mutex 직렬화)   │
   └────┬────┘     └────┬────┘      └─────────────────┘
        │               │
  ExoPlayer         ExoPlayer
  (primary)         (secondary, crossfade용)
```

### 6.2 듀얼 ExoPlayer

#### 6.2.1 왜 두 개인가

- **크로스페이드**는 두 트랙이 동시에 들려야 하므로 단일 player로는 불가능하다.
- **갭리스 재생**은 ExoPlayer의 `setMediaItems()` 큐 모드로 단일 player로 가능하지만, 그것을 크로스페이드 로직과 같은 player에 두면 상태 전이가 복잡해진다.
- 따라서 두 개의 player를 만들어 **A는 현재 재생, B는 다음 곡 프리로드/페이드 인** 역할로 분리한다. 트랙 전환 후 역할을 스왑한다.

#### 6.2.2 역할 정의

| 시점 | Player A | Player B | 비고 |
|---|---|---|---|
| 정지 상태 | idle, 미할당 | idle, 미할당 | |
| 재생 중 (크로스페이드 OFF, 갭리스 OFF) | playing track[i] | idle | 트랙 종료 후 A가 track[i+1] 로드 |
| 재생 중 (갭리스 ON) | playing track[i] | idle | A가 setNextMediaItem 사용 |
| 크로스페이드 진입 | fading out track[i], volume↓ | playing track[i+1], volume↑ | T초간 동시 재생 |
| 크로스페이드 종료 | idle (released) | playing track[i+1] | 역할 스왑: B→A |

#### 6.2.3 크로스페이드 3-branch

```kotlin
enum class CrossfadeMode { OFF, AUTO_ONLY, ALWAYS }

// OFF       : 크로스페이드 없음. 갭리스 옵션이 켜져 있으면 갭리스로 연결.
// AUTO_ONLY : 트랙이 자연 종료될 때만 크로스페이드.
//             사용자가 next/previous를 누르면 즉시 전환 (페이드 없음).
// ALWAYS    : 사용자 next/previous 포함, 모든 전환을 크로스페이드.
```

크로스페이드 길이 `T`는 `[1.0, 12.0]` 초. 곡 잔여 시간이 `T`보다 짧으면 잔여 시간만큼만 페이드한다.

#### 6.2.4 페이드 곡선

`equal-power crossfade`를 채택한다 (단순 선형은 중간에서 음량이 -3dB 떨어진다).

```
t ∈ [0, 1]
gainOut(t) = cos(t * π/2)
gainIn(t)  = sin(t * π/2)
```

20Hz 샘플링으로 `Player.volume`을 갱신한다 (50ms마다). ExoPlayer의 `volume`은 0.0~1.0이며 메인 스레드에서만 호출 가능하므로 메인 스레드에 디스패치한다.

### 6.3 CommandDispatcher (Mutex)

ExoPlayer는 메인 스레드에서만 호출 가능하지만, 우리가 구현하는 명령들(다음/이전/큐 변경/크로스페이드 트리거)은 동시에 들어올 수 있다. 이를 직렬화하지 않으면 **상태 경쟁**으로 인해 두 player가 동시에 같은 트랙을 재생하거나, 페이드 도중 next가 들어와 타이머가 누적되는 등의 버그가 발생한다.

```kotlin
class CommandDispatcher(
    private val mainScope: CoroutineScope,    // Main 디스패처
) {
    private val mutex = Mutex()

    suspend fun <T> run(block: suspend () -> T): T = mutex.withLock {
        withContext(mainScope.coroutineContext) { block() }
    }
}
```

모든 외부 명령(`PlaybackController`의 메서드)은 내부적으로 `dispatcher.run { ... }`로 감싸 실행한다. 페이드 타이머 자체는 별개의 코루틴 잡이며, 새로운 명령이 들어오면 이전 잡을 즉시 취소한다.

### 6.4 갭리스 재생

- 사전 조건: 두 트랙이 동일한 `sampleRate` 및 `channelCount`를 가질 때만 갭리스 보장.
- 구현: `ExoPlayer.setMediaItems`로 큐 전달 + `ConcatenatingMediaSource2` (Media3 1.4+ 권장).
- 크로스페이드 모드가 `ALWAYS`인 경우 갭리스 설정과 충돌한다. 우선순위: 크로스페이드 > 갭리스.

### 6.5 A-B 반복

- 사용자가 시작점 A를 마크 → 종료점 B를 마크 → A↔B 구간 반복.
- 구현: 1초마다 `currentPosition`을 폴링하지 않는다. ExoPlayer의 `Player.Listener.onPositionDiscontinuity` + 250ms 주기 `Handler` 폴링으로 B 도달을 감지하여 `seekTo(A)`.
- B에서 다시 A로 돌아갈 때 **페이드 처리하지 않는다** (의도된 점프).
- A=B 또는 B<A인 경우 무시.

### 6.6 ReplayGain

- 메타데이터 태그(`REPLAYGAIN_TRACK_GAIN`, `REPLAYGAIN_ALBUM_GAIN`)를 스캔 시 추출하여 DB에 저장.
- 재생 시 `mode`(off/track/album)에 따라 `ExoPlayer.volume` 보정.
- 보정 공식: `linear = 10^((gain_db + preamp_db) / 20)`. `[0.0, 1.0]`로 클립.
- 크로스페이드 곡선과 합성: `finalVolume = crossfadeGain × replayGainLinear`.

### 6.7 이퀄라이저

- `android.media.audiofx.Equalizer`를 ExoPlayer의 `audioSessionId`에 부착.
- 10밴드 설정 가능: 31, 62, 125, 250, 500, 1k, 2k, 4k, 8k, 16k Hz.
- 각 밴드는 `[-12, +12]` dB.
- 프리셋: Flat, Bass Boost, Vocal, Acoustic, Classical, Dance, Electronic, Hip-Hop, Jazz, Pop, R&B, Rock, Custom.
- 기기 호환성을 위해 시스템 EQ가 활성이면 사용자에게 경고 (이중 적용 가능성).
- Bluetooth 코덱별 비호환 가능 — EQ 적용 실패 시 사용자에게 알림 (Snackbar) 후 자동 비활성.

### 6.8 오디오 포커스 & 외부 이벤트

| 이벤트 | 동작 |
|---|---|
| `AUDIOFOCUS_LOSS` | 일시정지 (재생 중이었으면 재개 플래그 해제) |
| `AUDIOFOCUS_LOSS_TRANSIENT` | 일시정지 (재개 플래그 설정) |
| `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` | 볼륨 30%로 감소 |
| `AUDIOFOCUS_GAIN` | 재개 플래그가 있으면 재개, 볼륨 복원 |
| `ACTION_AUDIO_BECOMING_NOISY` (헤드폰 분리, BT 끊김) | `headset_unplug_pause` 설정에 따라 일시정지 |
| `ACTION_HEADSET_PLUG` | 자동 재생 안 함 (보안/예의) |

### 6.9 MediaSession

- 단일 `MediaSession` 인스턴스를 `PlaybackService`가 보유.
- 잠금화면/알림/Bluetooth 미디어 컨트롤/Wear OS/자동차 시스템 응답.
- `MediaMetadata`: title, artist, album, artworkUri, durationMs.
- 커스텀 액션 (선택, v1.1+): "별표", "태그 추가".

### 6.10 PlaybackService (Foreground Service)

- `MediaSessionService` 상속.
- `FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK`.
- `POST_NOTIFICATIONS` 권한 (API 33+) — 첫 재생 시 요청.
- 재생 시작 시 `startForeground` 호출, 큐가 비고 일시정지 상태로 5분 경과 시 `stopForeground`.
- 알림은 Media3가 자동 발행.

### 6.11 큐 관리 정책

| 명령 | 동작 |
|---|---|
| **재생** | 큐 교체. 새 컬렉션을 큐로 설정, startIndex부터 재생 |
| **다음에 재생** | 현재 인덱스 +1 위치에 삽입 (`is_user_added=1`) |
| **마지막에 재생** | 큐 끝에 추가 |
| **셔플 ON** | 현재 곡을 0번으로 옮기고, 나머지 무작위 셔플. 셔플 OFF 시 원래 순서 복원 |
| **반복 OFF** | 큐 마지막 곡 종료 시 정지 |
| **반복 ALL** | 큐 마지막 곡 종료 시 0번으로 복귀 |
| **반복 ONE** | 현재 곡 종료 시 같은 곡 재시작. 사용자 next는 큐 다음으로 이동 |

### 6.12 라이브러리 스캔

- 두 가지 모드: **MediaStore 스캔**(빠름, 권장) / **사용자 폴더 스캔** (Storage Access Framework, 정밀).
- 증분 스캔: 마지막 스캔 시각 이후 추가/수정된 파일만 처리.
- 백그라운드 `WorkManager` Job. 알림으로 진행률 표시.
- 메타 추출: Media3의 `MetadataRetriever` 또는 `MediaMetadataRetriever`. 임베디드 아트는 캐시 디렉터리에 저장 (`/cache/album_art/{album_id}.jpg`).
- 제외 폴더: 사용자 설정의 `excluded_folders`에 매칭되는 경로는 스킵.
- 짧은 파일(< 30초) 자동 제외 옵션 (벨소리/효과음 회피).

---

## 7. 프레젠테이션 레이어

### 7.1 MVI

```kotlin
abstract class BaseViewModel<S, I, E>(initialState: S) : ViewModel() {
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _events = MutableSharedFlow<E>(extraBufferCapacity = 16)
    val events: SharedFlow<E> = _events.asSharedFlow()

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }
    protected suspend fun emitEvent(event: E) {
        _events.emit(event)
    }
    abstract fun onIntent(intent: I)
}
```

- **State**: 스크린 렌더링에 필요한 모든 데이터.
- **Intent**: 사용자/시스템 액션 (sealed class).
- **Event**: 일회성 효과 (Snackbar, Navigation). UI는 `LaunchedEffect` + `collect`로 소비.

### 7.2 화면 목록

#### 7.2.1 `:feature:library`

| 화면 | 경로 | 설명 |
|---|---|---|
| LibraryHome | `library/` | 4개 탭: 트랙/앨범/아티스트/태그. 상단 검색 바 |
| TrackList | `library/tracks` | 정렬/필터 가능한 전체 트랙. 멀티선택 모드 |
| AlbumList | `library/albums` | 그리드 (2~3열) 또는 목록 |
| AlbumDetail | `library/albums/{id}` | 앨범 아트 헤더, 트랙 목록, 재생/셔플/추가 |
| ArtistList | `library/artists` | 알파벳 인덱스 |
| ArtistDetail | `library/artists/{id}` | 앨범 + 단일 트랙 |
| TagList | `library/tags` | 색상 도트와 사용량 |
| TagDetail | `library/tags/{id}` | 태그 보유 트랙 목록 |

#### 7.2.2 `:feature:player`

| 화면 | 경로 | 설명 |
|---|---|---|
| NowPlaying | `player/now` | 풀스크린. 앨범 아트, 컨트롤, 가사 토글 |
| Lyrics | `player/lyrics` | 동기화 가사 표시 (현재 줄 강조), 드래그로 시킹 |
| LyricsEditor | `player/lyrics/edit` | 시간 마커 편집, 라인 추가/삭제, 오프셋 슬라이더 |
| Queue | `player/queue` | 모달 시트. 재정렬, 삭제 |

> MiniPlayer는 화면이 아니라 **루트 Scaffold의 BottomBar 위에 부유하는 컴포넌트**. 모든 화면 위에 표시 (Now Playing, Settings, 큐 시트 제외).

#### 7.2.3 `:feature:playlist`

| 화면 | 설명 |
|---|---|
| PlaylistList | 플레이리스트 그리드. 핀, 재정렬 |
| PlaylistDetail | 트랙 목록, 드래그 재정렬, 가져오기/내보내기 |
| PlaylistCreate | 이름, 설명, 커버 (선택) |

#### 7.2.4 `:feature:search`

| 화면 | 설명 |
|---|---|
| Search | 단일 입력. 결과는 트랙/앨범/아티스트/태그/플레이리스트 섹션. 최근 검색어 |

#### 7.2.5 `:feature:settings`

| 화면 | 설명 |
|---|---|
| SettingsHome | 카테고리 목록 |
| GeneralSettings | 테마, 액센트, 언어, 글꼴 |
| PlaybackSettings | 크로스페이드, 갭리스, ReplayGain, 헤드폰 정책 |
| EqualizerSettings | 10밴드 슬라이더, 프리셋, 활성 토글 |
| LibrarySettings | 폴더 제외, 짧은 파일 제외, 수동 재스캔 |
| BackupSettings | 내보내기/가져오기, 자동 백업 빈도 (선택, v1.1) |
| AboutSettings | 버전, 라이선스, 변경 로그 |

### 7.3 Navigation

- `androidx.navigation:navigation-compose`.
- `app` 모듈에 `NavGraph`. 각 feature는 `NavGraphBuilder.feature{Library,Player,...}Graph()` 확장 함수를 노출.
- DeepLink: v1.0에서는 외부 인텐트 진입점만 (e.g., 다른 앱이 음악 파일을 "열기"로 전달).

---

## 8. 디자인 시스템 (`:core:design`)

### 8.1 색

라이트/다크 두 팔레트. 모든 색은 토큰을 통해서만 사용한다 (Composable에서 hex 직접 금지).

**라이트**

| 토큰 | 값 | 용도 |
|---|---|---|
| `surface.background` | `#FAFAF7` | 메인 배경 |
| `surface.elevated` | `#FFFFFF` | 카드, 시트 |
| `surface.muted` | `#F2F1ED` | 입력, 아이콘 보조 영역 |
| `divider` | `#E7E5DF` | |
| `text.primary` | `#1F1E1B` | |
| `text.secondary` | `#6B6962` | |
| `text.tertiary` | `#9A968D` | |
| `text.disabled` | `#C9C5BC` | |
| `accent.default` | `#2B6CB0` | 사용자가 변경 가능. 8색 팔레트 중 1 |
| `accent.muted` | accent + 90% 흰 혼합 | |

**다크**

| 토큰 | 값 |
|---|---|
| `surface.background` | `#161514` |
| `surface.elevated` | `#1F1E1C` |
| `surface.muted` | `#2A2926` |
| `divider` | `#2F2E2B` |
| `text.primary` | `#EDEAE3` |
| `text.secondary` | `#A8A49B` |
| `text.tertiary` | `#75726B` |
| `accent.default` | `#7BA7D9` |

**액센트 8색 팔레트** (사용자 선택)

| Slate | Stone | Sage | Moss | Slate Blue | Plum | Sand | Clay |
|---|---|---|---|---|---|---|---|
| #5C6770 | #6B6962 | #76876B | #4F6E50 | #2B6CB0 | #8E5572 | #B19063 | #A95740 |

### 8.2 타이포그래피

| 역할 | 폰트 | 크기 | 굵기 | 행간 | 자간 |
|---|---|---|---|---|---|
| Display | Pretendard / Noto Sans | 28sp | 600 | 1.2 | -0.01em |
| Title | Pretendard / Noto Sans | 20sp | 600 | 1.3 | -0.005em |
| Body | Pretendard / Noto Sans | 15sp | 400 | 1.5 | 0 |
| BodyStrong | 동상 | 15sp | 600 | 1.5 | 0 |
| Caption | 동상 | 12sp | 400 | 1.4 | 0.01em |
| Mono (메타) | JetBrains Mono | 12sp | 400 | 1.3 | 0 |
| Lyric | Pretendard / Noto Serif (옵션) | 16~24sp 가변 | 400 | 1.7 | 0 |

> 한국어/일본어 글리프는 Pretendard와 Noto의 폴백으로 처리. 폰트는 `app`에 raw resource로 임베디드 (네트워크 다운로드 금지).

### 8.3 간격 / 모서리 / 모션

| 토큰 | 값 |
|---|---|
| `space.xxs` | 2dp |
| `space.xs` | 4dp |
| `space.s` | 8dp |
| `space.m` | 12dp |
| `space.l` | 16dp |
| `space.xl` | 24dp |
| `space.xxl` | 32dp |
| `radius.s` | 4dp |
| `radius.m` | 8dp |
| `radius.l` | 12dp |
| `radius.xl` | 16dp |
| `motion.fast` | 150ms, easeOut |
| `motion.standard` | 250ms, easeInOutCubic |
| `motion.slow` | 400ms, easeInOutCubic |

### 8.4 컴포넌트 (`:core:ui`)

핵심 재사용 컴포넌트 (모두 Composable):

- `StackTopBar` — 좌측 타이틀, 우측 액션 슬롯
- `StackTabRow` — 4개 이하 텍스트 탭
- `TrackRow` — 좌: 아트/숫자, 본문: 제목+아티스트, 우: duration/star/menu
- `AlbumGridCell` / `AlbumListRow`
- `Section` — 제목 + 자식 컴포저블
- `EmptyState` — 일러스트 슬롯 + 제목 + 설명 + 액션
- `BottomSheet` — Material3 ModalBottomSheet 래퍼, 헤더 표준화
- `SnackbarHost` — 단일 스낵바 정책
- `MiniPlayer` — 64dp 높이, 좌: 아트, 본문: 제목+진행 바, 우: play/pause/next
- `LyricView` — 동기화 가사 렌더러, 자동 스크롤, 탭 시킹
- `Slider` — 커스텀 박형 슬라이더 (재생, EQ 공용)
- `RatingDots` — 5점 별 대신 5개 점

### 8.5 접근성

- 모든 인터랙티브 요소는 `Modifier.semantics { contentDescription = ... }` 또는 명시적 텍스트 라벨.
- 최소 터치 타깃 48dp.
- 색 대비 WCAG AA 이상 (정상 본문 4.5:1).
- TalkBack 테스트 체크리스트 (16절 참조).
- 시스템 글꼴 크기 200%까지 깨지지 않을 것.
- 다이내믹 컬러 (Material You)는 v1.0에서 **사용하지 않는다** (디자인 일관성 우선).

---

## 9. 기능 명세

### 9.1 라이브러리 관리

#### 9.1.1 첫 실행

1. 권한 요청: `READ_MEDIA_AUDIO` (API 33+) / `READ_EXTERNAL_STORAGE` (API ≤32).
2. 권한 부여 시: `WorkManager`로 초기 스캔 시작. 로딩 화면에 진행률.
3. 권한 거부 시: 빈 라이브러리 + "권한 부여" CTA.

#### 9.1.2 스캔 결과 표기

- 신규 트랙 N개, 갱신 M개, 삭제(파일 부재) K개, 오류 E개를 Snackbar로 보고.
- 오류는 별도 화면 (Settings > Library > Recent scan log)에서 확인 가능.

#### 9.1.3 멀티 선택

트랙 목록에서 길게 누르면 선택 모드 진입. 상단 바가 액션 모드로 전환.

| 액션 | 동작 |
|---|---|
| 다음에 재생 | 큐 head+1에 삽입 |
| 마지막에 재생 | 큐 끝에 추가 |
| 플레이리스트에 추가 | 시트로 플레이리스트 선택 |
| 태그 추가/제거 | 시트로 태그 선택 |
| 별표 토글 | |
| 평점 부여 | 5점 점 시트 |
| 정보 보기 | (단일 선택일 때만) 메타데이터 시트 |

### 9.2 커스텀 태그

- 사용자가 트랙에 임의 라벨 부여. e.g., "운동", "야간", "JPOP", "LP음원".
- 태그 색상: 디자인 토큰 8색 + 무채색 1 = 9색 중 선택.
- 정규화 이름으로 중복 방지 ("야간" / "야간 ").
- 단일 트랙당 태그 수 제한 없음 (UI는 5개까지 칩으로 표시, 나머지는 +N).
- 태그 삭제 시 모든 트랙에서 제거 (CASCADE).
- 태그로 가상 큐 생성: "이 태그의 모든 곡 셔플 재생".

### 9.3 검색

- 단일 텍스트 입력. 입력 후 250ms debounce.
- 스코프: 트랙/앨범/아티스트/태그/플레이리스트.
- FTS4 사용. 한글 부분 일치 (전방 일치 + 음절 단위).
- 최근 검색어 10개 (DataStore에 저장).
- 결과 항목 탭 → 해당 디테일 화면.
- 결과 목록에서 곧바로 멀티선택 액션 가능.

### 9.4 동기화 가사

#### 9.4.1 가사 소스 우선순위

1. 사용자가 직접 입력/편집한 것 (`source=2`)
2. 사이드카 `.lrc` (트랙 파일과 같은 폴더, 같은 이름)
3. 메타데이터 임베디드 (USLT 등)

스캔 시 최상위 가사를 자동 채택하여 `lyrics` 테이블에 저장.

#### 9.4.2 LRC 포맷 지원

- 표준 `[mm:ss.xx]text` 라인.
- 확장: 다중 타임스탬프, `[ar:]`, `[ti:]`, `[al:]`, `[offset:]` 헤더 파싱.
- 단어 단위 `<mm:ss.xx>` 강조는 v1.0에서는 **파싱하되 라인 단위로만 표시** (단어 강조는 v1.2+).

#### 9.4.3 가사 표시

- Now Playing 화면의 토글 (앨범 아트 ↔ 가사).
- 현재 라인은 `text.primary`, 이전/이후 라인은 `text.tertiary`.
- 자동 스크롤 (현재 라인을 화면 중앙). 사용자가 수동 스크롤 시 자동 스크롤 일시 중단 (탭으로 재개).
- 라인 탭 → 해당 시간으로 시킹.
- 한 줄 길이 초과 시 줄바꿈 (가운데 정렬).

#### 9.4.4 LRC 에디터

- 평문 가사 → 동기화 가사 마법사: 재생하며 매 라인의 시작점에 "마크" 버튼.
- 기존 LRC: 라인별 시간 편집 (HH:MM:SS.xx 입력 또는 드래그).
- 전역 오프셋 슬라이더: -2000ms ~ +2000ms.
- 저장 시 DB에 raw LRC 갱신.
- "내보내기": 현재 라이브러리 폴더에 `.lrc` 사이드카 작성 (SAF 사용, 사용자 동의 후).

### 9.5 이퀄라이저

- 시스템 EQ 활성 감지: 활성 시 사용자에게 "시스템 이퀄라이저가 활성화되어 있습니다. Stack EQ와 중복 적용될 수 있습니다." 안내.
- 슬라이더는 단일 컬럼 가로 배치, 또는 가로 모드에서 막대 그래프 형태.
- 프리셋 변경 시 Custom 슬롯에 현재 값 자동 백업.

### 9.6 슬립 타이머

- 옵션: 5/15/30/45/60/90분 + Custom.
- "현재 곡 끝까지" 옵션: 타이머 만료 시점에 재생 중이면 곡 끝까지 페이드아웃 후 정지.
- 종료 시 재생 정지 + 알림 해제 + 옵션에 따라 화면 끄기 명령 (필요 권한 없음, 시스템 동작).

### 9.7 백업 / 복원

#### 9.7.1 백업 내용

ZIP 단일 파일 (`stack-backup-yyyyMMdd-HHmm.zip`) :

```
manifest.json          # 버전, 시각, 기기 정보
playlists.json         # 플레이리스트 + 트랙 ID 목록
tags.json              # 태그 + 트랙 매핑
stats.json             # 평점, 별표, 재생수
lyrics/                # 트랙별 LRC
  {track_id}.lrc
prefs.json             # 사용자 설정 (테마, EQ 등)
```

> **트랙 자체는 백업하지 않는다.** 트랙은 사용자의 미디어 파일이며 별도로 관리.
> 복원은 `media_uri` 또는 `(title, album, duration)` 매칭으로 트랙을 식별한다.

#### 9.7.2 복원 모드

- **병합**: 기존 데이터 유지, 백업 데이터 추가.
- **덮어쓰기**: 기존 모두 삭제 후 백업으로 채움 (확인 다이얼로그 필수).

#### 9.7.3 자동 백업 (v1.1+)

- 주간/월간 빈도. 사용자가 지정한 폴더(SAF)에 작성. 최근 5개만 보관.

### 9.8 위젯 (v1.1)

- 4×1 위젯: 앨범 아트 + 제목 + 컨트롤 (이전/재생/다음).
- 4×2 위젯: + 진행 바 + 큐 미리보기 3곡.

### 9.9 잠금화면 / 알림

- Media3 자동 발행. 사용자 정의 색상은 적용하지 않는다.
- 액션: 이전, 재생/일시정지, 다음.
- 확장 시 추가 액션: 별표, 태그 (v1.2+).

---

## 10. 국제화

### 10.1 지원 언어

| 코드 | 표시 | 비고 |
|---|---|---|
| `ko` | 한국어 | 기본 |
| `en` | English | |
| `ja` | 日本語 | |

### 10.2 원칙

- **모든 사용자 노출 텍스트는 `strings.xml`에 위치한다.** 코드 내 하드코딩 금지 (lint 규칙으로 강제).
- 한 키 = 한 의미. 같은 단어라도 문맥이 다르면 별개 키.
- 복수형은 `plurals` 사용.
- 한국어/일본어 텍스트는 줄바꿈 시 단어 끊김에 유의 (CSS `word-break: keep-all` 상응 — Compose `LineBreak`).

### 10.3 키 명명 규칙

```
{화면}_{컴포넌트}_{역할}
e.g.,
  library_tracks_title
  player_action_play
  settings_eq_preset_flat
  common_action_cancel
```

### 10.4 날짜 / 숫자

- 날짜: `DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(currentLocale)`.
- 시간: `mm:ss` 형식 통일 (1시간 이상 트랙은 `H:mm:ss`).
- 숫자 그룹화: locale 기본 (한국/일본 = `1,234`, 영어 동일).

### 10.5 RTL

- v1.0은 RTL 언어를 지원하지 않으므로 `android:supportsRtl="false"`.

---

## 11. 보안 / 프라이버시

### 11.1 권한

| 권한 | 사용 목적 | 비고 |
|---|---|---|
| `READ_MEDIA_AUDIO` | 라이브러리 스캔 | API 33+ |
| `READ_EXTERNAL_STORAGE` | 라이브러리 스캔 | API ≤ 32 |
| `POST_NOTIFICATIONS` | 미디어 알림 | API 33+. 첫 재생 직전 요청 |
| `FOREGROUND_SERVICE` | 백그라운드 재생 | |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 동상 (API 34+) | |
| `WAKE_LOCK` | 재생 중 화면 꺼져도 지속 | (ExoPlayer가 자동 관리) |

### 11.2 명시적으로 선언하지 않는 권한

- `INTERNET` — **선언 금지**. 라이브러리 의존성이 의도치 않게 요구하면 manifest merger 정책으로 차단.
- `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`
- `READ_PHONE_STATE`, `RECORD_AUDIO`
- 위치 권한 일체

### 11.3 데이터 처리

- 모든 데이터는 앱 내부 저장소(`/data/data/com.stack.player/`).
- 백업 ZIP에 평문으로 저장 (사용자 SAF로 직접 관리).
- v1.1+에서 백업 암호화 옵션 (사용자 패스프레이즈, AES-GCM-256).
- 디버그 빌드는 `usesCleartextTraffic=false`이지만 어차피 네트워크 콜이 없음.

### 11.4 매니페스트 검증

CI에서 다음을 검사한다:

```bash
# 빌드 산출물 APK의 매니페스트에 INTERNET이 없는지 확인
aapt dump permissions app-release.apk | grep -i internet && exit 1 || exit 0
```

---

## 12. 성능 예산

### 12.1 콜드 스타트

| 단계 | 목표 |
|---|---|
| Application.onCreate 완료 | < 80ms |
| MainActivity 첫 프레임 | < 350ms (저사양 기준) |
| 라이브러리 첫 항목 표시 | < 600ms (1만 트랙 캐시 후) |

App Startup 라이브러리로 초기화 순서를 명시적으로 관리. Hilt 부트스트랩이 콜드 스타트를 늘리는 주범이므로 lazy injection 적극 사용.

### 12.2 메모리

- 정상 사용 시 RSS 평균 < 180MB (재생 중 + 라이브러리 화면).
- 1만 트랙 라이브러리에서 최대 < 280MB.
- 앨범 아트 캐시: 메모리 16MB, 디스크 64MB (Coil 설정).

### 12.3 프레임

- 모든 스크롤 화면에서 jank < 0.1% (Macrobenchmark 기준).
- LazyColumn은 `key` 명시. 트랙 항목은 `Modifier.animateItemPlacement()` 신중히 사용 (1만 항목에서 비싸다).

### 12.4 배터리

- 1시간 연속 재생 시 배터리 소비 < 5% (화면 꺼진 상태, 5세대급 표준 기기).
- ExoPlayer wakelock은 재생 중에만, 일시정지 시 즉시 해제.
- 백그라운드 작업(스캔, 통계 집계)은 `WorkManager` + `Constraints.requiresCharging=false, requiresBatteryNotLow=true`.

### 12.5 데이터베이스

- 모든 DAO 쿼리는 `EXPLAIN QUERY PLAN`으로 검증. 풀 스캔 발견 시 인덱스 추가.
- 1회 트랜잭션당 < 50ms.
- 라이브러리 첫 로드는 페이징 없이 1만 항목 정도까지 일괄 로드 가능 (LazyColumn이 가상화).

### 12.6 측정 도구

- Android Studio Profiler (수동 측정)
- Macrobenchmark + Baseline Profile
- LeakCanary (debug only)
- Compose Compiler Metrics 분석

---

## 13. 품질 보증

### 13.1 테스트 피라미드

| 레벨 | 도구 | 적용 |
|---|---|---|
| Unit | JUnit5 + Turbine + MockK | UseCase, ViewModel, Reducer, 유틸 |
| Integration | Robolectric | Repository, DAO (in-memory Room) |
| UI | Compose UI Test | 핵심 사용자 플로우 (각 feature당 3~5개) |
| Macrobenchmark | macrobenchmark-junit4 | 콜드 스타트, 스크롤 jank, 라이브러리 스캔 |
| Migration | MigrationTestHelper | 모든 마이그레이션 |

### 13.2 필수 테스트 시나리오

- [ ] 라이브러리 스캔: 빈 폴더, 1개, 100개, 10000개 트랙
- [ ] 재생: 재생/정지/다음/이전/시킹/큐 변경
- [ ] 크로스페이드: OFF/AUTO_ONLY/ALWAYS 각각, 곡 잔여 < 페이드 길이 케이스
- [ ] 갭리스: 동일 포맷, 다른 포맷
- [ ] A-B 반복: 정상 케이스, A=B, B<A
- [ ] 오디오 포커스: 통화 수신, 알림음
- [ ] 헤드폰 분리, BT 끊김
- [ ] 앱 강제 종료 후 큐 / 위치 / 셔플 / 반복 모드 복원
- [ ] 백그라운드에서 1시간 재생 후 알림 유지
- [ ] 가사 LRC 파싱: 표준, 다중 타임스탬프, 비표준 (관대하게 무시)
- [ ] 백업 → 복원 (병합, 덮어쓰기) 라운드트립
- [ ] EQ 적용 / 해제 / 프리셋 변경
- [ ] FTS 검색: 한국어 부분 일치, 일본어 가나/한자, 영어 케이스 무시
- [ ] 권한 거부 흐름
- [ ] 다크/라이트 테마 전환
- [ ] 언어 변경 (KO/EN/JA)
- [ ] 시스템 글꼴 200% 확대

### 13.3 정적 분석

- ktlint (Kotlin 코드 스타일)
- detekt (복잡도, 코드 스멜)
- Android Lint (vital + 사용자 정의 규칙)
  - 커스텀 규칙: `INTERNET` 권한 요청 시 빌드 실패
  - 커스텀 규칙: 하드코딩된 사용자 노출 문자열 검출

### 13.4 코드 리뷰 체크리스트

PR 템플릿:

- [ ] 본 명세서의 어느 섹션을 구현/수정했는가?
- [ ] 의존성 규칙(3.3) 위반이 없는가?
- [ ] 새 사용자 노출 문자열은 KO/EN/JA 모두 추가되었는가?
- [ ] 새 권한 요청이 있는가? 있다면 11.1과 일치하는가?
- [ ] 단위 테스트가 추가/갱신되었는가?
- [ ] Compose 프리뷰가 라이트/다크 모두 추가되었는가? (UI 변경 시)
- [ ] 접근성 (contentDescription, 터치 타깃) 확인했는가?

---

## 14. 빌드 & CI

### 14.1 Gradle 구성

- **Convention plugins** (`build-logic/`) 으로 Android/Kotlin 공통 설정 일원화.
  - `stack.android.application`
  - `stack.android.library`
  - `stack.android.library.compose`
  - `stack.android.feature`
  - `stack.kotlin.library`
  - `stack.hilt`
  - `stack.room`
- **Version catalog** (`gradle/libs.versions.toml`) — 모든 의존성 버전 단일 관리.
- **typesafe project accessors** 활성화 (`enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")`).
- 멀티 모듈 빌드 캐시 활성화.

### 14.2 빌드 변형

| Variant | 용도 |
|---|---|
| `debug` | 개발. minify/shrink off, R8 off |
| `release` | 배포. R8 full mode, baseline profile |

`debug`에 `applicationIdSuffix=".debug"`로 동시 설치 가능.

### 14.3 코드 축소 / 난독화

- R8 full mode.
- ProGuard/R8 규칙은 각 모듈의 `consumer-rules.pro`로 노출.
- Compose, Coil, Hilt, Room 공식 규칙 사용.

### 14.4 의존성 분석 태스크

```kotlin
// build-logic/convention/src/main/kotlin/StackDependencyAnalysis.kt
// 모듈 그래프를 검사하여 3.3의 의존성 규칙 위반 시 task 실패
```

CI에서 `./gradlew :checkModuleDependencies` 실행.

### 14.5 CI 파이프라인 (GitHub Actions 예시)

```
on: [push, pull_request]
jobs:
  verify:
    steps:
      - checkout
      - setup-java 17
      - cache gradle
      - ./gradlew ktlintCheck detekt
      - ./gradlew :app:lintRelease
      - ./gradlew :checkModuleDependencies
      - ./gradlew testDebugUnitTest
      - ./gradlew :app:assembleRelease
      - 매니페스트 INTERNET 검사
      - APK 사이즈 리포트 (전 PR 비교)
```

### 14.6 APK 크기 목표

- minSdk 26 단일 APK 기준 < 8MB (코드).
- App Bundle로 배포, 분할 APK 평균 < 5MB.

---

## 15. 배포

### 15.1 서명

- 업로드 키와 앱 서명 키 분리 (Play App Signing).
- 키 정보는 환경 변수 (`STACK_KEYSTORE_PATH`, `STACK_KEYSTORE_PASSWORD`, ...). 저장소 커밋 금지.

### 15.2 점진적 출시

- 내부 테스트 → 비공개 베타 (50명) → 공개 베타 → 프로덕션 5% → 25% → 50% → 100%.
- 각 단계 최소 48시간 관찰. 외부 크래시 리포터가 없으므로 사용자 피드백 채널을 명시.

### 15.3 변경 로그

- `CHANGELOG.md` (Keep a Changelog 형식).
- 앱 내 "정보 > 변경 로그" 화면에서 노출.

---

## 16. 부록

### 16.1 지원 오디오 포맷

| 포맷 | 컨테이너 | 비고 |
|---|---|---|
| MP3 | .mp3 | |
| AAC / ALAC | .m4a, .aac | ALAC은 ExoPlayer 1.4+ |
| FLAC | .flac | 24bit/96kHz까지 검증 |
| Vorbis | .ogg | |
| Opus | .opus, .ogg | |
| WAV (PCM) | .wav | |
| APE | — | 미지원 |
| DSD | .dsf, .dff | 미지원 (PCM 변환 불가, v1.x 범위 외) |

### 16.2 키보드/외부 컨트롤러 단축키

| 키 | 동작 |
|---|---|
| Space / 미디어 재생키 | 재생/일시정지 |
| 미디어 다음/이전 | 다음/이전 트랙 |
| ←/→ (NowPlaying에서) | 5초 시킹 |
| Shift + ←/→ | 이전/다음 트랙 |
| ↑/↓ | 시스템 볼륨 |
| L | 가사 토글 |
| Q | 큐 시트 |
| / | 검색 화면 |
| S | 별표 토글 |
| 1~5 | 평점 부여 |

### 16.3 알려진 제약 / 의도된 미지원

- 가사 자동 다운로드 없음 (1.3 비목표)
- 클라우드 동기화 없음
- DSD 직접 재생 없음
- 라디오/팟캐스트 없음
- 차량 헤드유닛 통합은 표준 MediaSession에 의존 (Android Auto 전용 빌드 없음)

### 16.4 v1.x 로드맵 (참고용, 본 명세서의 일부 아님)

- v1.1 — 자동 백업, 4×1/4×2 위젯, 다이내믹 컬러 옵션 (디자인 확장)
- v1.2 — LRC 단어 단위 강조, 알림 액션 확장 (별표/태그), 통계 화면
- v1.3 — 스마트 플레이리스트 (조건 기반), 슬립 알람 (지정 시각 시작)
- v2.0 — Wear OS 컴패니언, 카 모드 UI

### 16.5 라이선스 / 크레딧

- 앱 코드: 사용자 결정 (제안: GPLv3 or Mozilla Public License 2.0)
- Pretendard 폰트: SIL Open Font License 1.1
- Noto Sans JP: SIL OFL 1.1
- Material Icons: Apache 2.0
- 의존성 라이선스 표시: 앱 내 "정보 > 오픈소스 라이선스" 화면 (AboutLibraries 라이브러리 사용 가능)

---

## 17. 결정 보류 사항 (TODO)

코드 동결 전 모두 결정해야 한다.

- [ ] 폰트 파일 임베디드 vs Downloadable Fonts (네트워크 사용 여부 — 후자 채택 시 1.2 위배 가능, 검토 필요)
- [ ] 다이내믹 컬러 v1.1에서 토글 제공 여부
- [ ] 백업 ZIP 내 트랙 메타 스냅샷 포함 여부 (현재는 미포함; 포함 시 라이브러리 재구성 가능)
- [ ] 시스템 EQ 활성 시 Stack EQ 자동 비활성 vs 안내만
- [ ] 트랙 정렬 시 한국어 자모 정렬 규칙 (가나다 vs Unicode 코드포인트)
- [ ] 일본어 검색 시 외부 IME 가나/한자 변환 의존 vs 자체 인덱스 (TODO: Mecab 도입 검토)

---

## 부속 문서

- `STYLE_GUIDE.md` — 코드 스타일, 네이밍, 커밋 메시지 규약
- `CONTRIBUTING.md` — 기여 가이드
- `CHANGELOG.md` — 변경 로그
- `BUILD.md` — 로컬 빌드 가이드
- `THREADING_MODEL.md` — 코루틴 디스패처/스코프 정책 상세
- `DESIGN_TOKENS.md` — 디자인 토큰 시각 카탈로그

---

*— 본 문서 끝.*
