# 새 시나리오 설계 (v2)

기준 문서: [`ProjectPlan/new-scenario.md`](../ProjectPlan/new-scenario.md). 이 문서는 그 다섯 시나리오를 앱 구조로 옮긴 설계입니다. SDK 사실의 출처는 Pleos 문서와 AAR 확인 결과입니다.

## 바뀌는 전제

| 이전 (W01~W10) | v2 |
| --- | --- |
| 혼자 주차 중 내 이야기를 준비하고 끝나면 모두 비운다 | 프로필·화제·함께한 길 기록을 **기기에 저장하고 쌓는다** |
| 역할만 고른다 | 앱에 들어올 때마다 **나**를 고르고, 모드마다 **동승자**만 고른다 |
| 키보드 입력 | **STT SDK** 음성 입력, 전사 결과는 고칠 수 있다 |
| 주행 중에는 일반 안내 최대 1회 | 고속도로 진입·정체 때 **TTS로 능동 추천** |
| 없음 | 운전 중 **Gleo AI**로 사이길 기능을 음성 호출 |

기존 `Session` 상태 기계, `Method`·`Purpose`·`Known` catalog, W01~W10 화면은 삭제합니다.

## SDK로 정해진 제약

- Gleo Interface는 function calling만 지원합니다. 앱이 Gleo에 생성을 요청하고 답을 받는 API는 없습니다. 그래서 **추천 내용은 앱 안의 규칙 엔진이 만들고**, Gleo는 그 결과(`data`)를 바탕으로 운전자에게 말해 줍니다.
- `assets/schema.json`의 `actionCalls`는 최대 3개, description은 영어, 응답은 7초 안에 `APP_ACTION_RESPONSE` broadcast로 보냅니다.
- LLM SDK는 공모전 금지이고 에뮬레이터 v2.1.0에서 제공이 끝났습니다.
- NaviHelper에는 "지금 고속도로" 필드가 없습니다. `TBTInfo.type`의 `TURN_ENTER_IC_*`와 `RouteStateInfo`의 혼잡 구간으로 추정합니다. 에뮬레이터는 경로 주행을 시뮬레이션하지 못하므로 debug 트리거를 둡니다.
- DO를 선언하지 않은 앱은 Parking에서만 화면을 쓸 수 있습니다. SAIGIL은 `MainActivity`에 `distractionOptimized`를 선언하고, 주행 중에는 터치할 곳이 없는 **주행 보드**만 그립니다. 추천은 음성으로 말하고 보드에 같은 내용을 크게 보여 줍니다.

## Data shape

모두 `LibraryStore`가 `filesDir/library.json` 하나로 저장합니다(kotlinx.serialization). 쓰기는 임시 파일에 쓴 뒤 rename합니다.

```kotlin
@JvmInline value class PersonId(val value: String)
@JvmInline value class TopicId(val value: String)

data class Person(               // "나"도 동승자도 같은 방식으로 등록한다. 특별한 프로필은 없다
    val id: PersonId,
    val name: String,
    val places: List<String>,     // 자주 가는 곳. 시나리오 1의 "장소 등"
    val interests: List<String>,
)

data class Pair private constructor(val first: PersonId, val second: PersonId) {
    companion object { fun of(a: PersonId, b: PersonId): Pair? }  // 정렬해 (a,b) == (b,a). 같은 사람이면 null
}

enum class Relation { Parent, Child, Partner, Sibling, Friend, Other }

data class Tie(val from: PersonId, val to: PersonId, val relation: Relation)  // to는 from의 relation

data class Topic(
    val id: TopicId,
    val text: String,             // 화제 거리 원문 (STT 전사 후 사용자가 확인한 문장)
    val pair: Pair,               // 이 화제를 나눌 두 사람. 화제는 정확히 한 쌍에 속한다
    val addedAt: Long,
    val usedAt: Long?,            // 추천에서 골랐거나 함께한 길에서 썼다고 표시한 시각
)

enum class Flow { Smooth, Okay, Awkward }

data class Ride(                  // 함께한 길 하나. 쌍의 관계 데이터는 Ride 목록에서 파생한다
    val me: PersonId,
    val companion: PersonId,
    val at: Long,
    val flow: Flow,
    val discovered: List<String>, // 새로 발견한 관심사. 저장 시 companion.interests에도 더한다
)

data class Riders private constructor(val me: PersonId, val companion: PersonId)  // 저장하지 않는 지금의 쌍과 방향

data class Library(val people: List<Person>, val ties: List<Tie>, val topics: List<Topic>, val rides: List<Ride>)
```

- **지금의 나(`me`)는 저장하지 않습니다.** `SaigilApplication.me`에 프로세스 메모리로만 두므로 앱에 들어올 때마다 다시 고릅니다. 화면 모델이 새로 만들어질 때(activity가 새로 생성될 때)도 비웁니다.
- **관계는 쌍마다 방향을 가진 `Tie` 두 개로 저장합니다.** `Parent`를 a→b로 저장하면 b→a에는 `Child`가 함께 저장되고, `Partner`·`Sibling`·`Friend`·`Other`는 양쪽이 같습니다. 존댓말은 me→companion이 `Parent`일 때만이고, 관계가 없으면 반말입니다.
- 이전 `library.json`은 읽지 못하면 `LibraryStore`가 `library.json.unreadable`로 옮기고 빈 라이브러리로 시작합니다. 마이그레이션은 없습니다.

관계 데이터를 따로 저장하지 않고 `Ride`에서 파생하는 이유: 같은 사실을 두 곳에 두면 어긋납니다. `Bond(pair) = rides.filter { 같은 쌍 }`에서 횟수, 최근 흐름, 발견한 관심사를 읽습니다.

## 추천 엔진 (`recommend/`, 순수 함수, Android 의존 없음)

| 시나리오 | 함수 | 입력 | 출력 |
| --- | --- | --- | --- |
| 2 주제 말하기 | `openers(topic, pair, library)` | 사용자가 말한 주제 | 서로 다른 접근 3장 (`Approach`: 질문하기, 내 경험 나누기, 함께 기뻐하기, 경청하기 중). 각 카드에 첫마디와 이어 가기 |
| 3 맥락 말하기 | `topicsFor(context, pair, library)` | 지금 상황 한두 문장 | 저장된 화제 상위 3개와 고른 이유 |
| 4 능동 추천 | `proactive(situation, pair, library, said)` | `Situation.Highway(remainingKm)` 또는 `Situation.Jam(lengthM, avgSpeedKmh)` | `Suggestion` 하나(`SavedTopic`, `Trend`, `BalanceGame(a, b)`). 고속도로는 저장 화제 → 요즘 화제 → 밸런스 게임, 정체는 밸런스 게임 → 저장 화제 → 요즘 화제 순. 같은 주행에서 이미 말한 것은 건너뜀 |

- 첫마디 템플릿은 손으로 쓴 catalog이고 `{topic}` 자리에 주제를 넣습니다. 상대를 부르는 말투는 me→companion `Tie`로 정합니다. 동승자가 내 `Parent`면 존댓말, 그 외에는 반말입니다.
- `topicsFor`와 `proactive`는 **Pair(me, companion)에 속한 화제만** 봅니다. 누가 저장했든 같은 쌍이면 찾고, 다른 쌍의 화제는 나오지 않습니다.
- 맥락 매칭은 형태소 분석 없이 **한글 글자 bigram 겹침**으로 점수를 냅니다. 화제 원문, 동승자 관심사, 장소를 비교하고, 최근에 쓴 화제는 감산합니다.
- 최근 함께한 길의 흐름이 `Awkward`면 가벼운 접근(질문하기, 밸런스 게임)을 앞에 둡니다.
- 요즘 화제와 밸런스 게임은 앱에 들어 있는 손으로 쓴 목록입니다. 네트워크로 가져오지 않습니다.

## 화면 상태 (`Screen`, reducer)

```
(앱 진입) ─▶ WhoAmI ─ 프로필 고르기 ─▶ Home
                 └─ 프로필 추가 ─▶ PersonEdit(forMe) ─▶ Home (만든 프로필이 나)
Home ── 나: 이름 · 바꾸기 ──▶ WhoAmI
     ├─ 입력 모드 ──▶ People ─▶ PersonEdit(draft) ─▶ People
     │                    └─ 화제 저장 ─▶ PickCompanion(SaveTopic) ─▶ TopicCapture(riders) ─▶ People
     ├─ 추천 모드 ──▶ PickCompanion(Recommend) ─▶ ChooseInput ─▶ Listen(Topic|Context) ─▶ Openers | SavedTopics
     └─ 함께한 길 ──▶ PickCompanion(Reflect) ─▶ Reflect(flow, discovered) ─▶ Home
Driving (주행 제한 중이면 어느 화면이든 이 화면. 입력 없음, 음성 추천이 켜져 있다는 표시만)
```

- `PickCompanion`은 나를 고정해 보여 주고 동승자만 고릅니다. 관계가 없는 쌍이면 "OO 님은 나에게 어떤 사이인가요?"를 묻고 고르기 전에는 넘어가지 않습니다. 관계가 있는 쌍은 묻지 않고 "관계 바꾸기"만 둡니다.

- `Listen`은 STT 상태(`Idle | Listening(partial) | Heard(text) | Failed(reason)`)를 가집니다. 전사 결과는 텍스트 필드로 고칠 수 있고, 인식에 실패하면 키보드로 입력할 수 있습니다.
- 추천 결과 화면에 들어가면 그 쌍(me, companion)을 **active ride**로 둡니다. 능동 추천은 active ride의 쌍을, Gleo 호출은 같은 나의 active ride 동승자를 기본값으로 씁니다.

## 주행 중 능동 추천 (`drive/`)

- `RideCompanion`이 NaviHelper 신호를 `Situation`으로 바꿉니다.
  - `Highway`: `onTBTInfo`에 `TURN_ENTER_IC_*`가 나타나 거리가 500 m 이하일 때. 남은 거리는 마지막 `onDrivingInfo`의 목적지 거리입니다.
  - `Jam`: `RouteStateInfo` 혼잡 구간이 `SLOW`·`HEAVY`이고 offset이 300 m 이하일 때. 구간 길이와 평균 속도를 담습니다.
  - debug: `adb shell am broadcast -a com.gomsoo.saigil.DEBUG_TRIGGER --es trigger LongDrive|Jam [--ei remaining_km 42] [--ei jam_m 2300 --ei avg_kmh 12] -p com.gomsoo.saigil`.
- `Drive`가 앱 범위 `StateFlow<DriveBoard(situation, latest)>`를 냅니다. 상황은 신호가 올 때마다 바뀌고, `latest`는 실제로 말한 추천입니다. 주행 보드가 이 값을 그립니다.
- 같은 종류의 상황은 10분에 한 번만 말합니다. active ride가 없으면 말하지 않습니다.
- 말하기는 TextToSpeech SDK `Mode.ON_DEVICE`, `Locale.KOREA`.

## Gleo function (`assets/schema.json`, component `service`)

| name | parameters | 하는 일 |
| --- | --- | --- |
| `RECOMMEND_OPENER` | `topic` (필수), `companion` | 시나리오 2. 주제를 풀어갈 첫마디 3개를 `data`로 돌려준다 |
| `RECOMMEND_SAVED_TOPIC` | `context` (필수), `companion` | 시나리오 3. 맥락에 맞는 저장 화제 3개를 돌려준다 |
| `SAVE_TOPIC` | `topic` (필수), `companion` | 시나리오 1을 주행 중에. 화제를 저장한다 |

나는 앱 화면에서 고른 `SaigilApplication.me`입니다. `companion`은 프로필 이름이고, 없으면 나의 active ride 동승자를 씁니다. 나를 모르거나 동승자를 정할 수 없으면 `asking`으로 되묻습니다. `SAVE_TOPIC`은 Pair(me, companion)에 저장합니다. 수신은 manifest `BroadcastReceiver`가 `goAsync()`로 처리하고 7초 안에 응답합니다.

## 권한

`pleos.car.permission.STT_SERVICE`, `pleos.car.permission.TTS_SERVICE`, NaviHelper의 `NAVI_ROUTE`. 정확한 권한 문자열은 AAR manifest를 따릅니다.

## 구현 메모 (2026-11-15)

구현하면서 위 설계와 달라진 점입니다. 클래스 관계는 [`class-diagram.md`](./class-diagram.md)에 있습니다.

- **Driving은 `Screen`의 한 상태가 아닙니다.** `Model(screen, vehicle)`에서 `vehicle`이 `Restricted`이면 UI가 Driving 안내를 그립니다. 아래 화면을 지우지 않으므로 주차하면 하던 화면으로 돌아옵니다.
- **`Listen`의 `Heard(text)`는 따로 두지 않았습니다.** 최종 전사 결과는 편집 가능한 `text`에 바로 들어가고 STT 상태는 `Idle`로 돌아갑니다.
- **`topicsFor`와 `proactive`는 `now`를 더 받습니다.** 최근에 쓴 화제를 감산하려면 현재 시각이 필요해서입니다.
- **저장 화제 카드를 고르면** 그 화제의 `usedAt`을 기록하고 같은 화제의 첫마디 3장을 보여 줍니다.
- **Gleo 호출에 나가 정해지지 않았거나, `companion`도 같은 나의 active ride도 없으면** `asking` 상태로 되묻습니다. 나와 active ride는 프로세스 메모리에만 있으므로 앱 프로세스가 없을 때 온 호출도 이 경우에 해당합니다.
- **관계 질문은 함께한 길 모드에서도 같습니다.** 세 모드가 같은 `PickCompanion` 화면을 쓰므로, 처음 고른 쌍이면 함께한 길에서도 관계를 묻습니다.
- **TextToSpeech `addEventListener`는 `initialize` 성공 뒤에만 부를 수 있습니다.** 먼저 부르면 SDK 내부에서 `UninitializedPropertyAccessException`이 납니다. SpeechToText `addListener`도 같은 순서로 부릅니다.
- **에뮬레이터 STT:** ON_DEVICE 인식은 시작되고 Speech Service가 마이크를 엽니다. 하지만 호스트에서 재생한 한국어 음성으로는 결과가 오지 않았습니다. 키보드 입력으로 같은 흐름을 끝낼 수 있습니다.
