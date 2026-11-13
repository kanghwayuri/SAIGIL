# Pleos Connect UI 디자인 규칙 요약

Pleos Connect UI Design Guide(원문: [`design-guide/`](./design-guide/00-introduction-intro.md))에서 UI 구현 결정에 직접 영향을 주는 규칙만 뽑은 체크리스트입니다. 앱 동작·정책 규칙은 [`planning-rules.md`](./planning-rules.md)에 있습니다.

- 원문 가져온 날짜: 2026-11-12
- 출처: https://document.pleos.ai/docs/connect/guide/docs-design/Introduction/intro
- [필수]는 원문이 "해야 합니다", "안 됩니다"처럼 지시한 규칙, [권장]은 "권장", "지양"처럼 정도를 남긴 규칙입니다. 표시가 없는 줄은 사실이나 기준 설명입니다.

## 먼저 알아둘 것

- **UI 컴포넌트 라이브러리는 SDK로 제공되지 않습니다.** Pleos Connect SDK는 Vehicle, NaviHelper, TextToSpeech, SpeechToText, FusedLocation 5개 모듈뿐입니다. 컴포넌트는 이 가이드를 보고 Compose로 직접 구현합니다.
- **색상 토큰 값, 폰트 크기 체계, 컴포넌트 치수 대부분은 원문 이미지와 Figma 파일 안에만 있습니다.** 텍스트로 적힌 수치만 이 요약에 들어 있습니다. 정확한 값은 Figma 파일에서 가져옵니다.
  - [Pleos Connect design assets (.fig)](https://document.pleos.ai/img/Pleos_Connect_design_assets_215.fig)
  - [Pleos Connect display guide (.fig)](https://document.pleos.ai/img/Pleos_Connect_display_guide_215.fig)
  - Figma에 올린 파일의 node-id와 폼팩터별 화면 치수는 [`figma-map.md`](./figma-map.md)에 있습니다.
- 원문 이미지 중 1MB가 넘는 14개는 로컬에 저장하지 않고 원본 URL로 링크했습니다. 사이트가 바뀌면 깨질 수 있습니다.

## 1. 기본 원칙

원문: [00 Intro](./design-guide/00-introduction-intro.md)

- [필수] 주행 중 동영상, 애니메이션 이미지, **텍스트 자동 스크롤**로 시선을 끌지 않습니다.
- [필수] **한 손으로** 조작할 수 있어야 하며, 양손이나 손·발 동작을 요구하지 않습니다.
- [필수] 운전자가 **1~2초** 안에 파악할 수 있게 핵심 정보만 노출합니다.
- [필수] 입력 후 응답은 **0.25초 이내**. 더 걸리면 응답 중 상태를 시각적으로 표시합니다.
- [필수] 아이콘, 용어, 상호작용 패턴을 일관되게 유지하고, 기본 탑재 UI와 맞춥니다.
- 주간/야간 조명 환경에 따라 UI 밝기와 대비를 자동 조정합니다.
- 다국어와 다양한 해상도·화면 비율에 대응하도록 설계합니다.

## 2. 텍스트와 대비

원문: [00 Intro](./design-guide/00-introduction-intro.md), [07 Typography](./design-guide/07-foundations-typography.md)

| 항목 | 기준 |
|---|---|
| 아이콘·텍스트·이미지 대비율 | **4.5:1 이상** |
| 영어 단일 메시지 | 120자 또는 24단어 이하 |
| 일본어 단일 메시지 | 30자(영문 기준) 이하 |
| 한 줄 | 80자(영문 기준) 이하 |
| 문장·문단 | **3줄 이하** |
| 중요 정보 텍스트 높이 (속도, 내비 안내, 경고) | **5 mm 이상** |
| 보조 텍스트 높이 | 3 mm 이상 |
| 최소 폰트 크기 | 12sp 이상 권장, 실제 픽셀 높이 18px 이상 (18~20px 권장) |

- System UI 폰트는 [Asta Sans](https://fonts.google.com/specimen/Asta+Sans)(한국어·영어)입니다.
- [권장] 캐릭터가 강한 폰트는 지양하고, 정보 위계에 따라 컴포넌트 간 크기를 일관되게 씁니다.
- 폰트 위계는 Headline, Title, Body, Label 4단계이며, 구체적인 크기는 이미지·Figma에 있습니다.

## 3. 화면 구성과 window

원문: [01 Display Layout](./design-guide/01-getting-started-layout.md), [05 Window Policy](./design-guide/05-getting-started-window.md)

- 화면은 Status bar, Driving view, GNB, Application window로 구성됩니다.
- [필수] **가로 전용 게임처럼 한 모드로만 실행되는 앱이 아니면 landscape와 portrait 레이아웃을 모두 지원**합니다.
- Status bar, App library·공조 패널, 키보드 같은 System UI는 앱보다 위 레이어라 앱 화면을 일시적으로 가릴 수 있습니다.
- 디스플레이 유형:
  - **Connect-S** (Standard): 17", 14.6", 12.9"(구 Connect-L). 앱 2개면 1:1, 1개면 2:0.
  - **Connect-W** (Wide): 2:1 또는 1:2(기본 2:1). 세로가 늘어나는 Connect-W-Extended, 작은 해상도의 Connect-WF(Waterfall)가 있습니다.
- Connect-S에서 새 앱은 기존 앱의 orientation을 따르고, Connect-W에서는 **항상 landscape로 primary window**에 뜹니다.
- 화면에서 사라진 앱은 종료되지 않습니다. 표시 중인 앱이 종료되면 최근 앱 순으로 다시 표시됩니다.
- 사용자는 3 핑거 스와이프나 window bar로 앱 위치 변경, landscape↔portrait 전환, 앱 종료를 할 수 있습니다.
- Connect-W에서는 동시 실행 앱이 최대 2개이고, 조수석 App library에서 실행한 앱은 조수석 window에서만 실행됩니다.
- Deep link(App A → App B)는 B가 A를 덮고 실행되며, B가 끝나면 A로 돌아갑니다.

## 4. 시각 요소

원문: [08 Iconography](./design-guide/08-foundations-contents-creation-iconography.md), [09 App Icon](./design-guide/09-foundations-contents-creation-app-icon.md), [11 Shadow](./design-guide/11-foundations-shadow.md), [06 Colors](./design-guide/06-foundations-colors.md)

- 아이콘은 **48 x 48dp** 그리드 기준, Filled/Outlined 2가지 스타일. 국제적으로 통용되는 아이콘을 쓰고 텍스트 라벨을 함께 둡니다.
- 앱 아이콘은 **Android Adaptive Icon**(Foreground/Background 분리)으로 만듭니다. 브랜드별 마스킹 템플릿은 design assets Figma에 있습니다.
- 색상은 Basic Color(Grayscale, White/Gray Alpha, Static Light/Dark)와 System Color(Background, Button, Text, Surface 등 20여 그룹)로 나뉩니다. 값은 Figma에서 가져옵니다.
- 디자인 토큰 이름: prefix `ivi`, snake_case, 축약 금지(`vehicle_setting` O, `vs` X).

| Shadow | 레이어 (X, Y, Blur, Spread, Color) |
|---|---|
| Level 1 | 0, 6, 24, 0, `#14000000` (8%) |
| Level 2 | 0, 8, 32, 0, `#29000000` (16%) + 0, 2, 2, 0, `#14000000` (8%) |
| Level 3 | 0, 20, 40, 0, `#3D000000` (24%) + 0, 2, 4, 0, `#33000000` (20%) |

## 5. 인터랙션

원문: [39 Interaction](./design-guide/39-foundations-interaction.md), [00 Intro](./design-guide/00-introduction-intro.md)

- 터치 가능한 모든 요소는 **눌렀을 때 95%로 축소 + 모드별 배경색 변경**(shrink effect). 터치 영역 밖으로 손가락이 나가면 취소합니다.
- 스크롤 끝에서 bounce effect를 주고, overscroll은 전체 스크롤 영역의 약 **2/5**까지입니다.
- Long press(2~3초)는 편집 모드 진입에 쓰입니다.
- [권장] 터치 시 햅틱이나 음성 피드백을 제공합니다.
- 로딩 애니메이션은 Linear(짧은 로딩)와 Tension(긴 로딩) 2종이며, [Lottie 파일](https://document.pleos.ai/assets/files/Loading-a564bf9ddfbe2fa3c9640ec7708d9e1d.zip)로 제공됩니다.

## 6. 컴포넌트

원문: [12](./design-guide/12-components-actions-basic-button.md) ~ [33](./design-guide/33-components-widgets.md), [41](./design-guide/41-components-notification-vehicle-notification.md)

원문은 컴포넌트마다 anatomy, states, properties만 정의합니다. 구현할 때 아래 상태와 옵션을 지원 범위의 기준으로 삼습니다.

| 컴포넌트 | 상태 | 옵션 |
|---|---|---|
| Basic button | Pressed, Disabled, Loading | prefix/suffix icon |
| Toggle button | Toggled, Pressed, Disabled | Floating, Size L/M/S |
| Text button | Pressed, Disabled, Loading | Size M/S |
| Icon button | Pressed, Disabled | Style Basic/Plain/Circle, Size L/M/S |
| Controllers (slider, stepper) | Pressed/Dragging | Min, Max, Step. 끝값 도달 시 Disabled |
| Dropdown | Opened, Pressed, Disabled | Prefix(App icon, Icon), Suffix(Switch, Icon), Size M/S |
| Text field | Focused, Disabled, Read only, Invalid | Type Text/Tel/Url/Email/Password, Size M/S |
| Number field | Focused, Disabled, Read only, Invalid | Size L/M |
| Page navigation | Main, Depth(Back 버튼) | Primary/Secondary menu, Subtitle |
| System popup | | Header, Body, Size L/M, 버튼 정렬 Single/Vertical/Horizontal |
| Tabs | Enable, Pressed, Selected | Type Box/Line, Fitted |
| Checkbox | Checked, Pressed, Disabled | |
| Radio | Selected, Pressed, Disabled, Invalid | |
| Switch | Checked, Pressed, Disabled, Invalid | |
| Spinner | | Date, Time |

- Toast: side·bottom padding **24**, 전체 max width **792**, 메시지 max width **712**.
- Controllers의 볼륨 아이콘 단계: Min 0%, Low 1~25%, Mid 26~75%, High 76~100%, Max 100%.

## 7. 알림

원문: [00 Intro](./design-guide/00-introduction-intro.md), [31 System Notification](./design-guide/31-components-notification-system-notification.md)

- [필수] 알림은 운전자의 필요와 관련될 때만 간결하게 표시합니다.
- [필수] 알림으로 광고를 표시하지 않습니다. 광고주·제품 이름 외의 텍스트 광고도 금지입니다.

## 8. 시스템 영역 (앱이 직접 만들지 않는 UI)

원문: [02 Status bar](./design-guide/02-components-status-bar.md), [03 GNB](./design-guide/03-components-gnb.md), [04 App library](./design-guide/04-components-app-library.md), [34 Driving view](./design-guide/34-experiences-driving-view.md), [35 Gleo AI](./design-guide/35-experiences-gleo-ai.md)

앱 UI와 겹치거나 상호작용하는 부분만 적습니다.

- 스티어링 휠 버튼 조작 시 OSD 패널이 뜨고, 추가 조작이 없으면 **2초 후** 사라집니다. Connect-S에서는 GNB 영역, Connect-W에서는 GNB 오른쪽에 표시됩니다.
- Gleo AI 패널은 GNB 바로 아래 레이어라 앱 화면을 일시적으로 가릴 수 있습니다.
- CCP(조그 다이얼)를 쓸 수 있는 상황에서는 포커스 표시가 나타납니다. 앱도 OS의 CCP 정책을 따릅니다.

## 9. 3D 에셋 (사용할 경우)

원문: [40 3D Assets](./design-guide/40-foundations-contents-creation-3d-contents.md)

- [권장] 공조, 시트 위치 같은 차량 제어 UI에 3D 모델 활용을 권장합니다.
- 형식은 **glTF**, 엔진은 **Google Filament**. Lowpoly 기준 3Ds Max 30 MB, FBX 6 MB.
- 차량 부품 part name 규칙(예: 운전석 도어 `Door_F_L`)이 원문 Appendix 표에 있습니다.
