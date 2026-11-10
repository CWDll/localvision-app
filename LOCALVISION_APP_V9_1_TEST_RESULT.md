# LocalVision APP v9.1 테스트 결과

## 반영 기준

- APP 표기: `LOCALVISION TV APP V9.1`
- CMS 표기: `CMS v1.7.4`
- Player 표기: `Player v1.6.7`
- 기본 URL: `https://buly.kr/`
- 기본 CMS API: `https://localvision-cms.pages.dev`
- app-config 확인 주기: `1800000ms` = 30분
- APP Native heartbeat: `300000ms` = 5분
- Native 명령 확인: `300000ms` = 5분
- Player URL에 `native=1` 자동 부착

## 구현 확인

- `LocalVisionSettings.java` 버전/주기값 변경 확인
- `MainActivity.java` APP 시작 문구 v9.1 확인
- `MainActivity.java` `/api/native-command` 우선 조회 확인
- `/api/native-command` 실패 시 `/api/player-state` fallback 확인
- `/api/player-state` 실패 시 기존 `/api/player-config` fallback 유지 확인
- `captureAndUploadScreenshot()`에서 현재 화면을 PNG로 캡처 후 `/api/screenshots` 업로드 확인
- 설정 화면 숫자 0 버튼 5회 진입 로직 유지 확인
- 앱 빌드 버전 `versionCode 91`, `versionName 9.1` 확인

## R2 관련

별도로 R2에서 폴더를 만들 필요는 없습니다.
APP이 `/api/screenshots`로 PNG를 올리면 CMS가 자동으로 아래 경로에 저장합니다.

```txt
system/screenshots/<store>/<deviceId>/<YYYYMMDDHHMMSS>.png
```

예:

```txt
system/screenshots/goobne/lvapp_xxxxx/20260508093000.png
```

필요 조건:

- CMS의 R2 binding 이름: `MEDIA`
- CMS의 D1 binding 이름: `DB`
- `R2_PUBLIC_BASE` 환경변수 설정 권장

## 실제 APK 빌드 테스트 한계

이 작업 환경에는 Android SDK가 없어 `assembleDebug` 실제 빌드까지는 실행하지 못했습니다.
대신 소스 파일 구조/버전 문자열/명령 흐름/JS 문법/CMS endpoint 문법을 점검했습니다.
APK는 안준님 PC의 Android Studio에서 빌드해야 합니다.

## Android Studio 빌드 방법

```txt
1. 이 폴더를 Android Studio에서 열기
2. Gradle Sync 완료 대기
3. Build > Build Bundle(s) / APK(s) > Build APK(s)
4. app/build/outputs/apk/debug/app-debug.apk 확인
```
