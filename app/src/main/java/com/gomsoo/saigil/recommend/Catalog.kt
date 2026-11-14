package com.gomsoo.saigil.recommend

import com.gomsoo.saigil.library.Register

enum class Approach { Ask, ShareMine, Celebrate, Listen }

/** A first line in both registers. `{topic}` is replaced with what the user said. */
class OpenerTemplate(val approach: Approach, private val casual: String, private val formal: String, val followUp: String) {
    fun first(topic: String, register: Register): String =
        (if (register == Register.Formal) formal else casual).replace("{topic}", topic)
}

/** Two templates per approach. Lines never claim an experience the user did not tell the app about. */
val openerTemplates: Map<Approach, List<OpenerTemplate>> = listOf(
    OpenerTemplate(
        Approach.Ask,
        "{topic} 얘기 좀 해 줄래? 요즘 어때?",
        "{topic} 얘기 좀 들려주실래요? 요즘 어떠세요?",
        "대답에서 나온 단어 하나를 받아 \"그건 어땠어?\"처럼 한 번 더 물어요.",
    ),
    OpenerTemplate(
        Approach.Ask,
        "{topic} 하면 제일 먼저 뭐가 떠올라?",
        "{topic} 하면 제일 먼저 뭐가 떠오르세요?",
        "정답이 없는 질문이에요. 어떤 답이 나와도 이유를 한 번 더 물어요.",
    ),
    OpenerTemplate(
        Approach.ShareMine,
        "{topic} 얘기 나오니까 나도 생각나는 게 있어.",
        "{topic} 얘기 나오니까 저도 생각나는 게 있어요.",
        "떠오른 내 경험을 한두 문장으로 짧게 말하고, 상대에게도 비슷한 일이 있었는지 물어요.",
    ),
    OpenerTemplate(
        Approach.ShareMine,
        "나 요즘 {topic} 생각을 자주 해.",
        "저 요즘 {topic} 생각을 자주 해요.",
        "왜 그 생각이 났는지 내 이유를 먼저 말하면 상대도 자기 이야기를 꺼내기 쉬워요.",
    ),
    OpenerTemplate(
        Approach.Celebrate,
        "{topic} 쪽으로 좋은 일 있으면 나도 같이 기뻐하고 싶어.",
        "{topic} 쪽으로 좋은 일 있으시면 저도 같이 기뻐하고 싶어요.",
        "좋았던 일을 들으면 \"그때 기분이 어땠어?\"처럼 감정을 물어 함께 기뻐해요.",
    ),
    OpenerTemplate(
        Approach.Celebrate,
        "{topic}, 요즘 잘 돼 가? 자랑할 거 있으면 해 줘.",
        "{topic}, 요즘 잘 돼 가세요? 자랑하실 거 있으면 들려주세요.",
        "작은 자랑이라도 \"그걸 어떻게 했어?\"처럼 구체적으로 물으며 칭찬해요.",
    ),
    OpenerTemplate(
        Approach.Listen,
        "{topic} 얘기, 오늘은 내가 들을게. 편하게 말해 줘.",
        "{topic} 얘기, 오늘은 제가 들을게요. 편하게 말씀해 주세요.",
        "끼어들지 말고 고개를 끄덕이며, 상대가 한 말을 짧게 되짚어 줘요.",
    ),
    OpenerTemplate(
        Approach.Listen,
        "{topic}에 대해 네 생각이 궁금했어.",
        "{topic}에 대해 어떻게 생각하시는지 궁금했어요.",
        "조언하고 싶어도 참고, \"그다음엔?\"으로 이야기를 이어 가게 해요.",
    ),
).groupBy { it.approach }

/** Light, timeless prompts. Nothing here states a fact that could go stale. */
val trending: List<String> = listOf(
    "요즘 즐겨 보는 영상이나 프로그램이 있는지 물어봐요.",
    "최근에 가 본 곳 중 다시 가고 싶은 곳을 이야기해 봐요.",
    "요즘 자주 듣는 노래를 하나씩 말해 봐요.",
    "올해가 가기 전에 꼭 해 보고 싶은 일을 이야기해 봐요.",
    "최근에 산 것 중 제일 만족한 물건을 이야기해 봐요.",
    "요즘 하루 중 제일 좋은 시간이 언제인지 물어봐요.",
    "어릴 때 좋아했던 간식을 하나씩 떠올려 봐요.",
    "요즘 날씨에 가 보고 싶은 곳을 이야기해 봐요.",
)

val balanceGames: List<String> = listOf(
    "평생 여름만 살기, 평생 겨울만 살기 중에 고른다면요?",
    "산으로 여행 가기, 바다로 여행 가기 중에 고른다면요?",
    "짜장면, 짬뽕 중에 하나만 먹을 수 있다면요?",
    "아침 일찍 일어나기, 밤늦게 자기 중에 고른다면요?",
    "여행 계획을 꼼꼼히 짜기, 그날 기분대로 다니기 중에 고른다면요?",
    "말하는 고양이, 날아다니는 강아지 중에 키운다면요?",
    "과거로 한 번 가기, 미래로 한 번 가기 중에 고른다면요?",
    "평생 라면 끊기, 평생 치킨 끊기 중에 고른다면요?",
    "혼자 조용한 휴가, 여럿이 시끌벅적한 휴가 중에 고른다면요?",
    "답장 빠른 친구, 전화 자주 하는 친구 중에 고른다면요?",
)
