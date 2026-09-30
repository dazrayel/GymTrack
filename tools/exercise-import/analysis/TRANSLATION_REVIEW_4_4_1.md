# TRANSLATION_REVIEW_4_4_1 — Resolução dos Exercícios REVIEW

**Etapa**: 4.4.1  
**Data**: 2026-09-29  
**SHA-256 (16 chars) do translations.json resultante**: `f4e9371abeca0455`

---

## Resumo Executivo

| Status        | Antes (4.4) | Depois (4.4.1) | Delta   |
|---------------|:-----------:|:--------------:|:-------:|
| TRANSLATED    | 345         | **637**        | +292    |
| UNCHANGED     | 43          | **132**        | +89     |
| REVIEW        | 389         | **8**          | −381    |
| NOT_APPLICABLE| 0           | 0              | —       |
| **Total**     | 777         | 777            | —       |

Redução de REVIEW: **389 → 8 (−98%)**

---

## Método

Foi adicionado o dict `EXERCISE_OVERRIDES` em `generate_translations.py`.  
O dict é consultado **antes** da lógica genérica de `_determine_status_and_name()`.  
Formato: `externalId → (STATUS, "nome em pt-BR")`.

---

## Grupos Processados

### 1. Olympic Lifts (18 exercícios → UNCHANGED)

Termos olímpicos (Clean, Snatch, Jerk e variantes) são usados universalmente  
no meio fitness brasileiro sem tradução. Todos receberam `UNCHANGED` com nome  
em português parcial quando havia modificadores geográficos/de posição.

| externalId | Nome resultante |
|---|---|
| Bottoms-Up_Clean_From_The_Hang_Position | Bottoms-Up Clean da Posição de Suspensão |
| Clean_Pull | Clean Pull |
| Clean_from_Blocks | Clean dos Blocos |
| Hang_Clean_-_Below_the_Knees | Hang Clean Abaixo dos Joelhos |
| Hang_Snatch_-_Below_Knees | Hang Snatch Abaixo dos Joelhos |
| Heaving_Snatch_Balance | Heaving Snatch Balance |
| Jerk_Balance | Jerk Balance |
| Kettlebell_Dead_Clean | Dead Clean com Kettlebell |
| Muscle_Snatch | Muscle Snatch |
| One-Arm_Open_Palm_Kettlebell_Clean | Open Palm Clean Unilateral com Kettlebell |
| Open_Palm_Kettlebell_Clean | Open Palm Clean com Kettlebell |
| Power_Clean_from_Blocks | Power Clean dos Blocos |
| Power_Jerk | Power Jerk |
| Power_Snatch_from_Blocks | Power Snatch dos Blocos |
| Smith_Machine_Hang_Power_Clean | Hang Power Clean na Máquina Smith |
| Snatch_Balance | Snatch Balance |
| Snatch_Pull | Snatch Pull |
| Snatch_from_Blocks | Snatch dos Blocos |

### 2. SMR — Self Myofascial Release (11 exercícios → TRANSLATED)

Padrão: "Liberação Miofascial + {parte do corpo em pt-BR}".

| externalId | Nome resultante |
|---|---|
| Anterior_Tibialis-SMR | Liberação Miofascial do Tibial Anterior |
| Brachialis-SMR | Liberação Miofascial do Braquial |
| Calves-SMR | Liberação Miofascial de Panturrilhas |
| Foot-SMR | Liberação Miofascial do Pé |
| Hamstring-SMR | Liberação Miofascial de Posteriores |
| Latissimus_Dorsi-SMR | Liberação Miofascial do Latíssimo Dorsal |
| Lower_Back-SMR | Liberação Miofascial da Lombar |
| Peroneals-SMR | Liberação Miofascial dos Fibulares |
| Piriformis-SMR | Liberação Miofascial do Piriforme |
| Quadriceps-SMR | Liberação Miofascial de Quadríceps |
| Rhomboids-SMR | Liberação Miofascial dos Romboides |

### 3. Throw / Arremesso (7 exercícios → 6 TRANSLATED + 1 TRANSLATED)

| externalId | Status | Nome resultante |
|---|---|---|
| Backward_Medicine_Ball_Throw | TRANSLATED | Arremesso com Bola Medicinal para Trás |
| Catch_and_Overhead_Throw | TRANSLATED | Pegar e Arremessar Acima da Cabeça |
| Medicine_Ball_Scoop_Throw | TRANSLATED | Arremesso de Bola Medicinal em Scoop |
| Standing_Two-Arm_Overhead_Throw | TRANSLATED | Arremesso Acima da Cabeça com Duas Mãos em Pé |
| Supine_Chest_Throw | TRANSLATED | Arremesso de Peito Deitado |
| Supine_One-Arm_Overhead_Throw | TRANSLATED | Arremesso Unilateral Acima da Cabeça Deitado |
| Supine_Two-Arm_Overhead_Throw | TRANSLATED | Arremesso Acima da Cabeça com Duas Mãos Deitado |

### 4. Sled (3 exercícios)

| externalId | Status | Nome resultante |
|---|---|---|
| Bear_Crawl_Sled_Drags | UNCHANGED | Bear Crawl com Trenó |
| Sled_Drag_-_Harness | TRANSLATED | Arrasto de Trenó com Colete |
| Sled_Overhead_Backward_Walk | TRANSLATED | Caminhada para Trás com Trenó Acima da Cabeça |

### 5. Sprint (5 exercícios)

| externalId | Status | Nome resultante |
|---|---|---|
| Bench_Sprint | TRANSLATED | Sprint no Banco |
| Lunge_Sprint | TRANSLATED | Sprint com Afundos |
| Prowler_Sprint | UNCHANGED | Prowler Sprint |
| Side_Hop-Sprint | REVIEW | Side Hop-Sprint |
| Single-Cone_Sprint_Drill | REVIEW | Exercício de Sprint em Cone |

### 6. Windmill (3 exercícios → UNCHANGED)

"Windmill" não tem equivalente consolidado em português no ambiente fitness.

| externalId | Nome resultante |
|---|---|
| Advanced_Kettlebell_Windmill | Kettlebell Windmill Avançado |
| Double_Kettlebell_Windmill | Windmill Duplo com Kettlebell |
| Kettlebell_Windmill | Kettlebell Windmill |

### 7. Atlas (2 exercícios → UNCHANGED)

Strongman proper names. Mantidos em inglês.

### 8. Rocky (2 exercícios → UNCHANGED)

Variações named. Mantidos em inglês com nome composto.

### 9. Demais REVIEW (≈338 exercícios)

Todos os casos com tradução clara foram resolvidos via `EXERCISE_OVERRIDES`.  
Exemplos representativos:

| Categoria | Exemplos |
|---|---|
| Plyometric/Jumps | Box Jump, Hurdle Hops, Knee Tuck Jump, Plyo Push-up |
| Kettlebell moves | Turkish Get-Up, Pistol Squat, Thruster, Figure 8 |
| Strongman | Tire Flip, Sledgehammer Swings, Farmer's Walk, Log Lift |
| Cardio/Machines | Elliptical Trainer, Stairmaster, Air Bike |
| Stretch/Mobility | Peroneals Stretch, All Fours Quad Stretch |
| Core | Dead Bug, Jackknife Sit-Up, Pallof Press, Superman |
| Named presses | JM Press, Svend Press, Tate Press, Bradford Press |

---

## Exercícios que Permanecem em REVIEW (8)

Casos genuinamente ambíguos onde não há consenso de tradução ou o nome é  
um identificador técnico/de drill sem forma canônica em pt-BR:

| externalId | Razão |
|---|---|
| 90_90_Hamstring | Código de mobilidade (ângulo 90/90), sem PT canônico |
| Downward_Facing_Balance | Posição de yoga sem nome padronizado no fitness BR |
| Front_Cone_Hops_or_hurdle_hops | Nome composto com alternativa (or) |
| Pyramid | Ambíguo — pode ser método ou exercício específico |
| Rack_Delivery | Movimento de levantamento olímpico sem consenso |
| Return_Push_from_Stance | Drill funcional sem nome consagrado |
| Side_Hop-Sprint | Combinação composta sem tradução direta |
| Single-Cone_Sprint_Drill | Drill de agilidade sem nome padronizado |

---

## Verificação de Determinismo

```
Run 1: SHA-256[:16] = f4e9371abeca0455
Run 2: SHA-256[:16] = f4e9371abeca0455
```
✓ Saída idêntica em duas execuções consecutivas.

---

## Testes

**31 testes passam** em `test_generate_translations.py`.  
Novos testes adicionados (Etapa 4.4.1):

- `test_olympic_family_unchanged` — 9 IDs olímpicos validados
- `test_smr_family_translated` — 11 IDs SMR com "Liberação Miofascial"
- `test_throw_family_translated` — 6 IDs de arremesso com "Arremesso"
- `test_sled_translated_has_treno` — Sled Drag com "Trenó"
- `test_windmill_family_unchanged` — 3 IDs Windmill
- `test_atlas_family_unchanged` — 2 IDs Atlas
- `test_rocky_family_unchanged` — 2 IDs Rocky
- `test_sprint_bench_and_lunge_translated` — Sprint no Banco e Sprint com Afundos
- `test_review_count_le_eight` — máximo de 8 REVIEW após esta etapa

---

## Restrições Respeitadas

- ✅ Nenhum arquivo em `app/`, `output/`, `input/` foi modificado
- ✅ O asset Android (`gymtrack-exercises.json`) não foi tocado
- ✅ Nenhuma operação git realizada
- ✅ Nenhuma API externa utilizada
- ✅ "Pulldown" nunca traduzido como "Puxada"
- ✅ Identidade `(source, externalId)` preservada em todos os 777 registros
