# exercise-import

Pipeline **offline** de preparação do catálogo a partir do [Free Exercise DB](https://github.com/yuhonas/free-exercise-db).

O normalizador **não** é executado pelo app. O Android consome apenas o JSON intermediário já gerado.

```text
input/free-exercise-db/    dataset e schema oficiais (raw)
analysis/                  relatórios das etapas 1 / 2.5 / 3A
normalize/                 normalização → JSON intermediário
output/free-exercise-db/   gymtrack-exercises.json + normalization-report.json
```

Cópia empacotada no app (artefacto, não a fonte canónica do pipeline):

```text
app/src/main/assets/exercises/gymtrack-exercises.json
```

### Catálogo publicado V2.3 (Wave 2)

Fonte de seleção atual: `analysis/MAIN_CATALOG_V2_3.json` (**183** exercícios = V2.2 **172** + Wave 2 **11**).

| Camada | Quantidade |
| --- | ---: |
| Free Exercise DB original | 876 |
| Stage 2.5 importáveis | 777 |
| Stage 2.5 rejeitados | 99 |
| Curadoria V2.1 (histórica) | 136 |
| Wave 1 adicionados | 36 |
| Pack V2.2 (histórico) | 172 |
| Pendentes Wave 1 (mídia) | 4 |
| Wave 2 adicionados | 11 |
| Excluído Wave 2 (`High_Cable_Curls`) | 1 |
| **Pack publicado no app** | **183** |

Relatórios históricos V2.1/V2.2 (`MAIN_CATALOG_V2_1.*`, `PUBLISHED_CATALOG_V2_1.md`, `MAIN_CATALOG_V2_2.json`, `PUBLISHED_CATALOG_V2_2.md`) permanecem e **não** foram reescritos.

O arquivo completo normalizado fica em `output/free-exercise-db/gymtrack-exercises-full-876.json` (análise / Stage 2.5 / traduções). Os importáveis não selecionados **não** são rejeitados — apenas ficam fora do pacote.

Publicar V2.1 (histórico; não sobrescreve V2.3 sem cuidado):

```text
py -3 tools/exercise-import/curation/publish_main_catalog_v2_1.py
py -3 -m unittest discover -s tools/exercise-import/curation -p "test_publish_main_catalog_v2_1.py"
```

Publicar Wave 1 / V2.2 (histórico; staging Wave 1 em `%TEMP%`):

```text
py -3 tools/exercise-import/curation/publish_wave1_v2_2.py
```

Publicar Wave 2 / V2.3 (requer staging de mídia aprovado em `%TEMP%\gymtrack_v22_wave2_media`):

```text
py -3 tools/exercise-import/curation/publish_wave2_v2_3.py
```

Relatório atual: `analysis/PUBLISHED_CATALOG_V2_3.md`. O pacote contém **183** exercícios com mídia (**366** JPG).

Imagens da fonte não são descarregadas nesta etapa. Nomes pt-BR já aplicados na Etapa 4.8. Instruções/imagens **não** entram no Room.

## Normalização (etapa 2)

Converte o JSON bruto num formato intermediário (`muscleGroup`, `secondaryMuscles`, `equipmentType`), **sem** traduzir nomes e **sem** gravar na base.

| Campo | Significado |
| --- | --- |
| `source` | `"free-exercise-db"` |
| `externalId` | slug string da fonte (não é PK Long) |
| `name` | nome de apresentação (pt-BR após Etapa 4.8; gerado a partir de `translations.json`) |
| `muscleGroup` / `equipmentType` | valores canónicos do GymTrack, ou `null` se não mapeável |
| `sourceData` | campos da fonte (inclui `null`); o Room **não** persiste este objecto |

### Publicar nomes pt-BR (Etapa 4.8)

Aplica `translate/translations.json` ao campo `name` do catálogo (sem alterar `sourceData`):

```text
py -3 tools/exercise-import/translate/test_apply_name_translations.py
py -3 tools/exercise-import/translate/apply_name_translations.py --copy-asset
```

Mapas: `normalize/mappings.py`.

## Importador Room (etapa 3B)

Código: `ExerciseCatalogImporter` (`importJson` / `importDefaultAsset`). **Não** há `RoomDatabase.Callback`, download, nem importação no arranque.

Política (Etapa 2.5, arquivo completo): **876** → **777** importáveis + **99** recusados (primário neck/abductors/adductors, ou `equipment` JSON `null`). Não inventar `Outro` para equipamento desconhecido.

Pacote Android publicado (V2.3): **183** importáveis, **0** recusados no JSON do asset (os 99 rejeitados Stage 2.5 e os não selecionados não entram no pack).

Fallback **no importador** (não no JSON): medicine/exercise ball e foam roll → `Outro`; e-z curl bar → `Barra`.

Identidade: `(externalSource, externalId)`, ambos preenchidos. Manuais `(null, null)` não são actualizados nem apagados. Upsert: `insertImported` (ABORT) ou `updateImported` (preserva `id` e identidade). **Não** usa `ExerciseDao.insert` (`REPLACE`). Exercício ausente do pack **permanece**. Duplicado no próprio pack falha **antes** de persistir.

Não há UI de importação nesta etapa.

## Auditoria de tradução pt-BR (etapa 4.3)

Somente análise dos **777** importáveis. Não reescreve o JSON normalizado nem o asset Android.

```text
py -3 tools/exercise-import/translate/test_audit_translation.py
py -3 tools/exercise-import/translate/audit_translation.py
```

Saída: `analysis/translation-audit.json`, `analysis/TRANSLATION_AUDIT.md`.
Glossário: `translate/glossary.json` (Pulldown permanece termo técnico; não traduzir como Puxada).
