# TRANSLATION_REVIEW_4_4_1_FINAL — Auditoria final dos 8 REVIEW

**Etapa**: 4.4.1 Final  
**Data**: 2026-09-29  
**SHA-256[:16] do translations.json**: `e42e18bbfab11cfa`

---

## 1. Estado inicial

```text
TRANSLATED:     637
UNCHANGED:      132
REVIEW:           8
NOT_APPLICABLE:   0
TOTAL:          777
```

SHA-256[:16] pré-auditoria: `f4e9371abeca0455`

---

## 2. Tabela dos 8 casos

| externalId | Original | Sugestão | Decisão | Justificativa |
|---|---|---|---|---|
| `90_90_Hamstring` | 90/90 Hamstring | Alongamento de Posteriores 90/90 | **TRANSLATED** | Alongamento ativo de posteriores com quadril e joelho a 90°. "Alongamento de Posteriores 90/90" é nomenclatura natural em fisioterapia/mobilidade BR; preserva o marcador técnico 90/90 e alinha com `muscleGroup=Posteriores`. |
| `Downward_Facing_Balance` | Downward Facing Balance | Downward Facing Balance | **REVIEW** | Apesar do nome lembrar yoga, o movimento é equilíbrio estático de bruços sobre bola (mãos caminham, pernas elevam). Não há nomenclatura brasileira consolidada; tradução palavra a palavra ("Equilíbrio Virado para Baixo") fica artificial. |
| `Front_Cone_Hops_or_hurdle_hops` | Front Cone Hops (or hurdle hops) | Saltos Frontais sobre Cones | **TRANSLATED** | Pliometria clara: saltos bilaterais sobre cones. Alinha com `Hurdle_Hops` → "Saltos sobre Barreiras". O "(or hurdle hops)" é alternativa no nome original e não precisa ir para o nome final. |
| `Pyramid` | Pyramid | Pyramid | **REVIEW** | Alongamento/estabilidade com quadris no ápice sobre bola. "Pirâmide" conflita com método de treino (séries piramidais); "Alongamento em Pirâmide" / "Postura da Pirâmide" (yoga) não descrevem este exercício com segurança. Sem nomenclatura BR consolidada. |
| `Rack_Delivery` | Rack Delivery | Rack Delivery | **UNCHANGED** | Drill olímpico de entrega da barra à posição de front rack. Termo técnico internacional usado por coaches BR junto a Clean/Snatch/Jerk; traduzir ("Entrega ao Rack") não é nomenclatura estabelecida. |
| `Return_Push_from_Stance` | Return Push from Stance | Return Push from Stance | **REVIEW** | Drill esportivo com parceiro (stance atlética → receber bola medicinal → devolver). "Empurrada de Retorno da Posição" é artificial e imprecisa (é passe/arremesso, não empurrada). Sem nome canônico em academias BR. |
| `Side_Hop-Sprint` | Side Hop-Sprint | Saltos Laterais com Sprint | **TRANSLATED** | Sequência clara: hops laterais sobre obstáculo + sprint final. Nome descritivo natural e fiel ao movimento. |
| `Single-Cone_Sprint_Drill` | Single-Cone Sprint Drill | Drill de Sprint em Cone | **TRANSLATED** | Drill de agilidade/pés rápidos em torno de um cone. "Drill" é termo habitual no treinamento esportivo BR; nome completo fica claro e natural. |

---

## 3. Estado final

```text
TRANSLATED:     641
UNCHANGED:      133
REVIEW:           3
NOT_APPLICABLE:   0
TOTAL:          777
```

```text
641 + 133 + 3 + 0 = 777 ✓
```

SHA-256[:16]: `e42e18bbfab11cfa` (idêntico em duas execuções consecutivas)

---

## 4. Casos que permaneceram REVIEW

### `Downward_Facing_Balance` — Downward Facing Balance

* **Ambiguidade**: o nome sugere yoga ("Downward Facing"), mas as instruções descrevem um hold de equilíbrio de bruços sobre bola de exercícios.
* **Por que não traduzir**: não há equivalente brasileiro usado; tradução literal gera nome estranho e pode confundir com Down Dog / poses de yoga.
* **Para resolver depois**: confirmar com vídeo/fonte se existe nome de academia/fisioterapia consolidado (ex.: equilíbrio pronado na bola) e só então promover.

### `Pyramid` — Pyramid

* **Ambiguidade**: "Pirâmide" no Brasil geralmente significa método de progressão de cargas, não este alongamento.
* **Por que não traduzir**: qualquer PT inventada ("Alongamento em Pirâmide", "Postura da Pirâmide") ou conflita com yoga/método de treino, ou inventa nomenclatura não usada.
* **Para resolver depois**: decidir se o exercício deve ser renomeado descritivamente (ex. com base no vídeo) ou removido/renomeado na fonte.

### `Return_Push_from_Stance` — Return Push from Stance

* **Ambiguidade**: drill funcional/esportivo com parceiro; "push" aqui é devolução de passe, não empurrada de musculação.
* **Por que não traduzir**: tentativas anteriores ("Empurrada de Retorno da Posição") soam artificiais e alteram o sentido.
* **Para resolver depois**: se houver uso em treinamento esportivo BR, adotar o termo de campo; caso contrário, manter inglês ou descrever o drill de forma canônica.

---

## 5. Overrides adicionados / atualizados

| externalId | originalName | translatedName | motivo |
|---|---|---|---|
| `90_90_Hamstring` | 90/90 Hamstring | Alongamento de Posteriores 90/90 | TRANSLATED — alongamento 90/90 natural em BR |
| `Front_Cone_Hops_or_hurdle_hops` | Front Cone Hops (or hurdle hops) | Saltos Frontais sobre Cones | TRANSLATED — alinhado a Hurdle Hops |
| `Side_Hop-Sprint` | Side Hop-Sprint | Saltos Laterais com Sprint | TRANSLATED — descrição fiel do drill |
| `Single-Cone_Sprint_Drill` | Single-Cone Sprint Drill | Drill de Sprint em Cone | TRANSLATED — nomenclatura esportiva BR |
| `Rack_Delivery` | Rack Delivery | Rack Delivery | UNCHANGED — jargão olímpico |
| `Downward_Facing_Balance` | Downward Facing Balance | Downward Facing Balance | REVIEW — sem nomenclatura segura |
| `Pyramid` | Pyramid | Pyramid | REVIEW — ambiguidade com método pirâmide |
| `Return_Push_from_Stance` | Return Push from Stance | Return Push from Stance | REVIEW — drill sem nome canônico |

Os 769 exercícios já resolvidos **não** foram reformulados.

---

## Determinismo

```text
Run 1: e42e18bbfab11cfa
Run 2: e42e18bbfab11cfa
```

✓ Saídas idênticas.

---

## Integridade

```text
Catálogo normalizado alterado: NÃO
Asset Android alterado: NÃO
app/ alterado: NÃO
sourceData alterado: NÃO
Importer alterado: NÃO
Room alterado: NÃO
Instruções traduzidas nesta etapa: NÃO
Git commit criado: NÃO
Git push executado: NÃO
Git config alterado: NÃO
Co-authored-by adicionado: NÃO
```
