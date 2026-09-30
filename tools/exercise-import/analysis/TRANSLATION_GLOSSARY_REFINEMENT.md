# Refinamento do Glossário pt-BR (Etapa 4.3.1)

Somente análise e refinamento do glossário. **Não altera** `gymtrack-exercises.json`, o asset Android, Room, o importador nem nenhum arquivo em `app/` ou `tools/exercise-import/output/`.

---

## Resultado: Antes vs. Depois

| Classificação | Antes (4.3) | Depois (4.3.1) | Variação |
| --- | ---: | ---: | ---: |
| DIRECT | 195 | 262 | +67 |
| CONTEXTUAL | 67 | 90 | +23 |
| REVIEW | 515 | 425 | **−90** |
| **Total** | **777** | **777** | — |

Determinismo verificado: SHA-256 idêntico em duas execuções consecutivas.

---

## Fase 1 — Análise dos 515 REVIEW originais

Antes de modificar o glossário, os 515 exercícios REVIEW foram agrupados por padrão:

| Grupo | Qtde. original | Causa da REVIEW |
| --- | ---: | --- |
| Olympic (Clean / Snatch / Jerk) | 50 | `classHint: "REVIEW"` — levantamentos olímpicos intencionalmente mantidos |
| Jump / Plyometric | 25 | `jump`, `box`, `hop`, `plyo` sem cobertura no glossário |
| Stretch / Mobility | 17 | `stretch` apenas como modificador, sem coreMovement → sem `core_hits` |
| Chains | 6 | `chains` sem cobertura em equipment |
| SMR | 11 | padrão `{músculo}-SMR` tokenizado como token único |
| Sled | 7 | `sled` + outros tokens específicos sem cobertura |
| Throw | 7 | `throw` + vocabulário de atletismo |
| Leverage | 8 | `leverage` sem cobertura em equipment |
| Skull Crusher | 2 | `skull`, `crusher` sem cobertura |
| Atlas / Stone | 2 | `atlas`, `stone` — strongman, sem tradução consagrada |
| Rocky | 2 | `rocky` — exercício de nomenclatura histórica |
| Sprint | 4 | `sprint` — atletismo/cardio, sem tradução fitness estável |
| Windmill | 3 | `windmill` — exercício kettlebell sem padrão pt-BR |
| Outros | 379 | tokens variados: `leg`, `hip`, `delt`, `oblique`, `leverage`, `crossover`, etc. |

---

## Fase 2 — Regras adicionadas ao glossário

### 2.1 Equipamentos novos (`equipment`)

| Token(s) | Tradução | Raciocínio |
| --- | --- | --- |
| `chains` | `com Correntes` | Uso bem consolidado em treino de força |
| `leverage` | `Leverage` | Marca técnica de máquinas de alavanca (Hammer Strength-style); mantido em inglês |
| `plate` | `com Anilha` | Equipamento universal; `plate raise`, `plate twist` etc. |
| `low-pulley` | `na Polia Baixa` | Variante de cabo; termo técnico direto |
| `sled` | `no Trenó` | Equipamento de treino funcional; `sled` = trenó |
| `physioball` | `com Bola Suíça` | Sinônimo de stability/exercise ball |

### 2.2 Modificadores novos (`modifiers`)

| Token(s) | Tradução | Raciocínio |
| --- | --- | --- |
| `single leg` / `one leg` | `Unilateral` | Equivalente a `single-leg` já existente |
| `delt` / `delts` / `deltoid` / `deltoids` | `do Deltóide` | Abreviação de deltóide muito comum em bases de dados anglófonas |
| `quad` / `quads` | `de Quadríceps` | Abreviação frequente em nomes compostos |
| `calf` | `de Panturrilha` | `calf raise` já era coberto como coreMovement; `calf` isolado não era |
| `hip` | `de Quadril` | Fallback quando `hip` não forma frase com movimento |
| `elevated` | `Elevado` | Modificador de posição claro e direto |
| `narrow` | `Estreito` | Qualificador de postura (narrow stance, narrow grip) |
| `backward` | `para Trás` | Direção de movimento sem ambiguidade |
| `oblique` | `Oblíquo` | Qualificador anatômico de abdominal |
| `bodyweight` | `com Peso Corporal` | Classificação de implemento direto |
| `behind` | `por Trás` | Preposição de posição; frase `behind the neck` já existia |
| `stance` | `""` (vazio) | Consumido sem contribuir ao nome; evita que `stance` sozinho cause REVIEW em "Narrow Stance Squat" etc. |

### 2.3 Movimentos centrais novos (`coreMovements`)

#### Olimpismo — **mantidos como REVIEW**
`clean`, `snatch`, `jerk` e compostos já tinham `classHint: "REVIEW"`. Nenhuma alteração: levantamentos olímpicos requerem contexto técnico especializado para tradução.

#### Exercícios com Box
| Frase | Tradução | classHint |
| --- | --- | --- |
| `box squat` / `box squats` | `Agachamento no Box` | DIRECT |
| `box jump` / `box jumps` | `Box Jump` | DIRECT |

#### Saltos e Pliometria
| Frase | Tradução | classHint |
| --- | --- | --- |
| `jump squat` / `jump squats` / `squat jump` / `squat jumps` | `Agachamento com Salto` | DIRECT |
| `jump` (isolado) | `Salto` | CONTEXTUAL |
| `hop` (isolado) | `Salto` | CONTEXTUAL |

> **Nota:** 16 exercícios de jump/plyo continuam REVIEW por terem tokens adicionais sem cobertura (`depth`, `stride`, `rocket`, `scissors`, `cone`, `hurdle`, `plyo`, `tuck`, `linear`). Não foram adicionadas regras para esses termos por ausência de tradução pt-BR consagrada no contexto fitness.

#### Quadril
| Frase | Tradução | classHint |
| --- | --- | --- |
| `hip extension` / `hip extensions` | `Extensão de Quadril` | DIRECT |
| `hip flexion` | `Flexão de Quadril` | DIRECT |
| `hip abduction` | `Abdução de Quadril` | DIRECT |
| `hip adduction` | `Adução de Quadril` | DIRECT |
| `hip raise` / `hip raises` / `hip lift` | `Elevação de Quadril` | DIRECT |
| `hip bridge` | `Ponte de Quadril` | DIRECT |

#### Pernas
| Frase | Tradução | classHint |
| --- | --- | --- |
| `leg raises` / `leg raise` | `Elevação de Pernas` | DIRECT |
| `leg lift` / `leg lifts` | `Elevação de Pernas` | DIRECT |
| `leg extensions` | `Extensão de Pernas` | DIRECT |
| `leg curls` | `Flexão de Pernas` | DIRECT |

#### Outros movimentos
| Frase | Tradução | classHint |
| --- | --- | --- |
| `skull crusher` / `skull crushers` | `Skull Crusher` | DIRECT |
| `hack squat` / `hack squats` | `Agachamento Hack` | DIRECT |
| `trap bar deadlift` | `Levantamento Terra com Trap Bar` | DIRECT |
| `parallel bar dip` | `Mergulho na Barra Paralela` | DIRECT |
| `pull through` | `Pull Through` | DIRECT |
| `pull apart` | `Pull Apart` | DIRECT |
| `rack pull` | `Rack Pull` | DIRECT |
| `ab rollout` | `Rollout Abdominal` | DIRECT |
| `rollout` | `Rollout` | CONTEXTUAL |
| `glute ham raise` | `Glute Ham Raise` | DIRECT |
| `hyperextension` / `hyperextensions` | `Hiperextensão` | DIRECT |
| `crossover` | `Crossover` | CONTEXTUAL |
| `sled push` | `Empurrada no Trenó` | DIRECT |
| `sled row` | `Remada no Trenó` | DIRECT |
| `stretch` / `stretches` / `stretching` | `Alongamento` | CONTEXTUAL |

> **Nota sobre `stretch`:** O termo estava apenas em `modifiers` (não gerava `core_hits`), fazendo qualquer exercício de alongamento virar REVIEW mesmo sem tokens descobertos. Movido para `coreMovements` com `classHint: "CONTEXTUAL"`. Os 7 exercícios resolvidos são aqueles onde todos os demais tokens já tinham cobertura. 10 exercícios de alongamento continuam REVIEW por tokens anatômicos específicos (`tibialis`, `peroneals`, `groin`, `quad` em contextos com outros tokens, `all fours`, `chair`, etc.).

---

## Fase 3 — Grupos que permanecem em REVIEW (e por quê)

### Olympic Lifts (50 exercícios) — **INTENCIONAL**
Clean, Snatch, Jerk e todas as variações (`Hang Clean`, `Power Snatch`, `Clean and Jerk`, etc.) estão marcados com `classHint: "REVIEW"` desde a Etapa 4.3. Tradução requer avaliação técnica especializada; os nomes em inglês são amplamente utilizados por praticantes brasileiros de CrossFit e halterofilia.

### SMR (11 exercícios) — **Complexidade de tokenização**
Exercícios no padrão `{Músculo}-SMR` (ex.: `Calves-SMR`, `Quadriceps-SMR`) são tokenizados como um único token composto por hífen. Adicionar 11 entradas específicas seria possível, mas a tradução `Liberação Miofascial de {Músculo}` requer que o componente de músculo seja reconhecido em cada caso. Dado o volume reduzido e a complexidade, mantidos como REVIEW para revisão humana.

### Throw (7 exercícios) — **Vocabulário de atletismo**
`Backward Medicine Ball Throw`, `Medicine Ball Scoop Throw`, etc. usam vocabulário de atletismo/treino funcional (`scoop`, `catch`, `overhead throw`) sem equivalente pt-BR consensual no contexto de academias. Mantidos como REVIEW.

### Sprint (4 exercícios) — **Cardio/Atletismo**
`Bench Sprint`, `Lunge Sprint`, `Prowler Sprint`, `Side Hop-Sprint`: vocabulário de condicionamento físico e atletismo sem tradução pt-BR estável. Mantidos como REVIEW.

### Windmill (3 exercícios) — **Exercício kettlebell especializado**
`Kettlebell Windmill` e variações: sem padrão de tradução consolidado no Brasil. Mantidos como REVIEW.

### Atlas / Stone (2 exercícios) — **Strongman**
`Atlas Stones`, `Atlas Stone Trainer`: terminologia de strongman de uso muito restrito. Mantidos como REVIEW.

### Rocky (2 exercícios) — **Nomenclatura histórica**
`Bradford/Rocky Presses`, `Rocky Pull-Ups/Pulldowns`: nomes proprietários/históricos. `Rocky Pull-Ups/Pulldowns` contém barra `/` que interfere na tokenização. Mantidos como REVIEW.

### Outros (320 exercícios) — **Tokens variados sem cobertura**
Exercícios com tokens residuais como `ab`, `roller`, `windmill`, `suspension`, `air`, `crossbody`, `pike`, etc. Cada caso requereria análise individual; não foram adicionadas regras genéricas para evitar traduções incorretas.

---

## Fase 4 — Verificação de integridade

- ✅ Pulldown **não** traduzido como Puxada (guarda em `audit_name()` + 5 testes)
- ✅ Nenhum arquivo em `app/` modificado
- ✅ Nenhum arquivo em `tools/exercise-import/output/` modificado
- ✅ `gymtrack-exercises.json` intocado
- ✅ SHA-256 idêntico em duas execuções consecutivas
- ✅ 22 testes unitários passando (7 originais + 15 novos da Etapa 4.3.1)
- ✅ `totalImportable = 777` preservado
- ✅ Sem identidades duplicadas no output

---

## Arquivos modificados

| Arquivo | Tipo de mudança |
| --- | --- |
| `tools/exercise-import/translate/glossary.json` | +47 entradas (equipment, modifiers, coreMovements) |
| `tools/exercise-import/translate/test_audit_translation.py` | +15 testes da Etapa 4.3.1 |
| `tools/exercise-import/analysis/translation-audit.json` | Regenerado (REVIEW: 515 → 425) |
| `tools/exercise-import/analysis/TRANSLATION_AUDIT.md` | Regenerado (totais atualizados) |
| `tools/exercise-import/analysis/TRANSLATION_GLOSSARY_REFINEMENT.md` | Novo (este documento) |
