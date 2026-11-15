# 사이길 (SAIGIL)

**차 안에서 동승자와 자연스럽게 대화를 시작하고 이어 가도록 돕는 Pleos Connect 차량용 앱입니다.**

출발 전에 하고 싶은 이야기나 지금 상황을 말하면 대화를 여는 방법과 저장해 둔 화제를 추천하고, 주행 중에는 고속도로 진입이나 정체 구간에 맞춰 대화거리를 음성으로 제안합니다.

## APK 위치

`apk/아몰라엄마_사이길_release.apk`

## 주요 기능

| 시나리오 | 내용 |
| --- | --- |
| 1. 프로필과 화제 입력 | 누구든 같은 방식으로 프로필(이름, 자주 가는 곳, 관심사)을 만듭니다. 화제는 나와 동승자 두 사람의 관계에 저장합니다. |
| 2. 하고 싶은 이야기가 있을 때 | 동승자를 고르고 주제를 음성으로 말하면, 서로 다른 접근 3가지의 첫마디와 이어 가는 방법을 추천합니다. |
| 3. 무슨 이야기를 할지 모를 때 | 지금 상황을 음성으로 말하면, 두 사람에게 저장된 화제 중 상황에 맞는 것을 골라 줍니다. |
| 4. 주행 중 능동 추천 | 고속도로 진입이나 정체 구간을 감지하면 저장한 화제, 요즘 화제, 밸런스 게임을 음성으로 제안하고 주행 화면에 보여 줍니다. |
| 5. 함께한 길에 남기기 | 도착 후 대화가 어땠는지와 새로 알게 된 관심사를 남기면 다음 추천에 반영합니다. |

이 밖에 Gleo AI에 세 가지 기능(화제 저장, 첫마디 추천, 저장 화제 추천)을 등록해 음성으로 사이길을 부를 수 있습니다.

## 테스트 환경

| 항목 | 값 |
| --- | --- |
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 (AVD 시스템 이미지 "Pleos Connect v2.1.0 APIs", 42dot) |
| 플랫폼 | Android Automotive OS, Android 14 (API 34), arm64-v8a |
| 화면 규격 | 2560 × 1440, 160 dpi, 가로(landscape) |
| 호스트 | macOS (Apple Silicon), Android Studio |
| 빌드 | Android Gradle Plugin 9.4.1, Kotlin 2.2.10, compileSdk 37, minSdk 28, JDK 17 이상 |

사용한 Pleos SDK는 SpeechToText 3.3.1, TextToSpeech 3.3.1, NaviHelper 3.0.5입니다. 주행 상태는 Android Automotive의 `CarUxRestrictionsManager`로 판단합니다.

## 실행 방법

### APK로 실행

1. Pleos Connect Emulator v2.1.0을 실행하고 부팅이 끝날 때까지 기다립니다.
2. APK를 설치합니다.

   ```sh
   adb -s <에뮬레이터 serial> install -r "apk/아몰라엄마_사이길_release.apk"
   ```

3. 앱 목록에서 **사이길**을 실행합니다.
4. 처음 화면 "나는 누구인가요?"에서 **프로필 추가**로 내 프로필을 만들고, 입력 모드에서 동승자 프로필을 하나 더 만듭니다.

### 소스에서 빌드

```sh
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:installDebug         # 연결된 에뮬레이터에 설치
./gradlew :app:testDebugUnitTest    # 단위 테스트
```

[just](https://github.com/casey/just)가 있으면 `just install <serial>`로 빌드, 설치, 실행을 한 번에 할 수 있습니다. 레시피 목록은 `just`로 확인합니다.

release APK를 서명해 빌드하려면 저장소 루트에 `keystore.properties`가 필요합니다. 이 파일과 키는 저장소에 포함하지 않습니다. 파일이 없으면 release APK는 서명 없이 만들어집니다.

```properties
storeFile=/path/to/saigil-release.jks
storePassword=...
keyAlias=saigil
keyPassword=...
```

## 필요한 설정

### Pleos SDK 저장소

`settings.gradle.kts`에 Pleos Playground Maven 저장소(`https://nexus-playground.pleos.ai/repository/maven-releases/`)가 들어 있어 별도 설정 없이 SDK를 받습니다.

### 권한

`pleos.car.permission.STT_SERVICE`, `pleos.car.permission.TTS_SERVICE`, `pleos.car.permission.NAVI_ROUTE`를 사용합니다. 설치할 때 자동으로 허용되며 따로 허용할 것은 없습니다.

### 음성 입력 (에뮬레이터)

에뮬레이터에서 음성 입력을 쓰려면 두 가지가 필요합니다.

1. macOS 시스템 설정 > 개인정보 보호 및 보안 > 마이크에서 **Android Studio**를 허용하고, Android Studio와 에뮬레이터를 다시 실행합니다.
2. 에뮬레이터를 켤 때마다 호스트 마이크 입력을 켭니다.

   ```sh
   adb -s <에뮬레이터 serial> emu avd hostmicon
   ```

음성 인식이 어려우면 입력 칸에 키보드로 직접 적어도 됩니다.

### 주행 상태 바꾸기

에뮬레이터의 Virtual Console에서 기어를 D로 바꾸면 사이길이 주행 화면으로 바뀌고, P로 바꾸면 하던 화면으로 돌아옵니다.

### 주행 중 능동 추천 확인 (시나리오 4)

고속도로 진입과 정체 감지는 NaviHelper의 경로 안내 신호를 씁니다. 에뮬레이터는 실제 경로 주행을 재현하지 못하므로, **debug 빌드**에 들어 있는 시연용 신호로 확인합니다. 추천 모드에서 동승자를 고르고 추천 결과까지 본 뒤 기어를 D로 바꾸고 아래 명령을 보냅니다. release APK에는 이 시연용 신호가 들어 있지 않습니다.

```sh
# 고속도로 진입, 남은 거리 42km
adb shell am broadcast --user 10 -a com.gomsoo.saigil.DEBUG_TRIGGER --es trigger Highway --ei remaining_km 42 -p com.gomsoo.saigil
# 정체 구간 2.3km, 평균 12km/h
adb shell am broadcast --user 10 -a com.gomsoo.saigil.DEBUG_TRIGGER --es trigger Jam --ei jam_m 2300 --ei avg_kmh 12 -p com.gomsoo.saigil
```

Pleos 에뮬레이터는 운전자 사용자(user 10)로 실행되므로 `--user 10`이 필요합니다.

### Gleo AI 연동

Gleo는 자기 프로세스가 시작될 때 앱의 `assets/schema.json`을 읽습니다. 사이길을 설치한 뒤에는 에뮬레이터를 다시 시작하거나 아래 명령으로 Gleo를 다시 시작해야 사이길 기능이 등록됩니다. 그다음 "글레오"로 부르고 "사이길에서 엄마랑 대화를 어떻게 시작할지 추천해 줘"처럼 말합니다.

```sh
adb shell am force-stop --user 10 ai.umos.ambientai
```

## 프로젝트 구조

| 경로 | 역할 |
| --- | --- |
| `app/src/main/java/com/gomsoo/saigil/library` | 프로필, 두 사람의 관계, 화제, 함께한 길 기록과 기기 내 저장(JSON) |
| `app/src/main/java/com/gomsoo/saigil/recommend` | 첫마디 추천, 저장 화제 고르기, 주행 중 제안 순서 |
| `app/src/main/java/com/gomsoo/saigil/screen` | 화면 상태와 사용자 이벤트를 처리하는 reducer |
| `app/src/main/java/com/gomsoo/saigil/speech` | SpeechToText SDK 연동 |
| `app/src/main/java/com/gomsoo/saigil/drive` | NaviHelper 신호 감지, TextToSpeech 안내, 주행 화면 상태 |
| `app/src/main/java/com/gomsoo/saigil/gleo` | Gleo AI 기능 호출 처리 |
| `app/src/main/java/com/gomsoo/saigil/vehicle` | 주행 제한 상태 |
| `app/src/main/java/com/gomsoo/saigil/ui` | Compose 화면과 Pleos 디자인 컴포넌트 |

설계 문서는 [`docs/scenario-v2-design.md`](docs/scenario-v2-design.md), 클래스 구조는 [`docs/class-diagram.md`](docs/class-diagram.md)에 있습니다.