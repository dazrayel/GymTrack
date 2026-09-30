# Free Exercise DB — análise para importação (GymTrack)

**Etapa:** somente preparação. Este diretório **não** faz parte do código de produção do app.

**Fonte:** [yuhonas/free-exercise-db](https://github.com/yuhonas/free-exercise-db)  
**JSON:** `https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json`  
**Schema:** `schema.json` (baixado junto).  
**Análise gerada por:** `analysis/analyze_free_exercise_db.py` → `analysis/free-exercise-db-stats.json`

Nenhuma entidade Room, DAO, repository, ViewModel, UI ou migration foi alterada nesta etapa.

---

## 1. Estrutura atual do catálogo GymTrack

O catálogo **não** vem de JSON, assets, seed Room nem `createFromAsset`. A tabela `exercises` começa **vazia**. Exercícios existem só quando o utilizador os cria (tela Exercícios / `ExerciseViewModel.saveExercise` → `ExerciseRepository.save`).

| Peça | Ficheiro |
| --- | --- |
| Domain | `app/src/main/java/com/gymtrack/domain/model/Exercise.kt` |
| Catálogo fechado (músculos/equipamento) | `app/src/main/java/com/gymtrack/domain/model/ExerciseCatalog.kt` |
| Entity | `app/src/main/java/com/gymtrack/data/local/entity/ExerciseEntity.kt` |
| Secundários N:N | `app/src/main/java/com/gymtrack/data/local/entity/ExerciseSecondaryMuscleEntity.kt` |
| DAOs | `ExerciseDao.kt`, `ExerciseSecondaryMuscleDao.kt` |
| Repository | `ExerciseRepository.kt` / `ExerciseRepositoryImpl.kt` |
| UI cadastro | `ExercisesScreen.kt` + `ExerciseViewModel.kt` |
| Room | `GymTrackDatabase.kt` **versão 8**; `DatabaseModule.kt` (migrations 1→2 … 7→8; **sem** `RoomDatabase.Callback` / prepopulate) |
| Schema exportado | `app/schemas/com.gymtrack.data.local.GymTrackDatabase/8.json` |

### Modelo

`Exercise(id: Long = 0, name, muscleGroup, equipmentType, secondaryMuscles: List<String> = emptyList())`

`ExerciseEntity`: PK `id` **AUTOINCREMENT**, `name`, `muscleGroup`, `equipmentType`. **Sem** índice UNIQUE em `name`. **Sem** `externalId` / `externalSource`.

`exercise_secondary_muscles`: PK composta `(exerciseId, muscle)`, `ON DELETE CASCADE` para `exercises`.

`workout_exercises.exerciseId` → `exercises.id` **CASCADE**.  
`workout_session_exercises.exerciseId` → `exercises.id` **SET NULL**; o histórico usa snapshot `exerciseName` (e músculos/equipamento copiados em `startSession`).

### Constraints relevantes

- `muscleGroup` e `equipmentType` são TEXT NOT NULL; a UI restringe a `MUSCLE_GROUPS` / `EQUIPMENT_TYPES` (português). `normalizeMuscleGroup` / `normalizeEquipmentType` só mapeiam **sinónimos já listados**; valores desconhecidos **não** são forçados (ex. `"Legs"` permanece `"Legs"`).
- `sanitizedSecondaryMuscles`: remove vazio, remove o **primário**, `distinct`, `sorted`.
- `ExerciseDao.insert` usa `REPLACE`; `save` de edição usa `update` para não CASCADE-apagar relações.
- Nomes duplicados no catálogo **são permitidos** pelo schema.

### Testes relacionados ao catálogo (existentes)

- `ExerciseCatalogTest`, `ExerciseDomainModelTest`
- `ExerciseViewModelTest`
- `ExerciseDaoTest`, `ExerciseMigrationTest`, `ExerciseRepositoryImplTest`
- UI: `ExercisesScreenTest`, picker `WorkoutDetailScreenTest` / `ExercisePickerFilterTest`

---

## 2. Resumo do Free Exercise DB

Array JSON de **876** exercícios. `id` é **string** estilo slug (`3_4_Sit-Up`), não numérico. Nomes e instruções em **inglês**. `primaryMuscles` / `secondaryMuscles` são arrays de slugs em inglês. `equipment` pode ser `null`. `force` **não** está em `required` no schema, mas aparece nos registos (incluindo `null`).

Imagens **não** foram descarregadas; o JSON só contém caminhos relativos (`3_4_Sit-Up/0.jpg`).

---

## 3. Estatísticas (ficheiro `exercises.json`)

| Métrica | Valor |
| --- | --- |
| Tamanho | **1 005 327** bytes (~981,7 KiB) |
| Total de exercícios | **876** |
| IDs distintos | **876** |
| IDs duplicados | **nenhum** |
| Nomes duplicados | **nenhum** |
| IDs com padrão `^[0-9a-zA-Z_-]+$` | **876 / 876** |
| Campos no JSON | `id`, `name`, `force`, `level`, `mechanic`, `equipment`, `primaryMuscles`, `secondaryMuscles`, `instructions`, `category`, `images` |
| Schema `required` | `id`, `name`, `level`, `mechanic`, `equipment`, `primaryMuscles`, `secondaryMuscles`, `instructions`, `category`, `images` (**não** inclui `force`) |
| name vazio | **0** |
| equipment `null` | **77** |
| force `null` | **30** |
| mechanic `null` | **87** |
| primaryMuscles vazio | **0** |
| secondaryMuscles vazio | **272** |
| instructions vazio | **5** (`Iron_Cross`, `One-Arm_Kettlebell_Swings`, `Push_Press`, `Side_Bridge`, `Side_Jackknife`) |
| images vazio | **3** (`Kettlebell_Halo`, `Kettlebell_Halo_With_Overhead_Extension`, `Kettlebell_Overhead_Triceps_Extension`) |
| Referências de imagens | **1746** |
| Linhas com images duplicadas no mesmo exercício | **0** |
| `primaryMuscles` com mais de 1 valor | **1** (`Kettlebell_Halo_With_Overhead_Extension`: shoulders + triceps) |
| Sobreposição primary ∩ secondary | **9** (conflito com `sanitizedSecondaryMuscles`, que descarta o primário) |

### Valores distintos

**level:** beginner 525, intermediate 294, expert 57  

**force:** pull 371, push 371, static 104, null 30  

**mechanic:** compound 491, isolation 298, null 87  

**equipment:** barbell 170, dumbbell 123, other 122, body only 111, cable 81, null 77, machine 67, kettlebells 56, bands 20, medicine ball 17, exercise ball 12, foam roll 11, e-z curl bar 9  

**category:** strength 584, stretching 123, plyometrics 61, powerlifting 38, olympic weightlifting 35, strongman 21, cardio 14  

**primaryMuscles:** quadriceps 148, shoulders 129, abdominals 93, chest 84, hamstrings 79, triceps 73, biceps 53, lats 38, middle back 34, calves 28, lower back 27, forearms 25, glutes 22, traps 15, adductors 13, abductors 8, neck 8  

**secondaryMuscles:** glutes 220, shoulders 210, hamstrings 201, calves 181, triceps 148, lower back 104, forearms 94, traps 84, quadriceps 82, biceps 74, middle back 66, chest 63, abdominals 59, lats 56, adductors 41, abductors 35, neck 1  

---

## 4. Tabela de compatibilidade campo a campo

| Free Exercise DB | GymTrack atual | Compatível? | Tratamento necessário |
| ---------------- | -------------- | ----------- | --------------------- |
| `id` (string slug) | `Exercise.id` Long AUTOINCREMENT | Não direto | Guardar como identificador **externo**; não substituir a PK Room |
| `name` | `Exercise.name` | Parcial | Importar/traduzir; sem UNIQUE no Room — colisões com nomes do utilizador precisam de regra |
| `force` | sem equivalente | Não | Opcional futuro; ignorar no MVP de importação |
| `level` | sem equivalente | Não | Opcional futuro; ignorar no MVP |
| `mechanic` | sem equivalente | Não | Opcional futuro; ignorar no MVP |
| `equipment` (enum EN, nullable) | `equipmentType` (catálogo PT) | Parcial | Mapear + tratar `null` (ex. `Outro` ou recusar) |
| `primaryMuscles` (array) | `muscleGroup` (**um** TEXT) | Parcial | Escolher 1 primário; o caso com 2 primários precisa de regra |
| `secondaryMuscles` (array) | `exercise_secondary_muscles` | Parcial | Mapear; aplicar `sanitizedSecondaryMuscles`; arrays vazios são válidos no GymTrack |
| `instructions` | sem equivalente | Não | Fora do modelo atual; não persistir sem nova coluna |
| `category` | sem equivalente | Não | Fora do modelo atual |
| `images` | sem equivalente | Não | Não importar imagens nesta fase |
| — | `Exercise.id` interno | não fornecido pela fonte | Continua AUTOINCREMENT |
| — | `secondaryMuscles` como tabela N:N | derivado de `secondaryMuscles` da fonte | Mapping + sanitização |
| — | `externalSource` / `externalId` | não existem | Provável schema futuro (ver §8 e §11) |

---

## 5. Problemas relevantes para importação

- Idioma: dataset EN vs UI/catálogo PT.
- `primaryMuscles` é lista; GymTrack tem um único primário.
- Músculos FEDB sem sinónimo atual: `abdominals`, `abductors`, `adductors`, `lats`, `middle back`, `neck` (e `Costas`/`Abdômen` no GymTrack não batem 1:1 com um slug da fonte).
- Equipamento: `body only` ≠ `bodyweight` (sinónimo atual); `kettlebells` (plural) ≠ `kettlebell`; `null`; Smith **não** aparece na fonte; bola/foam/EZ bar sem valor no catálogo (candidatos a `Outro` ou novos tipos — **não** alterados agora).
- 77 equipamentos null; 5 sem instruções; 3 sem imagens.
- 9 exercícios com músculo ao mesmo tempo em primary e secondary.
- Categorias stretching/cardio/plyometrics misturadas com strength — o GymTrack não filtra por categoria.
- `REPLACE` no insert da PK **não** deve ser usado para “atualizar catálogo importado” sem `externalId` (risco de apagar o exercício errado / CASCADE em `workout_exercises`).

---

## 6. Mapeamento de músculos

Comparação **sem alterar** `MUSCLE_GROUPS` / `MUSCLE_SYNONYMS`.

| Free Exercise DB | GymTrack | Situação |
| ---------------- | -------- | -------- |
| `chest` | Peitoral | Compatível via sinónimo existente (`chest`) |
| `biceps` | Bíceps | Compatível (`biceps`) |
| `triceps` | Tríceps | Compatível |
| `shoulders` | Ombros | Compatível |
| `forearms` | Antebraço | Compatível (`forearms`) |
| `quadriceps` | Quadríceps | Compatível |
| `hamstrings` | Posteriores | Compatível |
| `glutes` | Glúteos | Compatível |
| `calves` | Panturrilhas | Compatível |
| `lower back` | Lombar | Compatível |
| `traps` | Trapézio | Compatível |
| `abdominals` | Abdômen | **Nomenclatura:** GymTrack tem `abs`/`abdomen`/`abdômen`, **não** `abdominals` |
| `lats` | Costas? | **Sem equivalente direto**; Costas no GymTrack mapeia `back`/`costas`, não `lats` |
| `middle back` | Costas? | **Sem equivalente direto** |
| `abductors` | — | Só na fonte; não existe no catálogo GymTrack |
| `adductors` | — | Só na fonte |
| `neck` | — | Só na fonte |
| — | Costas | Só no GymTrack como valor canónico; fonte usa lats/middle back/back (back não aparece como primary neste JSON) |
| — | Abdômen | Canónico GymTrack; fonte usa `abdominals` |

Colisões GymTrack: se o primário mapeado for igual a um secundário mapeado, `sanitizedSecondaryMuscles` remove o secundário (comportamento atual, desejável).

---

## 7. Mapeamento de equipamentos

| Free Exercise DB | GymTrack | Situação |
| ---------------- | -------- | -------- |
| `barbell` | Barra | Sinónimo existente |
| `dumbbell` | Halteres | Sinónimo existente |
| `machine` | Máquina | Sinónimo existente |
| `cable` | Cabos | Sinónimo existente |
| `bands` | Elástico | Sinónimo existente |
| `other` | Outro | Sinónimo existente |
| `body only` | Peso corporal | **Normalização nova** (`bodyweight` já existe; `body only` **não**) |
| `kettlebells` | Kettlebell | **Normalização nova** (fonte usa plural; GymTrack `kettlebell` singular) |
| `e-z curl bar` | Barra? / Outro? | Sem sinónimo; decisão futura |
| `medicine ball` | Outro? | Sem sinónimo |
| `exercise ball` | Outro? | Sem sinónimo |
| `foam roll` | Outro? | Sem sinónimo |
| `null` | — | Sem valor; precisa de política (`Outro` vs omitir exercício) |
| — | Smith | **Só no GymTrack**; a fonte não tem `smith` / `smith machine` neste JSON |

---

## 8. Análise de IDs

O `id` da fonte é string estável (slug), único nos 876 registos, adequado como **`externalId`**, não como PK Room.

**Não** usar o slug como `ExerciseEntity.id` (Long).

Estratégia conceptual (não implementada):

```text
id: Long                 // PK interna, AUTOINCREMENT
externalSource: String?  // ex. "free-exercise-db"
externalId: String?      // slug da fonte; NULL = criado pelo utilizador
name, muscleGroup, equipmentType
```

**Porquê necessário:** atualizar o catálogo importado sem apagar/reescrever linhas do utilizador; importação idempotente; UNIQUE `(externalSource, externalId)` no futuro. Sem isso, reimportar por nome é frágil (tradução, duplicados permitidos, CASCADE).

Sessões históricas: `exerciseId` SET NULL se o catálogo for apagado; o snapshot de nome permanece. Atualizar um exercício importado deve usar **UPDATE da mesma PK**, não REPLACE da linha.

---

## 9. Preservar exercícios do utilizador

- Catálogo atual = **só** criados na app (sem seed).
- Importação futura deve **inserir** linhas com `externalSource` preenchido, nunca `DELETE FROM exercises`.
- Upsert só onde `(externalSource, externalId)` já existe.
- `externalId IS NULL` (ou source null) = exercício do utilizador → **intocável** pelo job de atualização.
- Não apagar `workout_exercises` / sessões ao atualizar o dataset.
- Conflito de nome (utilizador já tem “Bench Press” e a fonte também): manter ambos ou matching opcional — **não** fundir automaticamente sem regra explícita.

---

## 10. Arquitetura proposta da pipeline (não implementada)

```text
Free Exercise DB (GitHub)
        ↓ download (CI ou script local)
tools/exercise-import/input/free-exercise-db/exercises.json   ← raw (já aqui)
        ↓ validation (schema, IDs únicos, required)
        ↓ normalization (null equipment, trim, primary único)
        ↓ mapping (músculos/equipamentos PT + regras GymTrack)
tools/exercise-import/output/gymtrack-seed.json               ← formato interno
        ↓ (futuro) import no app: assets OU first-run worker OU botão
Room exercises + exercise_secondary_muscles
        ↓
Catálogo UI (já existente) + exercícios manuais do utilizador
```

| Etapa | Onde deve viver |
| --- | --- |
| Download / raw | `tools/exercise-import/input/` — **fora** de `app/src/main` |
| Validation / mapping | `tools/exercise-import/` (script Python/Kotlin JVM) nesta fase |
| Seed gerado | `tools/exercise-import/output/` até haver decisão de copiar para `assets` |
| Aplicação no Room | **futuro**, camada Data (`ExerciseRepository` ou importer dedicado), **não** no Composable |
| UI | Continua a permitir criar exercício manual |

Atualizar o catálogo: reexecutar download → mapping → upsert por `externalId`. Sem cadastro manual obrigatório para o pack importado.

---

## 11. Migration (não criada)

**Provavelmente sim**, se se adotarem `externalSource` / `externalId` (e índice único).

- Tabela afetada: **`exercises`** (novas colunas nullable).
- **`exercise_secondary_muscles`**: sem mudança estrutural obrigatória.
- Versão Room atual **8** → futura **9** + `MIGRATION_8_9`.
- Sem essas colunas, um seed “cego” não distingue utilizador vs dataset.

Não há `MIGRATION_8_9` neste repositório; **não** foi adicionada nesta etapa.

---

## 12. Testes a criar na futura pipeline (não implementados agora)

- JSON válido vs `schema.json`; campos required presentes.
- IDs duplicados / nomes duplicados na fonte.
- Mapeamento de cada músculo e equipamento (incluindo `body only`, `kettlebells`, `abdominals`, `null`).
- `primaryMuscles` com 0, 1 e 2+ valores.
- Sobreposição primary/secondary.
- Importação **idempotente** (duas corridas, mesmas PKs externas).
- Exercícios do utilizador (`externalId == null`) intactos após update do pack.
- UPDATE do pack não dispara CASCADE em `workout_exercises`.
- Sessões: snapshot `exerciseName` inalterado; `exerciseId` SET NULL só se o catálogo for realmente apagado.
- Não exigir execução instrumentada nesta fase de tools.

---

## 13. Ficheiros criados nesta etapa

| Caminho | Função |
| --- | --- |
| `tools/exercise-import/README.md` | Aviso: não é produção |
| `tools/exercise-import/input/free-exercise-db/exercises.json` | Dataset raw |
| `tools/exercise-import/input/free-exercise-db/schema.json` | Schema oficial |
| `tools/exercise-import/analysis/analyze_free_exercise_db.py` | Análise automatizada |
| `tools/exercise-import/analysis/free-exercise-db-stats.json` | Saída da análise |
| `tools/exercise-import/analysis/REPORT.md` | Este relatório |

**Nenhum ficheiro em `app/src/main` foi modificado nesta tarefa.**
