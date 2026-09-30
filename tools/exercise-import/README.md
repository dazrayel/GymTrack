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

### Catálogo publicado V2.1 (Etapa 4.9)

Fonte de seleção: `analysis/MAIN_CATALOG_V2_1.json` (**136** exercícios).

| Camada | Quantidade |
| --- | ---: |
| Free Exercise DB original | 876 |
| Stage 2.5 importáveis | 777 |
| Stage 2.5 rejeitados | 99 |
| Curadoria V2.1 selecionados | 136 |
| Importáveis não selecionados (fora do pacote) | 641 |
| **Pack publicado no app** | **136** |

O arquivo completo normalizado fica em `output/free-exercise-db/gymtrack-exercises-full-876.json` (análise / Stage 2.5 / traduções). Os 641 importáveis não selecionados **não** são rejeitados — apenas ficam fora do pacote. Os 99 rejeitados Stage 2.5 continuam conceitualmente separados.

Publicar (determinístico; valida mídia `0.jpg`/`1.jpg` já adquirida):

```text
py -3 tools/exercise-import/curation/publish_main_catalog_v2_1.py
py -3 -m unittest discover -s tools/exercise-import/curation -p "test_publish_main_catalog_v2_1.py"
```

Relatório: `analysis/PUBLISHED_CATALOG_V2_1.md`. O pacote contém **136** exercícios com mídia (272 JPG).

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

Pacote Android publicado (Etapa 4.9): **136** importáveis, **0** recusados no JSON do asset (os 99 rejeitados e os 641 não selecionados não entram no pack).

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
