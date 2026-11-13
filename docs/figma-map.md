# Figma node 지도

Pleos Connect 디자인 가이드 Figma 파일(원본 .fig 버전 215)의 주요 node-id 목록입니다. 2026-11-13에 `figma-saigil` MCP의 `get_metadata`로 확인했습니다.

- 이 파일들은 `figma-saigil` MCP 서버로만 열립니다.
- 이 계정은 Starter 플랜의 View seat라 MCP 호출 한도가 작습니다. **페이지 전체 `get_metadata`는 다시 호출하지 말고**, 아래 node-id로 필요한 프레임에 바로 접근합니다.
- URL에서는 `:`를 `-`로 바꿔 씁니다. 예: `67:14834` → `?node-id=67-14834`

## Design assets

`fileKey`: `vC1Ey7lIVxBDYE2mGLLpnk`
https://www.figma.com/design/vC1Ey7lIVxBDYE2mGLLpnk/Pleos_Connect_design_assets_215

### ✦ Foundation 페이지 (`0:1`)

| 프레임 | node-id | 내용 |
|---|---|---|
| Colors | `67:14834` | 색상 토큰 전체 |
| └ Basic color | `67:5093` | `basic_gray` `67:5100`, `basic_alpha_light` `67:5117`, `basic_alpha_dark` `67:5126`, `basic_static_light` `67:5139`, `basic_static_dark` `67:5157` |
| └ System color | `67:5176` | `system_background` `67:5182`, `system_button` `67:5191`, `system_controller` `67:5219`, `system_climate` `67:5229`, `system_dropdown` `67:5247`, `system_dim` `67:5254`, `system_divider` `67:5266`, `system_driving` `67:5283`, `system_fields` `67:5293`, `system_icon` `67:5301`, `system_infomative` `67:5315`, `system_media` `67:5356`, `system_overlay` `67:5365`, `system_phone` `67:5382`, `system_regulation` `67:5392`, `system_surface` `67:5404`, `system_switch` `67:5413`, `system_text` `67:5425` |
| Typography | `67:11477` | 폰트 크기 체계 |
| Iconography | `67:13755` | 48dp 아이콘 그리드 (`Icon_48px_24Grid` `67:14966`) |
| App Cover | `67:15104` | 앱 아이콘 그리드, Adaptive Icon 브랜드별 마스킹 (`39667:4707`) |

### ❖ Components 페이지 (`1:2`)

컴포넌트 이름은 `ivi_` prefix입니다. variant 속성은 Figma에 정의된 그대로 적었습니다.

| 그룹 프레임 | 컴포넌트 (node-id) | variant 속성 |
|---|---|---|
| Buttons `67:22479` | `ivi_basic_button` `67:22525` | Type Primary/Secondary, Size L/M/S, State Enabled/Pressed/Disabled |
| | `ivi_toggle_button` `67:22600` | Size L/M/S, State, On active |
| | `ivi_text_button` `67:22681` | Size M/S, State |
| | `ivi_icon_button` `67:22708` | Type Basic/Plain/Circle, Size M/S, State |
| | `ivi_numeric_keypad` `67:22917` | Type Number/Delete/Clear, State |
| | `ivi_keypad_normal` `67:22936`, `ivi_keypad_static` `67:22975` | Type Primary/Delete/Shift/Secondary/Mic/Hyphen/Space/Enter, State, On |
| Sliders `67:24477` | `ivi_slider_centered` `67:24492` | Type Button/Basic/Number, OFF |
| | `ivi_slider_continuous` `67:24536` | Size L/M, Type Basic/Floating |
| | `slider_track` `67:24515`, `67:24556` / `slider_knob` `67:24528` / `slider_button` `67:24613` | 하위 부품 |
| Steppers `67:25154` | `ivi_stepper` 인스턴스 `67:25188` | |
| Dropdown Menu `67:25695` | `ivi_dropdown` `67:25738`, `ivi_dropdown_menus` `67:25745` | Size M/S |
| | `dropdown_button` `67:25760`, `dropdown_item` `67:25809` | Size, State (Selected/Enabled/Disabled/Footer 등) |
| Fields `67:27012` | `ivi_text_field` `67:27050` | Size M/S, State Enabled/Focused/Typing/Entered/Read Only/Error |
| | `ivi_number_field` `67:27184` | Size L/M, State Enabled/Focused/Entered/Error |
| Page Navigation `67:27529` | `ivi_header` `67:27600` | Type Radio/Phone/Music 등 |
| | `ivi_title` `67:27622` | Type Main/Depth/Search |
| Popup `67:29503` | `ivi_system_popup` `67:29509` | Size M/L, Extended |
| | `system_popup/header` `67:29529`, `/buttons` `67:29546`, `/content` `67:29553` | 하위 부품 |
| | `ivi_toast_popup` `67:32581` | Margin on/off |
| Tabs `67:32652` | `ivi_segmented_control/solid` `67:32705` | Size L/M, Segments 2~6 |
| | `ivi_segmented_control/line` `67:32807` | Bottom line |
| | `segmented_controls/goast_button` `67:32774`, `/line_button` `67:32820` | 하위 부품 |
| Selections `67:33563` | `ivi_checkbox` `67:33570` | Active False/True/Indeterminate, State |
| | `ivi_radio_button` `67:33597` | Active, State |
| | `ivi_switch` `67:33612` | Active, Size M/S, State |
| Spinner `67:33637` | `ivi_spinner` `67:33663`, `spinner_wheels` `67:33741` | Type Date/Time, Size L/M |
| System Notification `67:33989` | `ivi_notification_system` `67:34043` | Type Image+Text/Only Text/Call |

이 파일에 없는 컴포넌트: Bar(window/scroll), Indicator, Widgets, Status bar, GNB, Toast(일반), Vehicle Notification. 이들은 원문 문서 이미지에만 있습니다.

## Display guide

`fileKey`: `TWBNux2ofNoZHlzejk1tnL`
https://www.figma.com/design/TWBNux2ofNoZHlzejk1tnL/Pleos_Connect_display_guide_215

페이지는 `Form factors & Display` (`0:1`) 하나입니다.

| 섹션 | node-id | 내용 |
|---|---|---|
| Form factors | `2203:14909` | Connect-S/W 설명 |
| **All Formfactor** | `2203:12806` | **모든 폼팩터 레이아웃을 한곳에 모은 개요. 먼저 볼 곳** |
| Screen Content (S 계열) | `2124:7103`, `2124:7338` | Connect-S 3종 레이아웃. 같은 구성 2벌 |
| Screen Content (W) | `2124:7485`, `2124:7595` | Connect-W, W-Extended. 같은 구성 2벌 |
| Screen Content (WF) | `2124:7519`, `2124:7629` | Connect-WF. 같은 구성 2벌 |

같은 구성이 2벌씩 있는 이유(라이트/다크, 앱 배치 차이 등)는 메타데이터만으로는 알 수 없었습니다.

### 폼팩터별 화면 치수 (메타데이터 기준)

| 폼팩터 | 전체 | Status bar | 앱 영역(body) | GNB |
|---|---|---|---|---|
| Connect-S 17" (추정) | 2560 x 1440 | 96 | 2560 x 1184 | 가로 160 |
| Connect-S 14.6" | 2048 x 1152 | 76 | 2048 x 948 | 가로 128 |
| Connect-S 12.9" (구 Connect-L) | 1920 x 1080 | 84 | 1920 x 855 | 가로 141 |
| Connect-W | 3840 x 850 | | 2590 x 850 | 세로 140 x 2, Driving view 970 |
| Connect-W-Extended (추정) | 3840 x 1440 | | 2590 x 1440 | 세로 140 x 2, Driving view 970 |
| Connect-WF (추정) | 3072 x 816 | | 2016 x 816 | 세로 112 x 2, Driving view 832 |

- 단위는 Figma 프레임 크기 그대로입니다. px인지 dp인지는 Figma에 적혀 있지 않습니다.
- 14.6"와 12.9"는 Figma 라벨로 확인했습니다. 17", W-Extended, WF는 원문 문서의 설명과 크기로 추정했습니다.
- 수치는 레이아웃 예시 프레임의 크기라 실제 차량 해상도와 다를 수 있습니다. 실제 앱 영역은 런타임에 window 크기로 받아야 합니다.
