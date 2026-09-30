# Etapa 3A — Identidade externa (Room 8 → 9)

Infraestrutura apenas. **Não há importador.** Nenhum exercício do Free Exercise DB é inserido nesta etapa.

## Schema

- Versão Room: **9**
- Migration: `MIGRATION_8_9` (ALTER TABLE + índice único; sem rebuild da tabela `exercises`)
- Colunas: `externalSource TEXT`, `externalId TEXT` (ambas nullable)
- PK: `id` Long AUTOINCREMENT (inalterada)
- Índice: `UNIQUE (externalSource, externalId)`

## Identidade

| Tipo | `externalSource` | `externalId` |
| --- | --- | --- |
| Manual | `null` | `null` |
| Importado (futuro) | ex. `free-exercise-db` | slug da fonte |

Identidade lógica de importados: `(externalSource, externalId)`. Fontes diferentes podem partilhar o mesmo `externalId`.

## SQLite e NULL no índice único

O SQLite considera cada `NULL` distinto no UNIQUE:

- Vários `(null, null)` — **permitido** (manuais).
- Vários `(null, "abc")` — **permitido** (identidade incompleta; o importador não deve produzir isto).
- Vários `("free-exercise-db", null)` — **permitido** (idem).
- Dois `("free-exercise-db", "abc")` — **rejeitado** com INSERT ABORT.

## Risco para a Etapa 3B

`ExerciseDao.insert` usa `OnConflictStrategy.REPLACE`. Um segundo insert com a mesma identidade externa **apaga** a linha anterior e pode disparar CASCADE em `workout_exercises`. O importador deve usar `update` (ou upsert sem REPLACE) após localizar por `(externalSource, externalId)`.
