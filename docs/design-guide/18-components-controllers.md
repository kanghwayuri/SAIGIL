<!-- 원문: https://document.pleos.ai/docs/connect/guide/docs-design/components/controllers -->
<!-- 가져온 날짜: 2026-11-12. 원문이 바뀌었을 수 있으니 중요한 결정 전에는 원문을 확인할 것. -->

# Controllers

Controllers는 최솟값과 최댓값을 가지며, 범위 내 volume, temporature 등과 같은 속성을 슬라이더 또는 클릭을 통해 조작 가능한 컴포넌트입니다.

![Controller.svg](images/7694af7268.svg)

**States** <br>

| State | Value | Description |
|---|---|---|
| Pressed or Dragging | True / False | Handle 영역을 Pressed 시작할 때의 상태 |

<br>

**Options** <br>

| Options | Value | Description |
|---|---|---|
| Disabled | True / False | Min 또는 Max value에 도달했을 때 True로 전환 |
| MinValue | Number | 슬라이더에 지정하는 최솟값 |
| MaxValue | Number | 슬라이더에 지정하는 최댓값 |
| Step | Number | 슬라이더의 값을 제어하는 단계 지정 값 |

<br>

---

<br>

## Continuous Slider

### Anatomy

![ContinuousSlider_Anatomy.svg](images/c6f4530210.svg)<br>

**1. Root** : Slider 컴포넌트를 감싸는 최상위 컨테이너 영역 <br> **2. Control** : Slider의 track과 handle을 감싸는 컨테이너 영역 <br> **3. Handle** : 사용자가 드래그 할 수 있는 컨트롤 요소 <br> **4. Track** : Handle이 움직일 수 있는 영역을 표현하는 요소 <br> **5. Icon**(Prefix) : Label에 대한 시각적 부연설명을 하는 아이콘 영역(Optional) <br> **6. Icon**(Suffix) : Label에 대한 시각적 부연설명을 하는 아이콘 영역 <br> **7. Stepper** : 버튼을 눌러 handle을 제어할 수 있는 컨트롤 요소<br>

---

### Behaviors

#### Slider

**Basic** <br> 일반적인 슬라이더로 사용됩니다.

![Slider_Basic.svg](images/c5fbb32331.svg) <br>

**Floating** <br> 밝은 배경 또는 배경과 구분이 필요한 곳에서 사용됩니다.

![Slider_Float.svg](images/54ec24166f.svg) <br>

#### Buttons

![Slider_Button.svg](images/75afa09b7e.svg)<br>

---

#### Volume

**Max** 100% <br>

![Slider_Max.svg](images/cd407b8b73.svg)<br>

**High** 76~100% <br>

![Slider_High.svg](images/7341084910.svg)<br>

**Mid** 26~75% <br>

![Slider_Mid.svg](images/7b99c81692.svg)<br>

**Low** 1~25% <br>

![Slider_Low.svg](images/e67d0d0e54.svg)<br>

**Min** 0% <br>

![Slider_Min.svg](images/0408bf42ab.svg)<br>

---

#### Optional

**Label** True/False <br>

![Slider_Label.svg](images/d3270aa8de.svg)<br>

**Icon** True/False <br>

![Slider_Icon.svg](images/d2f2ea6ec1.svg)<br>

#### Control

![Slider_Control.svg](images/88faa5e92a.svg)

<br>

---

<br>

## Centered Slider

### Anatomy

![CenteredSlider_Anatomy.svg](images/bd972d5954.svg)<br>

**1. Root** : Slider 컴포넌트를 감싸는 최상위 컨테이너 영역 <br> **2. Control** : Slider의 track과 handle을 감싸는 컨테이너 영역 <br> **3. Handle** : 사용자가 드래그 할 수 있는 컨트롤 요소 <br> **4. Track** : Handle이 움직일 수 있는 영역을 표현하는 요소 <br> **5. Stepper** : 버튼을 눌러 handle을 제어할 수 있는 컨트롤 요소<br>

---

### Behaviors

**Slider**

![Slider_State.svg](images/f791f3430f.svg)<br> ![Slider_Control.svg](images/cd7e33fa48.svg)<br>

**Track**

![Slider_Track.svg](images/55e19d3417.svg)<br> ![Slider_Control.svg](images/9484c51ba4.svg)

<br>

---

<br>

## Stepper

### Anatomy

![Stepper_Anatomy.svg](images/e669a94f36.svg)<br>

**1. Root** : Stepper 컴포넌트를 감싸는 최상위 컨테이너 영역 <br> **2. Current value** : Current value와 Step 버튼을 감싸는 컨테이너 영역 <br> **3. i++** : Stepper 값이 증가하는 컨트롤 요소 <br> **4. i--** : Stepper 값이 감소하는 컨트롤 요소 <br>

<br>

### Behaviors

![Stepper_State.svg](images/ea1ef2fc2f.svg)<br> ![Stepper_Control.svg](images/b3d63beade.svg)<br>
