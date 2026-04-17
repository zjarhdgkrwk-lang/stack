# Stack — Design Tokens

> 본 문서는 Stack의 디자인 토큰 카탈로그입니다.
> Composable에서 hex/dp/sp 리터럴을 직접 쓰지 않고, 본 문서의 토큰을 통해 일관된 표현을 유지합니다.
> 코드 위치: `:core:design` 모듈.

---

## 1. 토큰 시스템 원칙

1. **모든 시각 값은 토큰으로.** Composable에서 `Color(0xFF...)`, `4.dp`, `16.sp` 직접 사용 금지.
2. **의미 기반 명명.** `colorBlue700` 대신 `text.primary`, `surface.elevated` 같이 역할로 명명.
3. **테마 인지.** 라이트/다크 양쪽에 정의된 값만 토큰. 한쪽만 있는 값은 토큰화하지 않음.
4. **확장보다 활용.** 새 토큰 도입 전, 기존 토큰의 조합으로 가능한지 먼저 검토.
5. **변경 = SSOT 변경.** 토큰 값을 바꾸려면 본 문서를 갱신하고 사용처 영향을 확인.

---

## 2. 색

### 2.1 Surface (표면)

| 토큰 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `surface.background` | `#FAFAF7` | `#161514` | 화면 기본 배경 |
| `surface.elevated` | `#FFFFFF` | `#1F1E1C` | 카드, 시트, 다이얼로그 |
| `surface.muted` | `#F2F1ED` | `#2A2926` | 입력 필드, 비활성 영역, 코드 블록 |
| `surface.inverse` | `#1F1E1C` | `#FAFAF7` | 토스트, 강조 영역 |

### 2.2 Divider / Border

| 토큰 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `divider.default` | `#E7E5DF` | `#2F2E2B` | 목록 구분선, 섹션 구분 |
| `divider.strong` | `#D4D1C8` | `#3A3936` | 강조 구분선 |
| `border.default` | `#E7E5DF` | `#2F2E2B` | 카드 테두리, 입력 테두리 |
| `border.focus` | `accent.default` | `accent.default` | 포커스 상태 |

### 2.3 Text

| 토큰 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `text.primary` | `#1F1E1B` | `#EDEAE3` | 본문, 제목 |
| `text.secondary` | `#6B6962` | `#A8A49B` | 보조 정보, 메타 |
| `text.tertiary` | `#9A968D` | `#75726B` | 캡션, 플레이스홀더 |
| `text.disabled` | `#C9C5BC` | `#4A4845` | 비활성 텍스트 |
| `text.inverse` | `#FAFAF7` | `#1F1E1B` | 인버스 표면 위 텍스트 |
| `text.accent` | `accent.default` | `accent.default` | 액센트 색 텍스트 (링크, 강조) |
| `text.danger` | `#B14A3A` | `#D67566` | 파괴적 액션 |

### 2.4 Icon

| 토큰 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `icon.primary` | `text.primary` | `text.primary` | 기본 아이콘 |
| `icon.secondary` | `text.secondary` | `text.secondary` | 보조 아이콘 |
| `icon.disabled` | `text.disabled` | `text.disabled` | 비활성 아이콘 |
| `icon.accent` | `accent.default` | `accent.default` | 액센트 아이콘 |

### 2.5 State

| 토큰 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `state.hover` | `rgba(0,0,0,0.04)` | `rgba(255,255,255,0.06)` | hover overlay |
| `state.pressed` | `rgba(0,0,0,0.08)` | `rgba(255,255,255,0.10)` | press overlay |
| `state.selected` | `accent.muted` | `accent.muted` | 선택 행 배경 |
| `state.focus` | `border.focus` (3dp ring) | 동상 | 포커스 링 |

### 2.6 Semantic

| 토큰 | 라이트 | 다크 | 용도 |
|---|---|---|---|
| `success.default` | `#4F7A5C` | `#7BA88A` | 성공 표시 (백업 완료 등) |
| `warning.default` | `#A8804A` | `#D6A878` | 경고 (시스템 EQ 충돌 등) |
| `danger.default` | `#B14A3A` | `#D67566` | 파괴 액션, 에러 |
| `info.default` | `#4A6F95` | `#7BA7D9` | 정보 안내 |

---

## 3. Accent 팔레트 (사용자 선택 8색)

사용자가 설정에서 액센트를 선택하면 `accent.default`가 다음 중 하나로 매핑됩니다.

| # | 이름 | 라이트 | 다크 |
|---|---|---|---|
| 0 | Slate Blue (기본) | `#2B6CB0` | `#7BA7D9` |
| 1 | Slate | `#5C6770` | `#8B96A0` |
| 2 | Stone | `#6B6962` | `#9A968D` |
| 3 | Sage | `#76876B` | `#A0B095` |
| 4 | Moss | `#4F6E50` | `#7CA17F` |
| 5 | Plum | `#8E5572` | `#B8859E` |
| 6 | Sand | `#B19063` | `#D4B58E` |
| 7 | Clay | `#A95740` | `#D08773` |

`accent.muted`는 `accent.default`에 흰색 90%(라이트) / 검정색 80%(다크) 혼합. 선택 행 배경, 약한 강조에 사용.

```kotlin
// :core:design/.../Accent.kt
internal fun Color.muted(isDark: Boolean): Color = if (isDark) {
    blend(Color.Black, ratio = 0.80f)
} else {
    blend(Color.White, ratio = 0.90f)
}
```

---

## 4. 타이포그래피

### 4.1 폰트 패밀리

| 패밀리 | 사용 |
|---|---|
| `Pretendard Variable` | 기본 sans (KO/EN/JA 모두) |
| `Noto Serif KR / JP` | 가사 serif 옵션 |
| `JetBrains Mono` | 메타 정보 (비트레이트, 시간 등) |

폰트 파일은 `app/src/main/assets/fonts/`에 임베디드.

```kotlin
// :core:design/.../Typography.kt
internal val Pretendard = FontFamily(
    Font("fonts/Pretendard-Regular.ttf", FontWeight.Normal),
    Font("fonts/Pretendard-Medium.ttf", FontWeight.Medium),
    Font("fonts/Pretendard-SemiBold.ttf", FontWeight.SemiBold),
    Font("fonts/Pretendard-Bold.ttf", FontWeight.Bold),
)
```

### 4.2 텍스트 스타일

| 토큰 | 폰트 | 크기 | 굵기 | 행간 | 자간 | 용도 |
|---|---|---|---|---|---|---|
| `display` | Pretendard | 28sp | 600 | 33.6sp (1.2) | -0.28sp | 화면 제목 (라이브러리, 설정) |
| `title` | Pretendard | 20sp | 600 | 26sp (1.3) | -0.10sp | 섹션 제목, 시트 제목 |
| `subtitle` | Pretendard | 17sp | 600 | 23.8sp (1.4) | 0 | 카드 제목 |
| `body` | Pretendard | 15sp | 400 | 22.5sp (1.5) | 0 | 본문, 트랙 제목 |
| `bodyStrong` | Pretendard | 15sp | 600 | 22.5sp (1.5) | 0 | 강조 본문 |
| `caption` | Pretendard | 12sp | 400 | 16.8sp (1.4) | +0.12sp | 보조 정보, 메타 |
| `captionStrong` | Pretendard | 12sp | 600 | 16.8sp (1.4) | +0.12sp | 강조 캡션 (탭 라벨 등) |
| `mono` | JetBrains Mono | 12sp | 400 | 15.6sp (1.3) | 0 | 비트레이트, 시간, 코덱 |
| `lyric` | Pretendard / Noto Serif | 가변 (16/19/22/26) | 400 | 가변 (1.7) | 0 | 가사 (사용자 설정 글꼴/크기) |

### 4.3 가사 크기 단계

| 설정 | sp |
|---|---|
| Small | 16 |
| Medium (기본) | 19 |
| Large | 22 |
| XLarge | 26 |

### 4.4 사용 예

```kotlin
Text(
    text = track.title,
    style = StackTheme.typography.body,
    color = StackTheme.colors.text.primary,
)

Text(
    text = "${track.format.codec} · ${track.bitrate / 1000} kbps",
    style = StackTheme.typography.mono,
    color = StackTheme.colors.text.tertiary,
)
```

---

## 5. 간격 (Spacing)

8 단계 스케일. 4dp 베이스의 1, 2, 3, 4, 6, 8 배수.

| 토큰 | 값 | 일반 용도 |
|---|---|---|
| `space.xxs` | 2dp | 텍스트 라인 사이, 매우 좁은 갭 |
| `space.xs` | 4dp | 아이콘과 라벨 사이, 칩 내부 패딩 |
| `space.s` | 8dp | 컴포넌트 내부 패딩, 작은 갭 |
| `space.m` | 12dp | 목록 항목 수직 패딩, 표준 갭 |
| `space.l` | 16dp | 화면 좌우 패딩, 카드 내부 패딩 |
| `space.xl` | 24dp | 섹션 사이 갭, 큰 영역 패딩 |
| `space.xxl` | 32dp | 화면 상단 여백, 큰 섹션 분리 |
| `space.xxxl` | 48dp | 매우 큰 분리 (Empty 상태 등) |

### 5.1 사용 규칙

- 화면 좌우 가로 패딩: 항상 `space.l` (16dp).
- LazyColumn item의 vertical padding: `space.m` (12dp).
- TopBar 높이: 56dp (Material 표준).
- BottomBar 높이: 56dp + 시스템 인셋.
- MiniPlayer 높이: 64dp.

### 5.2 코드

```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = StackTheme.spacing.l),
    verticalArrangement = Arrangement.spacedBy(StackTheme.spacing.m),
) { ... }
```

---

## 6. 모서리 (Corner radius)

| 토큰 | 값 | 용도 |
|---|---|---|
| `radius.none` | 0dp | 평면 영역 |
| `radius.s` | 4dp | 목록 행, 작은 칩, 입력 필드 |
| `radius.m` | 8dp | 버튼, 중간 카드 |
| `radius.l` | 12dp | 시트, 다이얼로그, 큰 카드 |
| `radius.xl` | 16dp | 앨범 아트 (작은), 큰 시트 |
| `radius.xxl` | 24dp | 앨범 아트 (큰), Now Playing 아트 |
| `radius.full` | 100% | 둥근 버튼 (FAB, 원형 아이콘) |

### 6.1 일관성 규칙

- 같은 화면의 같은 위계 컴포넌트는 같은 radius.
- 트랙 아트 (목록): `radius.s` (4dp)
- 앨범 카드: `radius.m` (8dp)
- Now Playing 아트: `radius.xxl` (24dp)

```kotlin
Image(
    modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(StackTheme.radius.s)),
    ...
)
```

---

## 7. 모션 (Motion)

### 7.1 지속시간

| 토큰 | 값 | 용도 |
|---|---|---|
| `motion.fast` | 150ms | 토글, 버튼 상태, 호버 |
| `motion.standard` | 250ms | 기본 화면 전환, 시트 등장 |
| `motion.slow` | 400ms | 풀스크린 전환, 큰 영역 변화 |
| `motion.crossfade` | 300ms | 콘텐츠 fade 교체 (가사 ↔ 아트) |

### 7.2 이즈

| 토큰 | 값 | 용도 |
|---|---|---|
| `easing.standard` | `CubicBezier(0.4, 0.0, 0.2, 1.0)` | 기본 (Material standard) |
| `easing.emphasized` | `CubicBezier(0.2, 0.0, 0.0, 1.0)` | 진입 강조 |
| `easing.linear` | `LinearEasing` | 진행 바, 가사 자동 스크롤 |

### 7.3 모션 원칙

- **의미가 있을 때만 모션.** 장식적 모션 금지.
- 스프링 바운스, 회전, 패럴럭스 사용 금지 (Notion 미학).
- 200~400ms 사이의 단순 fade/slide만 사용.
- 사용자가 시스템에서 "애니메이션 줄이기"를 켜면 모션 길이를 0으로 (접근성).

```kotlin
val transition = updateTransition(targetState = isPlaying, label = "playPauseTransition")
val iconScale by transition.animateFloat(
    transitionSpec = { tween(StackTheme.motion.fast.toInt(), easing = StackTheme.easing.standard) },
    label = "iconScale",
) { if (it) 1.0f else 0.95f }
```

---

## 8. 그림자 / Elevation

Stack은 그림자를 거의 사용하지 않음 (Notion + Soft-archiving 미학).

| 토큰 | 값 | 용도 |
|---|---|---|
| `elevation.flat` | 0dp | 거의 모든 영역 |
| `elevation.raised` | 1dp (subtle) | MiniPlayer (배경과 분리), 시트 헤더 |
| `elevation.overlay` | 4dp (modal) | Modal sheet, dialog |

분리는 그림자보다 **divider**와 **surface 색 변화**로 표현.

```kotlin
// 권장
Surface(
    color = StackTheme.colors.surface.elevated,
    border = BorderStroke(1.dp, StackTheme.colors.divider.default),
) { ... }

// 비권장 (Material 기본 elevation 강조)
Card(elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) { ... }
```

---

## 9. 컴포넌트별 토큰 매핑

### 9.1 TrackRow (목록 한 행)

| 속성 | 토큰 |
|---|---|
| 높이 | 64dp (고정) |
| 가로 패딩 | `space.l` (16dp) |
| 세로 패딩 | `space.m` (12dp) |
| 아트 사이즈 | 48dp |
| 아트 radius | `radius.s` (4dp) |
| 아트와 텍스트 갭 | `space.m` (12dp) |
| 제목 스타일 | `body` |
| 제목 색 | `text.primary` (재생 중이면 `text.accent`) |
| 부제목 (아티스트) 스타일 | `caption` |
| 부제목 색 | `text.secondary` |
| 우측 메타 (duration) 스타일 | `mono` |
| 우측 메타 색 | `text.tertiary` |
| 행간 divider | `divider.default`, 1dp |
| 선택 배경 | `state.selected` |

### 9.2 MiniPlayer

| 속성 | 토큰 |
|---|---|
| 높이 | 64dp |
| 배경 | `surface.elevated` |
| 상단 divider | `divider.default`, 1dp |
| 가로 패딩 | `space.l` (16dp) |
| 아트 사이즈 | 40dp, `radius.s` |
| 진행 바 | 2dp 두께, accent 채우기, `surface.muted` 트랙 |

### 9.3 Bottom Sheet

| 속성 | 토큰 |
|---|---|
| 배경 | `surface.elevated` |
| 모서리 | 상단 `radius.l` (12dp) |
| 핸들 | 36dp 너비, 4dp 높이, `surface.muted` |
| 내부 패딩 | `space.l` (16dp) |
| 헤더 텍스트 | `title` |

### 9.4 Empty State

| 속성 | 토큰 |
|---|---|
| 일러스트 영역 | 96dp (가운데) |
| 제목 스타일 | `subtitle` |
| 설명 스타일 | `body`, `text.secondary` |
| 액션 버튼 | accent 채우기, `radius.m` |

### 9.5 Tag Chip

| 속성 | 토큰 |
|---|---|
| 높이 | 28dp |
| 가로 패딩 | `space.s` (8dp) |
| 모서리 | `radius.s` (4dp) |
| 배경 | tag color에 흰 90% 혼합 (라이트) / 검정 80% (다크) |
| 텍스트 색 | tag color (라이트) / tag color에 흰 60% (다크) |
| 색 도트 사이즈 | 8dp |

---

## 10. 코드 구조

### 10.1 패키지

```
core/design/src/main/kotlin/com/stack/player/core/design/
├── theme/
│   ├── StackTheme.kt          # 진입점 (CompositionLocal 제공)
│   ├── Colors.kt              # 색 토큰 정의
│   ├── Typography.kt          # 텍스트 스타일
│   ├── Spacing.kt             # 간격 토큰
│   ├── Shapes.kt              # 모서리 토큰
│   └── Motion.kt              # 모션 토큰
├── accent/
│   └── AccentPalette.kt       # 8색 팔레트
└── tokens/
    └── DesignTokens.kt        # 모든 토큰 통합 인터페이스
```

### 10.2 진입점

```kotlin
// StackTheme.kt
@Composable
fun StackTheme(
    accent: AccentPalette = AccentPalette.SlateBlue,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = remember(accent, darkTheme) {
        if (darkTheme) darkColors(accent) else lightColors(accent)
    }
    val typography = StackTypography
    val spacing = StackSpacing
    val radius = StackRadius
    val motion = StackMotion
    
    CompositionLocalProvider(
        LocalStackColors provides colors,
        LocalStackTypography provides typography,
        LocalStackSpacing provides spacing,
        LocalStackRadius provides radius,
        LocalStackMotion provides motion,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
        ) {
            content()
        }
    }
}

object StackTheme {
    val colors: StackColors
        @Composable @ReadOnlyComposable
        get() = LocalStackColors.current
    
    val typography: StackTypography
        @Composable @ReadOnlyComposable
        get() = LocalStackTypography.current
    
    val spacing: StackSpacing
        @Composable @ReadOnlyComposable
        get() = LocalStackSpacing.current
    
    val radius: StackRadius
        @Composable @ReadOnlyComposable
        get() = LocalStackRadius.current
    
    val motion: StackMotion
        @Composable @ReadOnlyComposable
        get() = LocalStackMotion.current
}
```

### 10.3 사용

```kotlin
@Composable
fun MyComponent() {
    Surface(color = StackTheme.colors.surface.elevated) {
        Column(
            modifier = Modifier.padding(StackTheme.spacing.l),
            verticalArrangement = Arrangement.spacedBy(StackTheme.spacing.s),
        ) {
            Text(
                text = "Hello",
                style = StackTheme.typography.title,
                color = StackTheme.colors.text.primary,
            )
        }
    }
}
```

---

## 11. Material 3 통합

Material 3와의 매핑 (M3 컴포넌트 사용 시):

| Material 3 슬롯 | Stack 토큰 |
|---|---|
| `colorScheme.background` | `colors.surface.background` |
| `colorScheme.surface` | `colors.surface.elevated` |
| `colorScheme.surfaceVariant` | `colors.surface.muted` |
| `colorScheme.onBackground` | `colors.text.primary` |
| `colorScheme.onSurface` | `colors.text.primary` |
| `colorScheme.onSurfaceVariant` | `colors.text.secondary` |
| `colorScheme.primary` | `colors.accent.default` |
| `colorScheme.onPrimary` | `colors.text.inverse` |
| `colorScheme.outline` | `colors.divider.default` |
| `colorScheme.error` | `colors.danger.default` |

> Material You (다이내믹 컬러)는 v1.0에서 사용하지 않음. 디자인 일관성 우선.

---

## 12. 다크 테마 가이드

### 12.1 원칙

- 단순 색 반전 금지. 다크는 별도 팔레트.
- 다크 배경은 순흑(#000) 금지. `#161514` 같이 약간 따뜻한 톤.
- 다크 텍스트는 순백(#FFF) 금지. `#EDEAE3` 같이 약간 따뜻한 톤.
- 다크에서는 그림자 효과 거의 무효 → divider와 surface 색 차이로 분리.

### 12.2 검증

- 모든 화면을 라이트/다크 양쪽으로 프리뷰 (`@PreviewLightDark`).
- 대비비 검사 (WCAG AA 4.5:1 이상).
- 시스템 다크 모드 ↔ 라이트 모드 토글 시 깜박임 없이 전환.

---

## 13. 접근성

### 13.1 색 대비

모든 텍스트/배경 조합은 WCAG AA 이상:
- 일반 텍스트 (15sp+): 4.5:1
- 큰 텍스트 (24sp+): 3:1

본 토큰 세트는 위 기준을 만족하도록 설계됨. 새 토큰 추가 시 검증 필수.

### 13.2 터치 타깃

모든 인터랙티브 영역 최소 48dp × 48dp.

```kotlin
IconButton(
    onClick = onPlay,
    modifier = Modifier.size(48.dp),  // 최소 보장
) { ... }
```

### 13.3 시스템 글꼴 크기

토큰 사이즈는 sp 단위 → 시스템 글꼴 200%까지 자동 확대. UI가 깨지지 않게 LazyColumn 항목 높이는 가변(`wrapContentHeight`)으로.

### 13.4 모션 감소

```kotlin
@Composable
fun motionDuration(default: Long): Long {
    val context = LocalContext.current
    val reducedMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    return if (reducedMotion) 0 else default
}
```

---

## 14. 새 토큰 추가 절차

1. **필요성 검토.** 기존 토큰 조합으로 가능하면 추가 안 함.
2. **사용자 승인 받기.** 디자인 시스템 변경.
3. 본 문서에 토큰 정의 추가 (라이트/다크 양쪽 값).
4. `:core:design`의 해당 파일에 코드로 추가.
5. (필요 시) Material 3 매핑 §11 갱신.
6. 사용 예시를 본 문서에 추가.

---

## 15. 빠른 참조

```kotlin
// 색
StackTheme.colors.surface.background
StackTheme.colors.surface.elevated
StackTheme.colors.text.primary
StackTheme.colors.text.secondary
StackTheme.colors.accent.default
StackTheme.colors.divider.default
StackTheme.colors.danger.default

// 타이포
StackTheme.typography.display
StackTheme.typography.title
StackTheme.typography.body
StackTheme.typography.caption
StackTheme.typography.mono

// 간격
StackTheme.spacing.s    // 8dp
StackTheme.spacing.m    // 12dp
StackTheme.spacing.l    // 16dp
StackTheme.spacing.xl   // 24dp

// 모서리
StackTheme.radius.s     // 4dp
StackTheme.radius.m     // 8dp
StackTheme.radius.l     // 12dp

// 모션
StackTheme.motion.fast      // 150ms
StackTheme.motion.standard  // 250ms
StackTheme.easing.standard
```

---

*마지막 갱신: 2026-04-17 / SSOT v5.0 §8 기준*
