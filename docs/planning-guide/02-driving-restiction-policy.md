<!-- 원문: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy -->
<!-- 가져온 날짜: 2026-11-12. 원문이 바뀌었을 수 있으니 중요한 결정 전에는 원문을 확인할 것. -->

# 지역별 주행 규제 정책

> **정보**
>
> `CarUxRestrictions` 연동 방법은 [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)를 참조합니다. <br> 플랫폼 버전과 양산 차량에 따라 주행 규제 정책이 다를 수 있습니다.

## 주행 규제 개요

Pleos Connect의 주행 규제는 차량의 속도·기어·브레이크 등 하드웨어 신호와 지역별 안전 규정에 따라 적용됩니다.

## 주행 상황에 따른 앱 사용 여부

앱에서 Distraction Optimization(DO)를 선언하지 않았을 때, 주행 상황에 따라 앱 사용 가능 여부가 달라집니다. 다음 표는 상황별 기본 동작을 설명합니다.

| 상황 | DO 미선언 시 앱 사용 |
|---|---|
| Parking(주차) | 사용 가능 |
| Drive(주행) | 사용 불가 |

## 지역별 기능 허용 범위

주행 규제가 적용된 상태에서, 주행 상황(Parking, Idling, Driving)과 지역에 따라 기능별 사용 가능 여부가 달라집니다. ✅는 사용 가능, ⛔는 사용 제한, `음성한정`은 음성으로만 사용 가능함을 의미합니다.

### 주행 상황별 기능 제한

#### 북미 지역

| 구분 | 키보드<br>UX_RESTRICTIONS_NO_KEYBOARD(8) | 다이얼패드<br>UX_RESTRICTIONS_NO_DIALPAD(1) | 검색<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | 미디어 앱 로그인,<br>설정,<br>검색<br>UX_RESTRICTIONS_NO_SETUP(64)<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | 디지털키,<br>도어,<br>시트,<br>차량 설정<br>UX_RESTRICTIONS_NO_SETUP(64) | 동영상 시청<br>UX_RESTRICTIONS_NO_VIDEO(16) | 내비게이션 검색<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | QR 로그인<br>UX_RESTRICTIONS_NO_SETUP(64) | 매뉴얼 <br>(대용량 텍스트)<br>UX_RESTRICTIONS_LIMIT_CONTENT(32)<br>UX_RESTRICTIONS_LIMIT_STRING_LENGTH(4) |
|---|---|---|---|---|---|---|---|---|---|
| Parking | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Driving | ⛔ | ⛔ | `음성한정` | ⛔ | ⛔ | ⛔ | ⛔ | ⛔ | ⛔ |

- 표의 「검색」은 웹 검색이 아니라, 차량 인포테인먼트 시스템 내 통합 검색을 의미합니다.

> **지역별 키보드 분기 처리**
>
> InfoLINK용 앱을 개발하는 경우 지역별 키보드 정책에 따른 분기 처리가 필요할 수 있습니다. 자세한 사항은 [partnership@pleos.ai](mailto:partnership@pleos.ai)로 문의하시기 바랍니다.

#### 유럽 지역

| 구분 | 키보드<br>UX_RESTRICTIONS_NO_KEYBOARD(8) | 다이얼패드<br>UX_RESTRICTIONS_NO_DIALPAD(1) | 검색<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | 미디어 앱 로그인,<br>설정,<br>검색<br>UX_RESTRICTIONS_NO_SETUP(64)<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | 디지털키,<br>도어,<br>시트,<br>차량 설정<br>UX_RESTRICTIONS_NO_SETUP(64) | 동영상 시청<br>UX_RESTRICTIONS_NO_VIDEO(16) | 내비게이션 검색<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | QR 로그인<br>UX_RESTRICTIONS_NO_SETUP(64) | 매뉴얼 <br>(대용량 텍스트)<br>UX_RESTRICTIONS_LIMIT_CONTENT(32)<br>UX_RESTRICTIONS_LIMIT_STRING_LENGTH(4) |
|---|---|---|---|---|---|---|---|---|---|
| Parking | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Driving | ✅ | ✅ | ✅ | ⛔ | ⛔ | ⛔ | ✅ | ⛔ | ⛔ |

#### 그외 지역

| 구분 | 키보드<br>UX_RESTRICTIONS_NO_KEYBOARD(8) | 다이얼패드<br>UX_RESTRICTIONS_NO_DIALPAD(1) | 검색<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | 미디어 앱 로그인,<br>설정,<br>검색<br>UX_RESTRICTIONS_NO_SETUP(64)<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | 디지털키,<br>도어,<br>시트,<br>차량 설정<br>UX_RESTRICTIONS_NO_SETUP(64) | 동영상 시청<br>UX_RESTRICTIONS_NO_VIDEO(16) | 내비게이션 검색<br>UX_RESTRICTIONS_NO_KEYBOARD(8)<br>UX_RESTRICTIONS_NO_FILTERING(2) | QR 로그인<br>UX_RESTRICTIONS_NO_SETUP(64) | 매뉴얼 <br>(대용량 텍스트)<br>UX_RESTRICTIONS_LIMIT_CONTENT(32)<br>UX_RESTRICTIONS_LIMIT_STRING_LENGTH(4) |
|---|---|---|---|---|---|---|---|---|---|
| Parking | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| Driving | ✅ | ✅ | ✅ | ✅ | ⛔ | ⛔ | ✅ | ⛔ | ⛔ |

> **정보**
>
> 주행 규제 상태에서의 미디어 재생 제한 및 입력 처리 원칙은 [주행 안전을 고려한 설계](./01-driver-distraction-guideline.md)을 참고하시기 바랍니다.
