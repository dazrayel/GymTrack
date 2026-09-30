# Decisões de compatibilidade (Etapa 2.5)

Análise **somente**. Nenhum importador Room, migration, Entity ou alteração em `app/`.

Fontes: código actual (`ExerciseEntity`, `ExerciseCatalog.kt`, `ExerciseRepositoryImpl`, `GymTrackDatabase` v8, `ExercisesScreen`, FKs), `gymtrack-exercises.json`, `normalization-report.json`. Classificação: `analysis/classify_importability.py` → `importability-stats.json`.

---

## 1. Resumo executivo

O GymTrack guarda exercícios com **PK Long interna**, `name` / `muscleGroup` / `equipmentType` **TEXT NOT NULL**, secundários em tabela N:N. **Não há seed.** Catálogo de músculos/equipamentos é lista fechada em Kotlin (`MUSCLE_GROUPS`, `EQUIPMENT_TYPES`, inclui **Outro**). Estatísticas por exercício usam **`exerciseName` no snapshot da sessão**, não o grupo muscular.

**Recomendação para a Etapa 3 (import):**

| Pergunta | Resposta |
| --- | --- |
| Importar agora | **691** `IMPORTABLE` + **86** `IMPORTABLE_WITH_FALLBACK` = **777** |
| Recusar | **99** `REJECTED` |
| `REQUIRES_REVIEW` | **0** sob esta política (EZ-bar tratado como fallback Barra) |
| `equipment = null` | **Recusar** (não usar `Outro`) |
| neck / abductors / adductors como **primário** | **Recusar** (não inventar grupo) |
| Os mesmos como **secundário** | Importar; **omitir** o secundário não mapeado |
| medicine/exercise ball, foam roll | Fallback **`Outro`** (já existe no catálogo) |
| e-z curl bar | Fallback **`Barra`** |
| Vários primários (1 caso) | **Manter** a regra da Etapa 2 |
| Overlap 65 | **9** overlap real na fonte; **56** colapso canónico Costas; dedupe é correcto |
| Identidade | `(source, externalId)` no Room **futuro**; manuais com `externalId` null |
| Remoção na fonte | **Não apagar** linhas Room (CASCADE em templates) |
| Rename na fonte | Actualizar `name` do catálogo; snapshots de sessão **não** mudam |

Etapa 3 **precisará** de migration (colunas de identidade + política de upsert). Isso **não** foi implementado aqui.

---

## 2. Estado actual do modelo GymTrack

Confirmado no código:

| Peça | Facto |
| --- | --- |
| Room | versão **8**; `exercises.id` AUTOINCREMENT; `name`, `muscleGroup`, `equipmentType` **NOT NULL** |
| Sem UNIQUE em `name` | Duplicados de nome são possíveis |
| Sem `externalId` | Não há identidade de fonte |
| Secundários | `exercise_secondary_muscles`; `sanitizedSecondaryMuscles` remove o primário |
| UI | Dropdowns `MUSCLE_GROUPS` / `EQUIPMENT_TYPES`; **Outro** existe e tem string de UI |
| Save | `ExerciseViewModel` recusa `muscleGroup` ou `equipmentType` em branco |
| Seed | **Não existe** (`DatabaseModule` só `addMigrations`) |
| Template | `workout_exercises.exerciseId` → `ON DELETE CASCADE` |
| Sessão | `workout_session_exercises.exerciseId` **nullable**, `ON DELETE SET NULL`; snapshot `exerciseName`, `muscleGroup`, `equipmentType` |
| Stats | `ExerciseStatsViewModel` filtra por **`exerciseName`**, não por grupo |

Não há filtro de catálogo por músculo nas listas Home/Histórico. Grupos aparecem como texto nos cards (detalhe, execução, resumo).

---

## 3. Análise de músculos

Valores canónicos: Peitoral, Costas, Ombros, Bíceps, Tríceps, Antebraço, Quadríceps, Posteriores, Glúteos, Panturrilhas, Lombar, Trapézio, Abdômen.

Não há enum Java; são `List<String>`. Migrar 6→7 normalizou sinónimos conhecidos; **não** criou Neck/Abductors/Adductors.

Adicionar um grupo novo implica: `ExerciseCatalog.kt`, UI (dropdowns), testes `ExerciseCatalogTest`, dados manuais antigos intactos, stats **não** quebram (não agrupam por músculo). Custo baixo de código, custo alto de **produto** (catálogo deixa de ser o de musculação clássica do app).

---

## 4. Decisão para `neck`

```text
DECISÃO: não mapear; recusar se for músculo primário.
JUSTIFICATIVA: nenhum valor de MUSCLE_GROUPS representa o pescoço. Costas/Trapézio distorcem.
IMPACTO: 8 exercícios com neck como primary (9 ocorrências no pack incluindo secondary).
RECOMENDAÇÃO PARA IMPORT: REJECTED se primary; se só secondary, omitir esse secundário (IMPORTABLE_WITH_FALLBACK).
```

Não recomendar grupo `Pescoço` na Etapa 3 salvo decisão de produto explícita (nova fase de catálogo).

---

## 5. Decisão para `abductors`

```text
DECISÃO: não mapear para Glúteos/Quadríceps; recusar como primário.
JUSTIFICATIVA: abdutores de anca ≠ glúteo nem quad. Não há “Anca” no catálogo.
IMPACTO: 8 primários; 43 ocorrências no pack.
RECOMENDAÇÃO PARA IMPORT: REJECTED se primary; secondary → drop + fallback.
```

---

## 6. Decisão para `adductors`

```text
DECISÃO: igual a abductors.
JUSTIFICATIVA: adutores não são Posteriores nem Quadríceps.
IMPACTO: 13 primários; 54 ocorrências.
RECOMENDAÇÃO PARA IMPORT: REJECTED se primary; secondary → drop + fallback.
```

**Não** criar músculo `Outro` só para caber no NOT NULL.

---

## 7. Análise de equipamentos

`EQUIPMENT_TYPES`: Barra, Halteres, Máquina, Smith, Cabos, Peso corporal, Kettlebell, Elástico, **Outro**.

UI: `Outro` usa `exercise_equipment_other`. Valores fora da lista ainda podem aparecer no dropdown se já estiverem na linha (legado).

Não há filtro de equipamento nas stats. Impacto de novos tipos: catálogo + UI + testes; explosão de tipos prejudica UX.

### Opções (não implementadas)

| Opção | Domínio/UI | Banco | Fontes futuras | Risco |
| --- | --- | --- | --- | --- |
| A — usar `Outro` existente | Zero schema | Zero | Boa para “resto” | Perde o tipo exacto (fica em `sourceData`) |
| B — tipos específicos (bola, foam, EZ) | 3–4 valores novos | Sem migration de coluna | Melhor precisão | Catálogo inchado; tradução |
| C — recusar | Nenhum | Nenhum | Pack menor | Perde volume útil |
| D — `equipmentType` nullable | Migration + UI + VM | Room 9 | Modela “desconhecido” | Exercícios manuais e NOT NULL actual |

**Preferência:** A para bola/foam; EZ → Barra (B semântico sem novo tipo); C para `null` da fonte.

---

## 8–11. Equipamentos sem mapa específico

### medicine ball

```text
DECISÃO: fallback Outro no importador (não no JSON desta etapa).
JUSTIFICATIVA: Outro já existe; não é Barra/Halteres/Máquina. Recusar 17 exercícios é pior que perda de especificidade.
IMPACTO: 17 exercícios; sourceData.equipment permanece "medicine ball".
RECOMENDAÇÃO: IMPORTABLE_WITH_FALLBACK.
```

### exercise ball

```text
DECISÃO: fallback Outro.
JUSTIFICATIVA: igual à medicine ball (12).
RECOMENDAÇÃO: IMPORTABLE_WITH_FALLBACK.
```

### foam roll

```text
DECISÃO: fallback Outro.
JUSTIFICATIVA: não há “rolo” no catálogo (11).
RECOMENDAÇÃO: IMPORTABLE_WITH_FALLBACK.
```

### e-z curl bar

```text
DECISÃO: fallback Barra (não Outro).
JUSTIFICATIVA: é uma barra de curl, não “outro aparelho”. Distinto de medicine ball.
PONTO CONTROVERSO: utilizador pode querer tipo próprio; Etapa 3 pode mudar para Outro sem schema.
RECOMENDAÇÃO: IMPORTABLE_WITH_FALLBACK (9).
```

### `equipment = null`

Confirmado: `ExerciseEntity.equipmentType` **NOT NULL**; ViewModel recusa blank.

`null` na fonte **não** é o valor `other` (122 `other` já mapeados para Outro).

```text
DECISÃO: recusar import (não gravar Outro, não nullable nesta fase).
JUSTIFICATIVA: não inventar equipamento. Muitos são alongamentos sem implementação declarada.
IMPACTO: 77 na fonte; 70 REJECTED só por null (7 também têm primary inválido).
ALTERNATIVAS rejeitadas: Outro (mente); nullable (migration + UX “sem equipamento” sem desenho).
RECOMENDAÇÃO: REJECTED até haver campo “não especificado” de produto.
```

---

## 13. `muscleGroup` inválido / null

Confirmado NOT NULL no schema 8 e no save.

```text
Opção A Recusar — RECOMENDADA (29 primários neck/abductors/adductors).
Opção B Grupo muscular Outro — NÃO: não existe no catálogo; misturaria com abdômen/costas.
Opção C Nullable — NÃO na Etapa 3: UI/dropdown obrigatório; migration alargada.
```

Não inventar primário para satisfazer o SQLite.

---

## 14. Múltiplos primary muscles

Um caso: `Kettlebell_Halo_With_Overhead_Extension` (shoulders + triceps) → Ombros + Tríceps secundário.

`muscleGroup` na app é **um** dropdown; `secondaryMuscles` já é “também envolvidos” (`sanitizedSecondaryMuscles`). Perda: deixa de haver dois primários iguais — o modelo **já não** suporta isso.

```text
DECISÃO: manter a regra da Etapa 2.
JUSTIFICATIVA: alinhada ao domínio; sourceData conserva o array original.
```

---

## 15. Overlap primary/secondary

Os **65** do relatório **não** são todos erros.

| Tipo | Quantidade | Significado |
| --- | --- | --- |
| `source_string_overlap` | **9** | O mesmo token na fonte em primary e secondary (já previsto; o app também remove o primário dos secundários) |
| `canonical_collapse` | **56** | Ex.: primary `lats` + secondary `middle back` → ambos **Costas** |

Remover o secundário igual ao `muscleGroup` é **aceitável** e igual a `sanitizedSecondaryMuscles`.

Não tratar os 56 como defeito de dados da fonte.

---

## 16. Secondary duplicados (6)

Causa: `lats` e `middle back` → ambos **Costas**; a primeira ocorrência mantém-se.

| externalId | Primário fonte | Secundários fonte (relevantes) | Resultado |
| --- | --- | --- | --- |
| Barbell_Deadlift | lower back → Lombar | lats, middle back, … | um **Costas** |
| Barbell_Rear_Delt_Row | shoulders | lats, middle back | **Costas** uma vez |
| Pin_Presses | triceps | lats, middle back | **Costas** uma vez |
| Reverse_Band_Bench_Press | triceps | lats, middle back | **Costas** uma vez |
| Scapular_Pull-Up | traps | lats, middle back | **Costas**; equipment null → **REJECTED** no import |
| Sledgehammer_Swings | abdominals | lats, middle back | **Costas** uma vez |

```text
DECISÃO: manter dedupe, primeira posição.
```

---

## 17. Estratégia `source + externalId`

PK Room continua Long. Identidade de pack: `source` (`free-exercise-db`) + `externalId` (slug).

Manuais: `externalId` / `source` **NULL**.

Não usar `name` como chave (tradução, homónimos). Não usar PK como id da fonte (reinstalar / dois devices).

JSON actual **não** tem `sourceVersion`; para detectar pack v2 vs v1 o importador deve receber versão/checksum **no job**, não necessariamente em cada linha.

---

## 18. Preservação de exercícios manuais

Upsert **somente** `WHERE source = ? AND externalId = ?`.

Nunca `DELETE FROM exercises` em massa. Nunca fazer match por nome com linhas `externalId IS NULL`.

Utilizador com “Bench Press” manual e pack com o mesmo nome → **duas linhas**.

---

## 19. Exercícios removidos da fonte

`workout_exercises` tem **CASCADE**. Apagar o exercício do catálogo **apaga o item do treino**.

Sessões: SET NULL no `exerciseId`; o nome no histórico permanece.

```text
Opção A Deletar — PERIGOSA (templates).
Opção B Manter — RECOMENDADA na Etapa 3 (órfãos importados ficam).
Opção C Inactivo — melhor a médio prazo (coluna nova); não é obrigatória para o primeiro import.
```

---

## 20. Exercícios renomeados

`externalId` igual, `name` muda → **UPDATE** do `name` no catálogo.

Histórico: `exerciseName` no snapshot **não** segue o catálogo (cópia em `startSession`). Stats por nome antigo **não** juntam automaticamente com o nome novo — limitação já existente se o utilizador editar o nome à mão.

Não versionar nomes na Etapa 3.

---

## 21. Instruções e imagens

Manter em `sourceData` no JSON. **Não** importar para Room na Etapa 3 (sem colunas). Offline: imagens seriam assets/ficheiros à parte; tradução de instructions é etapa posterior. Não descarregar imagens agora.

---

## 22. Tradução futura

Modelo conceptual (não implementar): `externalId` estável; `name` canónico EN no pack; tabela ou campos `name_pt` / `instructions_pt` depois. Stats continuam frágeis se o nome visível mudar — possível chave de stats = `externalId` no snapshot **muito mais tarde**.

---

## 23. Classificação dos 876

Política da secção 1 (não altera o JSON normalizado). `REQUIRES_REVIEW` não é usado.

```text
IMPORTABLE:               691
IMPORTABLE_WITH_FALLBACK:  86
REQUIRES_REVIEW:            0
REJECTED:                  99
TOTAL:                    876
```

REJECTED: 29 unmapped primary + 70 só `equipment` null (7 linhas têm os dois; classificadas no primário).

FALLBACK (razões, uma linha pode ter várias no script mas as contagens de motivo somam 86 neste pack): 38 drop secondary; 38 Outro; 9 EZ→Barra; 1 multi-primary.

---

## 24. Contrato para o importador Room

### Identidade

`(source, externalId)` único para importados. PK `exercises.id` interna.

### Exercícios manuais

`source` e `externalId` NULL. Fora do upsert do pack.

### Actualização

UPDATE nome, músculo, equipamento, secundários **só** nas linhas com o mesmo `(source, externalId)`. Usar `ExerciseDao.update`, **não** INSERT REPLACE da PK (CASCADE).

### Removidos da fonte

Não apagar. (Inactivo = fase seguinte.)

### Inválidos (REJECTED)

Não inserir. Não preencher NOT NULL com sentinelas.

### Campos obrigatórios para INSERT

`name` não vazio; `muscleGroup` ∈ `MUSCLE_GROUPS`; `equipmentType` ∈ `EQUIPMENT_TYPES`; `externalId` string; `source` constante.

### Fallbacks permitidos

- EZ curl → Barra  
- medicine/exercise ball, foam roll → Outro  
- secundários neck/abductors/adductors omitidos  
- multi-primary: primeira → `muscleGroup`  

### Fallbacks **não** permitidos

- `equipment` null → Outro  
- neck/abductors/adductors → Costas/Glúteos/etc.  
- `muscleGroup` Outro inventado  

### Só no JSON por agora

`sourceData` (force, level, mechanic, category, instructions, images, arrays originais).

### Histórico

Não reescrever `workout_session_exercises` no import. Não apagar catálogo usado em templates.

### Idempotência

Correr o mesmo pack duas vezes: mesmas linhas importadas (mesmo `externalId` → mesmo `id` Room), sem duplicar, sem tocar em manuais.

---

## 25. Riscos

- 99 exercícios ficam de fora (alongamentos / anca / pescoço).  
- Outro agrupa bola e foam (perda de detalhe).  
- EZ→Barra pode ser contestado.  
- Rename parte stats por `exerciseName`.  
- Sem coluna inactivo, pack menor não limpa catálogo (lixo aceite).  
- JSON sem `sourceVersion` por linha.

---

## 26. Recomendação final

1. Importar **777** exercícios com as regras acima.  
2. Recusar **99**.  
3. Etapa 3: migration `exercises` + `source`/`externalId` (nullable), unique index parcial ou único em par; importer na camada Data; **sem** UI obrigatória no primeiro cut.  
4. Não alargar `MUSCLE_GROUPS` só por causa deste pack.  
5. Não tornar `equipmentType` nullable só para 77 nulls.  
6. Manter JSON intermediário; o importador aplica fallbacks de equipamento (ainda `null` no ficheiro actual para bola/EZ).  
7. Tradução e imagens **depois** do import estrutural.

### Ainda controversos

- EZ curl: Barra vs Outro vs tipo novo.  
- Se produto quiser alongamentos com `equipment` null como **Peso corporal**.  
- Expansão do catálogo (Pescoço, Abdutores, Adutores) vs pack reduzido.  
- Soft-delete na primeira versão do importer vs só “não apagar”.

---

## O que a Etapa 3 terá de fazer (não feito agora)

- Migration Room 8→9 (identidade).  
- Upsert por `(source, externalId)`.  
- Aplicar fallbacks de equipamento no import, ou re-normalizar o JSON.  
- Filtrar REJECTED.  
- Testes de idempotência, manuais intactos, CASCADE.

Produção: **zero** ficheiros `app/` alterados nesta etapa.
