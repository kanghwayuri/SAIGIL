package com.gomsoo.saigil.session

/** W09 purpose. */
enum class Purpose(val label: String) {
    Light("가볍게 시작하기"),
    Continue("이전 이야기 이어 가기"),
    Listen("고민 들어 주기"),
    Celebrate("좋은 일 함께 기뻐하기"),
}

/** W09 known topic. Only the kind is chosen, never its content. */
enum class Known(val label: String) {
    TodayEvent("오늘 있었던 일"),
    GoodNews("좋은 소식"),
    Earlier("전에 들은 이야기"),
}

/** W01 '평소 모습' chips, plus [UNSURE]. */
enum class Style(val label: String) { Lively("활발"), Calm("차분") }

/** W01 '관심사' chips, plus [UNSURE]. */
enum class Interest(val label: String) { Music("음악"), Sport("운동"), Travel("여행"), Food("음식") }

const val UNSURE = "모르겠어요"

/** The W01 companion details the rules read. Null when skipped or [UNSURE]. */
data class Profile(val style: Style? = null, val interest: Interest? = null)

fun profileOf(details: Map<StartField, String>): Profile = Profile(
    style = Style.entries.firstOrNull { it.label == details[StartField.CompanionStyle] },
    interest = Interest.entries.firstOrNull { it.label == details[StartField.CompanionInterest] },
)

/** Declaration order is the W10 left-to-right order. */
enum class Approach(val label: String) {
    Cheer("함께 기뻐하기"),
    Ask("질문하기"),
    Listen("경청하기"),
    Share("내 경험 나누기"),
}

/** Information a card can only use when the user gave it. */
enum class Need { Topic, Interest }

/** F16. The last line of every card's [Method.respond]. */
const val RESPECT = "짧게 대답하면 더 묻지 않아요."

/** F15. Shown on W05 with the chosen card. */
const val STOP = "이야기하고 싶어 하지 않으면 멈춰요. 다음에 해도 괜찮아요."

/**
 * The hand-written card catalog. Each entry is the card id, its text, and its rule.
 * An empty [purposes] makes a fallback card that fits any purpose without a bonus.
 * The text never names the companion, invents the user's experience, or guesses feelings.
 */
enum class Method(
    val approach: Approach,
    val purposes: Set<Purpose>,
    val title: String,
    val why: String,
    val start: String,
    private val casual: String,
    private val polite: String,
    private val follow: String,
    val needs: Need? = null,
    val suits: Style? = null,
) {
    CheerFirst(
        approach = Approach.Cheer,
        purposes = setOf(Purpose.Celebrate),
        title = "좋았던 순간 함께 기뻐하기",
        why = "좋은 일이 있던 날은 함께 기뻐하는 말로 시작하기 좋아요.",
        start = "반가움을 먼저 짧게 전해요.",
        casual = "소식 들었어. 정말 잘됐다!",
        polite = "소식 들었어요. 정말 잘됐어요!",
        follow = "더 말해 주면 좋았던 순간을 들어요. 점수나 다음 목표 이야기는 꺼내지 않아요.",
    ),
    AskOneMore(
        approach = Approach.Ask,
        purposes = setOf(Purpose.Continue, Purpose.Celebrate),
        title = "말해 준 것에서 하나만 더 묻기",
        why = "먼저 들은 이야기가 있으면 하나만 더 물어 이어 가기 좋아요.",
        start = "들은 이야기 하나를 짚어 물어요.",
        casual = "지난번에 말한 그거, 어떻게 됐어?",
        polite = "지난번에 말씀하신 그거, 어떻게 됐어요?",
        follow = "대답하면 그 말에 짧게 반응해요. 질문은 하나로 끝내요.",
        needs = Need.Topic,
    ),
    AskEasy(
        approach = Approach.Ask,
        purposes = emptySet(),
        title = "대답하기 쉬운 것 하나만 묻기",
        why = "짧게 답할 수 있는 질문 하나는 부담이 적어요.",
        start = "한두 마디로 답할 수 있게 물어요.",
        casual = "오늘 하루 어땠어?",
        polite = "오늘 하루 어떠셨어요?",
        follow = "대답에 짧게 맞장구쳐요. 더 하고 싶은 말이 있으면 들어요.",
        suits = Style.Lively,
    ),
    AskInterest(
        approach = Approach.Ask,
        purposes = setOf(Purpose.Light),
        title = "좋아하는 것에서 시작하기",
        why = "좋아하는 것 이야기는 가볍게 시작하기 좋아요.",
        start = "알고 있는 관심사 하나를 물어요.",
        casual = "요즘도 그거 자주 해?",
        polite = "요즘도 그거 자주 하세요?",
        follow = "신나서 말하면 들어요. 잘 모르는 부분은 물어봐도 괜찮아요.",
        needs = Need.Interest,
    ),
    AskPermission(
        approach = Approach.Ask,
        purposes = setOf(Purpose.Listen),
        title = "들어도 될지 먼저 묻기",
        why = "고민은 말할지 말지를 상대가 정하게 두면 편해요.",
        start = "이야기할 마음이 있는지 먼저 물어요.",
        casual = "신경 쓰이는 일 있으면, 내가 들어도 될까?",
        polite = "신경 쓰이는 일 있으시면, 제가 들어도 될까요?",
        follow = "괜찮다고 하면 들어요. 아니라고 하면 바로 넘어가요.",
    ),
    ListenBack(
        approach = Approach.Listen,
        purposes = setOf(Purpose.Continue, Purpose.Celebrate, Purpose.Listen),
        title = "이해한 내용을 확인하며 듣기",
        why = "들은 말을 짧게 되짚으면 잘 듣고 있다는 게 전해져요.",
        start = "상대가 꺼낸 말을 짧게 되짚어요.",
        casual = "아, 그래서 그렇게 된 거구나.",
        polite = "아, 그래서 그렇게 된 거군요.",
        follow = "틀리게 이해했으면 고쳐 달라고 해요. 내 생각은 물어볼 때 말해요.",
    ),
    ListenFirst(
        approach = Approach.Listen,
        purposes = setOf(Purpose.Listen),
        title = "해결보다 먼저 들어 주기",
        why = "고민을 꺼낼 때는 해결책보다 들어 주는 게 먼저예요.",
        start = "조언 대신 들을 준비가 됐다고 전해요.",
        casual = "말하고 싶은 만큼만 말해. 듣고 있을게.",
        polite = "말씀하고 싶은 만큼만 하세요. 듣고 있을게요.",
        follow = "말이 끊겨도 기다려요. 조언은 상대가 원할 때만 해요.",
        suits = Style.Calm,
    ),
    ListenQuiet(
        approach = Approach.Listen,
        purposes = emptySet(),
        title = "먼저 말할 틈 두기",
        why = "조용한 시간이 있으면 상대가 먼저 말을 꺼내기 쉬워요.",
        start = "짧게 한마디 건네고 기다려요.",
        casual = "하고 싶은 말 생기면 편하게 해.",
        polite = "하고 싶은 말씀 생기면 편하게 하세요.",
        follow = "말을 꺼내면 들어요. 조용하면 조용한 대로 괜찮아요.",
        suits = Style.Calm,
    ),
    ShareScene(
        approach = Approach.Share,
        purposes = setOf(Purpose.Light),
        title = "내게 있었던 일 하나 먼저 꺼내기",
        why = "내 이야기를 먼저 꺼내면 상대가 답할 부담이 줄어요.",
        start = "준비한 장면을 한 줄로 먼저 꺼내요.",
        casual = "오늘 나 이런 일이 있었어.",
        polite = "오늘 저 이런 일이 있었어요.",
        follow = "대답을 바라지 않아도 돼요. 상대가 말을 꺼내면 들어요.",
    ),
    ShareSimilar(
        approach = Approach.Share,
        purposes = setOf(Purpose.Continue, Purpose.Listen),
        title = "비슷했던 내 경험 나누고 멈추기",
        why = "비슷한 경험을 짧게 나누면 혼자가 아니라는 게 전해져요.",
        start = "준비한 내 경험을 짧게 꺼내고 멈춰요.",
        casual = "나도 비슷한 일이 있었어.",
        polite = "저도 비슷한 일이 있었어요.",
        follow = "내 이야기는 짧게 끝내요. 상대 이야기로 돌아가요.",
    ),
    ;

    /** An example first line in the register the speaker uses toward the companion. */
    fun firstWords(role: Role): String = if (role == Role.Child) polite else casual

    val respond: List<String> get() = listOf(follow, RESPECT)

    /** Sharing needs something the user really lived, so it goes through W02 instead of a written example. */
    val needsOwnStory: Boolean get() = approach == Approach.Share
}

/** One W10 card. [reasons] name only the inputs that picked it, such as "목적 · 좋은 일 함께 기뻐하기". */
data class Recommended(val method: Method, val reasons: List<String>)

/** Three cards with distinct [Approach]es in [Approach] order. A skipped purpose scores as [Purpose.Light]. */
fun recommend(purpose: Purpose?, known: Known?, profile: Profile): List<Recommended> {
    val scored = purpose ?: Purpose.Light
    fun Need.met() = when (this) {
        Need.Topic -> known != null
        Need.Interest -> profile.interest != null
    }
    fun score(method: Method) =
        (if (scored in method.purposes) 2 else 0) +
            (if (method.needs != null) 1 else 0) +
            (if (method.suits != null && method.suits == profile.style) 1 else 0)

    return Method.entries
        .filter { (it.purposes.isEmpty() || scored in it.purposes) && it.needs?.met() != false }
        .groupBy { it.approach }
        .values
        .map { candidates -> candidates.maxBy(::score) }
        .sortedWith(compareByDescending(::score).thenBy { it.approach.ordinal })
        .take(3)
        .sortedBy { it.approach.ordinal }
        .map { method ->
            Recommended(
                method,
                buildList {
                    if (purpose != null && purpose in method.purposes) add("목적 · ${purpose.label}")
                    when (method.needs) {
                        Need.Topic -> known?.let { add("소재 · ${it.label}") }
                        Need.Interest -> profile.interest?.let { add("관심사 · ${it.label}") }
                        null -> Unit
                    }
                    if (method.suits != null && method.suits == profile.style) add("평소 모습 · ${method.suits.label}")
                },
            )
        }
}
