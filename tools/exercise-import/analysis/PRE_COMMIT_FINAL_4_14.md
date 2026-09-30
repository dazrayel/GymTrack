# Stage 4.14 — Pre-Commit Final

Higiene final: aplicar OPÇÃO B (ignorar JPGs de `tools/input/.../exercises/`) e validar o estado pronto para commit **manual**.

Data (UTC): 2026-09-29

---

## Git hygiene

### Regra adicionada ao `.gitignore`

```text
tools/exercise-import/input/free-exercise-db/exercises/
```

Mantém versionáveis:

- `tools/exercise-import/input/free-exercise-db/exercises.json`
- `tools/exercise-import/input/free-exercise-db/schema.json`

Verificado com `git check-ignore`: JPGs/pasta **ignorados**; JSON/schema **não** ignorados.

### Caches / bytecode (já cobertos desde 4.11)

| Padrão | Status |
| ------ | ------ |
| `__pycache__/` | ignorado |
| `*.pyc` | ignorado |
| `_media_download_cache_v2_1/` | ignorado |
| `_assets_pre_hash_v2_1.json` | ignorado |
| `input/.../exercises/` | **ignorado (novo)** |

JPGs de input **permanecem no disco**; apenas saem do `git status`.

---

## Versionável (pronto para o commit manual)

### Código Android / Room / domínio

- Tracked modificados: `GymTrackDatabase`, DAOs, `ExerciseEntity`, `DatabaseModule`, repository, UI execução/detalhe/settings, strings, testes associados, `app/build.gradle.kts`
- Untracked: `data/catalogimport/*`, `domain/catalogimport/*`, `domain/exercise/ExerciseMedia.kt`, `ExerciseAnimation.kt`, `ExerciseAssetImageLoader.kt`, `RestCompletionBeep.kt`
- Debug/release: `DebugCatalogImport*`, `DebugSettingsSection` (debug + release stub)
- Schema: `app/schemas/com.gymtrack.data.local.GymTrackDatabase/9.json`

### Catálogo + mídia APK

- `app/src/main/assets/exercises/gymtrack-exercises.json`
- `app/src/main/assets/exercises/**/{0,1}.jpg` (**272** ficheiros)
- `tools/.../output/.../gymtrack-exercises.json` (136)
- `tools/.../output/.../gymtrack-exercises-full-876.json`
- `tools/.../output/.../normalization-report.json`
- Fixture: `app/src/test/resources/exercises/gymtrack-exercises-full-876.json`

### Pipeline `tools/exercise-import/`

- `README.md`
- `input/.../exercises.json` + `schema.json` (**sem** pasta `exercises/`)
- `normalize/`, `translate/`, `curation/` (scripts + testes Python)
- `analysis/` (MAIN_CATALOG*, MEDIA_*, stats, relatórios 4.10–4.14, classify/analyze)

### Documentação raiz / higiene

- `.gitignore`, `README.md`, `PROJECT_CONTEXT.md`

### Testes

- `ExerciseCatalogImporterTest`, `ExerciseCatalogImportPolicyTest`, `NormalizedExerciseCatalogParserTest`, `ExerciseMediaResolverTest`, `DebugCatalogImportControllerTest`, etc.

---

## Não versionável

| Item | Motivo |
| ---- | ------ |
| `tools/.../input/.../exercises/**` | OPÇÃO B — mirror local parcial (~21 MiB); APK é a fonte de mídia publicada |
| `_media_download_cache_v2_1/` | cache aquisição |
| `_assets_pre_hash_v2_1.json` | snapshot temporário |
| `__pycache__/` / `*.pyc` | bytecode |
| Pasta física de JPGs input | permanece local; **não** apagar nesta etapa |

---

## Catalog

| Item | Valor |
| ---- | ----- |
| Publicados | **136** |
| tools == asset | **sim** (source/externalId/name/sourceData) |
| JPG APK | **272** |
| Extra / missing / inválidos | **0 / 0 / 0** |
| SHA-256 | `11134036b42db0ada619016f5fda35ab264ce50f3c7c38c55bd43552e51c22b8` |

---

## Room

| Item | Status |
| ---- | ------ |
| `version = 9` | confirmado |
| `MIGRATION_8_9` | presente |
| `9.json` export | presente (untracked, versionável) |
| Migration nova nesta etapa | **não** |

---

## Build

| Comando | Resultado |
| ------- | --------- |
| `:app:compileDebugKotlin` | **OK** |
| `:app:testDebugUnitTest` | **OK** |
| `:app:assembleDebug` | **OK** |

---

## Known failure

```text
test_main_catalog_json_matches_curate
"3/4 Sit-Up" vs "Abdominal"
PRE-EXISTENTE / FORA DE ESCOPO
```

Teste **não** alterado.

---

## Git status

Snapshot de `git status --short --untracked-files=all` **antes** de criar este ficheiro (391 linhas: 26 `M` + 365 `??`).

Após este relatório, espera-se também:

```text
?? tools/exercise-import/analysis/PRE_COMMIT_FINAL_4_14.md
```

### Conteúdo completo do snapshot (4.14 pré-relatório)

```text
 M .gitignore
 M PROJECT_CONTEXT.md
 M README.md
 M app/build.gradle.kts
 M app/src/androidTest/java/com/gymtrack/WorkoutDetailNavigationTest.kt
 M app/src/androidTest/java/com/gymtrack/data/local/ExerciseDaoTest.kt
 M app/src/androidTest/java/com/gymtrack/data/local/ExerciseMigrationTest.kt
 M app/src/androidTest/java/com/gymtrack/data/repository/ExerciseRepositoryImplTest.kt
 M app/src/androidTest/java/com/gymtrack/workouts/detail/WorkoutDetailScreenTest.kt
 M app/src/main/java/com/gymtrack/data/local/GymTrackDatabase.kt
 M app/src/main/java/com/gymtrack/data/local/dao/ExerciseDao.kt
 M app/src/main/java/com/gymtrack/data/local/dao/ExerciseSecondaryMuscleDao.kt
 M app/src/main/java/com/gymtrack/data/local/entity/ExerciseEntity.kt
 M app/src/main/java/com/gymtrack/data/repository/ExerciseRepositoryImpl.kt
 M app/src/main/java/com/gymtrack/di/DatabaseModule.kt
 M app/src/main/java/com/gymtrack/domain/model/Exercise.kt
 M app/src/main/java/com/gymtrack/presentation/exercises/ExercisesScreen.kt
 M app/src/main/java/com/gymtrack/presentation/settings/SettingsScreen.kt
 M app/src/main/java/com/gymtrack/presentation/workouts/detail/WorkoutDetailScreen.kt
 M app/src/main/java/com/gymtrack/presentation/workouts/execution/WorkoutExecutionScreen.kt
 M app/src/main/java/com/gymtrack/presentation/workouts/execution/WorkoutExecutionUiState.kt
 M app/src/main/java/com/gymtrack/presentation/workouts/execution/WorkoutExecutionViewModel.kt
 M app/src/main/res/values/strings.xml
 M app/src/test/java/com/gymtrack/domain/model/ExerciseDomainModelTest.kt
 M app/src/test/java/com/gymtrack/presentation/exercises/ExerciseViewModelTest.kt
 M app/src/test/java/com/gymtrack/presentation/workouts/execution/WorkoutExecutionViewModelTest.kt
?? app/schemas/com.gymtrack.data.local.GymTrackDatabase/9.json
?? app/src/androidTest/java/com/gymtrack/data/catalogimport/ExerciseCatalogImporterTest.kt
?? app/src/debug/java/com/gymtrack/presentation/settings/DebugCatalogImportController.kt
?? app/src/debug/java/com/gymtrack/presentation/settings/DebugCatalogImportViewModel.kt
?? app/src/debug/java/com/gymtrack/presentation/settings/DebugSettingsSection.kt
?? app/src/main/assets/exercises/… (272 JPG + gymtrack-exercises.json)
?? app/src/main/java/com/gymtrack/data/catalogimport/…
?? app/src/main/java/com/gymtrack/domain/catalogimport/…
?? app/src/main/java/com/gymtrack/domain/exercise/ExerciseMedia.kt
?? app/src/main/java/com/gymtrack/presentation/exercises/ExerciseAnimation.kt
?? app/src/main/java/com/gymtrack/presentation/exercises/ExerciseAssetImageLoader.kt
?? app/src/main/java/com/gymtrack/presentation/workouts/execution/RestCompletionBeep.kt
?? app/src/release/java/com/gymtrack/presentation/settings/DebugSettingsSection.kt
?? app/src/test/java/com/gymtrack/data/catalogimport/…
?? app/src/test/java/com/gymtrack/domain/exercise/ExerciseMediaResolverTest.kt
?? app/src/test/resources/exercises/gymtrack-exercises-full-876.json
?? app/src/testDebug/java/com/gymtrack/presentation/settings/DebugCatalogImportControllerTest.kt
?? tools/exercise-import/README.md
?? tools/exercise-import/analysis/… (relatórios + MAIN_CATALOG* + MEDIA_* + stats)
?? tools/exercise-import/curation/… (scripts + testes)
?? tools/exercise-import/input/free-exercise-db/exercises.json
?? tools/exercise-import/input/free-exercise-db/schema.json
?? tools/exercise-import/normalize/…
?? tools/exercise-import/output/free-exercise-db/gymtrack-exercises.json
?? tools/exercise-import/output/free-exercise-db/gymtrack-exercises-full-876.json
?? tools/exercise-import/output/free-exercise-db/normalization-report.json
?? tools/exercise-import/translate/…
```

*Nota:* a listagem completa dos 272 JPG está no `git status` local; omitida aqui por tamanho. Contagem confirmada: **272** linhas `assets/exercises/*.jpg` + **0** linhas `input/.../exercises/`.

### Destaque — incluir no primeiro commit

1. `.gitignore` + docs (`README`, `PROJECT_CONTEXT`)  
2. Room v9 + importer + media UI + debug settings  
3. `app/src/main/assets/exercises/` (JSON 136 + 272 JPG)  
4. `tools/exercise-import/` **exceto** pastas/caches ignorados  
5. Testes e `9.json`  

### Confirmação Git

```text
sem git add
sem git commit
sem git push
sem reset / clean / restore / rebase
sem Co-authored-by Cursor
```

---

## Critério de sucesso

```text
[x] JPGs tools/input ignorados
[x] exercises.json + schema.json versionáveis
[x] caches/pyc ignorados
[x] catálogo 136 + 272 JPG + SHA preservados
[x] Room 9
[x] build OK
[x] teste V1 intacto
[x] PRE_COMMIT_FINAL_4_14.md criado
[x] nenhum commit/push
```
