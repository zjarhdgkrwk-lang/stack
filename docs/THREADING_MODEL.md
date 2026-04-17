# Stack — Threading Model

> 본 문서는 Stack 프로젝트의 스레드/코루틴/동시성 정책을 정의합니다.
> 음악 앱의 핵심 위험은 **오디오 글리치**(잘못된 스레딩으로 player 호출 실패), **ANR**(메인 스레드 차단), **race condition**(병행 명령으로 player 상태 손상)입니다. 본 문서가 이를 막습니다.

---

## 1. 원칙

1. **메인 스레드는 신성하다.** 16ms 이내 완료되지 않을 작업은 메인에서 절대 실행 금지.
2. **ExoPlayer는 메인 스레드 전용.** 모든 `Player` 호출은 `Dispatchers.Main`. 위반 시 `IllegalStateException`.
3. **Player 명령은 직렬화한다.** 동시 명령으로 인한 race를 막기 위해 모든 명령은 `CommandDispatcher`의 `Mutex`를 통과한다.
4. **scope는 항상 명시.** `GlobalScope` 절대 금지. 모든 `launch`는 명시된 lifecycle scope 안에서.
5. **취소는 협력적이다.** suspend 함수는 cancellable해야 하며, `CancellationException`을 절대 삼키지 않는다.

---

## 2. Dispatcher 정책

### 2.1 표준 디스패처

| 디스패처 | 용도 | 사용처 |
|---|---|---|
| `Dispatchers.Main` | UI 갱신, ExoPlayer 호출, MediaSession | ViewModel state 갱신, Composable callback, AudioEngine |
| `Dispatchers.Main.immediate` | 이미 메인 스레드면 즉시, 아니면 post | 메인 스레드 보장이 필요하나 즉시 실행 선호 |
| `Dispatchers.IO` | 파일 I/O, DB, 네트워크(없음) | Repository 구현, 라이브러리 스캔, 백업 |
| `Dispatchers.Default` | CPU 집약 (정렬, 파싱, 해싱) | LRC 파싱, FTS 인덱싱 워밍업, 큰 리스트 정렬 |
| `Dispatchers.Unconfined` | **사용 금지** | — |

### 2.2 Hilt qualifier

디스패처는 직접 import하지 않고 Hilt qualifier로 주입한다 (테스트에서 교체 가능):

```kotlin
// :core:common/src/main/kotlin/com/stack/player/core/common/di/Dispatchers.kt
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class DefaultDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class MainDispatcher
@Qualifier @Retention(AnnotationRetention.BINARY) annotation class MainImmediateDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO
    @Provides @DefaultDispatcher fun default(): CoroutineDispatcher = Dispatchers.Default
    @Provides @MainDispatcher fun main(): CoroutineDispatcher = Dispatchers.Main
    @Provides @MainImmediateDispatcher fun mainImmediate(): CoroutineDispatcher = Dispatchers.Main.immediate
}
```

사용 예:
```kotlin
class TrackRepositoryImpl @Inject constructor(
    private val dao: TrackDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : TrackRepository {
    override suspend fun get(id: String): Track? = withContext(ioDispatcher) {
        dao.getById(id)?.toDomain()
    }
}
```

### 2.3 디스패처 선택 기준

- **Room**: `suspend` DAO 메서드는 Room이 자동으로 `IO`로 디스패치 → 추가 `withContext` 불필요. 단, 결과 가공이 무겁다면 `withContext(default)`로 감싼다.
- **DataStore**: 자체 `Dispatchers.IO` 사용. 추가 디스패치 불필요.
- **파일 I/O (직접)**: 항상 `withContext(io)`.
- **JSON 파싱**: 작은 것은 호출자 컨텍스트, 큰 것(>100KB)은 `withContext(default)`.
- **이미지 디코딩**: Coil이 자체 처리. 직접 디코딩은 `withContext(default)`.
- **암호화 (백업)**: `withContext(default)`.

---

## 3. CoroutineScope 정책

### 3.1 허용된 scope

| Scope | 정의 | 사용처 |
|---|---|---|
| `viewModelScope` | ViewModel 생명주기. `Dispatchers.Main.immediate + SupervisorJob` | ViewModel 안의 모든 launch |
| `lifecycleScope` | LifecycleOwner 생명주기 | Composable 외부 (Activity, Fragment) |
| `rememberCoroutineScope()` | Composable 생명주기 | Composable 안의 사용자 액션 응답 |
| `serviceScope` | Service 생명주기. 직접 정의 | PlaybackService 내부 작업 |
| `applicationScope` | Application 생명주기 (앱 종료까지). 매우 제한적 사용 | 분리된 백업 작업, 통계 집계 |

### 3.2 PlaybackService scope 정의

```kotlin
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate
    )
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
```

### 3.3 applicationScope (제한적)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppScopeModule {
    @ApplicationScope
    @Singleton
    @Provides
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default
    )
}

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope
```

사용 사례:
- 사용자가 화면을 닫아도 완료해야 하는 백업 작업 (단, 앱 종료 시 중단)
- 비동기 통계 집계 (재생 종료 후 history → stats 갱신)

> ViewModel 작업은 절대 `applicationScope`에 넘기지 말 것. ViewModel은 `viewModelScope`에서.

### 3.4 GlobalScope

**절대 사용 금지.** detekt 규칙으로 검출.

---

## 4. Repository 레이어 스레딩

### 4.1 책임

- 외부 호출자 (ViewModel, UseCase)는 디스패처를 신경쓰지 않음.
- Repository 구현이 적절한 디스패처로 전환.

### 4.2 Flow 반환

```kotlin
override fun observeAll(): Flow<List<Track>> =
    dao.observeAllPresent()
        .map { entities -> entities.map(TrackEntity::toDomain) }
        .flowOn(ioDispatcher)  // 업스트림은 IO에서, 다운스트림(collector)은 호출자 컨텍스트
```

> `flowOn`은 **업스트림에만** 영향. 호출자가 `viewModelScope.launch { flow.collect { ... } }`하면 collect 블록은 메인에서 실행.

### 4.3 suspend 반환

```kotlin
override suspend fun get(id: String): Track? = withContext(ioDispatcher) {
    dao.getById(id)?.toDomain()
}
```

### 4.4 안티패턴

```kotlin
// bad — 호출자에게 디스패처 결정을 떠넘김
override suspend fun get(id: String): Track? = dao.getById(id)?.toDomain()
// (Room이 자동 IO로 가지만, mapper는 호출자 컨텍스트에서 실행)
```

---

## 5. ViewModel 스레딩

### 5.1 viewModelScope의 디폴트 디스패처

`viewModelScope`는 `Dispatchers.Main.immediate`를 사용. 즉, ViewModel의 모든 `launch { }`는 메인에서 시작.

### 5.2 패턴

```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val observeTracks: ObserveTracksUseCase,
    private val toggleStar: ToggleStarUseCase,
) : BaseViewModel<...>(...) {
    
    init {
        viewModelScope.launch {
            observeTracks(sort = TrackSort.TITLE_ASC).collect { tracks ->
                setState { copy(tracks = tracks.toImmutableList()) }
            }
        }
    }
    
    override fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.ToggleStar -> viewModelScope.launch {
                toggleStar(intent.trackId)
                // UI 갱신은 observeTracks Flow가 자동 발행
            }
        }
    }
}
```

### 5.3 무거운 계산

ViewModel에서 큰 리스트 정렬/필터:

```kotlin
private suspend fun applyFilter(all: List<Track>, filter: TrackFilter): List<Track> =
    withContext(defaultDispatcher) {
        all.filter { ... }.sortedBy { ... }
    }
```

`defaultDispatcher`는 Hilt로 주입.

---

## 6. Audio 모듈 스레딩 (특별 주의)

### 6.1 ExoPlayer는 메인 스레드 전용

```kotlin
// 항상 메인에서
exoPlayer.play()
exoPlayer.seekTo(positionMs)
exoPlayer.setMediaItems(items)
```

다른 스레드에서 호출 시:
```
java.lang.IllegalStateException: Player is accessed on the wrong thread.
```

### 6.2 CommandDispatcher

모든 `AudioEngine` 명령은 `CommandDispatcher`를 통해 직렬화.

```kotlin
// :core:audio/src/main/kotlin/com/stack/player/core/audio/playback/CommandDispatcher.kt
internal class CommandDispatcher @Inject constructor(
    @MainImmediateDispatcher private val mainDispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    
    /**
     * 명령을 직렬화하여 메인 스레드에서 실행한다.
     * 진행 중인 명령이 있으면 큐잉되어 순차 실행된다.
     */
    suspend fun <T> run(block: suspend CoroutineScope.() -> T): T = mutex.withLock {
        withContext(mainDispatcher) {
            block()
        }
    }
}
```

`AudioEngine` 메서드는 모두 다음 패턴:

```kotlin
override suspend fun next() = dispatcher.run {
    // 메인 스레드 + 직렬화 보장
    if (currentIndex < queue.lastIndex) {
        if (crossfadeMode == CrossfadeMode.ALWAYS) {
            startCrossfade(currentIndex + 1)
        } else {
            primaryPlayer.seekToNext()
        }
    }
}
```

### 6.3 왜 Mutex가 필요한가

CommandDispatcher 없이 메인 스레드만 사용해도 ExoPlayer 호출 자체는 안전합니다. 그러나 다음 시나리오가 발생합니다:

```
T=0ms  사용자: next()
T=10ms 사용자: next() 다시 (잘못 더블 탭)
T=20ms 자동: onPlaybackEnd → next()
```

각 명령이 비동기 작업을 포함 (e.g., 크로스페이드 코루틴 시작)하면, 세 호출이 메인 스레드에서 인터리빙되어:
- 두 개의 크로스페이드가 동시 실행됨
- queue index 갱신이 race
- player A/B 역할이 어긋남

Mutex로 명령 단위 직렬화 → 한 번에 한 명령만 진행, 나머지는 대기.

### 6.4 페이드 코루틴

페이드 자체는 명령 스코프 안에서 실행되지만 **취소 가능해야** 함:

```kotlin
private var activeFadeJob: Job? = null

private suspend fun startCrossfade(targetIndex: Int) {
    // 진행 중 페이드 취소
    activeFadeJob?.cancelAndJoin()
    
    activeFadeJob = serviceScope.launch {
        val outgoing = primary
        val incoming = secondary
        
        loadAndPrepare(incoming, queue[targetIndex])
        incoming.play()
        
        try {
            equalPowerFade(
                outgoing = outgoing,
                incoming = incoming,
                durationMs = effectiveFadeDurationMs(outgoing),
            )
        } catch (e: CancellationException) {
            // 취소 시 즉시 outgoing 정지 + incoming 풀 볼륨
            outgoing.stop()
            incoming.volume = 1f
            throw e
        }
        
        // 역할 스왑
        swapPlayerRoles()
        currentIndex = targetIndex
    }
}
```

### 6.5 Player.Listener

Listener 콜백은 메인 스레드에서 호출됨. 콜백 안에서 무거운 작업은 별도 스코프로:

```kotlin
private val listener = object : Player.Listener {
    override fun onPlaybackStateChanged(state: Int) {
        when (state) {
            Player.STATE_ENDED -> {
                serviceScope.launch {
                    // dispatcher.run으로 다음 곡 명령
                    dispatcher.run { handleTrackEnded() }
                }
            }
        }
    }
    
    override fun onMediaMetadataChanged(metadata: MediaMetadata) {
        // 가벼운 state 갱신만
        _state.update { it.copy(currentTitle = metadata.title?.toString()) }
    }
}
```

### 6.6 상태 노출

```kotlin
class AudioEngine @Inject constructor(...) {
    private val _state = MutableStateFlow(PlaybackState.Empty)
    val state: StateFlow<PlaybackState> = _state.asStateFlow()
    
    private val _queue = MutableStateFlow(QueueState.Empty)
    val queue: StateFlow<QueueState> = _queue.asStateFlow()
    
    // 위치는 별도 Flow (250ms 폴링)
    val position: Flow<Long> = flow {
        while (currentCoroutineContext().isActive) {
            emit(primaryPlayer.currentPosition)
            delay(POSITION_POLL_INTERVAL)
        }
    }.flowOn(mainDispatcher)
}
```

`position`은 매 250ms마다 폴링하므로 collect하지 않을 때는 비활성. `collectAsStateWithLifecycle()`이 lifecycle-aware 자동 stop.

### 6.7 A-B 반복 폴링

```kotlin
private var abLoopJob: Job? = null

fun setAbLoop(start: Long?, end: Long?) {
    abLoopJob?.cancel()
    if (start == null || end == null || end <= start) return
    
    abLoopJob = serviceScope.launch {
        while (isActive) {
            delay(AB_POLL_INTERVAL)  // 250ms
            withContext(mainDispatcher) {
                if (primaryPlayer.currentPosition >= end) {
                    primaryPlayer.seekTo(start)
                }
            }
        }
    }
}
```

곡 변경 또는 사용자 해제 시 `abLoopJob?.cancel()`.

---

## 7. PlaybackService 스레딩

### 7.1 onCreate / onDestroy

```kotlin
@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    
    @Inject lateinit var audioEngine: AudioEngine
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    
    override fun onCreate() {
        super.onCreate()
        audioEngine.initialize(applicationContext)
        mediaSession = audioEngine.createMediaSession(this)
    }
    
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession
    
    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        audioEngine.release()
        serviceScope.cancel()
        super.onDestroy()
    }
}
```

### 7.2 BroadcastReceiver

`ACTION_AUDIO_BECOMING_NOISY` 등 외부 이벤트 수신:

```kotlin
private val noisyReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // 메인 스레드에서 호출됨. 빠른 응답.
        serviceScope.launch {
            audioEngine.pauseFromExternal(reason = ExternalPauseReason.HEADSET_UNPLUGGED)
        }
    }
}
```

---

## 8. Database 스레딩

### 8.1 Room 자동 디스패치

`suspend fun` DAO 메서드와 `Flow` 반환 메서드는 Room이 자동으로 IO로 처리. 추가 `withContext` 불필요.

### 8.2 트랜잭션

```kotlin
@Dao
interface PlaylistDao {
    @Transaction
    suspend fun reorder(playlistId: String, fromPos: Int, toPos: Int) {
        // 내부 SQL은 같은 트랜잭션
        moveDown(playlistId, fromPos, toPos)
        moveUp(playlistId, fromPos, toPos)
    }
}
```

여러 DAO를 묶어야 하면:

```kotlin
suspend fun importPlaylist(playlist: PlaylistEntity, tracks: List<TrackRef>) = 
    db.withTransaction {
        playlistDao.insert(playlist)
        playlistTrackDao.insertAll(tracks.toEntities(playlist.id))
        statsDao.bumpPlaylistCount()
    }
```

### 8.3 long-running query

대량 read는 `PagingSource` 사용 (Paging 3). v1.0은 1만 트랙까지 일괄 로드 허용 (LazyColumn 가상화로 충분), 그 이상은 페이징.

---

## 9. Mutex 사용 정책

### 9.1 일반 규칙

- **Mutex는 짧게.** lock 보유 시간을 최소화.
- **lock 보유 중 외부 suspend 함수 호출 금지** (deadlock 위험). 
- 단, `CommandDispatcher`는 예외 — 명령 단위 직렬화가 의도.

### 9.2 사용 사례

| 사용처 | 이유 |
|---|---|
| `CommandDispatcher` | Player 명령 직렬화 |
| 큐 순서 변경 (in-memory) | 동시 reorder 방지 |
| 백업 파일 쓰기 | 동시 export 방지 |

### 9.3 사용 금지

- StateFlow 갱신은 Mutex 불필요 (`update { }` 사용).
- DB 트랜잭션은 Mutex 불필요 (`withTransaction` 사용).

---

## 10. Cancellation

### 10.1 cooperative cancellation

suspend 함수는 cancellable해야 함. long-running 루프:

```kotlin
suspend fun scanLibrary(folder: Uri) {
    val files = listFiles(folder)
    files.forEach { file ->
        currentCoroutineContext().ensureActive()  // ← 매 반복 검사
        processFile(file)
    }
}
```

또는 자연 cancellation point 활용 (`delay`, `withContext`, suspend 함수 호출 자체).

### 10.2 CancellationException

**절대 catch해서 삼키지 말 것.** 코루틴 cancellation 메커니즘 파괴.

```kotlin
// bad
try {
    operation()
} catch (e: Exception) {
    log(e)  // ← CancellationException도 잡힘 → cancel 무시됨
}

// good
try {
    operation()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    log(e)
}

// or
try {
    operation()
} catch (e: Exception) {
    currentCoroutineContext().ensureActive()  // cancelled면 throw
    log(e)
}
```

### 10.3 cleanup

cancellation 후 정리가 필요하면 `try-finally` 또는 `NonCancellable`:

```kotlin
suspend fun playWithCleanup() {
    try {
        play()
    } finally {
        withContext(NonCancellable) {
            // cancellation 중에도 실행되어야 하는 정리
            persistQueueState()
        }
    }
}
```

---

## 11. 에러 전파

### 11.1 SupervisorJob

부모 scope가 자식의 실패로 함께 죽지 않게 하려면 `SupervisorJob` 사용.

- `viewModelScope` — 이미 SupervisorJob 사용
- `serviceScope` — 위 §3.2에서 SupervisorJob 사용
- 자체 scope는 `CoroutineScope(SupervisorJob() + dispatcher)`

### 11.2 CoroutineExceptionHandler

uncaught exception 처리:

```kotlin
private val errorHandler = CoroutineExceptionHandler { _, throwable ->
    if (throwable is CancellationException) return@CoroutineExceptionHandler
    logger.error("Uncaught in serviceScope", throwable)
}

private val serviceScope = CoroutineScope(
    SupervisorJob() + Dispatchers.Main.immediate + errorHandler
)
```

### 11.3 사용자 노출 에러

Repository/UseCase는 도메인 sealed result로 에러를 표현:

```kotlin
sealed interface PlayResult {
    data object Success : PlayResult
    data class FileNotFound(val trackId: String) : PlayResult
    data class UnsupportedFormat(val mime: String) : PlayResult
    data class Unknown(val cause: Throwable) : PlayResult
}
```

ViewModel은 결과를 상태/이벤트로 변환하여 UI에 전달.

---

## 12. Flow 가이드

### 12.1 cold vs hot

- `Flow { }` (cold) — 매 collect마다 생산자 재실행
- `StateFlow` / `SharedFlow` (hot) — 단일 생산자, 여러 collector 공유

### 12.2 conflate / buffer

- 생산자가 빠르고 collector가 느릴 때: `conflate()` (중간값 drop) 또는 `buffer(N)`.
- StateFlow는 자동 conflation (동등성 비교).

### 12.3 sharing

여러 화면이 같은 Repository Flow를 collect하면 매번 새 DB 쿼리. 공유:

```kotlin
val playbackState: StateFlow<PlaybackState> = audioEngine.state
    .stateIn(
        scope = applicationScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PlaybackState.Empty,
    )
```

`WhileSubscribed(5_000)` — 마지막 collector가 떠난 후 5초 유지 후 정지 (configuration change 동안 재구독 비용 회피).

### 12.4 combine

```kotlin
val uiState: StateFlow<LibraryState> = combine(
    tracksFlow,
    sortFlow,
    filterFlow,
) { tracks, sort, filter ->
    LibraryState(
        tracks = tracks.applyFilter(filter).sortedBy(sort).toImmutableList(),
        sort = sort,
        filter = filter,
    )
}.flowOn(defaultDispatcher)
 .stateIn(viewModelScope, SharingStarted.Lazily, LibraryState.Loading)
```

---

## 13. 테스트

### 13.1 TestDispatcher

```kotlin
@Test
fun `별표 토글 시 상태가 갱신된다`() = runTest {
    val testDispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(testDispatcher)
    
    val viewModel = LibraryViewModel(...)
    viewModel.onIntent(LibraryIntent.ToggleStar("track-1"))
    advanceUntilIdle()
    
    assertThat(viewModel.state.value.tracks.first { it.id == "track-1" }.isStarred).isTrue()
    
    Dispatchers.resetMain()
}
```

### 13.2 가상 시간

`runTest`는 가상 시간을 사용 → `delay(1.hours)`도 즉시 진행.

### 13.3 ExoPlayer 테스트

ExoPlayer는 단위 테스트가 어려움. 다음 전략:
- `AudioEngine`의 인터페이스 추출 → ViewModel 테스트는 mock으로
- `AudioEngine` 자체 테스트는 `androidTest` (instrumentation)에서 실제 player로

### 13.4 Robolectric에서 메인 스레드

Robolectric은 `Dispatchers.Main`을 자동 매핑. `Dispatchers.setMain` 불필요한 경우가 있음 — 테스트별 확인.

---

## 14. 안티패턴 모음

| 패턴 | 대신 |
|---|---|
| `GlobalScope.launch { }` | 적절한 lifecycle scope |
| `runBlocking { }` 프로덕션 코드 | suspend 또는 callback |
| `Thread { }.start()` | `viewModelScope.launch(io) { }` |
| `try { ... } catch (e: Exception) { /* swallow */ }` | rethrow CancellationException, log others |
| Flow를 `.collect`로 두 번 → 두 collector | `.shareIn` 또는 `.stateIn` |
| `Flow<T>`에 `runBlocking { flow.first() }` | `viewModelScope.launch { val v = flow.first(); ... }` |
| ExoPlayer를 IO 디스패처에서 호출 | 항상 `CommandDispatcher.run { }` |
| Player.Listener에서 무거운 작업 | `serviceScope.launch { }` 로 위임 |
| ViewModel에서 `Dispatchers.Main` 직접 명시 | viewModelScope가 이미 Main, 추가 명시 불필요 |
| StateFlow를 Mutex로 감싸기 | `.update { }` 사용 |
| `Job.join()`으로 결과 대기 | `Deferred.await()` 또는 suspend 호출 |

---

## 15. 빠른 참조: "어떤 스코프에서 어떤 디스패처?"

| 작업 | scope | dispatcher |
|---|---|---|
| ViewModel state 갱신 | `viewModelScope` | (Main, 자동) |
| ViewModel에서 DB 호출 | `viewModelScope` | (UseCase가 IO로 전환) |
| ViewModel에서 큰 정렬/필터 | `viewModelScope` | `withContext(default)` |
| Composable에서 사용자 액션 응답 | `rememberCoroutineScope()` | (Main, 자동) |
| Composable에서 Flow 수집 | (LaunchedEffect) | (Main, 자동) |
| Service onStartCommand 처리 | `serviceScope` | (Main, 자동) |
| Service에서 외부 이벤트 응답 | `serviceScope` | (Main, 자동) |
| AudioEngine 명령 | (호출자 scope) | `CommandDispatcher.run { }` |
| Repository read (suspend) | (호출자 scope) | `withContext(io)` |
| Repository read (Flow) | (호출자 scope) | `.flowOn(io)` |
| 라이브러리 스캔 | WorkManager | (Worker가 자동 처리) |
| 백업 export | `applicationScope` 또는 WorkManager | `withContext(default)` |
| 통계 집계 (재생 종료 후) | `applicationScope` | `withContext(default)` |

---

*마지막 갱신: 2026-04-17 / SSOT v5.0 §6 기준*
