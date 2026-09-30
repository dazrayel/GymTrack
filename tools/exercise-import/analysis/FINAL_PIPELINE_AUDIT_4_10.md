# FINAL_PIPELINE_AUDIT — Stage 4.10

Auditoria **somente leitura** do pipeline de exercícios (Stages 1 → 4.9).  
Nenhuma correção de código/JSON/mídia/testes/docs (exceto este relatório) foi feita nesta etapa.  
Data da auditoria (UTC): 2026-09-29.

---

## 1. Status geral

```text
PASS_WITH_FINDINGS
```

O catálogo publicado, a mídia dos 136, o importer e o Room v9 estão coerentes com o estado esperado da Stage 4.9.  
Não há achado **CRITICAL** que impeça o funcionamento do APK com o pack atual.  
Há achados **HIGH/MEDIUM** a tratar **antes do primeiro commit consolidado** (caches/temporários não ignorados, documentação stale, higiene do `git add`).

---

## 2. Resumo executivo

| Severidade | Qtd |
| ---------- | --: |
| CRITICAL   | 0 |
| HIGH       | 3 |
| MEDIUM     | 4 |
| LOW        | 3 |
| INFO       | 8 |

---

## 3. Pipeline auditado

| Stage | Entrada | Saída | Esperado | Encontrado | Status |
| ----- | ------- | ----- | -------- | ---------- | ------ |
| Input FEDB | `tools/.../input/free-exercise-db/exercises.json` | dataset bruto | 876 IDs únicos | 876 / 0 dups | PASS |
| Analysis / Stage 2.5 | full-876 + `classify_importability.py` | `importability-stats.json` | 691+86+99 | 691 / 86 / 99 | PASS |
| Normalize | input + mappings | `gymtrack-exercises-full-876.json` (arquivo) + publish filtra | 876 | 876; IDs = raw | PASS |
| Translation | 777 importáveis | `translations.json` + names no full | 641/133/3 | 641/133/3/0 | PASS |
| Curation V2.1 | full + regras | `MAIN_CATALOG_V2_1.json` | 136 | 136; ⊂ 777; ∩ rejected=∅ | PASS |
| Media acquire | FEDB / cache | `app/.../assets/exercises/<id>/{0,1}.jpg` | 272 JPG | 272 válidos; 0 missing | PASS |
| Publish 4.9 | V2.1 + full-876 | `output/.../gymtrack-exercises.json` | 136 | 136; SHA estável | PASS |
| Android asset | publish output | `app/.../assets/exercises/gymtrack-exercises.json` | idêntico ao tools | bytes iguais | PASS |
| Importer | asset 136 | Room upsert | insertImported/updateImported | OK; sem delete de ausentes | PASS |
| Room | schema v9 | `externalSource`/`externalId` UNIQUE | v9; sem 9→10 | v9; MIGRATION_8_9 only | PASS |

Fonte de verdade do pack de produção: `MAIN_CATALOG_V2_1.json` → publish → asset JSON (136).  
Fonte de verdade Stage 2.5 / análise: `gymtrack-exercises-full-876.json` (não é asset de produção).

---

## 4. Catálogo

| Camada | Esperado | Encontrado |
| ------ | -------: | ---------: |
| Original FEDB | 876 | 876 |
| Importáveis Stage 2.5 | 777 (691+86) | 777 |
| Rejeitados Stage 2.5 | 99 | 99 |
| Publicados V2.1 | 136 | 136 |
| Importáveis não selecionados | 641 | 641 |
| IDs V2.1 = IDs publicados | sim | sim |
| Duplicatas identidade (pub) | 0 | 0 |
| Campos preservados vs full (136) | idênticos | `field_mismatch=0` |

Distribuição muscular V2.1 (contagens): Costas 18, Abdômen 16, Ombros/Peitoral/Quadríceps 14, Posteriores 10, Bíceps/Glúteos/Tríceps 9, Antebraço 7, Panturrilhas 6, Lombar/Trapézio 5 — **soma 136**, alinhada ao esperado da Stage 4.9.

SHA-256 do pack publicado:

```text
11134036b42db0ada619016f5fda35ab264ce50f3c7c38c55bd43552e51c22b8
```

Arquivo completo:

- `tools/exercise-import/output/free-exercise-db/gymtrack-exercises-full-876.json` (876)
- `app/src/test/resources/exercises/gymtrack-exercises-full-876.json` (bytes idênticos ao tools)

**Não** referenciado por `ExerciseCatalogImporter.ASSET_PATH`.

---

## 5. Tradução

Artefato: `tools/exercise-import/translate/translations.json`

| Métrica | Esperado | Encontrado |
| ------- | -------: | ---------: |
| Total | 777 | 777 |
| TRANSLATED | 641 | 641 |
| UNCHANGED | 133 | 133 |
| REVIEW | 3 | 3 |
| NOT_APPLICABLE | 0 | 0 |

Regras especiais (amostra no full / publicados):

| Regra | Resultado |
| ----- | --------- |
| Pulldown ↛ Puxada | Nenhum `*Pulldown*` com “Puxada” no `name` |
| Pull-Up → Barra Fixa | `externalId=Pullups` → `Barra Fixa` (no pack); ID canónico não é `Pull-Up` |
| Chin-Up → Barra Fixa Supinada | OK; está nos 136 |
| Face Pull permanece | OK; está nos 136 |

Nota: existem nomes com “Puxada” em exercícios **não**-Pulldown (`Kettlebell_Sumo_High_Pull`, `Overhead_Lat`) — tradução de *High Pull* / Lat overhead, **não** violação da regra Pulldown.

Identidade continua `(source, externalId)` no JSON / `(externalSource, externalId)` no Room — nome nunca usado como chave.

---

## 6. Mídia

| Métrica | Resultado |
| ------- | --------- |
| Exercícios publicados | 136 |
| Pastas em `app/.../assets/exercises/` (dirs) | 136 |
| `0.jpg` + `1.jpg` por selecionado | 136 + 136 |
| JPG totais sob assets | 272 |
| Missing | 0 |
| JPEG inválido / size 0 / HTML | 0 |
| Tamanho aproximado JPG no APK | ~16,26 MiB |
| `EXTRA_MEDIA` (pastas com JPG fora dos 136) | **0** |

Cache de aquisição (não é asset de produção):  
`tools/exercise-import/analysis/_media_download_cache_v2_1/` (~2,36 MiB, 42 JPG).

Imagens de origem locais em `input/.../exercises/`: **171** pastas (subset do FEDB) — coerente com aquisição remota/cache para completar os 136; não regenerado nesta auditoria.

---

## 7. Importer

| Item | Status |
| ---- | ------ |
| Asset | `exercises/gymtrack-exercises.json` → **136** |
| Não usa `full-876` | Confirmado |
| Identidade | `getByExternalIdentity(source, id)` |
| Insert path | `insertImported` (`ABORT`) |
| Update path | `updateImported` (WHERE id + identidade) |
| `ExerciseDao.insert` (`REPLACE`) | **Não** usado pelo importer |
| Delete de ausentes do pack | **Não** existe |
| Debug import | `importDefaultAsset()` → mesmo asset |

Política nos testes:

- `ExerciseCatalogImportPolicyTest`: full-876 → 777/99; published → 136/0 rejected
- `ExerciseCatalogImporterTest`: idempotência 136
- `DebugCatalogImportControllerTest`: sample 136/0

Os **641** não selecionados **não** são tratados como rejected no pack publicado (rejected=0 no asset).

---

## 8. Room

| Item | Status |
| ---- | ------ |
| `version` | **9** |
| Migration 9→10 | **Ausente** (correto) |
| Migrations registadas | até `MIGRATION_8_9` |
| `externalSource` / `externalId` | nullable |
| Índice | `UNIQUE(externalSource, externalId)` |
| PK | `id` AUTOINCREMENT |
| Manuais `(null, null)` | permitidos no modelo |
| Schema export | `app/schemas/.../9.json` (untracked) |

`ExerciseRepositoryImpl` continua a usar `dao.insert` (`REPLACE`) no caminho **CRUD manual** — separado do import de catálogo (ver achado LOW).

---

## 9. Referências stale / classificação numérica

### Produção (`app/src/main`, debug, UI, repos, VMs)

Busca por literais `777` / `876` / `full-876` como limite de catálogo: **nenhuma** ocorrência problemática.

| Local | Número | Classe |
| ----- | ------ | ------ |
| `ExerciseCatalogImporter.ASSET_PATH` | path 136 | VALID_SOURCE_DATA |
| Policy/Importer tests | 876/777/136/99 | VALID_TEST |
| `tools/exercise-import/README.md` | 876/777/136/641/99 | VALID_DOCUMENTATION |
| `PROJECT_CONTEXT.md` § pack 4.9 | 136/876/777/99 | VALID_DOCUMENTATION |
| `PROJECT_CONTEXT.md` rodapé (~L2000) “sem UI, sem tradução, sem imagens” | — | **STALE_REFERENCE** |
| `README.md` portfólio | menciona importador Free Exercise DB, não 136/JPG | STALE_REFERENCE (doc incompleta) |
| `translate/*` EXPECTED 777 | 777 | VALID_ANALYSIS |
| Curation/V1 tests | 777 baseline | VALID_TEST / analysis |
| `test_curate_main_catalog.test_main_catalog_json_matches_curate` | drift de `originalName` | VALID_TEST falhando (pré-existente) |

Nenhum **POTENTIAL_BUG** numérico em código de produção.

---

## 10. Arquivos temporários / caches

| Path | Classificação | Notas |
| ---- | ------------- | ----- |
| `tools/.../analysis/_media_download_cache_v2_1/` | **REMOVE_LATER** / **GITIGNORE_CANDIDATE** | Cache de download; untracked; ~2,36 MiB |
| `tools/.../analysis/_assets_pre_hash_v2_1.json` | **REMOVE_LATER** / **GITIGNORE_CANDIDATE** | Snapshot de hashes pré-aquisição |
| `tools/.../analysis/__pycache__/` | **GITIGNORE_CANDIDATE** | Bytecode Python |
| `tmp_diag_tap_settings.py` (raiz) | **REMOVE_LATER** | Script experimental untracked |
| `app/src/main/assets/exercises/**` | **KEEP_UNTRACKED** → versionar no commit | Runtime asset (136 JSON + 272 JPG) |
| `tools/exercise-import/output/**` | **KEEP_UNTRACKED** → versionar | Artefactos derivados oficiais |
| `tools/exercise-import/analysis/MAIN_*` / reports | **KEEP_UNTRACKED** → versionar | Analysis source-of-truth |
| `tools/exercise-import/input/**` | **KEEP_TRACKED** (quando commitado) | Dataset fonte |
| `app/src/test/resources/.../full-876.json` | **KEEP_UNTRACKED** → versionar | Test fixture |
| `app/src/release/.../DebugSettingsSection.kt` | **KEEP_UNTRACKED** → versionar | Stub release |

`.gitignore` atual **não** cobre caches `_media_*`, `_assets_pre_*`, nem `__pycache__` sob `tools/`.

---

## 11. Git

| Item | Valor |
| ---- | ----- |
| Branch | `main` |
| HEAD | `97069ee4ffcc3561c0f5eca45862cdbf5200d328` |
| Autor HEAD | Danilo B. Sales |
| Mensagem HEAD | `feat: improve exercise selection and text input UX` |
| Commit/push nesta etapa | **Não executados** |

### Modificados (tracked)

Inclui trabalho prévio + pipeline (amostra): `PROJECT_CONTEXT.md`, `README.md`, `app/build.gradle.kts`, Room/DAO/Entity, execution UI, testes androidTest/unit, etc. (`git diff --stat`: 25 files, +948/−111 nas tracked).

### Untracked (relevantes ao pipeline)

```text
app/schemas/.../9.json
app/src/androidTest/.../catalogimport/
app/src/debug/java/com/gymtrack/presentation/
app/src/main/assets/
app/src/main/java/com/gymtrack/data/catalogimport/
app/src/main/java/com/gymtrack/domain/catalogimport/
app/src/main/java/com/gymtrack/domain/exercise/
app/src/main/java/.../ExerciseAnimation.kt
app/src/main/java/.../ExerciseAssetImageLoader.kt
app/src/release/
app/src/test/java/com/gymtrack/data/
app/src/test/resources/
app/src/testDebug/
tmp_diag_tap_settings.py
tools/   (árvore completa exercise-import, incl. caches)
```

### Suspeitos / atenção no `git add`

- `tmp_diag_tap_settings.py` — não parece parte do produto
- `_media_download_cache_v2_1/`, `_assets_pre_hash_v2_1.json`, `__pycache__/` — não devem entrar no commit sem decisão explícita
- Binários JPG em assets (~16 MiB) — **esperados**; revisar tamanho do commit

### GITIGNORE_CANDIDATE

```text
tools/exercise-import/analysis/_media_download_cache_v2_1/
tools/exercise-import/analysis/_assets_pre_hash_v2_1.json
**/__pycache__/
*.pyc
tmp_diag_tap_settings.py   (ou /tmp_*.py)
```

*(Não alterado nesta etapa.)*

---

## 12. Secrets

```text
Nenhum SECRET_FOUND.
```

Busca por padrões tipicos (API keys, private keys, passwords) só encontrou usos legítimos da palavra “token” em NLP de tradução.  
`local.properties` continua ignorado pelo `.gitignore`.

---

## 13. Achados

### A-01 — HIGH
- **Arquivo:** `tools/exercise-import/analysis/_media_download_cache_v2_1/`
- **Descrição:** Cache de download de mídia untracked e **não** coberto por `.gitignore`.
- **Impacto:** Entraria no primeiro `git add tools/` acidentalmente (~2,36 MiB duplicando o que já está nos assets).
- **Ação recomendada:** Adicionar ao `.gitignore` e/ou excluir do staging; não versionar.

### A-02 — HIGH
- **Arquivo:** `tools/exercise-import/analysis/_assets_pre_hash_v2_1.json`
- **Descrição:** Artefacto auxiliar de auditoria pré-aquisição; untracked; não ignorado.
- **Impacto:** Ruído no commit; pouco valor permanente após Stage 4.9.
- **Ação recomendada:** `.gitignore` ou omitir do commit (`REMOVE_LATER`).

### A-03 — HIGH
- **Arquivo:** `tools/exercise-import/analysis/__pycache__/` (+ possíveis outros)
- **Descrição:** Bytecode Python não ignorado sob `tools/`.
- **Impacto:** Poluição do repositório / diffs inúteis.
- **Ação recomendada:** Ignorar `__pycache__/` / `*.pyc` globalmente.

### A-04 — MEDIUM
- **Arquivo:** `tmp_diag_tap_settings.py`
- **Descrição:** Script diagnóstico na raiz do repo, untracked.
- **Impacto:** Não faz parte do pipeline publicado.
- **Ação recomendada:** Não incluir no commit; apagar ou mover para fora do repo depois.

### A-05 — MEDIUM
- **Arquivo:** `PROJECT_CONTEXT.md` (~linha 2000)
- **Descrição:** Texto stale: importador “sem UI, sem tradução, sem imagens”.
- **Impacto:** Contradiz Debug Settings, nomes pt-BR no pack e 272 JPG no asset.
- **Ação recomendada:** Atualizar o rodapé/histórico para refletir Stages 4.3–4.9 (a secção do pack em ~1504 já está correta).

### A-06 — MEDIUM
- **Arquivo:** `README.md` (portfólio)
- **Descrição:** Menciona importador Free Exercise DB / Room 9, mas **não** documenta o pack publicado de **136** nem preview JPG 2 frames.
- **Impacto:** Doc pública incompleta face ao estado atual do app.
- **Ação recomendada:** Acrescentar 1–2 frases sobre catálogo curado V2.1 + animação local 0/1.jpg (sem alterar código).

### A-07 — MEDIUM
- **Arquivo:** processo de commit (`tools/` inteiro untracked)
- **Descrição:** Primeiro commit consolidado precisará de `git add` seletivo.
- **Impacto:** Risco de incluir caches/TMP se alguém fizer `git add tools` cego.
- **Ação recomendada:** Checklist de staging (incluir analysis oficiais, output, input, scripts; excluir A-01..A-04).

### A-08 — LOW
- **Arquivo:** `tools/exercise-import/curation/test_curate_main_catalog.py` → `test_main_catalog_json_matches_curate`
- **Descrição:** Falha pré-existente: `originalName` no JSON V1 (“3/4 Sit-Up”) ≠ regeneração atual (“Abdominal”) após Stage 4.8.
- **Impacto:** Ruído na suíte Python V1; **não** afeta pack 136 nem app.
- **Ação recomendada:** Regenerar `MAIN_CATALOG.json` ou relaxar asserção de `originalName` numa etapa futura.

### A-09 — LOW
- **Arquivo:** `ExerciseDao.insert` + `ExerciseRepositoryImpl`
- **Descrição:** CRUD manual ainda usa `@Insert(REPLACE)`.
- **Impacto:** Seguro se só usado para manuais; perigoso se reutilizado para catálogo (CASCADE). Importer **não** usa este path.
- **Ação recomendada:** Manter comentário/disciplina; eventual API separada no futuro.

### A-10 — LOW
- **Arquivo:** `publish_main_catalog_v2_1.py` → `PUBLISHED_CATALOG_V2_1.md`
- **Descrição:** Markdown de relatório inclui timestamp UTC (JSON do pack é determinístico).
- **Impacto:** Só o `.md` muda entre runs; SHA do JSON permanece estável.
- **Ação recomendada:** Opcional: remover timestamp do md numa limpeza futura.

### A-11 — INFO
- **Arquivo:** identidade Pull-Up
- **Descrição:** FEDB usa `externalId=Pullups`, não `Pull-Up`.
- **Impacto:** Nenhum; tradução `Barra Fixa` correta; presente nos 136.

### A-12 — INFO
- **Arquivo:** assets
- **Descrição:** `EXTRA_MEDIA=0` — só pastas dos 136 (sem órfãos JPG no APK).
- **Impacto:** Nenhum; limpeza destrutiva de extras **não** necessária agora.

### A-13 — INFO
- **Arquivo:** JSON pack
- **Descrição:** Campo `source` (não `externalSource`); parser mapeia corretamente.
- **Impacto:** Nenhum.

### A-14 — INFO
- **Arquivo:** Room UNIQUE + `(null,null)`
- **Descrição:** SQLite permite múltiplos manuais com ambos null.
- **Impacto:** Comportamento conhecido/desejado para manuais.

### A-15 — INFO
- **Arquivo:** `PROJECT_CONTEXT.md` ~1504
- **Descrição:** “Sem tradução, imagens ou instruções **no Room**” — correto no sentido de persistência (não guarda `sourceData`/JPG/instruções).
- **Impacto:** Não é stale se lido no contexto Room; nomes pt-BR vêm do JSON importado.

### A-16 — INFO
- **Arquivo:** input images
- **Descrição:** 171 pastas locais de mídia FEDB vs 876 exercícios.
- **Impacto:** Esperado; APK não depende de ter as 876 pastas no input.

### A-17 — INFO
- **Arquivo:** importer / bancos de desenvolvimento
- **Descrição:** Reimportar o pack 136 **não** remove os 641 antigos já no DB.
- **Impacto:** Instalação limpa → 136; DB legado pode ter >136 até limpeza manual/reinstall — **by design**.

### A-18 — INFO
- **Arquivo:** determinismo
- **Descrição:** `publish_main_catalog_v2_1.py` ordena por `(source, externalId)` e grava via `write_bytes` (evita CRLF no Windows). Scripts de translate/normalize usam ordenação explícita nos outputs principais. Não reexecutados com overwrite nesta auditoria.

---

## 14. Correções recomendadas

### BLOQUEADORAS (para o commit — não para o runtime)

1. Não versionar ` _media_download_cache_v2_1/ `, `_assets_pre_hash_v2_1.json`, `__pycache__/`, `tmp_diag_tap_settings.py` (A-01..A-04).  
2. Preferir atualizar `.gitignore` **antes** do `git add` (etapa futura — não feito aqui).

### ANTES DO COMMIT (recomendado)

1. Atualizar rodapé stale de `PROJECT_CONTEXT.md` (A-05).  
2. Atualizar `README.md` portfólio com pack 136 + mídia JPG 2 frames (A-06).  
3. Staging seletivo de `tools/` + `app/src/main/assets/` (A-07).  
4. Confirmar inclusão intencional dos ~16 MiB de JPG e do fixture full-876.

### PODEM FICAR PARA DEPOIS

1. Corrigir teste V1 `test_main_catalog_json_matches_curate` (A-08).  
2. Revisar uso futuro de `ExerciseDao.insert(REPLACE)` (A-09).  
3. Timestamp no `PUBLISHED_CATALOG_V2_1.md` (A-10).  
4. Qualquer limpeza adicional de analysis intermediários não listados como KEEP.

---

## Checklist de conclusão (4.10)

```text
[x] Pipeline 1→4.9 mapeado
[x] 876 → 777 → 136 verificado
[x] Traduções verificadas
[x] Mídia 272 verificada
[x] Importer verificado (sem REPLACE no path de import)
[x] Room v9 verificado
[x] Referências stale procuradas
[x] Caches/temporários identificados
[x] Documentação comparada
[x] Git auditado (somente leitura)
[x] Secrets procurados (nenhum)
[x] Este relatório criado
[x] Nenhuma outra alteração feita nesta etapa
[x] Nenhum commit/push
```

---

*Fim da auditoria Stage 4.10.*
