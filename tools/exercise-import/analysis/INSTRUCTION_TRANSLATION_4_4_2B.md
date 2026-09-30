# INSTRUCTION_TRANSLATION_4_4_2B

**Escopo**: tradução determinística de instruções para pt-BR.

## Antes (auditoria 4.4.2A)

```text
Total de instruções: 3441
TRANSLATED_EXISTING: 511
PASSTHROUGH_ENGLISH: 2928
PORTUGUESE: 0
MIXED: 0
UNKNOWN: 2
```

## Depois (4.4.2B)

```text
TRANSLATED:      672
UNCHANGED:       0
REVIEW:          2767
NOT_APPLICABLE:  2
```

Soma: 3441 (= 3441)

## Ganho de cobertura

```text
Instruções traduzidas antes: 511
Instruções traduzidas depois: 672
Aumento: 161
```

## Passthrough / REVIEW pendente

```text
Antes (PASSTHROUGH_ENGLISH): 2928
Depois (REVIEW pendente):    2767
Redução: 161
```

## Métodos

```text
empty: 2
lookup: 672
passthrough: 2706
review: 61
```

Tamanho do SENTENCE_LOOKUP: 174

## REVIEW por motivo

| Motivo | Ocorrências |
|--------|------------:|
| passthrough_pending | 2706 |
| partner | 61 |

## Principais regras adicionadas

- Expansão do `SENTENCE_LOOKUP` (frases frequentes + `extra_sentence_lookup.json`)
- Padrões estruturados: Return/Keep/Hold/Repeat/Breathe
- Compostos `Keep your …` com concordância (costas retas, braços estendidos…)
- Termos técnicos: Pulldown, Face Pull, Leg Press, Kettlebell, Clean…
- Pull-Up → Barra Fixa nas frases cobertas; Pulldown ≠ Puxada
- Compose genérico **desativado** (evita pt-BR sem artigos / MIXED)
- Parceiro/HTML/truncado → REVIEW sem forçar tradução
- Strings vazias → NOT_APPLICABLE

## Nomes dos exercícios

Classificação de nomes **inalterada**:

```text
TRANSLATED: 641
UNCHANGED:  133
REVIEW:       3
```

## Integridade

```text
sourceData.instructions alterado: NÃO
gymtrack-exercises.json alterado: NÃO
Asset Android alterado: NÃO
app/ alterado: NÃO
Git commit criado: NÃO
```

- Instruction statuses are TRANSLATED / REVIEW / NOT_APPLICABLE (UNCHANGED unused in 4.4.2B).
- Pending English instructions are classified as REVIEW (not forced TRANSLATED).
- Safe compose fallback disabled to avoid unnatural/missing-article Portuguese.
- Pulldown never becomes Puxada; Pull-Up → Barra Fixa in covered sentences.
