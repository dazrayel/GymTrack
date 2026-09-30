# Stage 4.12 — Pre-Commit Review

Revisão **somente leitura** do estado pós–Stage 4.11.  
Nenhum ficheiro de produção/catálogo/mídia/teste foi alterado nesta etapa (exceto a criação deste relatório).

Data (UTC): 2026-09-29

---

## Status geral

```text
PASS_WITH_FINDINGS
```

O pack runtime (136 + 272 JPG + SHA) está íntegro.  
Caches da Stage 4.11 estão cobertos pelo `.gitignore`.  
Há **1 inconsistência documental residual** (Room “versão 8” num cabeçalho antigo de `PROJECT_CONTEXT.md`) e o teste V1 conhecido continua a falhar — ambos **fora de escopo** para remediação aqui.

---

## Git status

### Resumo

| Tipo | Conteúdo |
| ---- | -------- |
| Branch | `main` (sem commit nesta etapa) |
| Tracked modificados | `.gitignore`, docs, Room/UI/execução, testes androidTest/unit (trabalho Stages 3–4.x) |
| Untracked | `tools/` (pipeline), `app/src/main/assets/`, importer/domain/media UI, schemas v9, debug/release stubs, test fixtures |
| Removido na 4.11 | `tmp_diag_tap_settings.py` (já ausente; não reapareceu) |

`git diff --stat` (tracked): 26 ficheiros, ~+986/−116.

Diff focado higiene 4.11: `.gitignore`, `PROJECT_CONTEXT.md`, `README.md`.

Caches / `__pycache__` / `_assets_pre_hash` **não** aparecem no `git status` como untracked (ignorados).

---

## Tools inventory

Árvore: `tools/exercise-import/` — **482 ficheiros** + **206 pastas** no disco (inclui caches/pyc).  
Candidatos versionáveis (excluindo C): **413 ficheiros**.

### Tabela (agrupada)

| Arquivo/pasta | Categoria | Motivo |
| ------------- | --------- | ------ |
| `README.md` | **A** | Documentação do pipeline |
| `analysis/classify_importability.py` | **A** | Classificação Stage 2.5 |
| `analysis/analyze_free_exercise_db.py` | **A** | Análise inicial FEDB |
| `analysis/importability-stats.json` | **A** | Snapshot classificação 876 |
| `analysis/free-exercise-db-stats.json` | **A** | Stats dataset |
| `analysis/COMPATIBILITY_DECISIONS.md` | **A** | Decisões Stage 2.5 |
| `analysis/STAGE_3A_IDENTITY.md` | **A** | Identidade externa |
| `analysis/MAIN_CATALOG.json` + `.md` | **A** | Curadoria V1 (histórico) |
| `analysis/MAIN_CATALOG_V2.json` + `.md` | **A** | Curadoria V2 |
| `analysis/MAIN_CATALOG_V2_1.json` + `.md` | **A** | **Fonte de verdade** dos 136 |
| `analysis/PUBLISHED_CATALOG_V2_1.md` | **A** | Relatório publicação 4.9 |
| `analysis/MEDIA_MANIFEST_V2_1.json` + `.md` | **A** | Manifesto mídia 136 |
| `analysis/MEDIA_CANDIDATES_V2_1.json` + `.md` | **A** | Probe mídia |
| `analysis/MEDIA_AUDIT_V2_1.json` | **A** | Auditoria mídia |
| `analysis/exercise-image-audit-4-5.json` + `EXERCISE_IMAGE_AUDIT.md` | **A** | Auditoria imagens 4.5 |
| `analysis/translation-audit.json` + `TRANSLATION_*.md` | **A** | Auditorias tradução |
| `analysis/instruction-audit-4-4-2.json` + `INSTRUCTION_*.md` | **A** | Auditorias instruções |
| `analysis/instruction-translation-4-4-2b.json` | **A** | Métricas 4.4.2B |
| `analysis/REPORT.md` | **A** | Relatório análise geral |
| `analysis/FINAL_PIPELINE_AUDIT_4_10.md` | **A** | Auditoria 4.10 |
| `analysis/PRE_COMMIT_HYGIENE_4_11.md` | **A** | Relatório 4.11 |
| `analysis/PRE_COMMIT_REVIEW_4_12.md` | **A** | Este relatório |
| `analysis/_media_download_cache_v2_1/` | **C** | Cache download; **já no `.gitignore`** |
| `analysis/_assets_pre_hash_v2_1.json` | **C** | Snapshot pré-aquisição; **já no `.gitignore`** |
| `**/__pycache__/` + `*.pyc` | **C** | Bytecode; **já no `.gitignore`** |
| `curation/*.py` (scripts + tests) | **A** | Curadoria, publish, media acquire/sync, testes |
| `curation/catalog_paths.py` | **A** | full-876 vs published paths |
| `normalize/mappings.py` | **A** | Mapas músculo/equipamento |
| `normalize/normalize_free_exercise_db.py` | **A** | Normalizador |
| `normalize/test_normalize_free_exercise_db.py` | **A** | Testes normalização |
| `translate/glossary.json` | **A** | Glossário pt-BR |
| `translate/translations.json` | **A** | Catálogo traduções 777 |
| `translate/*.py` + `test_*.py` | **A** | Pipeline tradução + testes |
| `translate/extra_sentence_lookup.json` | **A** | Lookup instruções |
| `translate/instruction_patterns.py` | **A** | Padrões instrução |
| `output/.../gymtrack-exercises.json` | **A** | **Pack publicado 136** |
| `output/.../gymtrack-exercises-full-876.json` | **A** | Fonte análise/testes (876) |
| `output/.../normalization-report.json` | **A** | Relatório normalização |
| `input/.../exercises.json` | **A** | Dataset bruto FEDB (876) |
| `input/.../schema.json` | **A** | Schema FEDB |
| `input/.../exercises/**/*.jpg` (~342 JPG / 171 pastas) | **B** | Fonte local parcial; útil para sync offline; APK já tem os 136; ~22 MiB |

### Notas de inventário

- Não há outros `_cache` / `.tmp` / `.bak` fora dos já classificados **C**.
- `input/exercises/` tem **171** pastas (não 876): subset local do FEDB — esperado; mídia completa dos 136 está no asset Android.

---

## Catalog

| Camada | Esperado | Encontrado |
| ------ | -------: | ---------: |
| Original (full JSON) | 876 | 876 |
| Importáveis | 777 | 777 (via classificação Stage 2.5) |
| Rejeitados | 99 | 99 |
| Publicados (tools) | 136 | 136 |
| Publicados (asset) | 136 | 136 |
| bytes tools == asset | sim | **sim** |
| identidades / names / sourceData | iguais | `FIELD_MISMATCH=0` |
| SHA-256 | `11134036…22b8` | **`11134036b42db0ada619016f5fda35ab264ce50f3c7c38c55bd43552e51c22b8`** |

Runtime usa somente o pack **136**. O **876** permanece como arquivo de análise/fixture (não é `ASSET_PATH`).

---

## Media (`app/src/main/assets/exercises/`)

| Métrica | Resultado |
| ------- | --------- |
| Pastas de exercício | **136** |
| Extra (fora V2.1) | **0** |
| Missing dirs | **0** |
| `0.jpg` + `1.jpg` | 136 + 136 |
| Total JPG | **272** |
| Missing JPG | **0** |
| Invalid JPEG | **0** |
| Outros ficheiros por pasta | **0** |
| JSON no asset | `gymtrack-exercises.json` apenas |

---

## Documentation

| Fonte | Estado |
| ----- | ------ |
| `tools/exercise-import/README.md` | Coerente: 876/777/99/136/641/272 |
| `README.md` (portfólio) | Coerente pós-4.11: 876/777/99/136/272, Room 9, 2 frames JPG |
| `PROJECT_CONTEXT.md` § identidade (~1504) | Coerente pós-4.11 |
| `PROJECT_CONTEXT.md` §6 Persistência (~L129) | **Inconsistência residual:** ainda diz `Room versão **8**` enquanto a linha seguinte e o resto do doc usam Room 9 / migrations até 8→9 |
| Relatórios `analysis/FINAL_*`, `PRE_COMMIT_HYGIENE_4_11` | Alinhados |

**Não corrigido nesta etapa** (só leitura).

---

## Tests

### Cobertura do pipeline (identificada; não alterada)

| Área | Testes |
| ---- | ------ |
| Normalização | `normalize/test_normalize_free_exercise_db.py` |
| Compatibilidade / policy | `ExerciseCatalogImportPolicyTest` (+ classify Python) |
| Tradução | `translate/test_*.py` |
| Curadoria V1/V2/V2.1 | `curation/test_curate_main_catalog*.py` |
| Publicação 4.9 | `curation/test_publish_main_catalog_v2_1.py` |
| Mídia | `curation/test_media_acquire_v2_1.py` |
| Importer / idempotência | `ExerciseCatalogImporterTest` |
| Identidade / media resolver | `ExerciseMediaResolverTest`, DAO/migration tests |
| Debug import | `DebugCatalogImportControllerTest` |

### Validação Gradle (4.12)

| Comando | Resultado |
| ------- | --------- |
| `:app:compileDebugKotlin` | **OK** |
| `:app:testDebugUnitTest` | **OK** |
| `:app:assembleDebug` | **OK** |

---

## Known pre-existing failure

```text
test_main_catalog_json_matches_curate
```

```text
"3/4 Sit-Up" vs "Abdominal"
```

Classificação: **PRE-EXISTENTE / FORA DO ESCOPO**.  
Não relacionado ao pack publicado 136. **Não corrigido.**

---

## Git recommendation

### 1. Podem entrar no próximo commit (A + assets app)

**Pipeline**

- Todo `tools/exercise-import/` **exceto** categoria **C** (já ignorada)
- Incluir `input/` (JSON+schema+JPGs locais) **se** quiserem reproducibilidade offline completa (**B** — opcional por tamanho ~22 MiB; recomendado versionar pelo menos `exercises.json` + `schema.json`; JPGs de input são **B**)

**App (pipeline-related, untracked)**

- `app/src/main/assets/exercises/` (JSON 136 + 272 JPG) — **obrigatório para o APK**
- `app/src/main/java/.../catalogimport/`, `domain/catalogimport/`, `domain/exercise/`
- `ExerciseAnimation.kt`, `ExerciseAssetImageLoader.kt`
- Debug/release Settings import stubs
- `app/src/test/...` e `androidTest/.../catalogimport/`
- `app/src/test/resources/exercises/gymtrack-exercises-full-876.json`
- `app/schemas/.../9.json`

**Docs / higiene já modificados**

- `.gitignore`, `README.md`, `PROJECT_CONTEXT.md` (aceitar inconsistência L129 ou corrigir noutro passo)

### 2. Devem continuar ignorados (C)

```text
tools/exercise-import/analysis/_media_download_cache_v2_1/
tools/exercise-import/analysis/_assets_pre_hash_v2_1.json
**/__pycache__/
*.pyc
```

### 3. Revisar manualmente antes do commit (D / decisões)

| Item | Nota |
| ---- | ---- |
| `input/.../exercises/**/*.jpg` (~22 MiB) | **B**: versionar vs regenerar de FEDB remoto; APK não depende deste subset |
| `PROJECT_CONTEXT.md` L129 “Room versão 8” | Doc stale menor; corrigir em commit de docs se desejado |
| Tamanho do commit com 272 JPG no asset (~16 MiB) | Esperado; confirmar intentional |
| Teste V1 falhando | Aceitar como conhecido ou corrigir em etapa futura |

---

## Critério de sucesso 4.12

```text
[x] inventário tools/exercise-import documentado
[x] versionáveis (A/B) identificados
[x] artefatos (C) identificados e confirmados no .gitignore
[x] nenhum ficheiro apagado nesta etapa
[x] catálogo 136
[x] mídia 272 JPG
[x] SHA 11134036b42db0ada619016f5fda35ab264ce50f3c7c38c55bd43552e51c22b8
[x] build OK
[x] sem mudança de comportamento
[x] sem commit/push
[x] este relatório criado
```

---

## Confirmação Git

```text
sem git add
sem git commit
sem git push
sem reset / clean / restore / rebase
sem alteração de user.name / user.email
sem Co-authored-by Cursor
```
