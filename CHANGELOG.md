# Changelog

본 파일은 [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) 형식을 따르며,
프로젝트는 [Semantic Versioning](https://semver.org/lang/ko/)을 준수합니다.

분류 카테고리:
- **Added** — 새 기능
- **Changed** — 기존 기능 변경
- **Deprecated** — 곧 제거될 기능
- **Removed** — 제거된 기능
- **Fixed** — 버그 수정
- **Security** — 보안 관련 변경
- **Performance** — 성능 개선

---

## [Unreleased]

### Added
- (예정) v1.0의 모든 기능 — Phase 0~14 진행 중

### Documentation
- SSOT v5.0 작성 (Stack 기술 명세서)
- CLAUDE.md 운영 매뉴얼
- STYLE_GUIDE.md, THREADING_MODEL.md, BUILD.md, DESIGN_TOKENS.md

---

## v1.0 출시 시 예정 항목 (참고)

본 섹션은 v1.0 출시 시점에 [Unreleased]에서 [1.0.0]으로 이동하여 정리합니다.

### Added (예정)
- 로컬 음원 라이브러리 자동 스캔 (MediaStore + 사용자 폴더)
- 트랙/앨범/아티스트/태그 4탭 라이브러리 브라우징
- 사용자 정의 태그 (색상 9종)
- 통합 검색 (FTS4, 한국어/일본어/영어 부분 일치)
- 재생 컨트롤 (재생/정지/다음/이전/시킹/큐 관리)
- 셔플, 반복 (off/all/one)
- 크로스페이드 (off / auto-only / always, 1~12초)
- 갭리스 재생
- A-B 구간 반복
- 10밴드 이퀄라이저 (12개 프리셋 + Custom)
- ReplayGain 지원 (track / album)
- 동기화 가사 표시 (LRC 포맷)
- 가사 에디터 (시간 마커 편집, 오프셋 슬라이더, 사이드카 .lrc 내보내기)
- 플레이리스트 CRUD (드래그 재정렬, M3U 가져오기/내보내기)
- 슬립 타이머 (5/15/30/45/60/90분 + Custom, "현재 곡 끝까지" 옵션)
- 백업/복원 (ZIP 단일 파일, 병합/덮어쓰기 모드)
- 잠금화면/알림 미디어 컨트롤 (MediaSession)
- 헤드폰 분리 / 블루투스 끊김 자동 일시정지
- 다크/라이트 테마 (자동/수동)
- 액센트 색 (8종 팔레트)
- KO/EN/JA 3개 언어
- 지원 포맷: MP3, AAC, ALAC, FLAC, Vorbis, Opus, WAV

### Architecture
- 13 Gradle 모듈 (`:app`, 7 core, 5 feature)
- Kotlin 2.0 (K2 컴파일러)
- Jetpack Compose UI
- Hilt DI
- Room 2.7+ (13 테이블 + FTS4)
- Media3 ExoPlayer 1.4+ (Dual player + CommandDispatcher Mutex)
- DataStore Preferences
- Coil 3 이미지 캐시

### Privacy
- INTERNET 권한 미선언 (네트워크 코드 없음)
- 분석/광고/크래시 SDK 없음
- 모든 데이터 로컬 저장

### Performance Targets
- 콜드 스타트 < 350ms (저사양 기기)
- 1만 트랙 라이브러리 < 280MB RSS
- 1시간 재생 시 배터리 < 5%
- 스크롤 jank < 0.1%

---

## 변경 로그 작성 규칙

### 작성 시점

- 매 PR 머지 시 [Unreleased] 섹션에 항목 추가.
- 릴리스 시 [Unreleased] → [X.Y.Z]로 이동, 버전과 날짜 명시.

### 항목 형식

```
### Added
- {기능 한 줄 설명} ({Phase 또는 Issue 참조})
```

예:
```
### Added
- 3-branch crossfade with equal-power curve (Phase 8c, #42)
- A-B repeat with 250ms polling (Phase 8d, #45)

### Fixed
- LazyColumn jank with 10K+ tracks due to missing key (#51)
```

### 사용자 관점 우선

내부 리팩터링은 일반적으로 changelog에 포함하지 않습니다. 단, 다음은 예외:
- API 변경 (모듈 외부 노출 변경)
- 빌드 명령 변경
- 의존성 메이저 버전 변경
- 성능에 사용자가 체감할 영향이 있는 변경

### 언어

- 한국어 또는 영어 일관성 유지 (혼용 금지).
- 기술 용어는 영어 그대로 사용 (e.g., crossfade, gapless).
- 모듈/Phase 참조는 표기 통일: `(Phase 8c)`, `(:core:audio)`, `(#42)`.

### 버전 부여

- **MAJOR** (X.0.0): 비호환 변경, 데이터 마이그레이션 필요
- **MINOR** (X.Y.0): 기능 추가, 호환 유지
- **PATCH** (X.Y.Z): 버그 수정, 호환 유지

v1.0 출시 전에는 v0.x.y 사용 가능 (단, 모든 v0.x는 internal/beta).

---

*마지막 갱신: 2026-04-17*
