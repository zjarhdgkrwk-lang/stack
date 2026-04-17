# Stack — Style Guide

> 본 문서는 Stack 프로젝트의 코드 작성 규약입니다.
> CLAUDE.md §7이 핵심을 요약하고 있고, 본 문서는 그 상세 버전입니다.
> 코드 리뷰 시 본 문서를 근거로 코멘트할 수 있습니다.

---

## 1. 원칙

1. **명료성 > 영리함.** 한 줄로 줄일 수 있어도 읽기 어려우면 두 줄로.
2. **일관성 > 개인 취향.** 본 가이드와 충돌하는 개인 선호는 가이드를 따른다.
3. **삭제 > 추가.** 코드는 부채. 같은 일을 적은 코드로 할 수 있으면 그렇게 한다.
4. **표준 > 자체 구현.** Kotlin/AndroidX 표준 API가 있으면 그것을 사용한다.
5. **표면 면적 최소화.** `public`은 의도된 경우만, 기본은 `internal`.

---

## 2. 파일 / 패키지

### 2.1 파일당 클래스 수

- 파일 1개 = public 최상위 선언 1개 (클래스, 함수, 객체).
- 단, 다음은 같은 파일에 함께 둘 수 있다:
  - sealed class와 그 자식들
  - 데이터 클래스와 그것을 변환하는 짧은 확장 함수
  - 한 화면을 구성하는 `*Screen`, `*Content`, `*State`, `*Intent`, `*Event`
- 파일명은 최상위 선언명과 일치 (e.g., `TrackRow.kt`).

### 2.2 패키지 구조

```
com.stack.player.{module}.{layer}.{feature}
```

- `layer`: `domain`, `data`, `ui`, `di`, `util`
- `feature`: 도메인 단위 (track, playlist, lyric, queue, ...)

예:
```
com.stack.player.core.audio.playback        ← AudioEngine, Players
com.stack.player.core.audio.eq              ← Equalizer
com.stack.player.core.audio.session         ← MediaSession, PlaybackService
com.stack.player.feature.library.ui.tracks  ← TrackListScreen
com.stack.player.feature.library.di         ← Hilt module
```

### 2.3 import 순서

ktlint 기본 규칙 (알파벳 순) + 와일드카드 import 금지. 단, 다음 패키지는 와일드카드 허용:
- `kotlinx.coroutines.*`
- `androidx.compose.runtime.*`
- `androidx.compose.foundation.layout.*`
- `androidx.compose.material3.*`

---

## 3. 명명

### 3.1 Kotlin 일반

| 종류 | 규칙 | 예 |
|---|---|---|
| 클래스 / 인터페이스 | `UpperCamelCase` | `TrackRepository`, `PlaybackController` |
| 함수 / 변수 | `lowerCamelCase` | `playTrack`, `currentIndex` |
| 상수 (`const val`, top-level `val`) | `UPPER_SNAKE_CASE` | `MAX_QUEUE_SIZE` |
| Companion `const` | `UPPER_SNAKE_CASE` | `Companion.DEFAULT_FADE_MS` |
| Composable | `UpperCamelCase` (PascalCase) | `TrackRow`, `MiniPlayer` |
| 패키지 | `lowercase`, 단어 구분 없음 | `playback`, `lyric` |

### 3.2 의미 단어 일관성

| 의미 | 사용 단어 | 사용 금지 |
|---|---|---|
| 반환형 Flow의 read | `observe...()` | `get...Flow()`, `watch...()`, `subscribe...()` |
| 1회성 read | `get...()` (suspend), `find...()` (nullable) | `fetch...()`, `load...()`, `retrieve...()` |
| 생성 | `create...()`, `insert...()` | `add...()` (단, 컬렉션 멤버십은 `add`) |
| 갱신 | `update...()`, `set...()` | `change...()`, `modify...()` |
| 삭제 | `delete...()`, `remove...()` | `destroy...()`, `clear...()` (전체 비울 때만 `clear`) |
| 토글 | `toggle...()` | `flip...()`, `switch...()` |
| 검사 | `is...`, `has...`, `can...` | `check...()` (boolean 반환 안 함) |

예:
```kotlin
// good
interface TrackRepository {
    fun observeAll(): Flow<List<Track>>
    suspend fun get(id: String): Track?
    suspend fun insert(track: Track)
    suspend fun delete(id: String)
}

// bad
interface TrackRepository {
    fun getAllAsFlow(): Flow<List<Track>>     // observeAll()
    suspend fun fetchTrack(id: String): Track? // get(id)
    suspend fun saveTrack(track: Track)        // insert(track)
    suspend fun removeTrack(id: String)        // delete(id)
}
```

### 3.3 Composable 명명

- 명사 또는 명사구. `TrackRow`, `MiniPlayer`, `EmptyLibrary`.
- 상태 표시는 형용사 prefix: `LoadingTrackRow`, `PlaceholderAlbumGrid`.
- 컨테이너는 suffix `Screen`, `Sheet`, `Dialog`, `Section`.

### 3.4 Boolean 변수

긍정형 사용. 부정형 변수명 금지.

```kotlin
// good
val isPlaying: Boolean
val hasLyrics: Boolean
val canCrossfade: Boolean

// bad
val isNotPlaying: Boolean   // !isPlaying
val noLyrics: Boolean       // !hasLyrics
```

---

## 4. Kotlin 언어

### 4.1 가시성

- 기본은 `internal`. `public`은 모듈 경계를 넘는 의도된 API에만.
- 클래스 멤버 기본은 `private`.

### 4.2 nullable

- nullable은 도메인 모델에서 가능한 한 제거.
- "값이 없음"이 의미를 가지면 sealed class로 명시: `Lyric.Unavailable`, `Lyric.Plain`, `Lyric.Synced`.
- platform type은 즉시 명시적 nullable 또는 non-null로 감쌈.

```kotlin
// bad
val title = mediaMetadata.title  // Platform type, nullability 모호

// good
val title: String = mediaMetadata.title?.toString() ?: track.displayName
```

### 4.3 `?:` 와 `?.let`

남용 금지. nullable 체인이 3단계 넘으면 sealed result로 리팩터링 신호.

```kotlin
// bad
val name = user?.profile?.preferences?.displayName?.value ?: "Unknown"

// better
val name = userPreferences.displayName  // nullable 제거된 도메인 모델
```

### 4.4 `data class`

- 동등성/해시/구조분해가 필요할 때만.
- 동작 메서드를 많이 가진다면 일반 `class`.
- DB Entity와 도메인 모델은 분리 (둘 다 `data class`여도 다른 클래스).

### 4.5 sealed class / sealed interface

- "유한한 변형"을 표현할 때 사용.
- sealed interface가 sealed class보다 일반적으로 선호됨 (다중 상속, value class와 호환).

```kotlin
sealed interface ScanResult {
    data class Success(val added: Int, val updated: Int, val removed: Int) : ScanResult
    data class PartialFailure(val succeededCount: Int, val errors: List<ScanError>) : ScanResult
    data object Cancelled : ScanResult
}
```

### 4.6 enum vs sealed

- 단순 식별자, 데이터 없음 → `enum class`
- 변형마다 다른 데이터를 갖는 경우 → `sealed`

### 4.7 Result 타입

- `kotlin.Result<T>` 사용 금지 (`@OptIn` 필요, suspend 반환에 부적합).
- 다음 중 하나 사용:
  - 도메인 sealed result (e.g., `ScanResult` 위 예시)
  - `:core:common`의 `Either<L, R>` (L=실패, R=성공)
  - 단순 nullable (실패 의미가 단일할 때)

### 4.8 확장 함수

- 같은 클래스를 여러 모듈에서 확장하지 않음 (한 곳에서 정의).
- 도메인 모델 변환은 `infix` 또는 `Mapper` 클래스보다 확장 함수 선호:
  ```kotlin
  internal fun TrackEntity.toDomain(): Track = Track(...)
  internal fun Track.toEntity(): TrackEntity = TrackEntity(...)
  ```
- 패키지: `com.stack.player.{module}.data.mapper` 또는 가까운 관련 파일.

### 4.9 `when` 분기

- sealed / enum의 `when`은 항상 exhaustive. 컴파일러가 강제하므로 `else` 추가하지 말 것.
- `else`를 추가하면 새 sealed 자식이 추가되어도 컴파일 에러가 안 남 → 버그 위험.

```kotlin
// bad
when (state) {
    is Loading -> showSpinner()
    is Success -> showContent()
    else -> Unit  // ← 새 sealed 자식 추가 시 에러 안 남
}

// good
when (state) {
    is Loading -> showSpinner()
    is Success -> showContent()
    is Error -> showError(state.message)
}
```

### 4.10 trailing comma

- 함수 파라미터, 인자, 컬렉션 리터럴, when 분기에서 trailing comma 사용.
- ktlint가 강제.

```kotlin
data class Track(
    val id: String,
    val title: String,
    val durationMs: Long,
)

playTrack(
    track = current,
    queue = upcoming,
    startPositionMs = 0L,
)
```

### 4.11 함수 체이닝

- 한 줄에 1~2 호출까지. 그 이상은 줄바꿈.
- 줄바꿈 시 점(`.`)을 다음 줄 시작에 위치.

```kotlin
// good
val recent = tracks
    .filter { it.isPresent }
    .sortedByDescending { it.addedAt }
    .take(20)
```

### 4.12 람다 인자

- 단일 람다는 `{ }` 직접, 명시적 파라미터명 권장 (`it` 남용 금지 — 중첩에서 모호함).
- 람다가 길면(5줄 이상) 명명된 함수로 추출.

```kotlin
// good
tracks.filter { track -> track.isPresent && track.durationMs > 30_000 }

// bad (중첩에서 it이 어느 것?)
albums.flatMap { it.tracks.filter { it.isPresent } }

// good
albums.flatMap { album -> album.tracks.filter { track -> track.isPresent } }
```

### 4.13 String 포맷

- 사용자 노출 문자열은 `strings.xml` (CLAUDE.md §1.3).
- 내부 로깅/디버그는 string template 사용 (`"track=$id, pos=$position"`).
- 복잡한 포맷은 `String.format(Locale.ROOT, ...)` (Locale 명시 필수).

### 4.14 magic number

- 0, 1, -1을 제외한 리터럴은 명명된 상수로.
- 시간 단위는 항상 단위 명시:
  ```kotlin
  // good
  private const val DEFAULT_FADE_DURATION_MS = 4_000L
  private val POLL_INTERVAL = 250.milliseconds
  
  // bad
  delay(250)  // 250 뭐?
  ```

### 4.15 숫자 리터럴 가독성

- 1만 이상은 `_`로 구분: `100_000L`, `1_000_000`.
- Hex는 명확히: `0xFF_AA_BB`.

---

## 5. 코루틴 / Flow

> 상세는 `THREADING_MODEL.md`. 여기서는 코드 스타일만.

### 5.1 디스패처는 항상 명시

- IO 작업: `withContext(Dispatchers.IO) { ... }`
- CPU 작업: `withContext(Dispatchers.Default) { ... }`
- UI/Player: `withContext(Dispatchers.Main) { ... }` 또는 자연스럽게 메인 스코프
- 디스패처 선택은 호출자가 아닌 **호출되는 함수가** 결정 (caller가 신경쓰지 않게)

### 5.2 scope 명시

- ViewModel: `viewModelScope`
- Composable: `rememberCoroutineScope()` 또는 `LaunchedEffect`
- Service: `serviceScope` (직접 정의: `CoroutineScope(Dispatchers.Main + SupervisorJob())`)
- `GlobalScope` **절대 금지**. 어떤 경우에도.

### 5.3 Flow 연산자 선택

| 의도 | 사용 |
|---|---|
| 변환 | `map`, `mapLatest` |
| 필터 | `filter`, `distinctUntilChanged` |
| 결합 | `combine` (병렬) — `zip`은 정말 짝 매칭일 때만 |
| 평탄화 | `flatMapLatest` (구독 갱신), `flatMapConcat` (순차), `flatMapMerge` (동시) |
| 디바운스 | `debounce(250.milliseconds)` |
| 첫 값 대기 | `first()` |
| 종료 | `take(N)`, `takeWhile`, `onCompletion` |

`flatMapLatest`는 검색 입력 같은 "마지막 입력만 유효" 케이스의 표준.

### 5.4 SharedFlow / StateFlow

- 상태 = `StateFlow` (항상 현재값 있음, 동등성 conflation).
- 일회성 이벤트 = `SharedFlow(replay=0, extraBufferCapacity=N)`.
- `MutableStateFlow.update { }` 사용 (스레드 안전 갱신).

### 5.5 collect

- Compose에서: `collectAsStateWithLifecycle()` (lifecycle-aware).
- 일반: `viewModelScope.launch { flow.collect { } }`.
- "마지막 값만 처리": `collectLatest`.

### 5.6 cancellation

- `CancellationException`은 catch하지 말 것 (코루틴 cancellation 메커니즘 파괴).
- catch 후 `if (e is CancellationException) throw e` 패턴 또는:
  ```kotlin
  try { ... } catch (e: Exception) {
      currentCoroutineContext().ensureActive()  // cancelled면 throw
      // 일반 에러 처리
  }
  ```

### 5.7 suspend 함수 시그니처

- suspend는 메서드명에 명시하지 않음 (`getX()` 그대로).
- suspend 함수는 cancellable해야 함 (자체 long-running 루프는 `yield()` 또는 `ensureActive()`).

---

## 6. Compose

### 6.1 함수 시그니처

```kotlin
@Composable
fun TrackRow(
    track: Track,
    modifier: Modifier = Modifier,    // 항상 첫 옵셔널
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    isSelected: Boolean = false,
    isPlaying: Boolean = false,
)
```

- `modifier`는 첫 옵셔널 파라미터.
- 콜백은 `onXxx` 명명.
- `private @Composable` 도우미는 `*Content`, `*Inner` 같은 suffix 권장.

### 6.2 상태 호이스팅

- 가능한 한 stateless. 상태는 호출자가 보유.
- 내부 상태가 외부에 노출될 필요 없으면 `remember` 내부 사용.

```kotlin
// stateless (선호)
@Composable
fun PlayPauseButton(isPlaying: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) { ... }

// stateful은 명시적으로 
@Composable
fun rememberQueueListState(initialIndex: Int = 0): QueueListState { ... }
```

### 6.3 stable / immutable

- ViewModel state는 `data class` + `@Immutable` 또는 모든 필드 `val` + immutable 컬렉션.
- 컬렉션은 `kotlinx.collections.immutable`의 `ImmutableList<T>` 또는 `PersistentList<T>` 사용 (recomposition 최적화).
- `MutableList`, `ArrayList`를 state에 노출 금지.

### 6.4 remember

- 키 명시: `remember(key1, key2) { ... }`.
- 빈 키 `remember { }`는 영구 캐시 의도일 때만.
- `derivedStateOf`는 "다른 state로부터 파생"인 경우만 (단순 계산은 그냥 변수).

### 6.5 LazyColumn / LazyRow

- `items(list, key = { it.id }) { ... }` — `key` 필수.
- 항목 컴포저블은 별도 함수로 추출.
- `Modifier.animateItemPlacement()`는 측정 후 도입 (1만+ 항목에서 비쌈).

### 6.6 Modifier 체이닝

- 순서 중요: padding → background → border → clickable.
- Click을 padding 안쪽에 두려면 `clickable`을 padding 뒤에 (그래야 padding 영역도 터치).
- `Modifier.clickable`은 `onClickLabel` 함께:
  ```kotlin
  Modifier.clickable(
      onClickLabel = stringResource(R.string.player_action_play),
      onClick = onPlay,
  )
  ```

### 6.7 프리뷰

- `@PreviewLightDark` 사용 (라이트/다크 자동 생성).
- 추가로 글꼴 크기, RTL 등 별도 프리뷰는 필요 시.
- 프리뷰용 더미 데이터는 `internal val previewTrack: Track`처럼 같은 파일 또는 `core/ui/src/main/.../preview/` 패키지에.

```kotlin
@PreviewLightDark
@Composable
private fun TrackRowPreview() {
    StackTheme {
        TrackRow(track = previewTrack, isPlaying = true)
    }
}
```

### 6.8 effect 사용

- `LaunchedEffect(key)` — key 변경 시 재실행되는 비동기 작업.
- `DisposableEffect` — 리소스 등록/해제.
- `SideEffect` — 매 successful recomposition 후 (드물게).
- `produceState` — Flow를 State로 변환할 때 (또는 `collectAsStateWithLifecycle`).

### 6.9 이벤트 (Event/Effect) 소비

```kotlin
@Composable
fun LibraryScreen(viewModel: LibraryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is LibraryEvent.ScanComplete -> snackbar.showSnackbar(...)
                is LibraryEvent.Error -> snackbar.showSnackbar(...)
            }
        }
    }
    
    LibraryContent(state = state, onIntent = viewModel::onIntent)
}
```

---

## 7. Hilt

### 7.1 ViewModel

```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val observeTracks: ObserveTracksUseCase,
    private val play: PlayTrackUseCase,
) : BaseViewModel<LibraryState, LibraryIntent, LibraryEvent>(LibraryState.Loading) { ... }
```

### 7.2 Module / Binds vs Provides

- 인터페이스 → 구현 바인딩은 항상 `@Binds` (abstract function, Provides 아님).
- 외부 라이브러리 객체 생성은 `@Provides`.

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class TrackRepositoryModule {
    @Binds
    abstract fun bindTrackRepository(impl: TrackRepositoryImpl): TrackRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): StackDatabase = ...
}
```

### 7.3 Scope

- `@Singleton` — 정말 단일 인스턴스가 필요한 경우만 (DB, ExoPlayer, Repository).
- 미명시 — 매 요청마다 새 인스턴스 (UseCase 등 stateless).

### 7.4 Component 선택

- 99% 케이스: `@InstallIn(SingletonComponent::class)`.
- ViewModel 한정: `@InstallIn(ViewModelComponent::class)` + `@ViewModelScoped`.
- Service 한정: `@InstallIn(ServiceComponent::class)` + `@ServiceScoped`.

### 7.5 Assisted injection

ViewModel이 navigation argument를 생성자에 받아야 할 때:

```kotlin
@HiltViewModel(assistedFactory = AlbumDetailViewModel.Factory::class)
class AlbumDetailViewModel @AssistedInject constructor(
    @Assisted private val albumId: String,
    private val observeAlbum: ObserveAlbumUseCase,
) : BaseViewModel<...>(...) {
    @AssistedFactory
    interface Factory {
        fun create(albumId: String): AlbumDetailViewModel
    }
}
```

---

## 8. Room / Database

### 8.1 Entity

- 도메인 모델과 분리된 별도 클래스.
- Entity 클래스는 `internal` (모듈 외부 노출 X).
- 컬럼명은 `snake_case` (`@ColumnInfo(name = "...")` 명시).
- 모든 인덱스는 `@Entity(indices = [...])`로 명시.

```kotlin
@Entity(
    tableName = "tracks",
    indices = [
        Index("album_id"),
        Index("title", orders = [Index.Order.ASC]),
        Index("added_at", orders = [Index.Order.DESC]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = AlbumEntity::class,
            parentColumns = ["id"],
            childColumns = ["album_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
internal data class TrackEntity(
    @PrimaryKey val id: String,
    @ColumnInfo("media_uri") val mediaUri: String,
    @ColumnInfo("display_name") val displayName: String,
    ...
)
```

### 8.2 DAO

- 인터페이스로 정의 (Room이 구현 생성).
- 메서드 시그니처:
  - `Flow<List<T>>` — 변경을 관찰 (suspend X)
  - `suspend fun ...(): T` — 1회 read
  - `suspend fun insert/update/delete(...)`
- `@Query`는 `EXPLAIN QUERY PLAN`으로 검증.
- 복잡한 read는 `@Transaction` 사용 (M:N join).

```kotlin
@Dao
internal interface TrackDao {
    @Query("SELECT * FROM tracks WHERE is_present = 1 ORDER BY added_at DESC")
    fun observeAllPresent(): Flow<List<TrackEntity>>
    
    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getById(id: String): TrackEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tracks: List<TrackEntity>)
    
    @Query("DELETE FROM tracks WHERE id = :id")
    suspend fun delete(id: String)
}
```

### 8.3 Repository 구현

- DAO를 받아 도메인 모델로 변환.
- Mapper는 같은 모듈의 `data/mapper/` 패키지 또는 같은 파일의 확장 함수.

```kotlin
internal class TrackRepositoryImpl @Inject constructor(
    private val dao: TrackDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TrackRepository {
    override fun observeAll(sort: TrackSort): Flow<List<Track>> =
        dao.observeAllPresent()
            .map { entities -> entities.map(TrackEntity::toDomain) }
            .flowOn(ioDispatcher)
    
    override suspend fun get(id: String): Track? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }
}
```

### 8.4 Migration

- 모든 마이그레이션 명시. destructive 금지.
- `MIGRATION_X_Y` 객체는 `core/database/.../migration/Migrations.kt`에 모음.
- 모든 마이그레이션은 `MigrationTestHelper`로 테스트.

### 8.5 트랜잭션

- 여러 DAO 호출이 함께 성공/실패해야 하면 `@Transaction` (DAO 메서드 또는 `RoomDatabase.withTransaction { }`).

---

## 9. Audio (`:core:audio`)

### 9.1 ExoPlayer 호출

- 모든 호출은 메인 스레드.
- `:core:audio`의 `CommandDispatcher`를 통해서만 호출. 직접 호출 금지.
- `Player.Listener`는 메인 스레드에서 콜백 — 무거운 작업은 별도 dispatcher로.

### 9.2 Lifecycle

- `Player`는 `MediaSessionService.onCreate`에서 생성, `onDestroy`에서 `release()`.
- 앱 종료 시 누수 방지 위해 `Player.removeListener` 명시적 호출.

### 9.3 상태 노출

- `AudioEngine`은 `StateFlow<PlaybackState>`만 외부에 노출.
- `Player.Listener` → 내부 채널 → StateFlow 변환 패턴.

상세는 `THREADING_MODEL.md` §6.

---

## 10. 테스트

### 10.1 디렉토리

```
src/
  main/
  test/                  ← JVM 단위 테스트 (Robolectric 포함)
  androidTest/           ← 기기/에뮬레이터 (instrumentation, UI 테스트, Room migration)
```

### 10.2 명명

- 테스트 클래스: `{ProductionClass}Test`
- 테스트 메서드: `백틱으로 한국어 또는 자연어 영어`
  ```kotlin
  @Test
  fun `크로스페이드가 OFF일 때 다음 곡으로 즉시 전환된다`() { ... }
  
  @Test
  fun `잔여 시간이 페이드 길이보다 짧으면 잔여 시간만큼만 페이드한다`() { ... }
  ```

### 10.3 Given-When-Then

```kotlin
@Test
fun `플레이리스트에 트랙 추가 시 position이 끝에 들어간다`() = runTest {
    // Given
    val playlist = createPlaylist(name = "test")
    repository.addTracks(playlist.id, trackIds = listOf("a", "b"))
    
    // When
    repository.addTracks(playlist.id, trackIds = listOf("c"))
    
    // Then
    val tracks = repository.observeTracksOf(playlist.id).first()
    assertThat(tracks.map { it.id }).containsExactly("a", "b", "c").inOrder()
}
```

### 10.4 도구

- `JUnit5` — 단위 테스트 러너.
- `Truth` 또는 `kotlin.test` — assertion. (Truth가 메시지 가독성 우수.)
- `MockK` — mocking. (Mockito 금지.)
- `Turbine` — Flow 테스트.
- `kotlinx-coroutines-test` — `runTest`, `TestDispatcher`.
- `Robolectric` — Android API 필요 시 (Context 등).
- Compose UI Test — `createComposeRule()`.

### 10.5 Flow 테스트 (Turbine)

```kotlin
@Test
fun `정렬 변경 시 새 결과가 발행된다`() = runTest {
    repository.observeAll(TrackSort.TITLE_ASC).test {
        val first = awaitItem()
        // ... 정렬 변경 트리거
        val second = awaitItem()
        assertThat(second).isNotEqualTo(first)
        cancelAndIgnoreRemainingEvents()
    }
}
```

### 10.6 ViewModel 테스트

- TestDispatcher 주입.
- state Flow는 `viewModel.state.test { }` 또는 `value` 직접 검사.
- `viewModelScope`는 자동으로 `Main` dispatcher 사용 → 테스트에서 `Dispatchers.setMain(testDispatcher)`.

### 10.7 Compose UI 테스트

```kotlin
@get:Rule
val composeRule = createComposeRule()

@Test
fun `재생 버튼 탭 시 onPlay가 호출된다`() {
    var called = false
    composeRule.setContent {
        StackTheme { PlayPauseButton(isPlaying = false, onToggle = { called = true }) }
    }
    composeRule.onNodeWithContentDescription("재생").performClick()
    assertThat(called).isTrue()
}
```

semantics matcher는 `contentDescription` 또는 명시적 `testTag` 사용.

---

## 11. 주석 / KDoc

### 11.1 일반 주석

- "무엇"이 아니라 "왜"를 적습니다.
- 자명한 코드에 주석 금지.
- `TODO`, `FIXME`, `XXX` 금지 → `phases/NN-OPEN.md` 또는 GitHub Issue.

```kotlin
// bad
// 트랙 ID로 트랙을 가져온다
suspend fun getTrack(id: String): Track? = ...

// good
// 갭리스 재생 사전 조건: 두 트랙의 sampleRate와 channelCount가 같아야 함.
// (Media3 ConcatenatingMediaSource2의 제약)
private fun canPlayGapless(a: Track, b: Track): Boolean = ...
```

### 11.2 KDoc

- public API에만 작성.
- 내부 함수는 명명으로 충분히 설명되면 KDoc 생략.
- KDoc은 영어 또는 한국어 일관성 (모듈 단위로 결정).

```kotlin
/**
 * 두 트랙 사이의 equal-power crossfade를 수행한다.
 *
 * @param outgoing 페이드 아웃될 player. 호출 종료 후 `release()` 책임은 호출자.
 * @param incoming 페이드 인될 player. 함수 진입 시 이미 `prepare()` 상태.
 * @param durationMs 페이드 길이. outgoing의 잔여 시간보다 길 수 없음.
 * @return 페이드 완료 시 [Result.Success], 중도 취소 시 [Result.Cancelled].
 */
suspend fun fade(outgoing: Player, incoming: Player, durationMs: Long): Result
```

---

## 12. Git 커밋 메시지

### 12.1 형식 (Conventional Commits)

```
<type>(<scope>): <subject>

<body (optional)>

<footer (optional)>
```

### 12.2 type

| type | 의미 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조 개선 |
| `perf` | 성능 개선 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서만 변경 |
| `chore` | 빌드/설정/의존성 |
| `style` | 포맷팅, 세미콜론 등 동작 무관 |

### 12.3 scope

모듈 또는 도메인. 예: `audio`, `library`, `player`, `db`, `i18n`, `build`.

### 12.4 예시

```
feat(audio): 3-branch crossfade 구현

equal-power 곡선으로 50ms 주기 게인 갱신.
잔여 시간 < 페이드 길이 시 잔여만큼만 적용.

Refs: SSOT v5.0 §6.2.3, §6.2.4
Phase: 8c
```

```
fix(library): 1만 트랙 스크롤 시 jank 감소

LazyColumn items에 key 명시 누락 → 추가.
Modifier.animateItemPlacement 제거 (성능 우선).
```

### 12.5 브랜치

- `phase/NN-short-name` (예: `phase/08c-crossfade`)
- `fix/short-description`
- `refactor/short-description`

### 12.6 PR 본문 템플릿

```markdown
## 무엇
이 PR이 무엇을 하는가 (1~3 문장).

## 왜
근거. SSOT 또는 phase 문서 참조.

## 어떻게
주요 설계 결정과 trade-off.

## 검증
- [ ] `./gradlew checkAll` 통과
- [ ] `./gradlew :app:assembleDebug` 통과
- [ ] 수동 검증 체크리스트 (phase 문서 인용)
- [ ] (UI 변경 시) 라이트/다크 스크린샷 첨부

## 개방 항목
phases/NN-OPEN.md 참조 (있는 경우).
```

---

## 13. 정적 분석 도구 설정

### 13.1 ktlint

- 버전: 1.4.0+
- 설정: `.editorconfig`로 관리.
- pre-commit hook 권장.

`.editorconfig` 핵심:
```ini
[*.{kt,kts}]
indent_size = 4
max_line_length = 120
ktlint_standard_no-wildcard-imports = enabled
ktlint_standard_trailing-comma-on-call-site = enabled
ktlint_standard_trailing-comma-on-declaration-site = enabled
```

### 13.2 detekt

- 버전: 1.23.7+
- 설정: `config/detekt/detekt.yml`.
- 핵심 활성 규칙:
  - `MaxLineLength: 120`
  - `LongMethod: 60`
  - `LongParameterList: 6`
  - `ComplexCondition: 4`
  - `MagicNumber` (config로 제외 목록 관리)
  - `NoWildcardImports`
  - `ForbiddenComment` (TODO/FIXME 금지)

### 13.3 Android Lint

- `lint.xml`에 활성 규칙 + 커스텀 검사 등록.
- `checkOnly` 핵심: `MissingTranslation`, `HardcodedText`, `ContentDescription`, `IconMissingDensityFolder`.

### 13.4 커스텀 lint 규칙

- 모듈: `build-logic/lint/`
- 검사:
  - INTERNET 권한 사용 금지
  - println/Log.d 직접 호출 금지
  - hex color 직접 사용 금지 (디자인 토큰 외)
  - Dispatchers.Main 직접 사용 금지 (서비스 레이어에서)

---

## 14. 코드 리뷰 체크리스트

PR 머지 전 자동/수동 검증:

### 자동 (CI가 강제)
- [ ] `ktlintCheck` 통과
- [ ] `detekt` 통과
- [ ] `lintDebug` 통과
- [ ] `:checkModuleDependencies` 통과
- [ ] `testDebugUnitTest` 통과
- [ ] `:app:assembleDebug` 통과
- [ ] APK 매니페스트 INTERNET 미포함

### 수동 (리뷰어 확인)
- [ ] CLAUDE.md §1 절대 규칙 준수
- [ ] SSOT의 어느 §을 구현/수정했는가 PR 본문 명시
- [ ] 새 의존성이면 사용자 승인 받았는가
- [ ] 새 사용자 노출 문자열은 KO/EN/JA 모두 추가되었는가
- [ ] 새 권한 요청 없는가
- [ ] DAO 변경 시 인덱스 추가/마이그레이션 작성되었는가
- [ ] Compose 변경 시 라이트/다크 프리뷰 추가되었는가
- [ ] 접근성: contentDescription, 터치 타깃 48dp+
- [ ] 단위 테스트 추가/갱신되었는가
- [ ] KDoc 필요한 public API에 작성되었는가

---

*마지막 갱신: 2026-04-17 / SSOT v5.0 기준*
