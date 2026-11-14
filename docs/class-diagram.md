# 사이길 클래스 다이어그램

기준: 2026-11-15 · 설계: [`scenario-v2-design.md`](./scenario-v2-design.md)

v2 시나리오(프로필·화제 저장, 대화 추천, 능동 음성 추천, Gleo 호출, 함께한 길) 구현의 클래스 관계입니다. 이전 `Session`·`Method` 구조는 삭제했습니다.

## 1. 저장 데이터 (`library/`)

```mermaid
classDiagram
    class Library {
        +List~Person~ people
        +List~Topic~ topics
        +List~Ride~ rides
        +upsert(Person) Library
        +add(Topic) Library
        +markUsed(TopicId, Long) Library
        +record(Ride) Library
        +bond(Riders) Bond
    }
    class Person {
        +PersonId id
        +String name
        +Relation relation
        +List~String~ places
        +List~String~ interests
    }
    class Topic {
        +TopicId id
        +String text
        +Set~PersonId~ about
        +Long addedAt
        +Long? usedAt
    }
    class Ride {
        +PersonId driver
        +PersonId passenger
        +Long at
        +Flow flow
        +List~String~ discovered
    }
    class Bond {
        +Int count
        +Flow? lastFlow
        +List~String~ discovered
    }
    class LibraryStore {
        +StateFlow~Library~ library
        +update(change) Library
    }
    Library *-- Person
    Library *-- Topic
    Library *-- Ride
    Library ..> Bond : derives from rides
    LibraryStore --> Library : filesDir/library.json
```

- `Relation.register`는 동승자가 `Parent`면 `Formal`(존댓말), 그 외에는 `Casual`(반말)입니다.
- 관계 데이터(`Bond`)는 저장하지 않고 `Ride` 목록에서 매번 계산합니다.
- `LibraryStore.update`는 임시 파일에 쓴 뒤 rename하고, 쓰기가 끝난 다음에 `StateFlow` 값을 바꿉니다.

## 2. 추천 엔진 (`recommend/`, 순수 Kotlin)

```mermaid
classDiagram
    class Recommend {
        <<functions>>
        +openers(topic, Riders, Library) List~Opener~
        +topicsFor(context, Riders, Library, now) List~TopicPick~
        +proactive(Trigger, Riders, Library, said, now) Suggestion?
    }
    class Opener {
        +Approach approach
        +String first
        +String followUp
    }
    class TopicPick {
        +Topic topic
        +List~Reason~ reasons
    }
    class Suggestion {
        +String key
        +String sentence
    }
    class Catalog {
        <<data>>
        openerTemplates: Approach x 2
        trending: 8
        balanceGames: 10
    }
    Recommend --> Catalog
    Recommend --> Opener
    Recommend --> TopicPick
    Recommend --> Suggestion
```

- `topicsFor`는 한글 글자 bigram 겹침(맥락 x3), 동승자 관심사·장소 겹침(+1), `about`에 동승자 포함(+2), 다른 사람 화제(-1), 3일 안에 쓴 화제(-2)로 점수를 냅니다.
- `proactive`는 저장 화제 → 요즘 이야깃거리 → 밸런스 게임 순서이고, `said`에 있는 key는 건너뜁니다.

## 3. 화면 (`screen/`, `ui/`)

```mermaid
classDiagram
    class SaigilViewModel {
        +StateFlow~Ui~ ui
        +send(Event)
    }
    class Model {
        +Screen screen
        +Restriction vehicle
    }
    class reduce {
        <<function>>
        (Model, Event, Library, Stamp) Next
    }
    class Next {
        +Model model
        +List~Effect~ effects
    }
    class Screen {
        <<sealed>>
        Home, People, PersonEdit, TopicCapture
        PickPair, ChooseInput, Listen
        Openers, SavedTopics, Reflect
    }
    class Effect {
        <<sealed>>
        Write: PutPerson, AddTopic, UseTopic, RecordRide
        BeginRide, StartListening, StopListening
    }
    class SpeechInput {
        <<interface>>
        +start(onHeard)
        +stop()
        +release()
    }
    class PleosSpeechInput
    SaigilViewModel --> reduce
    reduce --> Next
    Next --> Model
    Next --> Effect
    Model --> Screen
    SaigilViewModel --> SpeechInput
    PleosSpeechInput ..|> SpeechInput
    SaigilViewModel --> LibraryStore : Effect.Write
    SaigilViewModel --> Rides : Effect.BeginRide
```

- reducer는 순수 함수입니다. 시각과 새 id는 `Stamp`로 받고, 저장·음성 인식·active ride는 `Effect`로 돌려줍니다.
- `TopicCapture`와 `Listen`은 `Capture(stt, text)`를 가집니다. `stt`는 `Idle | Listening(partial) | Failed(reason)`이고, 전사 결과는 `text`에 들어가 사용자가 고칠 수 있습니다.
- 주행 제한(`Restriction.Restricted`)이면 UI는 어느 화면이든 Driving 안내를 그리고, reducer는 입력 이벤트를 무시하며 듣기를 멈춥니다. 주차하면 원래 화면으로 돌아옵니다.

## 4. 주행 중 능동 추천 (`drive/`)

```mermaid
classDiagram
    class SaigilApplication {
        +LibraryStore store
        +Rides rides
        +RideCompanion companion
    }
    class Rides {
        +StateFlow~ActiveRide?~ active
        +begin(Riders)
        +said(key)
    }
    class ActiveRide {
        +Riders riders
        +Set~String~ said
    }
    class RideCompanion {
        +start()
        +onTrigger(Trigger)
    }
    class decide {
        <<function>>
        (Trigger, ActiveRide?, Library, spokenAt, now) Decision
    }
    class DebugTriggerReceiver {
        <<debug only>>
    }
    SaigilApplication --> Rides
    SaigilApplication --> RideCompanion
    Rides --> ActiveRide
    RideCompanion --> decide
    RideCompanion --> NaviHelper : onTBTInfo, onRouteStateInfo
    RideCompanion --> TextToSpeech : ON_DEVICE, ko-KR
    DebugTriggerReceiver --> RideCompanion
```

- `longDriveFrom`: 첫 TBT가 `TURN_ENTER_IC_*`이고 500 m 이내면 `LongDrive`. `jamFrom`: `SLOW`·`HEAVY` 정체가 300 m 이내면 `Jam`.
- `decide`는 active ride가 없으면 `NoRide`, 같은 종류를 10분 안에 말했으면 `CoolingDown`입니다.
- active ride는 프로세스 메모리에만 있습니다. 첫 `BeginRide` 때 NaviHelper와 TTS를 연결합니다.

## 5. Gleo AI (`gleo/`)

```mermaid
classDiagram
    class GleoReceiver {
        +onReceive(Context, Intent)
    }
    class answer {
        <<function>>
        (Function, params, Library, ActiveRide?, Stamp) Answer
    }
    class Answer {
        +Status status
        +String message
        +JsonArray? data
        +Effect.Write? write
    }
    class Function {
        <<enum>>
        RECOMMEND_OPENER
        RECOMMEND_SAVED_TOPIC
        SAVE_TOPIC
    }
    GleoReceiver --> answer
    answer --> Answer
    GleoReceiver --> LibraryStore : Answer.write
    GleoReceiver ..> APP_ACTION_RESPONSE : broadcast within 6 s
```

- 함수 선언은 `app/src/main/assets/schema.json`에 있습니다. `companion`이 없으면 active ride의 동승자를 쓰고, 둘 다 없으면 `asking`으로 누구와 타는지 되묻습니다.
