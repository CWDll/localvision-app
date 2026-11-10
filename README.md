# LocalVision Android TV APP v9.2

기준 조합:
- APP: v9.2-cms172-player165-appid
- CMS: v1.7.5 auto app id
- Player: v1.6.8 watchdog / refresh 480000

## 핵심 변경

기존 앱은 `store`를 직접 입력하는 구조였지만, v9.2부터는 TV 앱에 `APP ID`만 저장합니다.

동작 흐름:
1. TV 앱 실행
2. 저장된 APP ID 확인 예: `lv001`
3. CMS API 호출: `/api/app-config?id=lv001`
4. CMS가 실제 `store`, `playerUrl`, 운영 주기값을 반환
5. 앱 WebView가 최신 `playerUrl`을 실행
6. 이후 Player가 heartbeat/명령을 처리하고 APP은 스크린샷 Native bridge만 제공

## 기본값

- APP ID: `lv001`
- CMS API: `https://localvision-cms.pages.dev`
- Fallback Player: `https://buly.kr/`
- heartbeat: `300000` ms
- commandPoll: `300000` ms
- noticePollMs: `300000` ms
- refresh/contentCheck: `480000` ms

## 설정 진입

Player 화면에서 리모컨 숫자 `0` 버튼을 5번 누르면 설정 화면이 열립니다.
MENU / SETTINGS 키는 비상 진입용으로 유지됩니다.

## 현장 사용법

1. CMS에서 업체를 등록합니다.
2. CMS가 자동 발급한 APP ID를 확인합니다. 예: `lv001`, `lv002`
3. Android TV 앱 설정 화면에서 APP ID만 입력합니다.
4. 저장 후 실행합니다.

## 빌드 방법

Android Studio에서 이 폴더를 열고:

- `Build > Make Project`
- `Build > Build Bundle(s) / APK(s) > Build APK(s)`

생성 APK 위치:

`app/build/outputs/apk/debug/app-debug.apk`

