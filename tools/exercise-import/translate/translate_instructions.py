"""Rule-based pt-BR translation engine for Free Exercise DB instructions.

Etapa 4.4 / 4.4.2B — deterministic, no external APIs.

Strategy (priority):
  1. Exact SENTENCE_LOOKUP (+ optional extra_sentence_lookup.json)
  2. Structured full-sentence patterns (Keep/Return/Hold/Repeat/…)
  3. Fragile cases → REVIEW (partner, HTML, truncated…)
  4. Otherwise REVIEW with original English (never MIXED gibberish)

PHRASE_SUBS / WORD_SUBS are kept for reference but are NOT applied at runtime.
"""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import NamedTuple

from instruction_patterns import (
    STATUS_NOT_APPLICABLE,
    STATUS_REVIEW,
    STATUS_TRANSLATED,
    STATUS_UNCHANGED,
    force_review_reason,
    try_safe_compose,
    try_structured,
)

# ---------------------------------------------------------------------------
# 1. SENTENCE_LOOKUP — exact matches (highest confidence)
# ---------------------------------------------------------------------------

SENTENCE_LOOKUP: dict[str, str] = {
    # ---- Closers (most repeated) ------------------------------------------
    "Repeat for the recommended amount of repetitions.":
        "Repita pelo número recomendado de repetições.",
    "Repeat the movement for the prescribed amount of repetitions.":
        "Repita o movimento pelo número prescrito de repetições.",
    "Repeat this motion for the prescribed amount of repetitions.":
        "Repita esse movimento pelo número prescrito de repetições.",
    "Repeat for the desired number of repetitions.":
        "Repita pelo número desejado de repetições.",
    "Repeat the movement for the prescribed amount of repetitions of your training program.":
        "Repita o movimento pelo número prescrito de repetições do seu programa de treino.",
    "Repeat for the recommended amount of repetitions and then switch to the other arm.":
        "Repita pelo número recomendado de repetições e depois troque para o outro braço.",
    "Repeat for the recommended amount of repetitions and switch arms.":
        "Repita pelo número recomendado de repetições e troque os braços.",
    "Repeat for the recommended amount of times.":
        "Repita pelo número recomendado de vezes.",
    "Repeat for the recommended amount of repetitions prescribed in your program.":
        "Repita pelo número prescrito de repetições do seu programa.",
    "Repeat.":
        "Repita.",
    "Repeat to failure.":
        "Repita até a falha.",
    "After a second pause at the contracted position, repeat the movement for the prescribed amount of repetitions.":
        "Após uma pausa de um segundo na posição contraída, repita o movimento pelo número prescrito de repetições.",

    # ---- Return / pause ----------------------------------------------------
    "After a brief pause, return to the starting position.":
        "Após uma breve pausa, retorne à posição inicial.",
    "Slowly return to the starting position as you breathe in.":
        "Retorne lentamente à posição inicial ao inspirar.",
    "Slowly lower the weight again to the starting position as you inhale.":
        "Abaixe o peso lentamente de volta à posição inicial ao inspirar.",
    "After a second on the contracted position, start to inhale and slowly lower your torso back to the starting position when your arms are fully extended and the lats are fully stretched.":
        "Após um segundo na posição contraída, comece a inspirar e abaixe lentamente o tronco de volta à posição inicial, quando os braços estiverem totalmente estendidos e o latíssimo totalmente alongado.",

    # ---- Bar back in rack --------------------------------------------------
    "When you are done, place the bar back in the rack.":
        "Ao terminar, coloque a barra de volta no suporte.",
    "Hold on to the bar using both arms at each side and lift it off the rack by first pushing with your legs and at the same time straightening your torso.":
        "Segure a barra com ambos os braços em cada lado e levante-a do suporte empurrando primeiro com as pernas e ao mesmo tempo endireitando o tronco.",

    # ---- Switch sides / arms -----------------------------------------------
    "Switch arms and repeat the exercise.":
        "Troque os braços e repita o exercício.",
    "Switch arms and repeat the movement.":
        "Troque os braços e repita o movimento.",
    "Then, reposition and repeat the same series of movements on the opposite side.":
        "Em seguida, reposicione-se e repita a mesma série de movimentos no lado oposto.",
    "Repeat the movement with the left hand. This equals one repetition.":
        "Repita o movimento com a mão esquerda. Isso equivale a uma repetição.",
    "Continue alternating in this manner for the recommended amount of repetitions.":
        "Continue alternando dessa forma pelo número recomendado de repetições.",

    # ---- Wrist / forearm ---------------------------------------------------
    "Start out by curling your wrist upwards and exhaling.":
        "Comece enrolando o pulso para cima e expirando.",
    "Slowly lower your wrists back down to the starting position while inhaling.":
        "Abaixe lentamente os pulsos de volta à posição inicial ao inspirar.",
    "Your forearms should be stationary as your wrist is the only movement needed to perform this exercise.":
        "Seus antebraços devem permanecer estacionários, pois o pulso é o único movimento necessário para realizar este exercício.",
    "Hold the weight on the initial position for a second and repeat the motion for the prescribed number of repetitions.":
        "Segure o peso na posição inicial por um segundo e repita o movimento pelo número prescrito de repetições.",

    # ---- Calf / ankle ------------------------------------------------------
    "Go back slowly to the starting position as you breathe in by lowering your heels as you bend the ankles until calves are stretched.":
        "Retorne lentamente à posição inicial ao inspirar, abaixando os calcanhares ao dobrar os tornozelos até que as panturrilhas estejam alongadas.",

    # ---- Squat / bar setup -------------------------------------------------
    "This exercise is best performed inside a squat rack for safety purposes. To begin, first set the bar on a rack that best matches your height. Once the correct height is chosen and the bar is loaded, step under the bar and place the back of your shoulders (slightly below the neck) across it.":
        "Este exercício é melhor executado dentro de um rack de agachamento por questões de segurança. Para começar, ajuste a barra no suporte na altura adequada para você. Após escolher a altura correta e carregar a barra, passe por baixo dela e posicione-a na parte posterior dos ombros, levemente abaixo do pescoço.",
    "Begin by stepping under the bar and placing it across the back of the shoulders. Squeeze your shoulder blades together and rotate your elbows forward, attempting to bend the bar across your shoulders. Remove the bar from the rack, creating a tight arch in your lower back, and step back into position. Place your feet wider for more emphasis on the back, glutes, adductors, and hamstrings, or closer together for more quad development. Keep your head facing forward.":
        "Comece passando por baixo da barra e posicionando-a na parte posterior dos ombros. Junte as escápulas e gire os cotovelos para frente tentando curvar a barra sobre os ombros. Retire a barra do suporte criando uma curvatura firme na lombar e dê um passo para trás. Coloque os pés mais afastados para maior ênfase nas costas, glúteos, adutores e posteriores, ou mais juntos para maior ênfase no quadríceps. Mantenha a cabeça voltada para frente.",
    "Begin to slowly lower the bar by bending the knees as you maintain a straight posture with the head up. Continue down until the angle between the upper leg and the calves becomes slightly less than 90-degrees (which is the point in which the upper legs are below parallel to the floor). Inhale as you perform this portion of the movement. Tip: If you performed the exercise correctly, the front of the knees should make an imaginary straight line with the toes that is perpendicular to the front. If your knees are past that imaginary line (if they are past your toes) then you are placing undue stress on the knee and the exercise has been performed incorrectly.":
        "Comece a abaixar lentamente a barra dobrando os joelhos enquanto mantém a postura ereta com a cabeça erguida. Continue descendo até que o ângulo entre a coxa e a panturrilha seja ligeiramente menor que 90 graus (ponto em que as coxas ficam abaixo do paralelo ao chão). Inspire durante esta fase do movimento. Dica: se o exercício for executado corretamente, a parte frontal dos joelhos deve formar uma linha imaginária com os dedos dos pés perpendicular à frente. Se os joelhos ultrapassarem essa linha, há sobrecarga desnecessária no joelho.",
    "With your back, shoulders, and core tight, push your knees and butt out and you begin your descent. Sit back with your hips until you are seated on the box. Ideally, your shins should be perpendicular to the ground. Pause when you reach the box, and relax the hip flexors. Never bounce off of a box.":
        "Com costas, ombros e core firmes, afaste os joelhos e glúteos para fora ao iniciar a descida. Recue os quadris até sentar no box. Idealmente, as canelas devem estar perpendiculares ao chão. Pause ao alcançar o box e relaxe os flexores do quadril. Nunca salte de um box.",
    "Stand facing a Smith machine bar or sturdy elevated platform at an appropriate height.":
        "Fique de frente para a barra da máquina Smith ou uma plataforma elevada robusta em altura adequada.",
    "Place your feet flat on the floor, at a distance that is slightly wider than shoulder width apart.":
        "Coloque os pés planos no chão, a uma distância ligeiramente maior que a largura dos ombros.",
    "As you have both arms extended in front of you holding the bar at the chosen grip width, bring your torso back around 30 degrees or so while creating a curvature on your lower back and sticking your chest out. This is your starting position.":
        "Com os braços estendidos à sua frente segurando a barra na largura de pegada escolhida, leve o tronco cerca de 30 graus para trás criando uma curvatura na lombar e projetando o peito para fora. Essa é a sua posição inicial.",

    # ---- Decline bench / chest press ---------------------------------------
    "Secure your legs at the end of the decline bench and slowly lay down on the bench.":
        "Prenda as pernas na extremidade do banco declinado e deite-se lentamente no banco.",
    "Pause when the barbell touches your torso, and then drive the bar up with as much force as possible. The elbows should be tucked in until lockout.":
        "Pause quando a barra tocar o tronco e, em seguida, empurre a barra para cima com o máximo de força possível. Os cotovelos devem permanecer fechados até a extensão completa.",

    # ---- Deadlift / hip hinge ----------------------------------------------
    "Lower the bar by bending at the hips and guiding it to the floor.":
        "Abaixe a barra dobrando nos quadris e guiando-a até o chão.",
    "Lift the bar back up to the starting position as you exhale.":
        "Eleve a barra de volta à posição inicial ao expirar.",
    "As the bar passes through the knees, lean back and drive the hips into the bar, pulling your shoulder blades together.":
        "Conforme a barra passa pelos joelhos, incline o tronco para trás e impulsione os quadris em direção à barra, juntando as escápulas.",
    "Return the weight to the ground by bending at the hips and controlling the weight on the way down.":
        "Retorne o peso ao chão dobrando nos quadris e controlando o movimento na descida.",
    "Take a breath, and then lower your hips, looking forward with your head with your chest up. Drive through the floor, spreading your feet apart, with your weight on the back half of your feet. Extend through the hips and knees.":
        "Inspire e abaixe os quadris, olhando para frente com a cabeça e o peito erguido. Empurre o chão, afastando os pés, com o peso na metade posterior dos pés. Estenda quadris e joelhos.",

    # ---- Olympic / clean pulls --------------------------------------------
    "Begin the first pull by driving through the heels, extending your knees. Your back angle should stay the same, and your arms should remain straight. Move the weight with control as you continue to above the knees.":
        "Inicie o primeiro puxão impulsionando pelos calcanhares, estendendo os joelhos. O ângulo das costas deve permanecer o mesmo e os braços devem ficar estendidos. Mova o peso com controle até acima dos joelhos.",
    "Immediately recover by driving through the heels, keeping the torso upright and elbows up. Continue until you have risen to a standing position.":
        "Recupere-se imediatamente impulsionando pelos calcanhares, mantendo o tronco ereto e os cotovelos elevados. Continue até chegar à posição em pé.",
    "Transition into the second pull by extending through the hips knees and ankles, driving the bar up as quickly as possible. The bar should be close to the body. At peak extension, shrug the shoulders and allow the elbows to flex to the side.":
        "Passe para o segundo puxão estendendo quadris, joelhos e tornozelos, impulsionando a barra para cima o mais rápido possível. A barra deve permanecer próxima ao corpo. No pico da extensão, eleve os ombros e deixe os cotovelos flexionarem para o lado.",

    # ---- Biceps ------------------------------------------------------------
    "Continue the movement until your biceps are fully contracted and the bar is at shoulder level. Hold the contracted position for a second and squeeze the biceps hard.":
        "Continue o movimento até que o bíceps esteja totalmente contraído e a barra esteja na altura dos ombros. Segure a posição contraída por um segundo e contraia o bíceps com força.",
    "While holding the upper arms stationary, curl the weights forward while contracting the biceps as you breathe out. Tip: Only the forearms should move.":
        "Mantendo os braços superiores estacionários, enrole os pesos para frente contraindo o bíceps ao expirar. Dica: apenas os antebraços devem se mover.",
    "Slowly begin to bring the bar back to starting position as your breathe in.":
        "Lentamente, comece a trazer a barra de volta à posição inicial ao inspirar.",
    "Slowly begin to bring the dumbbells back to starting position as your breathe in.":
        "Lentamente, comece a trazer os halteres de volta à posição inicial ao inspirar.",

    # ---- Push-up / press ---------------------------------------------------
    "Keeping your body straight, lower your chest to the bar by bending the arms.":
        "Mantendo o corpo reto, abaixe o peito em direção à barra dobrando os braços.",
    "Return to the starting position by extending the elbows, pressing yourself back up.":
        "Retorne à posição inicial estendendo os cotovelos e empurrando-se de volta para cima.",
    "Lower yourself until your chest almost touches the floor as you inhale.":
        "Abaixe-se até que o peito quase toque o chão ao inspirar.",
    "Kneel down on both of your knees so that your body is facing the flat bench.":
        "Ajoelhe-se nos dois joelhos de forma que o seu corpo fique voltado para o banco reto.",
    "Dip your body by bending the knees, keeping your torso upright.":
        "Abaixe o corpo dobrando os joelhos, mantendo o tronco ereto.",
    "After a second pause, bring the bar back to the starting position as you breathe out and push the bar using your triceps muscles. Lock your arms in the contracted position, hold for a second and then start coming down slowly again. Tip: It should take at least twice as long to go down than to come up.":
        "Após uma pausa de um segundo, leve a barra de volta à posição inicial ao expirar e empurre a barra usando os tríceps. Trave os braços na posição contraída, segure por um segundo e comece a descer lentamente. Dica: a descida deve demorar pelo menos o dobro do tempo da subida.",

    # ---- Misc small/common ------------------------------------------------
    "Place a dumbbell standing up on a flat bench.":
        "Coloque um halter em pé sobre um banco reto.",
    "Place two kettlebells between your feet. To get in the starting position, push your butt back and look straight ahead.":
        "Coloque dois kettlebells entre os pés. Para chegar à posição inicial, empurre os glúteos para trás e olhe para frente.",
    "Begin to raise the bar as you exhale by pushing the floor with the heel of your foot as you straighten the legs again and go back to the starting position.":
        "Comece a elevar a barra ao expirar, empurrando o chão com o calcanhar enquanto estende as pernas novamente e retorna à posição inicial.",
    "Stand facing a Smith machine bar or sturdy elevated platform at an appropriate height.":
        "Fique de frente para a barra da máquina Smith ou uma plataforma elevada robusta em altura adequada.",
    "To begin, seat yourself on the bike and adjust the seat to your height.":
        "Para começar, sente-se na bicicleta e ajuste o banco à sua altura.",

    # ---- Etapa 4.4.2B high-frequency additions ----------------------------
    "Return to the starting position.":
        "Retorne à posição inicial.",
    "Return to a standing position.":
        "Retorne à posição em pé.",
    "Return the weight to the starting position.":
        "Retorne o peso à posição inicial.",
    "Return the leg to the starting position.":
        "Retorne a perna à posição inicial.",
    "As you breathe in, slowly go back to the starting position.":
        "Ao inspirar, retorne lentamente à posição inicial.",
    "Slowly return to the starting position as you inhale.":
        "Retorne lentamente à posição inicial ao inspirar.",
    "Slowly go back to the starting position as you breathe in.":
        "Retorne lentamente à posição inicial ao inspirar.",
    "Slowly go back down to the starting position as you inhale.":
        "Desça lentamente de volta à posição inicial ao inspirar.",
    "After a brief pause, return the weight to the starting position.":
        "Após uma breve pausa, retorne o peso à posição inicial.",
    "After a second pause, go back to the starting position as you inhale.":
        "Após uma pausa de um segundo, retorne à posição inicial ao inspirar.",
    "After a second of contraction, inhale as you go back to the starting position.":
        "Após um segundo de contração, inspire ao retornar à posição inicial.",
    "Pause at the top of the motion, and return to the starting position.":
        "Faça uma pausa no topo do movimento e retorne à posição inicial.",
    "Continue as far as you are able, pause, and then return to the starting position.":
        "Continue o máximo que puder, pause e então retorne à posição inicial.",
    "Repeat for the prescribed number of repetitions.":
        "Repita pelo número prescrito de repetições.",
    "Repeat the same movement to failure.":
        "Repita o mesmo movimento até a falha.",
    "Repeat until you have performed your set repetitions.":
        "Repita até completar as repetições da série.",
    "Switch legs and repeat the movement.":
        "Troque as pernas e repita o movimento.",
    "Switch arms while performing this exercise.":
        "Alterne os braços ao executar este exercício.",
    "Press the handles forward by extending through the elbow.":
        "Empurre as alças para frente estendendo os cotovelos.",
    "Grab the pull-up bar with the palms facing forward using a wide grip.":
        "Segure a barra de Barra Fixa com as palmas para frente, usando uma pegada aberta.",
    "Hold for a second at the contracted position and repeat the movement for the prescribed amount of repetitions.":
        "Segure por um segundo na posição contraída e repita o movimento pelo número prescrito de repetições.",
    "Hold the contracted position for a second as you squeeze the biceps.":
        "Segure a posição contraída por um segundo contraindo o bíceps.",
    "Squeeze the biceps hard for a second at the contracted position and repeat for the recommended amount of repetitions.":
        "Contraia o bíceps com força por um segundo na posição contraída e repita pelo número recomendado de repetições.",
    "Lower the bar back down slowly to the starting position. Inhale as you perform this portion of the movement.":
        "Abaixe a barra lentamente de volta à posição inicial. Inspire durante esta fase do movimento.",
    "Slowly lower the dumbbell to the starting position as you inhale.":
        "Abaixe o halter lentamente até a posição inicial ao inspirar.",
    "Slowly lower the dumbbells to the starting position as you inhale.":
        "Abaixe os halteres lentamente até a posição inicial ao inspirar.",
    "Lower the handles slowly back to the starting position as you inhale.":
        "Abaixe as alças lentamente de volta à posição inicial ao inspirar.",
    "Lower the dumbbells back down slowly to the starting position as you inhale.":
        "Abaixe os halteres lentamente de volta à posição inicial ao inspirar.",
    "Slowly begin to lower the dumbbell down as you breathe in.":
        "Comece a abaixar o halter lentamente ao inspirar.",
    "Lower the bar down to the collarbone slowly as you inhale.":
        "Abaixe a barra lentamente até a clavícula ao inspirar.",
    "When you are done, lock the bar back in the rack.":
        "Ao terminar, trave a barra de volta no suporte.",
    "When finished, simply lower the dumbbells to the floor.":
        "Ao terminar, simplesmente abaixe os halteres até o chão.",
    "Use care when lowering yourself to the ground.":
        "Tenha cuidado ao se abaixar até o chão.",
    "Sit on a flat bench with a dumbbell in your right hand.":
        "Sente-se em um banco reto com um halter na mão direita.",
    "Extend your arms above you with a slight bend at the elbows.":
        "Estenda os braços acima da cabeça com uma leve flexão nos cotovelos.",
    "Place your non lifting hand on your bicep for support.":
        "Coloque a mão que não está levantando sobre o bíceps para apoio.",
    "With the bar at thigh level, load an appropriate weight.":
        "Com a barra na altura das coxas, carregue um peso adequado.",
    "Start out by placing a barbell on one side of a flat bench.":
        "Comece colocando uma barra em um dos lados de um banco reto.",
    "Start out by placing two dumbbells on one side of a flat bench.":
        "Comece colocando dois halteres em um dos lados de um banco reto.",
    "Start out by placing two dumbbells on the floor in front of a flat bench.":
        "Comece colocando dois halteres no chão à frente de um banco reto.",
    "Place a bar on the ground behind the head of an incline bench.":
        "Coloque uma barra no chão atrás da cabeceira de um banco inclinado.",
    "As you breathe in, come down slowly until you feel the bar on your lower chest.":
        "Ao inspirar, desça lentamente até sentir a barra no peito inferior.",
    "As you breathe in, come down slowly until you feel the bar on your middle chest.":
        "Ao inspirar, desça lentamente até sentir a barra no meio do peito.",
    "Lower the bar as far as possible while inhaling and keeping a tight grip.":
        "Abaixe a barra o máximo possível ao inspirar, mantendo a pegada firme.",
    "The ball can be thrown to a partner or bounced off of a wall.":
        "A bola pode ser arremessada para um parceiro ou quicada em uma parede.",
    "Stand to the side of a box with your inside foot on top of it, close to the edge.":
        "Fique ao lado de um box com o pé interno sobre ele, perto da borda.",
    "Position your feet back from the bar with arms and body straight. This will be your starting position.":
        "Posicione os pés atrás da barra com braços e corpo estendidos. Essa é a sua posição inicial.",
    "Go back to the starting position by using the triceps to raise the dumbbell. Breathe out as you perform this step.":
        "Retorne à posição inicial usando o tríceps para elevar o halter. Expire ao executar esta etapa.",
    "Using your pectoral muscles, press your upper body back up to the starting position and squeeze your chest. Breathe out as you perform this step.":
        "Usando os peitorais, empurre o tronco de volta à posição inicial e contraia o peito. Expire ao executar esta etapa.",
    "Keep your back straight.":
        "Mantenha as costas retas.",
    "Keep your back straight throughout the movement.":
        "Mantenha as costas retas durante todo o movimento.",
    "Keep your arms extended.":
        "Mantenha os braços estendidos.",
    "Keep your knees slightly bent.":
        "Mantenha os joelhos levemente flexionados.",
    "Keep your chest up.":
        "Mantenha o peito erguido.",
    "Keep your head up.":
        "Mantenha a cabeça erguida.",
    "Keep your core tight.":
        "Mantenha o core firme.",
    "This will be your starting position.":
        "Essa é a sua posição inicial.",
    "This is your starting position.":
        "Essa é a sua posição inicial.",
}

# Load optional extra exact lookups (deterministic JSON)
_EXTRA_LOOKUP_PATH = Path(__file__).with_name("extra_sentence_lookup.json")
if _EXTRA_LOOKUP_PATH.exists():
    _extra_data = json.loads(_EXTRA_LOOKUP_PATH.read_text(encoding="utf-8"))
    if isinstance(_extra_data, dict):
        for _ek, _ev in _extra_data.items():
            if isinstance(_ek, str) and isinstance(_ev, str) and _ek not in SENTENCE_LOOKUP:
                SENTENCE_LOOKUP[_ek] = _ev

# ---------------------------------------------------------------------------
# 2. PHRASE_SUBS — ordered phrase substitutions (NOT applied by default)
#    Kept for reference / future structured use. Runtime uses exact lookup
#    + structured patterns + validated compose only (see translate_sentence).
# ---------------------------------------------------------------------------

PHRASE_SUBS: list[tuple[str, str]] = [
    # Breathing cues
    (r"\bas you (exhale|breathe out)\b", "ao expirar"),
    (r"\bas you (inhale|breathe in)\b", "ao inspirar"),
    (r"\bwhile (exhaling|breathing out)\b", "enquanto expira"),
    (r"\bwhile (inhaling|breathing in)\b", "enquanto inspira"),
    (r"\bexhale as\b", "expire ao"),
    (r"\binhale as\b", "inspire ao"),

    # Starting position
    (r"\bThis (will be|is) your starting position\.?", "Essa é a sua posição inicial."),
    (r"\bReturn to the starting position\.?", "Retorne à posição inicial."),
    (r"\bback to (the )?starting position", "de volta à posição inicial"),
    (r"\bto the starting position", "à posição inicial"),
    (r"\bstarting position", "posição inicial"),

    # Common imperatives
    (r"\bMake sure to\b", "Certifique-se de"),
    (r"\bMake sure\b", "Certifique-se"),
    (r"\bNote:\b", "Nota:"),
    (r"\bTip:\b", "Dica:"),
    (r"\bCaution:\b", "Atenção:"),

    # Muscle actions
    (r"\bfully contracted\b", "totalmente contraído"),
    (r"\bfully extended\b", "totalmente estendido"),
    (r"\bfully stretched\b", "totalmente alongado"),
    (r"\bsqueeze the\b", "contraia o"),
    (r"\bsqueeze your\b", "contraia seus"),
    (r"\bcontract(ing)? the\b", "contrair o"),

    # Position descriptors
    (r"\bshoulder[- ]width apart\b", "na largura dos ombros"),
    (r"\bshoulder[- ]width\b", "na largura dos ombros"),
    (r"\bshoulder height\b", "altura dos ombros"),
    (r"\bperpendicular to the (ground|floor)\b", "perpendicular ao chão"),
    (r"\bparallel to the (ground|floor)\b", "paralelo ao chão"),
    (r"\bpronated grip\b", "pegada pronada"),
    (r"\bsupinated grip\b", "pegada supinada"),
    (r"\bneutral grip\b", "pegada neutra"),
    (r"\boverhand grip\b", "pegada pronada"),
    (r"\bunderhand grip\b", "pegada supinada"),
    (r"\bpalms (facing )?up\b", "palmas para cima"),
    (r"\bpalms (facing )?down\b", "palmas para baixo"),
    (r"\bpalms (facing )?forward\b", "palmas para frente"),
    (r"\bpalms (facing )?inward\b", "palmas voltadas para dentro"),

    # Equipment
    (r"\bbarbell\b", "barra"),
    (r"\bdumbbell(s)?\b", r"halter\1"),
    (r"\bkettlebell(s)?\b", "kettlebell"),
    (r"\bEZ[- ]bar\b", "barra EZ"),
    (r"\bsmith machine\b", "máquina Smith"),
    (r"\bfoam roller\b", "rolo de liberação miofascial"),
    (r"\bfoam roll\b", "rolo de liberação miofascial"),
    (r"\bmedicine ball\b", "bola medicinal"),
    (r"\bexercise ball\b", "bola de exercícios"),
    (r"\bstability ball\b", "bola de exercícios"),
    (r"\bresistance band(s)?\b", "faixa(s) elástica(s)"),
    (r"\bbench press\b", "supino"),
    (r"\bpower rack\b", "power rack"),
    (r"\bsquat rack\b", "rack de agachamento"),
    (r"\bbench\b", "banco"),
    (r"\bthe rack\b", "o suporte"),
    (r"\ba rack\b", "um suporte"),

    # Body parts
    (r"\bshoulder blade(s)?\b", "escápula(s)"),
    (r"\bshoulder(s)?\b", "ombro(s)"),
    (r"\bupper arm(s)?\b", "braço(s) superior(es)"),
    (r"\blower arm(s)?\b", "antebraço(s)"),
    (r"\bforearm(s)?\b", "antebraço(s)"),
    (r"\bupper back\b", "parte superior das costas"),
    (r"\blower back\b", "lombar"),
    (r"\bspine\b", "coluna"),
    (r"\btorso\b", "tronco"),
    (r"\bcore\b", "core"),
    (r"\bchest\b", "peito"),
    (r"\bback\b", "costas"),
    (r"\bglute(s)?\b", "glúteo(s)"),
    (r"\bhamstring(s)?\b", "posterior(es) de coxa"),
    (r"\bquadricep(s)?\b", "quadríceps"),
    (r"\bquad(s)?\b", "quadríceps"),
    (r"\bcalf\b", "panturrilha"),
    (r"\bcalves\b", "panturrilhas"),
    (r"\babdominal(s)?\b", "abdominal(is)"),
    (r"\bbicep(s)?\b", "bíceps"),
    (r"\btricep(s)?\b", "tríceps"),
    (r"\blatissimus dorsi\b", "latíssimo do dorso"),
    (r"\blat(s)?\b", "latíssimo"),
    (r"\btraps?\b", "trapézio"),
    (r"\bdelt(s|oid(s)?)?\b", "deltóide(s)"),
    (r"\belbow(s)?\b", "cotovelo(s)"),
    (r"\bwrist(s)?\b", "pulso(s)"),
    (r"\bknee(s)?\b", "joelho(s)"),
    (r"\bhip(s)?\b", "quadril"),
    (r"\bankle(s)?\b", "tornozelo(s)"),
    (r"\bheel(s)?\b", "calcanhar(es)"),
    (r"\btoe(s)?\b", "dedo(s) do pé"),
    (r"\bfoot\b", "pé"),
    (r"\bfeet\b", "pés"),
    (r"\bhead\b", "cabeça"),
    (r"\bneck\b", "pescoço"),
    (r"\bhand(s)?\b", "mão/mãos"),
    (r"\bfinger(s)?\b", "dedo(s)"),
    (r"\bthigh(s)?\b", "coxa(s)"),
    (r"\bshin(s)?\b", "canela(s)"),

    # Directional / positional
    (r"\bstraight back\b", "costas retas"),
    (r"\bstraight arm(s)?\b", "braço(s) estendido(s)"),
    (r"\bstraight leg(s)?\b", "perna(s) estendida(s)"),
    (r"\bkeep your back straight\b", "mantenha as costas retas"),
    (r"\bflat on (the|your)\b", "plano sobre o"),
    (r"\bupright\b", "ereto"),
    (r"\bneutral position\b", "posição neutra"),
    (r"\bneutral spine\b", "coluna neutra"),
    (r"\bpronated\b", "pronado"),
    (r"\bsupinated\b", "supinado"),
    (r"\blocked out\b", "em extensão completa"),
    (r"\bfull extension\b", "extensão completa"),
    (r"\bfull range of motion\b", "amplitude completa de movimento"),

    # Movement verbs (imperative)
    (r"\bHold(ing)?\b", "Segure" if True else ""),
    (r"\bGrip\b", "Segure"),
    (r"\bGrasp\b", "Segure"),
    (r"\bLift(ing)?\b", "Eleve"),
    (r"\bLower(ing)?\b", "Abaixe"),
    (r"\bRaise(ing)?\b", "Eleve"),
    (r"\bBend(ing)?\b", "Dobre"),
    (r"\bExtend(ing)?\b", "Estenda"),
    (r"\bFlex(ing)?\b", "Flexione"),
    (r"\bPress(ing)?\b", "Pressione"),
    (r"\bPush(ing)?\b", "Empurre"),
    (r"\bPull(ing)?\b", "Puxe"),
    (r"\bDrive\b", "Impulsione"),
    (r"\bSquat(ting)?\b", "Agache"),
    (r"\bLunge\b", "Dê um afundo"),
    (r"\bStand(ing)?\b", "Fique em pé" if True else ""),
    (r"\bSit(ting)?\b", "Sente-se"),
    (r"\bLie (down )?\b", "Deite-se"),
    (r"\bLay(ing)? down\b", "Deite-se"),
    (r"\bKneel(ing)?\b", "Ajoelhe-se"),
    (r"\bStep(ping)?\b", "Dê um passo"),
    (r"\bWalk(ing)?\b", "Caminhe"),
    (r"\bJump(ing)?\b", "Salte"),
    (r"\bLeap\b", "Salte"),
    (r"\bPlace\b", "Posicione"),
    (r"\bPosition\b", "Posicione"),
    (r"\bMaintain\b", "Mantenha"),
    (r"\bKeep\b", "Mantenha"),
    (r"\bReturn(ing)?\b", "Retorne"),
    (r"\bPause\b", "Pause"),
    (r"\bContract(ing)?\b", "Contraia"),
    (r"\bRelax(ing)?\b", "Relaxe"),
    (r"\bExhale\b", "Expire"),
    (r"\bInhale\b", "Inspire"),
    (r"\bBreathe out\b", "Expire"),
    (r"\bBreathe in\b", "Inspire"),
    (r"\bBegin\b", "Comece"),
    (r"\bStart(ing)?\b", "Comece"),
    (r"\bContinue\b", "Continue"),
    (r"\bPerform\b", "Execute"),
    (r"\bExecute\b", "Execute"),
    (r"\bComplete\b", "Complete"),
    (r"\bRemove\b", "Retire"),
    (r"\bAdjust\b", "Ajuste"),
    (r"\bSecure\b", "Prenda"),
    (r"\bAttach\b", "Prenda"),
    (r"\bSelect\b", "Selecione"),
    (r"\bChoose\b", "Escolha"),
    (r"\bLoad\b", "Carregue"),
    (r"\bSet\b", "Configure"),
    (r"\bPlace your\b", "Posicione seus"),

    # Adverbs / modifiers
    (r"\bslowly\b", "lentamente"),
    (r"\bsmoothly\b", "suavemente"),
    (r"\bquickly\b", "rapidamente"),
    (r"\bfirmly\b", "firmemente"),
    (r"\bslightly\b", "levemente"),
    (r"\bgently\b", "suavemente"),
    (r"\bcontrolled(ly)?\b", "com controle"),
    (r"\bsteadily\b", "de forma estável"),
    (r"\bfully\b", "completamente"),
    (r"\bpartially\b", "parcialmente"),
    (r"\btightly\b", "firmemente"),
    (r"\bsimultaneously\b", "simultaneamente"),
    (r"\bimmediately\b", "imediatamente"),
    (r"\bideally\b", "idealmente"),

    # Common phrases
    (r"\bfor safety purposes\b", "por questões de segurança"),
    (r"\bat shoulder level\b", "na altura dos ombros"),
    (r"\bat shoulder height\b", "na altura dos ombros"),
    (r"\bwith the other (arm|hand|leg|foot)\b", r"com o outro \1"),
    (r"\bon the opposite side\b", "no lado oposto"),
    (r"\bon the other side\b", "no outro lado"),
    (r"\bThis is\b", "Isso é"),
    (r"\bThis will be\b", "Essa será"),
    (r"\bThis equals\b", "Isso equivale a"),
]

# ---------------------------------------------------------------------------
# 3. WORD_SUBS — single word replacements applied after phrases
# ---------------------------------------------------------------------------

WORD_SUBS: dict[str, str] = {
    # Anatomical
    "abdominals": "abdominais",
    "abdominal": "abdominal",
    "adductors": "adutores",
    "adductor": "adutor",
    "abductors": "abdutores",
    "abductor": "abdutor",
    "trapezius": "trapézio",
    "rhomboids": "romboides",
    "rhomboid": "romboide",
    "serratus": "serrátil",
    "obliques": "oblíquos",
    "oblique": "oblíquo",
    "piriformis": "piriforme",
    "erectors": "eretores",
    "erector": "eretor",
    "pectorals": "peitorais",
    "pectoral": "peitoral",
    "gastrocnemius": "gastrocnêmio",
    "soleus": "sóleo",
    "tibialis": "tibial",
    # Common fitness words
    "repetitions": "repetições",
    "repetition": "repetição",
    "reps": "repetições",
    "rep": "repetição",
    "sets": "séries",
    "set": "série",
    "rest": "descanso",
    "tempo": "cadência",
    "contraction": "contração",
    "extension": "extensão",
    "flexion": "flexão",
    "rotation": "rotação",
    "movement": "movimento",
    "motion": "movimento",
    "position": "posição",
    "posture": "postura",
    "form": "forma",
    "range": "amplitude",
    "strength": "força",
    "resistance": "resistência",
    "weight": "peso",
    "load": "carga",
    "intensity": "intensidade",
    "volume": "volume",
    "frequency": "frequência",
    # Actions
    "perform": "execute",
    "complete": "complete",
    "control": "controle",
    "stabilize": "estabilize",
    "activate": "ative",
    "engage": "ative",
    "contract": "contraia",
    "relax": "relaxe",
    "stretch": "alongue",
    "compress": "comprima",
    # Positional
    "forward": "para frente",
    "backward": "para trás",
    "upward": "para cima",
    "downward": "para baixo",
    "inward": "para dentro",
    "outward": "para fora",
    "sideways": "lateralmente",
    "horizontally": "horizontalmente",
    "vertically": "verticalmente",
    "diagonally": "diagonalmente",
    # Descriptors
    "straight": "reto",
    "flat": "plano",
    "inclined": "inclinado",
    "declined": "declinado",
    "elevated": "elevado",
    "bent": "dobrado",
    "extended": "estendido",
    "flexed": "flexionado",
    "neutral": "neutro",
    "tight": "firme",
    "loose": "relaxado",
    "wide": "largo",
    "narrow": "estreito",
    "close": "fechado",
    "open": "aberto",
    "parallel": "paralelo",
    "perpendicular": "perpendicular",
    # Time / sequence
    "slowly": "lentamente",
    "quickly": "rapidamente",
    "briefly": "brevemente",
    "momentarily": "momentaneamente",
    "immediately": "imediatamente",
    "gradually": "gradualmente",
    "simultaneously": "simultaneamente",
    "initially": "inicialmente",
    "finally": "por fim",
    "then": "em seguida",
    "next": "em seguida",
    "first": "primeiro",
    "second": "segundo",
    "third": "terceiro",
    # Quantity
    "recommended": "recomendado",
    "prescribed": "prescrito",
    "desired": "desejado",
    "appropriate": "adequado",
    "maximum": "máximo",
    "minimum": "mínimo",
}


# ---------------------------------------------------------------------------
# 4. Translation functions (Etapa 4.4.2B)
# ---------------------------------------------------------------------------

# Re-export statuses for callers/tests
__all_statuses__ = (
    STATUS_TRANSLATED,
    STATUS_UNCHANGED,
    STATUS_REVIEW,
    STATUS_NOT_APPLICABLE,
)


class TranslationResult(NamedTuple):
    text: str
    status: str
    method: str = "lookup"

    @property
    def exact_match(self) -> bool:
        """Backward compatible: True when a confident PT translation was produced."""
        return self.status == STATUS_TRANSLATED


def translate_sentence(sentence: str) -> TranslationResult:
    """Translate a single instruction sentence deterministically.

    Priority:
      1. empty → NOT_APPLICABLE
      2. exact SENTENCE_LOOKUP → TRANSLATED
      3. fragile source (partner/html/…) → REVIEW (keep English)
      4. structured full-sentence patterns → TRANSLATED
      5. validated safe compose → TRANSLATED (rejects MIXED)
      6. otherwise → REVIEW with original English
    """
    if sentence is None:
        return TranslationResult("", STATUS_NOT_APPLICABLE, "empty")

    s = sentence.strip()
    if not s:
        return TranslationResult("", STATUS_NOT_APPLICABLE, "empty")

    if s in SENTENCE_LOOKUP:
        return TranslationResult(SENTENCE_LOOKUP[s], STATUS_TRANSLATED, "lookup")

    reason = force_review_reason(s)
    if reason in {"html", "image-ref", "truncated", "partner", "visual-ref", "url"}:
        return TranslationResult(s, STATUS_REVIEW, f"review:{reason}")

    structured = try_structured(s)
    if structured:
        return TranslationResult(structured, STATUS_TRANSLATED, "pattern")

    # Safe compose is intentionally NOT used as a general fallback:
    # partial mapping produces unnatural Portuguese (missing articles/agreement).
    # Prefer expanding SENTENCE_LOOKUP / structured patterns instead.
    _ = try_safe_compose  # kept available for tests / offline tooling

    if reason == "fragment":
        return TranslationResult(s, STATUS_REVIEW, "review:fragment")

    return TranslationResult(s, STATUS_REVIEW, "passthrough")


def translate_instructions(instructions: list[str]) -> tuple[list[str], bool]:
    """Translate a list of instruction strings.

    Preserves length and order. Empty strings stay empty (NOT_APPLICABLE).
    Returns (translated_list, all_translated).
    """
    if not instructions:
        return [], True

    translated: list[str] = []
    all_ok = True
    for sentence in instructions:
        result = translate_sentence(sentence)
        if result.status == STATUS_NOT_APPLICABLE and not (sentence or "").strip():
            translated.append("")
        else:
            translated.append(result.text)
        if result.status not in {STATUS_TRANSLATED, STATUS_NOT_APPLICABLE}:
            all_ok = False
    return translated, all_ok


def translate_instructions_detailed(
    instructions: list[str],
) -> tuple[list[str], list[TranslationResult]]:
    """Like translate_instructions, also returning per-sentence results."""
    if not instructions:
        return [], []
    results = [translate_sentence(s) for s in instructions]
    texts: list[str] = []
    for s, r in zip(instructions, results):
        if r.status == STATUS_NOT_APPLICABLE and not (s or "").strip():
            texts.append("")
        else:
            texts.append(r.text)
    return texts, results
