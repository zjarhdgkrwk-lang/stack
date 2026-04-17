# Stack

로컬 음원을 정중하게 다루는 Android 음악 앱.

- **패키지**: `com.stack.player`
- **타깃**: Android 8.0 (API 26) ~ 15 (API 35)
- **언어**: Kotlin 2.0, Jetpack Compose

## 원칙

- 로컬 우선. 제로 네트워크. 분석 SDK 없음.
- 1만 트랙 라이브러리에서도 즉시 응답.
- KO / EN / JA 동등 1급 지원.

## 문서

- `docs/SSOT_v5.0.md` — 기술 명세 (Single Source of Truth)
- `CLAUDE.md` — 개발 운영 매뉴얼
- `docs/STYLE_GUIDE.md` — 코드 스타일
- `docs/THREADING_MODEL.md` — 스레드/코루틴 정책
- `docs/BUILD.md` — 빌드 가이드
- `docs/DESIGN_TOKENS.md` — 디자인 토큰
- `CHANGELOG.md` — 변경 로그

## 빌드

```bash
./gradlew :app:assembleDebug
./gradlew checkAll      # 통합 검증 게이트
```

자세한 빌드 절차는 `docs/BUILD.md` 참조.

## 라이선스

TBD (v1.0 출시 전 결정).
