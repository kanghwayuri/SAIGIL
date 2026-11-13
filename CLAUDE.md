# SAIGIL

## Pleos Connect 앱 설계 지침

- 기능 설계나 UI·생명주기·차량 API 관련 결정 전에 `docs/planning-rules.md`를 읽는다.
- 화면·컴포넌트·색상·텍스트 같은 UI 구현 전에 `docs/design-rules.md`를 읽는다. UI 컴포넌트 라이브러리는 SDK에 없으므로 Compose로 직접 구현한다.
- Figma(디자인 가이드 원본 .fig 버전 215). `figma-saigil` MCP 서버로 접근한다. 다른 Figma MCP 서버의 계정은 이 파일들에 권한이 없다.
  - 이 계정은 MCP 호출 한도가 작다(Starter, View seat). 페이지 전체 `get_metadata`를 호출하지 말고, `docs/figma-map.md`에서 node-id를 찾아 필요한 프레임만 읽는다.
  - Design assets: https://www.figma.com/design/vC1Ey7lIVxBDYE2mGLLpnk/Pleos_Connect_design_assets_215
    - Foundation 페이지 `0:1`: Colors `67:14834`, Typography `67:11477`, Iconography `67:13755`, App Cover `67:15104`
    - Components 페이지 `1:2`: Buttons `67:22479`, Sliders `67:24477`, Dropdown `67:25695`, Fields `67:27012`, Page Navigation `67:27529`, Popup `67:29503`, Tabs `67:32652`, Selections `67:33563`, Spinner `67:33637`, System Notification `67:33989`
  - Display guide: https://www.figma.com/design/TWBNux2ofNoZHlzejk1tnL/Pleos_Connect_display_guide_215
    - All Formfactor `2203:12806`: 모든 폼팩터 레이아웃 개요
- 근거 문장이 필요하면 `docs/planning-guide/`, `docs/design-guide/`의 원문(2026-11-12 사본)을 확인한다. 원문 사이트가 바뀌었을 수 있으니 수치·정책이 결정을 좌우하면 사이트도 확인한다.
