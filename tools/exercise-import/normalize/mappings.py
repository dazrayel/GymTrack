"""Mapeamentos Free Exercise DB → valores canónicos do catálogo GymTrack.

Os valores de destino devem coincidir exactamente com MUSCLE_GROUPS / EQUIPMENT_TYPES
em app/.../ExerciseCatalog.kt. Este módulo NÃO altera o aplicativo.

Decisões explícitas (pipeline, não domínio):

Músculos
--------
- abdominals / abs / abdomen → Abdômen
  (o app já tem abs/abdomen; a fonte usa abdominals)
- lats → Costas
  (GymTrack só tem o grupo Costas; lats não existe como valor separado)
- middle back → Costas
  (região da coluna torácica/média; lower back continua Lombar)
- neck, abductors, adductors → sem mapeamento
  (não existem no catálogo; não inventar grupos)

Equipamento
-----------
- body only → Peso corporal  (a fonte não usa bodyweight)
- kettlebells → Kettlebell   (plural na fonte)
- null → null                (não inventar)
- medicine ball, exercise ball, foam roll, e-z curl bar → sem mapeamento
  (não forçar Outro nesta etapa)
"""

from __future__ import annotations

# Exact GymTrack catalog values (do not invent new ones).
GYMTRACK_MUSCLE_GROUPS: frozenset[str] = frozenset(
    {
        "Peitoral",
        "Costas",
        "Ombros",
        "Bíceps",
        "Tríceps",
        "Antebraço",
        "Quadríceps",
        "Posteriores",
        "Glúteos",
        "Panturrilhas",
        "Lombar",
        "Trapézio",
        "Abdômen",
    }
)

GYMTRACK_EQUIPMENT_TYPES: frozenset[str] = frozenset(
    {
        "Barra",
        "Halteres",
        "Máquina",
        "Smith",
        "Cabos",
        "Peso corporal",
        "Kettlebell",
        "Elástico",
        "Outro",
    }
)

# Keys: lowercase source tokens. Values: GymTrack catalog strings.
MUSCLE_GROUP_MAP: dict[str, str] = {
    "peito": "Peitoral",
    "chest": "Peitoral",
    "peitoral": "Peitoral",
    "back": "Costas",
    "costas": "Costas",
    "lats": "Costas",
    "middle back": "Costas",
    "shoulders": "Ombros",
    "ombros": "Ombros",
    "biceps": "Bíceps",
    "bíceps": "Bíceps",
    "triceps": "Tríceps",
    "tríceps": "Tríceps",
    "forearm": "Antebraço",
    "forearms": "Antebraço",
    "antebraço": "Antebraço",
    "antebraços": "Antebraço",
    "quadriceps": "Quadríceps",
    "quads": "Quadríceps",
    "quadríceps": "Quadríceps",
    "hamstrings": "Posteriores",
    "posteriores": "Posteriores",
    "posteriores de coxa": "Posteriores",
    "glutes": "Glúteos",
    "glúteos": "Glúteos",
    "calves": "Panturrilhas",
    "panturrilhas": "Panturrilhas",
    "lower back": "Lombar",
    "lombar": "Lombar",
    "traps": "Trapézio",
    "trapézio": "Trapézio",
    "abs": "Abdômen",
    "abdomen": "Abdômen",
    "abdômen": "Abdômen",
    "abdominals": "Abdômen",
}

EQUIPMENT_TYPE_MAP: dict[str, str] = {
    "barra": "Barra",
    "barbell": "Barra",
    "halteres": "Halteres",
    "dumbbell": "Halteres",
    "dumbbells": "Halteres",
    "máquina": "Máquina",
    "maquina": "Máquina",
    "machine": "Máquina",
    "smith": "Smith",
    "smith machine": "Smith",
    "cabo": "Cabos",
    "cabos": "Cabos",
    "cable": "Cabos",
    "cables": "Cabos",
    "peso corporal": "Peso corporal",
    "bodyweight": "Peso corporal",
    "body only": "Peso corporal",
    "kettlebell": "Kettlebell",
    "kettlebells": "Kettlebell",
    "elástico": "Elástico",
    "elastico": "Elástico",
    "band": "Elástico",
    "bands": "Elástico",
    "outro": "Outro",
    "other": "Outro",
}

SOURCE_NAME = "free-exercise-db"

# Documented rule when primaryMuscles has more than one value:
# muscleGroup := mapped value of the first source primary (list order).
# Remaining mapped primaries that differ from muscleGroup are appended to
# secondaryMuscles (then overlap/dedupe rules apply). Original arrays stay in sourceData.
MULTIPLE_PRIMARY_RULE = (
    "first_source_primary_becomes_muscleGroup; extra mapped primaries append to secondaryMuscles"
)
