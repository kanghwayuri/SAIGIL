import sys, html
OUT = sys.argv[1]
F = "Apple SD Gothic Neo"
DARK, MID, LINE, BOX, BG, ACC = "#1e3a2b", "#52665a", "#7fa58c", "#ffffff", "#f3f7f3", "#236947"

def text(x, y, s, size=20, weight=400, fill=DARK, anchor="start"):
    return f'<text x="{x}" y="{y}" font-family="{F}" font-size="{size}" font-weight="{weight}" fill="{fill}" text-anchor="{anchor}">{html.escape(s)}</text>'

def box(x, y, w, h, title, lines, accent=False, dashed=False):
    stroke = ACC if accent else LINE
    dash = ' stroke-dasharray="8 6"' if dashed else ''
    out = [f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="14" fill="{BOX}" stroke="{stroke}" stroke-width="{3 if accent else 2}"{dash}/>']
    out.append(text(x + 22, y + 38, title, 22, 800))
    for i, l in enumerate(lines):
        out.append(text(x + 22, y + 70 + i * 26, l, 16, 500, MID))
    return "\n".join(out)

def arrow(points, dashed=False):
    d = "M" + " L".join(f"{x},{y}" for x, y in points)
    dash = ' stroke-dasharray="8 6"' if dashed else ''
    return f'<path d="{d}" fill="none" stroke="{ACC}" stroke-width="2.5"{dash} marker-end="url(#ah)"/>'

def svg(w, h, body):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">
<defs><marker id="ah" markerUnits="userSpaceOnUse" markerWidth="16" markerHeight="14" refX="14" refY="7" orient="auto"><path d="M0,0 L16,7 L0,14 z" fill="{ACC}"/></marker></defs>
<rect width="{w}" height="{h}" fill="{BG}"/>
{body}
</svg>'''

def fig4():
    W, H = 2000, 1060
    b = [text(60, 80, "Pleos SDK 연동 구조", 36, 800), text(60, 118, "실선은 주차 중 흐름, 점선은 주행 중 흐름입니다.", 18, 500, MID)]
    # column headers
    for x, s in [(60, "차량·Pleos 입력"), (740, "사이길 앱"), (1420, "출력")]:
        b.append(text(x, 176, s, 20, 800, ACC))
    bw, bh = 520, 130
    L = [("SpeechToText SDK", ["화제·주제·상황 음성 입력", "ON_DEVICE · 한국어(ko-KR)"]),
         ("AAOS 주행 제한", ["CarUxRestrictionsManager", "주차·주행 상태 전환"]),
         ("NaviHelper SDK", ["고속도로 진입(TBT IC)·정체 구간 감지", "남은 거리·정체 길이·평균 속도"]),
         ("Gleo AI (App Schema)", ["음성으로 사이길 기능 호출", "화제 저장 · 첫마디 · 저장 화제 추천"])]
    M = [("화면 상태 관리", ["순수 함수 reducer", "입력·추천·회고 화면 전환"]),
         ("추천 엔진", ["첫마디 3가지 · 저장 화제 고르기", "주행 중 제안 순서 결정"]),
         ("주행 동반", ["상황 판단 · 같은 제안 반복 안 함", "상황별 10분에 한 번"]),
         ("기기 내 데이터", ["프로필 · 두 사람의 관계 · 화제", "함께한 길 기록 (JSON)"])]
    R = [("차량 화면 (주차 중)", ["프로필 · 대화 추천 · 회고", "음성 인식 결과 확인·수정"]),
         ("TextToSpeech SDK", ["주행 중 제안을 음성으로 안내", ""]),
         ("주행 화면", ["현재 상황 칩 · 제안 문장", "밸런스 게임 A vs B, 터치 없음"]),
         ("Gleo 응답", ["앱이 돌려준 결과를", "Gleo가 운전자에게 음성으로 전달"])]
    ys = [210, 420, 630, 840]
    xs = [60, 740, 1420]
    for col, items in zip(xs, [L, M, R]):
        for y, (t, ls) in zip(ys, items):
            b.append(box(col, y, bw, bh, t, [l for l in ls if l], accent=(col == 740)))
    cy = [y + bh // 2 for y in ys]
    l_r, m_l, m_r, r_l = 60 + bw, 740, 740 + bw, 1420
    b += [arrow([(l_r, cy[0]), (m_l, cy[0])]),                                  # STT -> state
          arrow([(l_r, cy[1]), (630, cy[1]), (630, cy[0] + 30), (m_l, cy[0] + 30)]),  # AAOS -> state
          arrow([(l_r, cy[2]), (m_l, cy[2])], dashed=True),                    # Navi -> companion
          arrow([(l_r, cy[3]), (660, cy[3]), (660, cy[1] + 30), (m_l, cy[1] + 30)]),  # Gleo -> recommend
          arrow([(1000, ys[0] + bh), (1000, ys[1])]),                          # state -> recommend
          arrow([(1000, ys[2]), (1000, ys[1] + bh)], dashed=True),             # companion -> recommend
          arrow([(m_r, cy[1] - 25), (1300, cy[1] - 25), (1300, cy[3]), (m_r + 8, cy[3])]),  # recommend -> data
          arrow([(m_r, cy[0]), (r_l, cy[0])]),                                  # state -> car screen
          arrow([(m_r, cy[2] - 20), (1360, cy[2] - 20), (1360, cy[1]), (r_l, cy[1])], dashed=True),  # companion -> TTS
          arrow([(m_r, cy[2] + 20), (r_l, cy[2] + 20)], dashed=True),          # companion -> driving screen
          arrow([(m_r, cy[1] + 40), (1390, cy[1] + 40), (1390, cy[3]), (r_l, cy[3])])]  # recommend -> gleo response
    b.append(text(60, 1030, "대화 음성과 상대의 발언은 저장하지 않습니다. 고속도로·정체 신호는 경로 안내 중일 때 NaviHelper에서 받습니다.", 16, 500, MID))
    return svg(W, H, "\n".join(b)), H

def fig5():
    lanes = ["사용자", "차량 화면", "화면 상태", "SpeechToText", "추천 엔진", "기기 저장소", "주행 동반", "차량·내비", "TextToSpeech"]
    W = 2280; x0, gap = 150, 247
    X = [x0 + i * gap for i in range(len(lanes))]
    msgs = [("주차 중", None),
            (0, 1, "동승자 선택 · 말하기"), (1, 2, "동승자 · 입력 종류"), (2, 3, "듣기 시작"),
            (3, 2, "중간·최종 인식 결과"), (2, 4, "주제 또는 지금 상황"), (4, 5, "두 사람의 화제·관계 조회"),
            (4, 1, "첫마디 3가지 / 맞는 저장 화제"), (2, 6, "함께 타는 두 사람 등록"),
            ("주행 중", None),
            (7, 2, "주행 제한 → 주행 화면으로 전환"), (7, 6, "고속도로 진입 / 정체 구간"), (6, 4, "상황에 맞는 제안 요청"),
            (6, 8, "제안 문장 읽기"), (6, 1, "상황 칩 · 제안 표시"),
            ("도착 후", None),
            (0, 1, "함께한 길에 남기기"), (2, 5, "기록 저장 · 새 관심사 반영"), (1, 0, "기록을 저장했어요")]
    y = 230; rows = []
    for m in msgs:
        if m[1] is None:
            rows.append(("band", y, m[0])); y += 60
        else:
            rows.append(("msg", y, m)); y += 62
    H = y + 70
    b = [text(60, 80, "대화 준비부터 주행 중 추천까지의 처리 흐름", 36, 800), text(60, 118, "구현된 흐름 기준", 18, 500, MID)]
    for x, n in zip(X, lanes):
        b.append(f'<rect x="{x-105}" y="150" width="210" height="50" rx="10" fill="#e3f0e5" stroke="{LINE}" stroke-width="2"/>')
        b.append(text(x, 183, n, 19, 800, DARK, "middle"))
        b.append(f'<line x1="{x}" y1="200" x2="{x}" y2="{H-40}" stroke="{LINE}" stroke-width="2" stroke-dasharray="6 6"/>')
    for kind, yy, m in rows:
        if kind == "band":
            b.append(f'<rect x="20" y="{yy}" width="{W-40}" height="40" rx="8" fill="#dcebe0"/>')
            b.append(text(40, yy + 28, m, 19, 800, ACC))
        else:
            a, c, label = m
            xa, xc = X[a], X[c]
            dirn = 1 if xc > xa else -1
            b.append(f'<line x1="{xa}" y1="{yy+30}" x2="{xc - dirn*2}" y2="{yy+30}" stroke="{ACC}" stroke-width="2.5" marker-end="url(#ah)"/>')
            lw = len(label) * 16 + 24
            b.append(f'<rect x="{(xa + xc) / 2 - lw / 2}" y="{yy + 2}" width="{lw}" height="24" rx="6" fill="{BG}"/>')
            b.append(text((xa + xc) / 2, yy + 20, label, 17, 600, DARK, "middle"))
    return svg(W, H, "\n".join(b)), H

for name, (s, h) in [("fig4", fig4()), ("fig5", fig5())]:
    open(f"{OUT}/{name}.svg", "w", encoding="utf-8").write(s)
    print(name, h)
