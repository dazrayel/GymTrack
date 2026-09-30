# INSTRUCTION_AUDIT_4_4_2 — Auditoria de instruções (Etapa 4.4.2A)

**Escopo**: análise apenas — **nenhuma tradução** foi aplicada.

A detecção de idioma é **heurística e conservadora**. `UNKNOWN` significa confiança insuficiente, não detecção perfeita.

---

## 1. Resumo

```text
Exercícios analisados: 777
Com instruções: 772
Sem instruções: 5

Total de instruções: 3441
Instruções únicas: 2754
Únicas repetidas (count>1): 274
Únicas exclusivas (count=1): 2480
```

## 2. Estado atual (mecanismo `translate_instructions.py`)

```text
TRANSLATED_EXISTING: 672
PASSTHROUGH_ENGLISH: 2767
PORTUGUESE:          0
MIXED:               0
UNKNOWN:             2
```

Soma das categorias: **3441** (deve igualar total de instruções = 3441)

Regra: TRANSLATED_EXISTING = exact SENTENCE_LOOKUP hit with different PT text; else PORTUGUESE/MIXED by conservative heuristic; else PASSTHROUGH_ENGLISH for English/passthrough; else UNKNOWN. Categories are mutually exclusive by priority.

## 3. Distribuição por número de instruções

| Nº de instruções | Exercícios |
|-----------------:|-----------:|
| 0 | 5 |
| 1 | 5 |
| 2 | 56 |
| 3 | 132 |
| 4 | 221 |
| 5 | 196 |
| 6 | 104 |
| 7 | 45 |
| 8 | 8 |
| 9 | 3 |
| 10 | 1 |
| 24 | 1 |

## 4. 50 frases mais frequentes

| Ocorrências | Classificação | Frase |
|------------:|---------------|-------|
| 223 | TRANSLATED_EXISTING | Repeat for the recommended amount of repetitions. |
| 28 | TRANSLATED_EXISTING | Repeat the movement for the prescribed amount of repetitions. |
| 11 | TRANSLATED_EXISTING | Hold on to the bar using both arms at each side and lift it off the rack by first pushing with your legs and at the same time straightening your torso. |
| 9 | TRANSLATED_EXISTING | After a brief pause, return to the starting position. |
| 9 | TRANSLATED_EXISTING | When you are done, place the bar back in the rack. |
| 8 | TRANSLATED_EXISTING | Switch arms and repeat the exercise. |
| 8 | TRANSLATED_EXISTING | Switch arms and repeat the movement. |
| 7 | TRANSLATED_EXISTING | Repeat for the desired number of repetitions. |
| 7 | TRANSLATED_EXISTING | Repeat this motion for the prescribed amount of repetitions. |
| 6 | TRANSLATED_EXISTING | Repeat the movement for the prescribed amount of repetitions of your training program. |
| 6 | TRANSLATED_EXISTING | Slowly begin to bring the bar back to starting position as your breathe in. |
| 6 | TRANSLATED_EXISTING | Start out by curling your wrist upwards and exhaling. |
| 6 | TRANSLATED_EXISTING | Then, reposition and repeat the same series of movements on the opposite side. |
| 5 | TRANSLATED_EXISTING | Dip your body by bending the knees, keeping your torso upright. |
| 5 | TRANSLATED_EXISTING | Go back slowly to the starting position as you breathe in by lowering your heels as you bend the ankles until calves are stretched. |
| 5 | TRANSLATED_EXISTING | Lower the bar by bending at the hips and guiding it to the floor. |
| 5 | TRANSLATED_EXISTING | Slowly return to the starting position as you breathe in. |
| 5 | TRANSLATED_EXISTING | Your forearms should be stationary as your wrist is the only movement needed to perform this exercise. |
| 4 | TRANSLATED_EXISTING | After a second on the contracted position, start to inhale and slowly lower your torso back to the starting position when your arms are fully extended and th… |
| 4 | TRANSLATED_EXISTING | After a second pause at the contracted position, repeat the movement for the prescribed amount of repetitions. |
| 4 | TRANSLATED_EXISTING | As the bar passes through the knees, lean back and drive the hips into the bar, pulling your shoulder blades together. |
| 4 | TRANSLATED_EXISTING | As you have both arms extended in front of you holding the bar at the chosen grip width, bring your torso back around 30 degrees or so while creating a curva… |
| 4 | TRANSLATED_EXISTING | Begin by stepping under the bar and placing it across the back of the shoulders. Squeeze your shoulder blades together and rotate your elbows forward, attemp… |
| 4 | TRANSLATED_EXISTING | Begin the first pull by driving through the heels, extending your knees. Your back angle should stay the same, and your arms should remain straight. Move the… |
| 4 | TRANSLATED_EXISTING | Begin to slowly lower the bar by bending the knees as you maintain a straight posture with the head up. Continue down until the angle between the upper leg a… |
| 4 | TRANSLATED_EXISTING | Continue the movement until your biceps are fully contracted and the bar is at shoulder level. Hold the contracted position for a second and squeeze the bice… |
| 4 | TRANSLATED_EXISTING | Immediately recover by driving through the heels, keeping the torso upright and elbows up. Continue until you have risen to a standing position. |
| 4 | TRANSLATED_EXISTING | Keeping your body straight, lower your chest to the bar by bending the arms. |
| 4 | TRANSLATED_EXISTING | Kneel down on both of your knees so that your body is facing the flat bench. |
| 4 | TRANSLATED_EXISTING | Lift the bar back up to the starting position as you exhale. |
| 4 | TRANSLATED_EXISTING | Lower yourself until your chest almost touches the floor as you inhale. |
| 4 | TRANSLATED_EXISTING | Place your feet flat on the floor, at a distance that is slightly wider than shoulder width apart. |
| 4 | TRANSLATED_EXISTING | Repeat for the recommended amount of repetitions and then switch to the other arm. |
| 4 | TRANSLATED_EXISTING | Repeat for the recommended amount of repetitions prescribed in your program. |
| 4 | TRANSLATED_EXISTING | Repeat for the recommended amount of times. |
| 4 | TRANSLATED_EXISTING | Repeat to failure. |
| 4 | TRANSLATED_EXISTING | Return the weight to the ground by bending at the hips and controlling the weight on the way down. |
| 4 | TRANSLATED_EXISTING | Return to the starting position by extending the elbows, pressing yourself back up. |
| 4 | TRANSLATED_EXISTING | Secure your legs at the end of the decline bench and slowly lay down on the bench. |
| 4 | TRANSLATED_EXISTING | Slowly begin to bring the dumbbells back to starting position as your breathe in. |
| 4 | TRANSLATED_EXISTING | Slowly lower the weight again to the starting position as you inhale. |
| 4 | TRANSLATED_EXISTING | Slowly lower your wrists back down to the starting position while inhaling. |
| 4 | TRANSLATED_EXISTING | Stand facing a Smith machine bar or sturdy elevated platform at an appropriate height. |
| 4 | TRANSLATED_EXISTING | Take a breath, and then lower your hips, looking forward with your head with your chest up. Drive through the floor, spreading your feet apart, with your wei… |
| 4 | TRANSLATED_EXISTING | This exercise is best performed inside a squat rack for safety purposes. To begin, first set the bar on a rack that best matches your height. Once the correc… |
| 4 | TRANSLATED_EXISTING | Transition into the second pull by extending through the hips knees and ankles, driving the bar up as quickly as possible. The bar should be close to the bod… |
| 4 | TRANSLATED_EXISTING | With your back, shoulders, and core tight, push your knees and butt out and you begin your descent. Sit back with your hips until you are seated on the box. … |
| 3 | PASSTHROUGH_ENGLISH | After a brief pause at the top, return the weight just above the start position, keeping tension on the muscles by not returning the weight to the stops unti… |
| 3 | TRANSLATED_EXISTING | After a second pause, bring the bar back to the starting position as you breathe out and push the bar using your triceps muscles. Lock your arms in the contr… |
| 3 | TRANSLATED_EXISTING | As you breathe in, slowly go back to the starting position. |

## 5. Principais padrões (passthrough)

| Prioridade | Ocorrências | Padrão |
|------------|------------:|--------|
| HIGH | 41 | Repeat… |
| HIGH | 20 | Return to the starting position… |
| MEDIUM | 10 | Step away from the rack and position your legs using a shoulder width medium … |
| MEDIUM | 9 | This exercise is best performed inside a squat rack for safety purposes. To b… |
| MEDIUM | 8 | After a second pause, bring the bar back to the starting position as you brea… |
| MEDIUM | 7 | Keeping the weight on your heels and pushing your feet and knees out, drive u… |
| MEDIUM | 5 | As you move your feet into the receiving position, forcefully pull yourself b… |
| MEDIUM | 5 | Clean two kettlebells to your shoulders. Clean the kettlebells to your should… |
| MEDIUM | 5 | Immediately reverse direction, driving through the heels, in essence jumping … |
| LOW | 4 | As full extension is achieved, transition into the third pull by aggressively… |
| LOW | 4 | As you breathe out, push the dumbbells up using your pectoral muscles. Lock y… |
| LOW | 4 | Begin the first pull by driving through the front of the heels, raising the b… |
| LOW | 4 | Begin to raise the bar as you exhale by pushing the floor with the heel of yo… |
| LOW | 4 | Bend your knees slightly and bring your torso forward, by bending at the wais… |
| LOW | 4 | Keeping the bar aligned over the front of the heels, your head and chest up, … |
| LOW | 4 | Load an appropriate weight onto the pins and adjust the seat for your height.… |
| LOW | 4 | Next comes the second pull, the main source of acceleration for the clean. As… |
| LOW | 3 | Adjust the weight to an appropriate amount and be seated, grasping the handle… |
| LOW | 3 | After a brief pause at the top, return the weight just above the start positi… |
| LOW | 3 | Approach the bar so that it is centered over your feet. You feet should be ab… |
| LOW | 3 | As you breathe in, come down slowly until you feel the bar on your middle che… |
| LOW | 3 | As you breathe out, externally rotate your forearm so that the dumbbell is li… |
| LOW | 3 | As you exhale, use the biceps to curl the weight up until your biceps is full… |
| LOW | 3 | At this moment as the feet leave the floor, the feet must be placed into the … |
| LOW | 3 | Attach dual handles to a sled connected by a rope or chain. Load the sled to … |
| LOW | 3 | Begin by stepping under the bar and placing it across the back of the shoulde… |
| LOW | 3 | Begin the first pull by driving through the heels, extending your knees. Your… |
| LOW | 3 | Begin with a bar loaded on the ground. Approach the bar so that the bar inter… |
| LOW | 3 | Begin with a shoulder width, double overhand or hook grip, with the bar hangi… |
| LOW | 3 | Bend the elbows of the arm holding the dumbbell so that it creates a #-degree… |
| LOW | 3 | By using your thighs to help you get the dumbbells up, clean the dumbbells on… |
| LOW | 3 | Clean a kettlebell to your shoulder. Clean the kettlebell to your shoulder by… |
| LOW | 3 | Ensuring that the dumbbell stays securely placed at the top of the bench, lie… |
| LOW | 3 | Grab the bar with the palms facing forward using the prescribed grip. Note on… |
| LOW | 3 | Immediately recover by driving through the heels, keeping the torso upright a… |
| LOW | 3 | Keeping the kettlebell locked out at all times, push your butt out in the dir… |
| LOW | 3 | Keeping the torso stationary, pull the handles back towards your torso while … |
| LOW | 3 | Lift the bar off the rack by first pushing with your legs and at the same tim… |
| HIGH | 3 | Lower the safety bars holding the weighted platform in place and press the pl… |
| LOW | 3 | Once at shoulder width, rotate your wrists forward so that the palms of your … |

### Verbos iniciais no passthrough

| Verbo (início) | Ocorrências |
|----------------|------------:|
| begin | 188 |
| as | 129 |
| now | 120 |
| lie | 87 |
| while | 84 |
| after | 83 |
| stand | 83 |
| place | 82 |
| to | 75 |
| with | 74 |
| keeping | 72 |
| slowly | 70 |
| lower | 63 |
| hold | 50 |
| return | 49 |
| position | 48 |
| sit | 48 |
| using | 43 |
| repeat | 41 |
| grab | 40 |
| start | 37 |
| continue | 36 |
| pause | 35 |
| at | 33 |
| once | 33 |

## 6. Verbos

| Verbo | Ocorrências |
|-------|------------:|
| position | 1567 |
| repeat | 478 |
| lower | 428 |
| hold | 388 |
| begin | 386 |
| grip | 331 |
| keep | 328 |
| place | 274 |
| breathe | 269 |
| pull | 216 |
| lift | 210 |
| move | 206 |
| return | 193 |
| bend | 186 |
| exhale | 184 |
| inhale | 173 |
| bring | 167 |
| continue | 167 |
| stand | 156 |
| raise | 145 |
| start | 138 |
| pause | 137 |
| step | 129 |
| lie | 127 |
| grab | 117 |
| extend | 114 |
| push | 108 |
| press | 102 |
| squeeze | 94 |
| sit | 92 |
| rotate | 87 |
| maintain | 83 |
| drive | 82 |
| flex | 74 |
| curl | 70 |
| switch | 61 |
| grasp | 60 |
| reach | 55 |
| lean | 45 |
| stretch | 45 |

## 7. Anatomia

| Termo (EN encontrado) | Ocorrências |
|-----------------------|------------:|
| back | 1040 |
| arms | 795 |
| shoulder | 470 |
| feet | 421 |
| torso | 418 |
| knees | 410 |
| head | 353 |
| legs | 348 |
| hips | 342 |
| elbows | 331 |
| hands | 331 |
| chest | 300 |
| arm | 281 |
| hand | 273 |
| shoulders | 260 |
| leg | 207 |
| foot | 156 |
| knee | 149 |
| forearms | 146 |
| elbow | 143 |
| biceps | 125 |
| heels | 114 |
| thighs | 113 |
| toes | 111 |
| wrist | 81 |
| hip | 77 |
| triceps | 61 |
| forearm | 54 |
| wrists | 54 |
| ankles | 46 |
| hamstrings | 42 |
| calves | 40 |
| heel | 40 |
| glutes | 38 |
| neck | 36 |
| thigh | 28 |
| lats | 27 |
| ankle | 25 |
| spine | 24 |
| abs | 21 |

## 8. Equipamentos

| Termo | Ocorrências |
|-------|------------:|
| bar | 978 |
| bench | 340 |
| dumbbell | 216 |
| dumbbells | 198 |
| barbell | 197 |
| kettlebell | 176 |
| rack | 175 |
| handles | 148 |
| step | 129 |
| machine | 109 |
| ball | 104 |
| pulley | 94 |
| handle | 88 |
| box | 80 |
| rope | 78 |
| cable | 74 |
| band | 72 |
| platform | 66 |
| kettlebells | 48 |
| plate | 30 |
| bands | 26 |
| plates | 26 |
| sled | 24 |
| smith | 21 |
| mat | 19 |
| chain | 15 |
| chains | 14 |
| roller | 14 |
| cone | 13 |
| medicine ball | 13 |
| tire | 12 |
| exercise ball | 11 |
| rings | 6 |
| towel | 6 |
| collar | 5 |
| hurdle | 5 |
| suspension | 5 |
| bosu | 4 |
| collars | 4 |
| ez bar | 4 |
| foam roller | 4 |
| sandbag | 4 |
| yoke | 4 |
| cones | 3 |
| hurdles | 2 |
| ez-bar | 1 |
| trap bar | 1 |

## 9. Termos técnicos

| Termo | Ocorrências | Observação |
|-------|------------:|------------|
| starting position | 998 | posição inicial |
| kettlebell | 176 | Kettlebell — manter |
| rack | 175 | Rack — contexto olímpico/suporte |
| squat | 95 | Squat / Agachamento |
| clean | 65 | Clean — olímpico UNCHANGED |
| swing | 27 | Swing — frequentemente mantido |
| smith | 21 | Smith — máquina Smith |
| pull-up | 19 | Pull-Up → Barra Fixa (nomes; validar em instruções) |
| hook grip | 16 | Hook grip — olímpico |
| lockout | 15 | Lockout — termo técnico |
| lunge | 11 | Lunge / Afundo |
| jerk | 10 | Jerk — olímpico UNCHANGED |
| snatch | 7 | Snatch — olímpico UNCHANGED |
| chin-up | 6 | Chin-Up → Barra Fixa Supinada |
| deadlift | 5 | Deadlift / Levantamento Terra |
| leg press | 5 | Leg Press — manter em inglês |
| pulldown | 4 | Pulldown — NUNCA traduzir como Puxada |
| chinup | 1 | Chin-Up → Barra Fixa Supinada |
| pullup | 1 | Pull-Up → Barra Fixa |

## 10. Casos POTENTIAL_REVIEW

Total: **83**

### depende de parceiro/contexto (63)

| externalId | idx | Instrução (trecho) |
|------------|----:|--------------------|
| Backward_Medicine_Ball_Throw | 0 | This exercise is best done with a partner. If you lack a partner, the ball can be thrown and retr… |
| Backward_Medicine_Ball_Throw | 1 | Begin standing a few meters in front of your partner, both facing the same direction. Begin holdi… |
| Backward_Medicine_Ball_Throw | 2 | Squat down and then forcefully reverse direction, coming to full extension and you toss the ball … |
| Backward_Medicine_Ball_Throw | 3 | Your partner can then roll the ball back to you. Repeat for the desired number of repetitions. |
| Behind_Head_Chest_Stretch | 0 | Sit upright on the floor with your partner behind you. |
| Behind_Head_Chest_Stretch | 1 | Place your hands behind your hand, and push your elbows back as far as you can. Your partner shou… |
| Behind_Head_Chest_Stretch | 2 | Gently attempt to pull your elbows forward with your hands still behind your head for 10 or more … |
| Behind_Head_Chest_Stretch | 3 | Now, relax your muscles and have your partner gently pull the elbows back as far as it comfortabl… |
| Catch_and_Overhead_Throw | 0 | Begin standing while facing a wall or a partner. |
| Catch_and_Overhead_Throw | 2 | Ensure that you follow your throw through, being prepared to receive your rebound from your throw… |
| Chest_Push_multiple_response | 0 | Begin in a kneeling position facing a wall or utilize a partner. Hold the ball with both hands ti… |
| Decline_Barbell_Bench_Press | 1 | Using a medium width grip (a grip that creates a 90-degree angle in the middle of the movement be… |
| Decline_Close-Grip_Bench_To_Skull_Crusher | 1 | Using a close grip (a grip that is slightly less than shoulder width), lift the bar from the rack… |
| Decline_EZ_Bar_Triceps_Extension | 1 | Using a close grip (a grip that is slightly less than shoulder width), lift the EZ bar from the r… |
| Handstand_Push-Ups | 1 | Kick yourself up against the wall with your arms straight. Your body should be upside down with t… |
| Lying_Glute | 0 | Lie on your back with your partner kneeling beside you. |
| Lying_Glute | 1 | Flex the hip of one leg, raising it off of the floor. Rotate the leg so the foot is over the oppo… |
| Lying_Glute | 2 | Attempt to push your leg towards your partner, who should be preventing any actual movement of th… |
| Lying_Glute | 3 | After 10-20 seconds, completely relax as your partner gently pushes the ankle and knee towards yo… |
| Lying_Hamstring | 0 | Lie on your back with your legs extended. Your partner should be kneeling beside you. Raise one l… |
| Lying_Hamstring | 1 | With your partner holding your leg in place, attempt to flex the knee, contracting the hamstrings… |
| Lying_Hamstring | 2 | Then relax your leg, allowing your partner to gently push the leg towards your head. Be sure to i… |
| Lying_Prone_Quadriceps | 0 | Lay face down on the floor with your partner kneeling beside you. Flex one knee and raise that le… |
| Lying_Prone_Quadriceps | 1 | Attempt to extend your knee while your partner prevents any actual movement. |
| Lying_Prone_Quadriceps | 2 | After 10-20 seconds, relax your muscles as your partner gently pushes the foot towards your glute… |
| Medicine_Ball_Chest_Pass | 0 | You will need a partner for this exercise. Lacking one, this movement can be performed against a … |
| Medicine_Ball_Chest_Pass | 1 | Begin facing your partner holding the medicine ball at your torso with both hands. |
| Medicine_Ball_Chest_Pass | 3 | Your partner should catch the ball, and throw it back to you. |
| Medicine_Ball_Full_Twist | 0 | For this exercise you will need a medicine ball and a partner. Stand back to back with your partn… |
| Medicine_Ball_Full_Twist | 1 | Hold the ball in front of the trunk. Open the hips and turn the shoulders at the same time as you… |
| … | … | *+33 casos* |

### fragmento/muito curto (10)

| externalId | idx | Instrução (trecho) |
|------------|----:|--------------------|
| Barbell_Squat_To_A_Bench | 1 |  |
| Body-Up | 3 | Repeat. |
| Clean | 1 |  |
| Close-Grip_EZ-Bar_Press | 4 | Repeat. |
| EZ-Bar_Skullcrusher | 3 | Repeat. |
| On-Your-Back_Quad_Stretch | 3 | Switch sides. |
| One-Arm_High-Pulley_Cable_Side_Bends | 6 | Repeat to failure. |
| Pallof_Press_With_Rotation | 7 | Repeat to failure. |
| Standing_Cable_Lift | 6 | Repeat to failure. |
| Standing_Cable_Wood_Chop | 6 | Repeat to failure. |

### nome próprio / variação named (2)

| externalId | idx | Instrução (trecho) |
|------------|----:|--------------------|
| Atlas_Stone_Trainer | 0 | This trainer is effective for developing Atlas Stone strength for those who don't have access to … |
| Atlas_Stones | 0 | Begin with the atlas stone between your feet. Bend at the hips to wrap your arms vertically aroun… |

### possível erro gramatical na fonte (8)

| externalId | idx | Instrução (trecho) |
|------------|----:|--------------------|
| Box_Squat | 2 | With your back, shoulders, and core tight, push your knees and butt out and you begin your descen… |
| Box_Squat_with_Bands | 2 | With your back, shoulders, and core tight, push your knees and butt out and you begin your descen… |
| Box_Squat_with_Chains | 3 | With your back, shoulders, and core tight, push your knees and butt out and you begin your descen… |
| Reverse_Band_Box_Squat | 2 | With your back, shoulders, and core tight, push your knees and butt out and you begin your descen… |
| Reverse_Band_Power_Squat | 2 | Keep your head facing forward. With your back, shoulders, and core tight, push your knees and but… |
| Seated_Good_Mornings | 1 | Remove the bar from the rack, creating a tight arch in your lower back. Keep your head facing for… |
| Squat_with_Bands | 2 | With your back, shoulders, and core tight, push your knees and butt out and you begin your descen… |
| Squat_with_Chains | 2 | With your back, shoulders, and core tight, push your knees and butt out and you begin your descen… |

## 11. Recomendações para a próxima etapa

**Não traduzir nesta etapa.** Próximos passos sugeridos:

### [HIGH] expand-sentence-lookup

Expandir SENTENCE_LOOKUP com frases exact-match de alta frequência. Atualmente TRANSLATED_EXISTING=672, PASSTHROUGH_ENGLISH=2767.

Exemplos:

- `Repeat…`
- `Return to the starting position…`
- `Lower the safety bars holding the weighted platform in place and press the pl…`
- `Return your arms back to the starting position as you squeeze your chest musc…`
- `Slowly return to the starting position…`

### [HIGH] consistent-verb-glossary

Definir glossário consistente de verbos de ação para a próxima etapa (Keep→Mantenha, Lower→Abaixe, Raise→Eleve, Return→Retorne, etc.).

Exemplos:

- `position (1567)`
- `repeat (478)`
- `lower (428)`
- `hold (388)`
- `begin (386)`
- `grip (331)`
- `keep (328)`
- `place (274)`

### [HIGH] pull-pulldown-rules

Validar nas instruções as regras já estabelecidas: Pulldown≠Puxada; Pull-Up→Barra Fixa; Chin-Up→Barra Fixa Supinada; Face Pull permanece Face Pull.

Exemplos:

- `pull-up (19)`
- `chin-up (6)`
- `pulldown (4)`
- `pullup (1)`

### [HIGH] no-partial-substitution

Não reativar PHRASE_SUBS/WORD_SUBS parciais — produzem MIXED EN/PT. Preferir exact-match + templates estruturados.

### [MEDIUM] next-lookup-candidates

Próximos candidatos exact-match pelo impacto (ocorrências).

Exemplos:

- `After a brief pause at the top, return the weight just above the start position, keeping tension on the muscles by no…`
- `Keeping the weight on your heels and pushing your feet and knees out, drive upward as you lead the movement with your…`
- `Next comes the second pull, the main source of acceleration for the clean. As the bar approaches the mid-thigh positi…`
- `To begin, step onto the treadmill and select the desired option from the menu. Most treadmills have a manual setting,…`
- `Adjust the seat to the appropriate height and make your weight selection. Place your upper arms against the pads and …`
- `Adjust the weight to an appropriate amount and be seated, grasping the handles. Your upper arms should be about 45 de…`
- `After a second pause, bring the bar back to the starting position as you breathe out and push the bar using your ches…`
- `After a second pause, bring the bar back to the starting position as you breathe out and push the bar using your ches…`

### [MEDIUM] manual-review-queue

Tratar POTENTIAL_REVIEW separadamente (parceiro, Tip embutido, nomes próprios, fragmentos) — não traduzir em lote cego.

### [LOW] keep-english-technical

Manter termos técnicos já decididos em inglês quando aparecerem nas instruções (Pulldown, Face Pull, Good Morning, Skull Crusher, Clean/Snatch/Jerk) salvo contexto que exija adaptação.

Exemplos:

- `starting position`
- `kettlebell`
- `rack`
- `squat`
- `clean`
- `swing`
- `smith`
- `pull-up`

---

## Integridade

```text
Instruções traduzidas nesta etapa: NÃO
sourceData.instructions alterado: NÃO
gymtrack-exercises.json alterado: NÃO
translations.json alterado: NÃO
Asset Android alterado: NÃO
app/ alterado: NÃO
Git commit criado: NÃO
```
