# Stage 4.13 — Pre-Commit Decision

Fechamento dos achados da Stage 4.12.  
Sem alteração de catálogo, mídia do APK, importer, Room schema, `.gitignore` (para input JPG), nem Git history.

Data (UTC): 2026-09-29

---

## Documentation

### Correção feita

Em `PROJECT_CONTEXT.md` §6 Persistência:

| Antes | Depois |
| ----- | ------ |
| `Room versão **8**` | `Room versão **9**` |

### Outras referências a “8” no mesmo ficheiro

| Local | Tratamento |
| ----- | ---------- |
| § Identidade externa — `Room **versão 9**` | Já correto |
| Tabela final — `Room — versão 9` | Já correto |
| `MIGRATION_8_9` / “migrations até 8→9” | Histórico de migration — **mantido** |
| Secção `## Room — versões 7 e 8` | Documentação histórica do schema — **mantida** |
| Commit notes (`15c03f8` Room 7–8; exclusão histórico “continua **8**”) | Contexto histórico da época — **mantido** |

Nenhuma outra referência ao **estado atual** como Room 8 permaneceu.

---

## Known test

| Item | Valor |
| ---- | ----- |
| Teste | `test_main_catalog_json_matches_curate` |
| Drift | `"3/4 Sit-Up"` vs `"Abdominal"` |
| Status | **PRE-EXISTENTE / FORA DE ESCOPO** |
| Ação nesta etapa | **Nenhuma** (teste e `MAIN_CATALOG.json` intactos) |

---

## `tools/input` media analysis

Path: `tools/exercise-import/input/free-exercise-db/exercises/`

| Métrica | Valor |
| ------- | ----: |
| Pastas | **171** |
| Ficheiros JPG | **342** |
| Tamanho total | **~21,4 MiB** |
| Pastas que pertencem aos 136 publicados | **115** |
| Pastas **fora** dos 136 | **56** |
| Selecionados V2.1 com `0.jpg`+`1.jpg` no input | **115** |
| Selecionados V2.1 **ausentes** do input | **21** |

Os 21 ausentes no input foram obtidos na aquisição V2.1 via cache/remoto (ex.: `Arnold_Dumbbell_Press`, `Rack_Pulls`, `Straight-Arm_Pulldown`, …) e já estão no asset do APK.

### Necessidade

| Uso | Necessário? |
| --- | ----------- |
| Runtime Android / APK | **Não** — mídia de produção está em `app/src/main/assets/exercises/` (272 JPG) |
| Catálogo publicado / importer | **Não** — JSON + asset paths |
| Testes unitários Android / publish validation | **Não** — validam o asset do app |
| Scripts de curadoria V2/V2.1 | **Opcional** — consultam `SOURCE_IMAGES` para flags `hasLocalSourceMedia` / elegibilidade; funcionam com subset ou ausência (marcam pending / usam remoto) |
| Reproduzir pack 136 + mídia APK | **Não a partir só deste input** — incompleto (21/136 em falta); APK + `MAIN_CATALOG_V2_1` + output JSON já fecham o estado publicado |
| Papel real | Cópia local parcial do Free Exercise DB usada durante aquisição/`sync_source_only` |

`exercises.json` + `schema.json` no mesmo `input/` continuam relevantes como dataset bruto (876) — **separados** desta decisão sobre JPG.

---

## Decision

### Recomendação: **OPÇÃO B — Não versionar** os JPG de `input/.../exercises/`

**Motivo técnico**

1. O runtime e o catálogo publicado **não** dependem deste diretório.  
2. O conjunto é **incompleto** para os 136 (faltam 21 pares) e inclui **56** pastas irrelevantes ao pack atual (~⅓ das pastas).  
3. Versionar ~21 MiB duplicaria parcialmente o que já deve ir no Git como asset (`app/.../assets/exercises/`, ~16 MiB, completo e canónico).  
4. A fonte FEDB é Unlicense; os scripts de aquisição já sabem obter frames em falta.  
5. Para análise offline futura, o material pode permanecer **local + ignorado**, sem entrar no commit.

**Não executado nesta etapa:** nenhum JPG removido; nenhum padrão novo no `.gitignore` para `input/.../exercises/`.

**Sugestão para o commit futuro (fora desta etapa):**

```text
# versionar
tools/exercise-import/input/free-exercise-db/exercises.json
tools/exercise-import/input/free-exercise-db/schema.json

# ignorar (opcional, etapa posterior)
tools/exercise-import/input/free-exercise-db/exercises/
```

**OPÇÃO A** seria justificável só se a política do repo for “clone 100% offline sem redownload FEDB” — mas mesmo assim o input atual **não** basta para regenerar os 136 sem a pasta de assets ou o cache.  
**OPÇÃO C** (só os 115) cria um subset frágil e ainda incompleto; não recomendada.

---

## Published catalog

| Item | Resultado |
| ---- | --------- |
| Exercícios (tools + asset) | **136** |
| Identidade / name / sourceData | idênticos (`MISMATCH=0`) |
| JPG no APK | **272** |
| Pastas extra / missing / inválidos | **0 / 0 / 0** |
| SHA-256 | `11134036b42db0ada619016f5fda35ab264ce50f3c7c38c55bd43552e51c22b8` |

---

## Build

| Comando | Resultado |
| ------- | --------- |
| `:app:compileDebugKotlin` | **OK** |
| `:app:testDebugUnitTest` | **OK** |
| `:app:assembleDebug` | **OK** |

---

## Git

```text
 M .gitignore
 M PROJECT_CONTEXT.md
 M README.md
 M app/build.gradle.kts
 ... (demais tracked do branch)
?? app/schemas/...
?? app/src/main/assets/
?? tools/
...
```

Alteração **desta** etapa no tracked: correção Room 8→9 em `PROJECT_CONTEXT.md` (+ este relatório untracked sob `tools/`).

### Confirmação

```text
sem git add
sem git commit
sem git push
sem reset / clean / restore / rebase
sem alteração user.name / user.email
sem Co-authored-by Cursor
```

---

## Critério de sucesso

```text
[x] PROJECT_CONTEXT coerente com Room 9 (estado atual)
[x] teste V1 intacto / fora de escopo
[x] JPGs input analisados quantitativamente
[x] recomendação explícita: OPÇÃO B
[x] catálogo 136 + 272 JPG + SHA preservados
[x] build OK
[x] relatório criado
[x] nenhum commit/push
```
