<!-- 원문: https://document.pleos.ai/docs/connect/guide/docs-design/components/actions/text-button -->
<!-- 가져온 날짜: 2026-11-12. 원문이 바뀌었을 수 있으니 중요한 결정 전에는 원문을 확인할 것. -->

# Text button

#### 텍스트 버튼은 부가 정보에 대한 링크를 위해 주로 사용되며, 앞/뒤 아이콘을 통해 정보 전달력을 강화할 수 있습니다.

![text_button_anatomy.svg](images/f209c65520.svg)<br>

**1. Root** : 버튼을 감싸는 최상위 컨테이너 영역 <br> **2. Label** : 버튼 레이블 <br> **3. Icon**(Prefix) : Label에 대한 시각적 부연설명을 하는 아이콘 영역 <br> **4. Icon**(Suffix) : Label에 대한 시각적 부연설명을 하는 아이콘 영역 <br>

---

## States

| State | Value | Description |
|---|---|---|
| Pressed | Boolean | 터치로 클릭 중인 상태일 때 True로 전환 |
| Disabled | Boolean | True일 경우, Button이 제어불가능한 상태 |
| Loading | Boolean | True일 경우, Button내 Loader가 표시되고 제어불가한 상태가 됨 |

![text_state.svg](images/aa1650b4e6.svg)

<br>

---

## Properties

| State | Value | Description |
|---|---|---|
| Size | Medium, Small | Button의 사이즈 옵션을 선택하여 사용할 수 있습니다. |

![text_property.svg](images/212c675d83.svg)

<br>
