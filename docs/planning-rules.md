# Pleos Connect 앱 설계 규칙 요약

Pleos Connect 앱 설계 가이드(원문: [`planning-guide/`](./planning-guide/00-intro.md))에서 설계·구현 결정에 직접 영향을 주는 규칙만 뽑은 체크리스트입니다.

- 원문 가져온 날짜: 2026-11-12
- 출처: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/intro
- 원문은 바뀔 수 있습니다. 수치나 정책을 근거로 중요한 결정을 내리기 전에는 원문 사이트를 다시 확인합니다.
- [필수]는 원문이 "해야 합니다", "준수합니다", "반드시"처럼 지시한 규칙, [권장]은 "권장", "지양", "최소화"처럼 정도를 남긴 규칙입니다. 표시가 없는 줄은 사실이나 기준 설명입니다.

## 1. 주행 안전과 주행 규제

원문: [01 주행 안전](./planning-guide/01-driver-distraction-guideline.md), [02 지역별 주행 규제](./planning-guide/02-driving-restiction-policy.md)

- [필수] `CarUxRestrictions`로 주행 중 제한 상태를 확인하고, 제한 대상 기능의 UI와 동작을 축소하거나 비활성화합니다.
- [필수] Distraction Optimization(DO)을 선언하지 않은 앱은 **Parking에서만 사용 가능, Drive에서는 사용 불가**입니다.
- [필수] 주행 규제 상태에서 영상 재생을 시작하거나 재개하지 않습니다.
- [필수] [AAOS 운전자 주의 분산 가이드라인](https://source.android.com/docs/automotive/driver_distraction/guidelines?hl=ko)을 준수합니다.
- [권장] 주행 중 동영상, 애니메이션, 움직이는 이미지, 반복 화면 전환을 최소화합니다.
- [권장] 자주 쓰는 기능은 첫 화면·상단에 두고, 드롭다운·다단 메뉴를 남용하지 않습니다.
- [권장] 큰 터치 영역, 높은 대비, 텍스트 최소화. 버튼 크기와 간격은 [Pleos Connect UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro)를 따릅니다.
- [권장] 키보드 입력보다 프리셋, 최근 항목, 음성 입력을 우선합니다.

### 지역별 Driving 상태 기능 허용 (Parking에서는 모두 허용)

✅ 허용, ⛔ 제한, 음성 = 음성으로만 허용.

| 기능 (UX restriction) | 북미 | 유럽 | 그외 |
|---|---|---|---|
| 키보드 (`NO_KEYBOARD`) | ⛔ | ✅ | ✅ |
| 다이얼패드 (`NO_DIALPAD`) | ⛔ | ✅ | ✅ |
| 통합 검색 (`NO_KEYBOARD`, `NO_FILTERING`) | 음성 | ✅ | ✅ |
| 미디어 앱 로그인·설정·검색 (`NO_SETUP`, `NO_KEYBOARD`, `NO_FILTERING`) | ⛔ | ⛔ | ✅ |
| 디지털키·도어·시트·차량 설정 (`NO_SETUP`) | ⛔ | ⛔ | ⛔ |
| 동영상 시청 (`NO_VIDEO`) | ⛔ | ⛔ | ⛔ |
| 내비게이션 검색 (`NO_KEYBOARD`, `NO_FILTERING`) | ⛔ | ✅ | ✅ |
| QR 로그인 (`NO_SETUP`) | ⛔ | ⛔ | ⛔ |
| 매뉴얼·대용량 텍스트 (`LIMIT_CONTENT`, `LIMIT_STRING_LENGTH`) | ⛔ | ⛔ | ⛔ |

- **도어·시트·차량 설정 조작은 모든 지역에서 Driving 중 제한**됩니다. 차량 제어 UI는 주행 중 비활성화를 기본값으로 설계합니다.
- 플랫폼 버전과 양산 차량에 따라 정책이 다를 수 있습니다.

## 2. 앱 카테고리

원문: [03 앱 카테고리](./planning-guide/03-app-category.md)

- [필수] 설계 시점에 앱 카테고리를 정합니다. (Maps/Navigation, Mobility Services, Lifestyle, Entertainment, Business, Music/Audio, Kids, Social/Communication)
- [필수] 매니페스트 `<application>`에 `android:appCategory`를 앱 목적에 맞게 선언합니다. (`game`, `audio`, `video`)
- 주행 중 기능이 제한적으로 허용될 수 있는 카테고리는 **Music/Audio, Maps/Navigation, Mobility Services**뿐입니다. 이 경우에도 주행 상태와 지역 정책을 따릅니다.

## 3. 화면 구조와 생명주기

원문: [04 앱 구조 및 동작 모델](./planning-guide/04-app-structure-screen-model.md)

- 화면은 고정된 Driving view와 앱 영역(window 1~2개)으로 구성됩니다. Driving view는 앱이 가리거나 종료할 수 없습니다.
- `screenOrientation` 미지정 시 **portrait, window 1개**. `landscape` 지정 시 **window 2개**를 사용합니다.
- [필수] portrait(핵심 기능)와 landscape(부가 정보·확장 기능)를 각각 설계합니다.
- [필수] window 크기·방향이 바뀌어도 핵심 기능이 동작하도록 반응형 레이아웃을 씁니다.
- 화면에서 사라진 앱은 종료되지 않고 백그라운드에 남을 수 있고, 다시 foreground로 돌아올 수 있습니다.
- [필수] foreground 진입 시 상태 복원과 UI 재구성, background 전환 시 화면 갱신·애니메이션·센서 수집 정리.
- [필수] 프로세스 재생성 후에도 사용자 데이터와 진행 상태를 복원할 수 있게 상태 저장 전략을 마련합니다.
- [권장] background에서 상주, 타이머, 폴링, 네트워크 요청을 최소화합니다.

## 4. 하드웨어 접근과 API 선택

원문: [05 하드웨어 접근](./planning-guide/05-platform-api-architecture.md), [06 SDK API 정책](./planning-guide/06-pleos-connect-sdk-api.md), [07 API 호환성](./planning-guide/07-api-compatibility-availability.md)

- [필수] 차량 하드웨어·센서에 직접 접근하지 않습니다. 차량 상태는 표준 플랫폼 API로만 확인·제어합니다.
- API 선택: **표준 데이터(속도, 기어, 외기 온도, 연료 등)는 AAOS Car API**, **제조사 특화 기능(시트 제어, 라이팅, 보안 인증 등)은 Pleos Connect SDK**. AAOS에 같은 기능이 있으면 SDK는 중복 제공하지 않습니다.
- SDK API가 AAOS 표준으로 편입돼도 기존 SDK API는 **최소 2개 메이저 버전**까지 하위 호환됩니다.
- **Vehicle SDK의 API 지원 여부는 차량 모델마다 다릅니다.** 도메인별 `checkXXXCapabilities` API로 확인한 뒤 사용합니다.
- FusedLocation SDK는 모델마다 다르며 `isSupported`로 확인합니다. NaviHelper SDK는 내비게이션 탑재 차량에서만 전체 호환, TextToSpeech·SpeechToText SDK는 전체 호환입니다.
- [필수] AAOS Vehicle Property는 **모델명으로 분기하지 말고** `VehiclePropertyIds`와 `CarPropertyManager`로 실제 지원 여부를 확인합니다.
- [필수] SDK가 제공하지 않는 특수 프로퍼티가 필요하면 직접 구현 전에 기술 지원팀 검토를 받습니다.
- 모바일 API 중 Telephony, SMS, 일부 센서(자이로, 근접 등), 캘린더, 주소록은 제한될 수 있습니다. 필수가 아니면 제거하거나 지원 여부 확인 후 동작하는 Optional 구조로 만듭니다.

## 5. 네트워크

원문: [05 하드웨어 접근](./planning-guide/05-platform-api-architecture.md)

- [권장] 주행 중 연결 끊김·저대역폭에 대비해 최근 콘텐츠 로컬 캐싱, retry와 적절한 timeout, 오프라인 상태 안내 UI를 제공합니다.

## 6. 보안과 개인정보

원문: [08 보안 및 개인정보](./planning-guide/08-security-privacy.md)

- [필수] 차량 소유자·운전자가 바뀔 때 쉽게 로그아웃하고 계정을 전환할 수 있어야 합니다.
- [필수] 로그인 토큰은 Keystore 등 안전한 스토리지에 저장합니다.
- [필수] 위치·음성·계정 정보를 요구할 때 이유를 명확히 하고 동의 플로우를 단순하게 만듭니다.
- 불필요한 PII는 저장하지 않고, 로그의 개인정보는 마스킹합니다.
- [필수] telemetry 설계 시 차량 ID·사용자 ID의 익명화·가명화 정책을 세우고, 운전 중 행동 데이터를 마케팅에 쓰지 않도록 구분합니다.
- App Market 등록 보안 기준은 별도 문서 [앱 보안 가이드](https://document.pleos.ai/docs/pleos-only/app-registration-guide/app-security-guide)에 있습니다.

## 7. 성능과 리소스

원문: [09 성능 및 리소스](./planning-guide/09-performance-resource-mgmt.md)

| 항목 | 기준 |
|---|---|
| CPU, 내비게이션 앱 (주행 중) | 15% 이하 |
| CPU, 내비게이션이 아닌 앱 (주행 중) | **5% 미만** |
| CPU, 모든 앱 (정차 중) | 25% 이하 |
| RAM | 400 MB 이하 |

- [권장] cold start 때 최소한만 초기화하고 나머지는 lazy loading으로 처리해 스플래시에 오래 머물지 않게 합니다.
- [필수] 오디오를 재생하는 앱은 Pleos Connect 오디오 포커스 정책을 따르고, 포커스 획득·상실에 대응하는 로직을 반드시 구현합니다. ([오디오 포커스 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development))

## 이 섹션에 없는 관련 문서

원문이 참조하지만 이번에 가져오지 않은 문서입니다. 해당 작업을 할 때 사이트에서 확인합니다.

- [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide): `CarUxRestrictions` 연동 구현
- [오디오 포커스 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development)
- [Pleos Connect UI Design Guide](https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro): 버튼 크기, 터치 간격 등 수치
- [앱 보안 가이드](https://document.pleos.ai/docs/pleos-only/app-registration-guide/app-security-guide)
