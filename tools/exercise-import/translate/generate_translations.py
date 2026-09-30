"""Generate tools/exercise-import/translate/translations.json — Etapa 4.4.

Produces a deterministic pt-BR translation catalog for the 777 importable
exercises from the Free Exercise DB.

Input:
  - tools/exercise-import/output/free-exercise-db/gymtrack-exercises.json
  - tools/exercise-import/analysis/translation-audit.json
  - tools/exercise-import/translate/glossary.json  (used for metadata only)

Output:
  - tools/exercise-import/translate/translations.json

Rules:
  - Exactly 777 entries (importable only).
  - Sorted by (source, externalId) — fully deterministic.
  - No timestamps, no UUIDs, no random elements.
  - source and externalId copied verbatim from catalog.
  - name derived from audit suggestedName (cleaned).
  - instructions translated via translate_instructions engine.
  - status: TRANSLATED | UNCHANGED | REVIEW | NOT_APPLICABLE.

Constraints:
  - NEVER modifies gymtrack-exercises.json or any app/ files.
  - NEVER modifies tools/exercise-import/output/ files.
  - Does NOT use external APIs.
"""

from __future__ import annotations

import json
import re
import sys
from collections import OrderedDict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "analysis"))
sys.path.insert(0, str(ROOT / "translate"))
sys.path.insert(0, str(ROOT / "curation"))

from catalog_paths import analysis_catalog_path  # noqa: E402
from classify_importability import classify as classify_importability
from translate_instructions import translate_instructions

NORMALIZED = analysis_catalog_path()
AUDIT_JSON = ROOT / "analysis" / "translation-audit.json"
OUT_FILE = ROOT / "translate" / "translations.json"
RAW_SOURCE = ROOT / "input" / "free-exercise-db" / "exercises.json"

IMPORTABLE_LABELS = frozenset({"IMPORTABLE", "IMPORTABLE_WITH_FALLBACK"})

VERSION = 1

# ---------------------------------------------------------------------------
# Status constants
# ---------------------------------------------------------------------------

STATUS_TRANSLATED = "TRANSLATED"
STATUS_UNCHANGED = "UNCHANGED"
STATUS_REVIEW = "REVIEW"
STATUS_NOT_APPLICABLE = "NOT_APPLICABLE"

# ---------------------------------------------------------------------------
# EXERCISE_OVERRIDES — explicit per-externalId decisions (Etapa 4.4.1)
#
# Format: externalId -> (STATUS, "pt-BR name")
#
# Rules:
#   TRANSLATED  — verified Portuguese translation
#   UNCHANGED   — deliberate decision to keep English technical term
#   REVIEW      — still ambiguous / insufficient context
#
# Overrides take priority over the generic logic in _determine_status_and_name.
# ---------------------------------------------------------------------------

EXERCISE_OVERRIDES: dict[str, tuple[str, str]] = {

    # ---- OLYMPIC LIFTS — UNCHANGED (technical terms used in Brazil) --------
    "Bottoms-Up_Clean_From_The_Hang_Position": (STATUS_UNCHANGED, "Bottoms-Up Clean da Posição de Suspensão"),
    "Clean_Pull":                    (STATUS_UNCHANGED, "Clean Pull"),
    "Clean_from_Blocks":             (STATUS_UNCHANGED, "Clean dos Blocos"),
    "Hang_Clean_-_Below_the_Knees": (STATUS_UNCHANGED, "Hang Clean Abaixo dos Joelhos"),
    "Hang_Snatch_-_Below_Knees":    (STATUS_UNCHANGED, "Hang Snatch Abaixo dos Joelhos"),
    "Heaving_Snatch_Balance":        (STATUS_UNCHANGED, "Heaving Snatch Balance"),
    "Jerk_Balance":                  (STATUS_UNCHANGED, "Jerk Balance"),
    "Kettlebell_Dead_Clean":         (STATUS_UNCHANGED, "Dead Clean com Kettlebell"),
    "Muscle_Snatch":                 (STATUS_UNCHANGED, "Muscle Snatch"),
    "One-Arm_Open_Palm_Kettlebell_Clean": (STATUS_UNCHANGED, "Open Palm Clean Unilateral com Kettlebell"),
    "Open_Palm_Kettlebell_Clean":    (STATUS_UNCHANGED, "Open Palm Clean com Kettlebell"),
    "Power_Clean_from_Blocks":       (STATUS_UNCHANGED, "Power Clean dos Blocos"),
    "Power_Jerk":                    (STATUS_UNCHANGED, "Power Jerk"),
    "Power_Snatch_from_Blocks":      (STATUS_UNCHANGED, "Power Snatch dos Blocos"),
    "Smith_Machine_Hang_Power_Clean":(STATUS_UNCHANGED, "Hang Power Clean na Máquina Smith"),
    "Snatch_Balance":                (STATUS_UNCHANGED, "Snatch Balance"),
    "Snatch_Pull":                   (STATUS_UNCHANGED, "Snatch Pull"),
    "Snatch_from_Blocks":            (STATUS_UNCHANGED, "Snatch dos Blocos"),

    # ---- SMR — TRANSLATED (Self Myofascial Release) -----------------------
    "Anterior_Tibialis-SMR": (STATUS_TRANSLATED, "Liberação Miofascial do Tibial Anterior"),
    "Brachialis-SMR":        (STATUS_TRANSLATED, "Liberação Miofascial do Braquial"),
    "Calves-SMR":            (STATUS_TRANSLATED, "Liberação Miofascial de Panturrilhas"),
    "Foot-SMR":              (STATUS_TRANSLATED, "Liberação Miofascial do Pé"),
    "Hamstring-SMR":         (STATUS_TRANSLATED, "Liberação Miofascial de Posteriores"),
    "Latissimus_Dorsi-SMR":  (STATUS_TRANSLATED, "Liberação Miofascial do Latíssimo Dorsal"),
    "Lower_Back-SMR":        (STATUS_TRANSLATED, "Liberação Miofascial da Lombar"),
    "Peroneals-SMR":         (STATUS_TRANSLATED, "Liberação Miofascial dos Fibulares"),
    "Piriformis-SMR":        (STATUS_TRANSLATED, "Liberação Miofascial do Piriforme"),
    "Quadriceps-SMR":        (STATUS_TRANSLATED, "Liberação Miofascial de Quadríceps"),
    "Rhomboids-SMR":         (STATUS_TRANSLATED, "Liberação Miofascial dos Romboides"),

    # ---- THROW — TRANSLATED (arremesso) -----------------------------------
    "Backward_Medicine_Ball_Throw":   (STATUS_TRANSLATED, "Arremesso com Bola Medicinal para Trás"),
    "Catch_and_Overhead_Throw":       (STATUS_TRANSLATED, "Pegar e Arremessar Acima da Cabeça"),
    "Medicine_Ball_Scoop_Throw":      (STATUS_TRANSLATED, "Arremesso de Bola Medicinal em Scoop"),
    "Standing_Two-Arm_Overhead_Throw":(STATUS_TRANSLATED, "Arremesso Acima da Cabeça com Duas Mãos em Pé"),
    "Supine_Chest_Throw":             (STATUS_TRANSLATED, "Arremesso de Peito Deitado"),
    "Supine_One-Arm_Overhead_Throw":  (STATUS_TRANSLATED, "Arremesso Unilateral Acima da Cabeça Deitado"),
    "Supine_Two-Arm_Overhead_Throw":  (STATUS_TRANSLATED, "Arremesso Acima da Cabeça com Duas Mãos Deitado"),

    # ---- SLED -------------------------------------------------------------
    "Bear_Crawl_Sled_Drags":      (STATUS_UNCHANGED, "Bear Crawl com Trenó"),
    "Sled_Drag_-_Harness":        (STATUS_TRANSLATED, "Arrasto de Trenó com Colete"),
    "Sled_Overhead_Backward_Walk":(STATUS_TRANSLATED, "Caminhada para Trás com Trenó Acima da Cabeça"),

    # ---- SPRINT -----------------------------------------------------------
    "Bench_Sprint":         (STATUS_TRANSLATED, "Sprint no Banco"),
    "Lunge_Sprint":         (STATUS_TRANSLATED, "Sprint com Afundos"),
    "Prowler_Sprint":       (STATUS_UNCHANGED,  "Prowler Sprint"),
    "Side_Hop-Sprint":      (STATUS_TRANSLATED, "Saltos Laterais com Sprint"),
    "Single-Cone_Sprint_Drill": (STATUS_TRANSLATED, "Drill de Sprint em Cone"),

    # ---- WINDMILL — UNCHANGED (no consolidated Brazilian term) ------------
    "Advanced_Kettlebell_Windmill": (STATUS_UNCHANGED, "Kettlebell Windmill Avançado"),
    "Double_Kettlebell_Windmill":   (STATUS_UNCHANGED, "Windmill Duplo com Kettlebell"),
    "Kettlebell_Windmill":          (STATUS_UNCHANGED, "Kettlebell Windmill"),

    # ---- ATLAS — UNCHANGED (strongman proper name) ------------------------
    "Atlas_Stone_Trainer": (STATUS_UNCHANGED, "Atlas Stone Trainer"),
    "Atlas_Stones":        (STATUS_UNCHANGED, "Atlas Stones"),

    # ---- ROCKY — UNCHANGED (named variation) ------------------------------
    "Bradford_Rocky_Presses":  (STATUS_UNCHANGED, "Bradford/Rocky Press"),
    "Rocky_Pull-Ups_Pulldowns":(STATUS_UNCHANGED, "Rocky Pull-Ups/Pulldowns"),

    # ---- OTHER — resolved cases from the 'other' group -------------------

    # Clearly good partial translations promoted to TRANSLATED
    "90_90_Hamstring":           (STATUS_TRANSLATED, "Alongamento de Posteriores 90/90"),
    "Ab_Crunch_Machine":         (STATUS_TRANSLATED, "Abdominal na Máquina"),
    "Ab_Roller":                 (STATUS_TRANSLATED, "Rolo Abdominal"),
    "Axle_Deadlift":             (STATUS_UNCHANGED, "Axle Deadlift"),
    "Back_Flyes_-_With_Bands":   (STATUS_TRANSLATED, "Crucifixo Posterior com Faixa Elástica"),
    "Barbell_Full_Squat":        (STATUS_TRANSLATED, "Agachamento Completo com Barra"),
    "Barbell_Guillotine_Bench_Press": (STATUS_TRANSLATED, "Supino Guilhotina com Barra"),
    "Barbell_Shrug_Behind_The_Back":  (STATUS_TRANSLATED, "Encolhimento por Trás da Nuca com Barra"),
    "Barbell_Side_Bend":         (STATUS_TRANSLATED, "Flexão Lateral com Barra"),
    "Barbell_Step_Ups":          (STATUS_TRANSLATED, "Subida no Banco com Barra"),
    "Bent-Arm_Barbell_Pullover": (STATUS_TRANSLATED, "Pullover com Barra (Braços Dobrados)"),
    "Bent-Arm_Dumbbell_Pullover":(STATUS_TRANSLATED, "Pullover com Halteres (Braços Dobrados)"),
    "Bent-Knee_Hip_Raise":       (STATUS_TRANSLATED, "Elevação de Quadril com Joelhos Dobrados"),
    "Bent_Over_One-Arm_Long_Bar_Row": (STATUS_TRANSLATED, "Remada Curvada Unilateral com Barra Longa"),
    "Bent_Over_Two-Arm_Long_Bar_Row": (STATUS_TRANSLATED, "Remada Curvada Bilateral com Barra Longa"),
    "Bent_Over_Two-Dumbbell_Row":     (STATUS_TRANSLATED, "Remada Curvada com Dois Halteres"),
    "Bent_Over_Two-Dumbbell_Row_With_Palms_In": (STATUS_TRANSLATED, "Remada Curvada com Dois Halteres, Palmas para Dentro"),
    "Bent_Over_Low-Pulley_Side_Lateral": (STATUS_TRANSLATED, "Elevação Lateral na Polia Baixa Curvado"),
    "Board_Press":               (STATUS_TRANSLATED, "Supino com Board"),
    "Bodyweight_Mid_Row":        (STATUS_TRANSLATED, "Remada com Peso Corporal"),
    "Box_Jump_Multiple_Response":(STATUS_TRANSLATED, "Box Jump (Múltiplas Respostas)"),
    "Box_Skip":                  (STATUS_TRANSLATED, "Skip no Box"),
    "Cable_Deadlifts":           (STATUS_TRANSLATED, "Levantamento Terra na Polia"),
    "Cable_Hammer_Curls_-_Rope_Attachment": (STATUS_TRANSLATED, "Rosca Martelo na Polia com Corda"),
    "Cable_Internal_Rotation":   (STATUS_TRANSLATED, "Rotação Interna na Polia"),
    "Cable_Rope_Rear-Delt_Rows": (STATUS_TRANSLATED, "Remada para Deltóide Posterior na Polia com Corda"),
    "Calf-Machine_Shoulder_Shrug":(STATUS_TRANSLATED, "Encolhimento de Ombros na Máquina de Panturrilha"),
    "Car_Deadlift":              (STATUS_UNCHANGED, "Car Deadlift"),
    "Chair_Squat":               (STATUS_TRANSLATED, "Agachamento na Cadeira"),
    "Chain_Press":               (STATUS_TRANSLATED, "Supino com Correntes"),
    "Chain_Handle_Extension":    (STATUS_TRANSLATED, "Extensão com Correntes"),
    "Clock_Push-Up":             (STATUS_TRANSLATED, "Flexão de Braços em Relógio"),
    "Close-Grip_Push-Up_off_of_a_Dumbbell": (STATUS_TRANSLATED, "Flexão de Braços Pegada Fechada sobre Halter"),
    "Concentration_Curls":       (STATUS_TRANSLATED, "Rosca Concentrada"),
    "Cross_Body_Hammer_Curl":    (STATUS_TRANSLATED, "Rosca Martelo Cruzada"),
    "Cross-Body_Crunch":         (STATUS_TRANSLATED, "Abdominal Cruzado"),
    "Crunch_-_Hands_Overhead":   (STATUS_TRANSLATED, "Abdominal com Mãos Acima da Cabeça"),
    "Crunch_-_Legs_On_Exercise_Ball": (STATUS_TRANSLATED, "Abdominal com Pernas na Bola de Exercícios"),
    "Cuban_Press":               (STATUS_UNCHANGED, "Cuban Press"),
    "Dead_Bug":                  (STATUS_TRANSLATED, "Dead Bug"),
    "Deficit_Deadlift":          (STATUS_TRANSLATED, "Levantamento Terra em Déficit"),
    "Depth_Jump_Leap":           (STATUS_TRANSLATED, "Salto em Profundidade"),
    "Dips_-_Chest_Version":      (STATUS_TRANSLATED, "Paralelas (Ênfase no Peito)"),
    "Dips_-_Triceps_Version":    (STATUS_TRANSLATED, "Paralelas (Ênfase no Tríceps)"),
    "Donkey_Calf_Raises":        (STATUS_TRANSLATED, "Elevação de Panturrilhas (Donkey)"),
    "Double_Leg_Butt_Kick":      (STATUS_TRANSLATED, "Chute de Calcanhares com Duas Pernas"),
    "Drag_Curl":                 (STATUS_TRANSLATED, "Rosca Drag"),
    "Dumbbell_Side_Bend":        (STATUS_TRANSLATED, "Flexão Lateral com Halter"),
    "Dumbbell_Step_Ups":         (STATUS_TRANSLATED, "Subida no Banco com Halteres"),
    "Dumbbell_Bench_Press_with_Neutral_Grip": (STATUS_TRANSLATED, "Supino com Halteres Pegada Neutra"),
    "EZ-Bar_Skullcrusher":       (STATUS_UNCHANGED, "Skull Crusher com Barra EZ"),
    "Elevated_Back_Lunge":       (STATUS_TRANSLATED, "Afundo com Perna Traseira Elevada"),
    "Exercise_Ball_Pull-In":     (STATUS_TRANSLATED, "Pull-In com Bola de Exercícios"),
    "Extended_Range_One-Arm_Kettlebell_Floor_Press": (STATUS_TRANSLATED, "Supino no Chão Unilateral com Kettlebell (Amplitude Estendida)"),
    "External_Rotation":         (STATUS_TRANSLATED, "Rotação Externa"),
    "External_Rotation_with_Band":  (STATUS_TRANSLATED, "Rotação Externa com Faixa Elástica"),
    "External_Rotation_with_Cable": (STATUS_TRANSLATED, "Rotação Externa na Polia"),
    "Farmers_Walk":              (STATUS_UNCHANGED, "Farmer's Walk"),
    "Finger_Curls":              (STATUS_TRANSLATED, "Rosca de Dedos"),
    "Flat_Bench_Leg_Pull-In":    (STATUS_TRANSLATED, "Pull-In com Pernas no Banco Reto"),
    "Flexor_Incline_Dumbbell_Curls": (STATUS_TRANSLATED, "Rosca Inclinado com Halteres (Flexores)"),
    "Freehand_Jump_Squat":       (STATUS_TRANSLATED, "Agachamento com Salto sem Peso"),
    "Hanging_Leg_Raise":         (STATUS_TRANSLATED, "Elevação de Pernas na Barra"),
    "Rack_Pulls":                (STATUS_UNCHANGED, "Rack Pulls"),
    "Rickshaw_Deadlift":         (STATUS_UNCHANGED, "Rickshaw Deadlift"),
    "Ring_Dips":                 (STATUS_TRANSLATED, "Paralelas nos Anéis"),
    "Romanian_Deadlift_from_Deficit": (STATUS_TRANSLATED, "Levantamento Terra Romeno em Déficit"),
    "Russian_Twist":             (STATUS_TRANSLATED, "Rotação de Tronco (Russian Twist)"),
    "Scissors_Jump":             (STATUS_TRANSLATED, "Salto Tesoura"),
    "Seated_Close-Grip_Concentration_Barbell_Curl": (STATUS_TRANSLATED, "Rosca Concentrada Pegada Fechada Sentado com Barra"),
    "Seated_Dumbbell_Inner_Biceps_Curl": (STATUS_TRANSLATED, "Rosca Interna de Bíceps Sentado com Halteres"),
    "Seated_Good_Mornings":      (STATUS_TRANSLATED, "Good Morning Sentado"),
    "Seated_Palm-Up_Barbell_Wrist_Curl": (STATUS_TRANSLATED, "Rosca de Punho Sentado com Barra, Palmas para Cima"),
    "Single_Dumbbell_Raise":     (STATUS_TRANSLATED, "Elevação com Um Halter"),
    "Single_Leg_Butt_Kick":      (STATUS_TRANSLATED, "Chute de Calcanhar Unilateral"),
    "Single_Leg_Push-off":       (STATUS_TRANSLATED, "Impulsão Unilateral"),
    "Single-Leg_Hop_Progression":(STATUS_TRANSLATED, "Progressão de Salto Unilateral"),
    "Single-Leg_Stride_Jump":    (STATUS_TRANSLATED, "Salto Passado Unilateral"),
    "Sledgehammer_Swings":       (STATUS_TRANSLATED, "Swing com Marreta"),
    "Smith_Machine_Behind_the_Back_Shrug": (STATUS_TRANSLATED, "Encolhimento por Trás na Máquina Smith"),
    "Smith_Machine_Pistol_Squat":(STATUS_TRANSLATED, "Agachamento Pistol na Máquina Smith"),
    "Smith_Machine_Stiff-Legged_Deadlift": (STATUS_TRANSLATED, "Levantamento Terra Stiff na Máquina Smith"),
    "Spider_Curl":               (STATUS_TRANSLATED, "Rosca Spider"),
    "Standing_Bradford_Press":   (STATUS_UNCHANGED, "Bradford Press em Pé"),
    "Standing_Barbell_Press_Behind_Neck": (STATUS_TRANSLATED, "Desenvolvimento por Trás da Nuca em Pé com Barra"),
    "Standing_Dumbbell_Straight-Arm_Front_Delt_Raise_Above_Head": (STATUS_TRANSLATED, "Elevação Frontal do Deltóide com Braços Estendidos Acima da Cabeça com Halteres"),
    "Standing_Front_Barbell_Raise_Over_Head": (STATUS_TRANSLATED, "Elevação Frontal Acima da Cabeça em Pé com Barra"),
    "Standing_Long_Jump":        (STATUS_TRANSLATED, "Salto em Distância em Pé"),
    "Standing_Olympic_Plate_Hand_Squeeze": (STATUS_TRANSLATED, "Aperto de Anilha em Pé"),
    "Standing_Palm-In_One-Arm_Dumbbell_Press": (STATUS_TRANSLATED, "Desenvolvimento Unilateral em Pé com Halter, Palma para Dentro"),
    "Standing_Palms-In_Dumbbell_Press": (STATUS_TRANSLATED, "Desenvolvimento em Pé com Halteres, Palmas para Dentro"),
    "Standing_Palms-Up_Barbell_Behind_The_Back_Wrist_Curl": (STATUS_TRANSLATED, "Rosca de Punho por Trás das Costas em Pé com Barra"),
    "Standing_Towel_Triceps_Extension": (STATUS_TRANSLATED, "Extensão de Tríceps em Pé com Toalha"),
    "Star_Jump":                 (STATUS_TRANSLATED, "Salto Estrela"),
    "Step-up_with_Knee_Raise":   (STATUS_TRANSLATED, "Subida no Banco com Elevação de Joelho"),
    "Stiff_Leg_Barbell_Good_Morning": (STATUS_TRANSLATED, "Good Morning com Pernas Rígidas com Barra"),
    "Stiff-Legged_Barbell_Deadlift":  (STATUS_TRANSLATED, "Levantamento Terra Stiff com Barra"),
    "Stiff-Legged_Dumbbell_Deadlift": (STATUS_TRANSLATED, "Levantamento Terra Stiff com Halteres"),
    "Straight_Bar_Bench_Mid_Rows":    (STATUS_TRANSLATED, "Remada Média no Banco com Barra"),
    "Suspended_Fallout":         (STATUS_TRANSLATED, "Rollout em Suspensão"),
    "Suspended_Push-Up":         (STATUS_TRANSLATED, "Flexão de Braços em Suspensão"),
    "Suspended_Reverse_Crunch":  (STATUS_TRANSLATED, "Abdominal Reverso em Suspensão"),
    "Suspended_Row":             (STATUS_TRANSLATED, "Remada em Suspensão"),
    "Suspended_Split_Squat":     (STATUS_TRANSLATED, "Agachamento Afundado em Suspensão"),
    "Svend_Press":               (STATUS_UNCHANGED, "Svend Press"),
    "Tate_Press":                (STATUS_UNCHANGED, "Tate Press"),
    "T-Bar_Row_with_Handle":     (STATUS_TRANSLATED, "Remada T-Bar com Handle"),
    "Tire_Flip":                 (STATUS_TRANSLATED, "Virada de Pneu"),
    "Torso_Rotation":            (STATUS_TRANSLATED, "Rotação de Tronco"),
    "Triceps_Pushdown_-_Rope_Attachment":  (STATUS_TRANSLATED, "Tríceps na Polia com Corda"),
    "Triceps_Pushdown_-_V-Bar_Attachment": (STATUS_TRANSLATED, "Tríceps na Polia com Barra V"),
    "Tuck_Crunch":               (STATUS_TRANSLATED, "Abdominal com Joelhos no Peito"),
    "Vertical_Swing":            (STATUS_TRANSLATED, "Swing Vertical"),
    "Weighted_Sissy_Squat":      (STATUS_TRANSLATED, "Agachamento Sissy com Carga"),
    "Zottman_Curl":              (STATUS_TRANSLATED, "Rosca Zottman"),
    "Zottman_Preacher_Curl":     (STATUS_TRANSLATED, "Rosca Scott Zottman"),
    "Zercher_Squats":            (STATUS_TRANSLATED, "Agachamento Zercher"),

    # ---- ADDITIONAL CLEAR CASES (remaining REVIEW after first pass) ------
    "Front_Squats_With_Two_Kettlebells": (STATUS_TRANSLATED, "Agachamento Frontal com Dois Kettlebells"),
    "Front_Two-Dumbbell_Raise":  (STATUS_TRANSLATED, "Elevação Frontal com Dois Halteres"),
    "Gironda_Sternum_Chins":     (STATUS_UNCHANGED, "Gironda Sternum Chins"),
    "Goblet_Squat":              (STATUS_TRANSLATED, "Agachamento Goblet"),
    "Good_Morning_off_Pins":     (STATUS_TRANSLATED, "Good Morning nos Pinos"),
    "Gorilla_Chin_Crunch":       (STATUS_TRANSLATED, "Abdominal Gorila na Barra"),
    "Hammer_Grip_Incline_DB_Bench_Press": (STATUS_TRANSLATED, "Supino Inclinado com Halteres Pegada Martelo"),
    "Handstand_Push-Ups":        (STATUS_TRANSLATED, "Flexão de Braços em Parada de Mão"),
    "Hanging_Bar_Good_Morning":  (STATUS_TRANSLATED, "Good Morning na Barra Pendurado"),
    "Hanging_Pike":              (STATUS_TRANSLATED, "Pike Pendurado"),
    "Heavy_Bag_Thrust":          (STATUS_TRANSLATED, "Empurrão no Saco de Pancadas"),
    "Hug_A_Ball":                (STATUS_TRANSLATED, "Abraçar a Bola"),
    "Hyperextensions_Back_Extensions": (STATUS_TRANSLATED, "Hiperextensão Lombar"),
    "Hyperextensions_With_No_Hyperextension_Bench": (STATUS_TRANSLATED, "Hiperextensão sem Banco Específico"),
    "Inchworm":                  (STATUS_TRANSLATED, "Inchworm"),
    "Incline_Bench_Pull":        (STATUS_TRANSLATED, "Remada no Banco Inclinado"),
    "Incline_Dumbbell_Bench_With_Palms_Facing_In": (STATUS_TRANSLATED, "Supino Inclinado com Halteres, Palmas para Dentro"),
    "Incline_Inner_Biceps_Curl": (STATUS_TRANSLATED, "Rosca Interna Inclinada de Bíceps"),
    "Incline_Push-Up_Medium":    (STATUS_TRANSLATED, "Flexão Inclinada Média"),
    "Internal_Rotation_with_Band": (STATUS_TRANSLATED, "Rotação Interna com Faixa Elástica"),
    "Inverted_Row_with_Straps":  (STATUS_TRANSLATED, "Remada Invertida com Alças"),
    "Iron_Cross":                (STATUS_UNCHANGED, "Iron Cross"),
    "Isometric_Chest_Squeezes":  (STATUS_TRANSLATED, "Contração Isométrica de Peito"),
    "Isometric_Wipers":          (STATUS_TRANSLATED, "Limpadores Isométricos"),
    "JM_Press":                  (STATUS_UNCHANGED, "JM Press"),
    "Jackknife_Sit-Up":          (STATUS_TRANSLATED, "Abdominal Canivete"),
    "Janda_Sit-Up":              (STATUS_UNCHANGED, "Janda Sit-Up"),
    "Jefferson_Squats":          (STATUS_UNCHANGED, "Jefferson Squat"),
    "Jogging_Treadmill":         (STATUS_UNCHANGED, "Corrida Leve na Esteira"),
    "Keg_Load":                  (STATUS_UNCHANGED, "Keg Load"),
    "Kettlebell_Figure_8":       (STATUS_TRANSLATED, "Kettlebell Figure 8"),
    "Kettlebell_Halo":           (STATUS_UNCHANGED, "Kettlebell Halo"),
    "Kettlebell_Halo_With_Overhead_Extension": (STATUS_UNCHANGED, "Kettlebell Halo com Extensão Acima da Cabeça"),
    "Kettlebell_One-Legged_Deadlift": (STATUS_TRANSLATED, "Levantamento Terra Unilateral com Kettlebell"),
    "Kettlebell_Pass_Between_The_Legs": (STATUS_TRANSLATED, "Passe de Kettlebell entre as Pernas"),
    "Kettlebell_Pirate_Ships":   (STATUS_UNCHANGED, "Kettlebell Pirate Ships"),
    "Kettlebell_Pistol_Squat":   (STATUS_TRANSLATED, "Agachamento Pistol com Kettlebell"),
    "Kettlebell_Seesaw_Press":   (STATUS_UNCHANGED, "Kettlebell Seesaw Press"),
    "Kettlebell_Sumo_High_Pull": (STATUS_TRANSLATED, "Puxada Alta Sumo com Kettlebell"),
    "Kettlebell_Thruster":       (STATUS_TRANSLATED, "Thruster com Kettlebell"),
    "Kettlebell_Turkish_Get-Up_Lunge_style":  (STATUS_UNCHANGED, "Turkish Get-Up com Kettlebell (estilo Afundo)"),
    "Kettlebell_Turkish_Get-Up_Squat_style":  (STATUS_UNCHANGED, "Turkish Get-Up com Kettlebell (estilo Agachamento)"),
    "Kipping_Muscle_Up":         (STATUS_UNCHANGED, "Kipping Muscle-Up"),
    "Knee_Circles":              (STATUS_TRANSLATED, "Círculos de Joelho"),
    "Knee_Hip_Raise_On_Parallel_Bars": (STATUS_TRANSLATED, "Elevação de Joelho e Quadril nas Barras Paralelas"),
    "Kneeling_Cable_Crunch_With_Alternating_Oblique_Twists": (STATUS_TRANSLATED, "Abdominal na Polia Ajoelhado com Torção Oblíqua Alternada"),
    "Landmine_180s":             (STATUS_UNCHANGED, "Landmine 180°"),
    "Landmine_Linear_Jammer":    (STATUS_UNCHANGED, "Landmine Linear Jammer"),
    "Leg-Over_Floor_Press":      (STATUS_TRANSLATED, "Supino no Chão com Perna Cruzada"),
    "Leg_Pull-In":               (STATUS_TRANSLATED, "Pull-In de Pernas"),
    "Leverage_Iso_Row":          (STATUS_TRANSLATED, "Remada Isoateral em Máquina"),
    "Log_Lift":                  (STATUS_UNCHANGED, "Log Lift"),
    "London_Bridges":            (STATUS_UNCHANGED, "London Bridges"),
    "Low_Cable_Crossover":       (STATUS_TRANSLATED, "Crossover na Polia Baixa"),
    "Low_Cable_Triceps_Extension":(STATUS_TRANSLATED, "Extensão de Tríceps na Polia Baixa"),
    "Low_Pulley_Row_To_Neck":    (STATUS_TRANSLATED, "Remada na Polia Baixa ao Pescoço"),
    "Lower_Back_Curl":           (STATUS_TRANSLATED, "Enrolamento Lombar"),
    "Lunge_Pass_Through":        (STATUS_TRANSLATED, "Afundo com Passagem"),
    "Lying_Cambered_Barbell_Row":(STATUS_TRANSLATED, "Remada Deitado com Barra Curvada"),
    "Lying_Close-Grip_Bar_Curl_On_High_Pulley": (STATUS_TRANSLATED, "Rosca Pegada Fechada Deitado na Polia Alta"),
    "Lying_Close-Grip_Barbell_Triceps_Extension_Behind_The_Head": (STATUS_TRANSLATED, "Extensão de Tríceps Pegada Fechada Deitado Atrás da Cabeça"),
    "Lying_Close-Grip_Barbell_Triceps_Press_To_Chin": (STATUS_TRANSLATED, "Extensão de Tríceps Pegada Fechada Deitado até o Queixo"),
    "Lying_Prone_Quadriceps":    (STATUS_TRANSLATED, "Alongamento de Quadríceps Deitado Pronado"),
    "Lying_Supine_Dumbbell_Curl":(STATUS_TRANSLATED, "Rosca com Halteres Deitado de Costas"),
    "Lying_T-Bar_Row":           (STATUS_TRANSLATED, "Remada T-Bar Deitado"),
    "Medicine_Ball_Chest_Pass":  (STATUS_TRANSLATED, "Passe de Peito com Bola Medicinal"),
    "Medicine_Ball_Full_Twist":  (STATUS_TRANSLATED, "Torção Completa com Bola Medicinal"),
    "Middle_Back_Shrug":         (STATUS_TRANSLATED, "Encolhimento do Médio Dorsal"),
    "Mixed_Grip_Chin":           (STATUS_TRANSLATED, "Barra Fixa Pegada Mista"),
    "Muscle_Up":                 (STATUS_UNCHANGED, "Muscle-Up"),
    "Natural_Glute_Ham_Raise":   (STATUS_TRANSLATED, "Glute Ham Raise Natural"),
    "Neck_Press":                (STATUS_TRANSLATED, "Supino pelo Pescoço"),
    "Olympic_Squat":             (STATUS_TRANSLATED, "Agachamento Olímpico"),
    "One-Arm_High-Pulley_Cable_Side_Bends": (STATUS_TRANSLATED, "Flexão Lateral Unilateral na Polia Alta"),
    "One-Arm_Kettlebell_Para_Press": (STATUS_UNCHANGED, "One-Arm Kettlebell Para Press"),
    "One-Arm_Long_Bar_Row":      (STATUS_TRANSLATED, "Remada Unilateral com Barra Longa"),
    "One-Arm_Medicine_Ball_Slam":(STATUS_TRANSLATED, "Slam de Bola Medicinal Unilateral"),
    "One-Arm_Side_Laterals":     (STATUS_TRANSLATED, "Elevação Lateral Unilateral"),
    "One-Legged_Cable_Kickback": (STATUS_TRANSLATED, "Kickback Unilateral na Polia"),
    "One_Arm_Pronated_Dumbbell_Triceps_Extension": (STATUS_TRANSLATED, "Extensão de Tríceps Pronada Unilateral com Halter"),
    "One_Arm_Supinated_Dumbbell_Triceps_Extension": (STATUS_TRANSLATED, "Extensão de Tríceps Supinada Unilateral com Halter"),
    "One_Handed_Hang":           (STATUS_TRANSLATED, "Suspensão Unilateral"),
    "Otis-Up":                   (STATUS_UNCHANGED, "Otis-Up"),
    "Overhead_Lat":              (STATUS_TRANSLATED, "Puxada de Latíssimo Dorsal Acima da Cabeça"),
    "Overhead_Slam":             (STATUS_TRANSLATED, "Slam Acima da Cabeça"),
    "Pallof_Press":              (STATUS_UNCHANGED, "Pallof Press"),
    "Pallof_Press_With_Rotation":(STATUS_UNCHANGED, "Pallof Press com Rotação"),
    "Pin_Presses":               (STATUS_TRANSLATED, "Supino nos Pinos"),
    "Plate_Pinch":               (STATUS_TRANSLATED, "Pinçada de Anilha"),
    "Platform_Hamstring_Slides": (STATUS_TRANSLATED, "Deslizamento de Posteriores na Plataforma"),
    "Plie_Dumbbell_Squat":       (STATUS_TRANSLATED, "Agachamento Plié com Halteres"),
    "Power_Partials":            (STATUS_TRANSLATED, "Repetições Parciais de Força"),
    "Power_Stairs":              (STATUS_UNCHANGED, "Power Stairs"),
    "Pullups":                   (STATUS_TRANSLATED, "Barra Fixa"),
    "Push-Ups_-_Close_Triceps_Position": (STATUS_TRANSLATED, "Flexão de Braços Pegada Fechada (Tríceps)"),
    "Push-Ups_With_Feet_Elevated":(STATUS_TRANSLATED, "Flexão de Braços com Pés Elevados"),
    "Push_Up_to_Side_Plank":     (STATUS_TRANSLATED, "Flexão de Braços para Prancha Lateral"),
    "Side_to_Side_Box_Shuffle":  (STATUS_TRANSLATED, "Shuffle Lateral no Box"),
    "Down_Dog_Pose":             (STATUS_REVIEW, "Down Dog"),
    "Downward_Facing_Balance":   (STATUS_REVIEW, "Downward Facing Balance"),


    # External rotation family
    "Reverse_Flyes_With_External_Rotation": (STATUS_TRANSLATED, "Crucifixo Reverso com Rotação Externa"),

    # Bench/machine variants with clear translations
    "Bench_Press_-_Powerlifting": (STATUS_TRANSLATED, "Supino (Powerlifting)"),
    "Dips_-_Chest_Version":       (STATUS_TRANSLATED, "Paralelas (Ênfase no Peito)"),   # duplicate handled below
    "Body_Tricep_Press":          (STATUS_TRANSLATED, "Extensão de Tríceps com Peso Corporal"),
    "See-Saw_Press_Alternating_Side_Press": (STATUS_TRANSLATED, "Desenvolvimento Alternado See-Saw"),
    "Bosu_Ball_Cable_Crunch_With_Side_Bends": (STATUS_TRANSLATED, "Abdominal na Polia com Flexão Lateral na Bola Bosu"),
    "Dumbbell_Prone_Incline_Curl":(STATUS_TRANSLATED, "Rosca Inclinada Pronada com Halteres"),
    "Speed_Squats":               (STATUS_TRANSLATED, "Agachamento de Velocidade"),
    "Speed_Box_Squat":            (STATUS_TRANSLATED, "Agachamento no Box de Velocidade"),
    "Rocking_Standing_Calf_Raise":(STATUS_TRANSLATED, "Elevação de Panturrilhas em Pé (Rocking)"),
    "Donkey_Calf_Raises":         (STATUS_TRANSLATED, "Elevação de Panturrilhas (Donkey)"),
    "Straight_Raises_on_Incline_Bench": (STATUS_TRANSLATED, "Elevação no Banco Inclinado com Braços Estendidos"),
    "Reverse_Band_Power_Squat":   (STATUS_TRANSLATED, "Agachamento com Faixa Elástica Reversa"),
    "Shotgun_Row":                (STATUS_TRANSLATED, "Remada Shotgun"),
    "Side_Bridge":                (STATUS_TRANSLATED, "Prancha Lateral"),
    "Side_Laterals_to_Front_Raise":(STATUS_TRANSLATED, "Elevação Lateral para Frontal"),
    "Side_To_Side_Chins":         (STATUS_TRANSLATED, "Barra Fixa Lateral"),
    "Smith_Machine_Behind_the_Back_Shrug": (STATUS_TRANSLATED, "Encolhimento por Trás na Máquina Smith"),  # already above
    "Bent_Over_Dumbbell_Rear_Delt_Raise_With_Head_On_Bench": (STATUS_TRANSLATED, "Elevação do Deltóide Posterior Curvado com Halteres (Cabeça no Banco)"),

    # Cardio / machines — UNCHANGED
    "Air_Bike":             (STATUS_UNCHANGED, "Air Bike"),
    "Bicycling":            (STATUS_UNCHANGED, "Ciclismo"),
    "Bicycling_Stationary": (STATUS_UNCHANGED, "Ciclismo Estacionário"),
    "Elliptical_Trainer":   (STATUS_UNCHANGED, "Elliptical Trainer"),
    "Recumbent_Bike":       (STATUS_UNCHANGED, "Bicicleta Reclinada"),
    "Rowing_Stationary":    (STATUS_UNCHANGED, "Remo Estacionário"),
    "Running_Treadmill":    (STATUS_UNCHANGED, "Corrida na Esteira"),
    "Stairmaster":          (STATUS_UNCHANGED, "Stairmaster"),
    "Walking_Treadmill":    (STATUS_UNCHANGED, "Caminhada na Esteira"),
    "Step_Mill":            (STATUS_UNCHANGED, "Step Mill"),

    # Specific functional / strongman — UNCHANGED
    "Battling_Ropes":       (STATUS_UNCHANGED, "Battling Ropes"),
    "Conans_Wheel":         (STATUS_UNCHANGED, "Roda do Conan"),
    "Farmers_Walk":         (STATUS_UNCHANGED, "Farmer's Walk"),
    "Rope_Climb":           (STATUS_UNCHANGED, "Escalada de Corda"),
    "Rope_Jumping":         (STATUS_UNCHANGED, "Pular Corda"),
    "Sandbag_Load":         (STATUS_UNCHANGED, "Sandbag Load"),
    "Yoke_Walk":            (STATUS_UNCHANGED, "Yoke Walk"),
    "Rickshaw_Carry":       (STATUS_UNCHANGED, "Rickshaw Carry"),
    "Circus_Bell":          (STATUS_UNCHANGED, "Circus Bell"),
    "Stomach_Vacuum":       (STATUS_TRANSLATED, "Vacuum Abdominal"),
    "Superman":             (STATUS_TRANSLATED, "Superman"),
    "Dead_Bug":             (STATUS_TRANSLATED, "Dead Bug"),

    # Plyometric / jump — remaining
    "Box_Skip":             (STATUS_TRANSLATED, "Skip no Box"),  # already above
    "Front_Cone_Hops_or_hurdle_hops": (STATUS_TRANSLATED, "Saltos Frontais sobre Cones"),
    "Hurdle_Hops":          (STATUS_TRANSLATED, "Saltos sobre Barreiras"),
    "Incline_Push-Up_Depth_Jump": (STATUS_TRANSLATED, "Flexão Inclinada com Salto em Profundidade"),
    "Knee_Tuck_Jump":       (STATUS_TRANSLATED, "Salto com Joelhos no Peito"),
    "Kneeling_Jump_Squat":  (STATUS_TRANSLATED, "Agachamento com Salto da Posição Ajoelhado"),
    "Linear_Depth_Jump":    (STATUS_TRANSLATED, "Salto em Profundidade Linear"),
    "Rocket_Jump":          (STATUS_TRANSLATED, "Salto Foguete"),
    "Plyo_Kettlebell_Pushups": (STATUS_TRANSLATED, "Flexões Pliométricas com Kettlebell"),
    "Plyo_Push-up":         (STATUS_TRANSLATED, "Flexão Pliométrica"),
    "Bench_Jump":           (STATUS_TRANSLATED, "Salto no Banco"),
    "Dumbbell_Seated_Box_Jump": (STATUS_TRANSLATED, "Box Jump Sentado com Halteres"),
    "Frog_Sit-Ups":         (STATUS_TRANSLATED, "Abdominal Sapo"),
    "Flutter_Kicks":        (STATUS_TRANSLATED, "Flutter Kicks"),
    "Scissors_Jump":        (STATUS_TRANSLATED, "Salto Tesoura"),  # already above
    "Stride_Jump_Crossover":(STATUS_TRANSLATED, "Salto Cruzado"),
    "Side_Hop-Sprint":      (STATUS_TRANSLATED, "Saltos Laterais com Sprint"),
    "Split_Jump":           (STATUS_TRANSLATED, "Agachamento com Salto em Afundo"),

    # Stretch / mobility (remaining)
    "All_Fours_Quad_Stretch":      (STATUS_TRANSLATED, "Alongamento de Quadríceps em Quatro Apoios"),
    "Chair_Leg_Extended_Stretch":  (STATUS_TRANSLATED, "Alongamento de Pernas na Cadeira"),
    "Chair_Upper_Body_Stretch":    (STATUS_TRANSLATED, "Alongamento de Membros Superiores na Cadeira"),
    "Intermediate_Groin_Stretch":  (STATUS_TRANSLATED, "Alongamento de Virilha (Intermediário)"),
    "Intermediate_Hip_Flexor_and_Quad_Stretch": (STATUS_TRANSLATED, "Alongamento de Flexores do Quadril e Quadríceps (Intermediário)"),
    "On-Your-Back_Quad_Stretch":   (STATUS_TRANSLATED, "Alongamento de Quadríceps Deitado"),
    "Peroneals_Stretch":           (STATUS_TRANSLATED, "Alongamento dos Fibulares"),
    "Posterior_Tibialis_Stretch":  (STATUS_TRANSLATED, "Alongamento do Tibial Posterior"),
    "Round_The_World_Shoulder_Stretch": (STATUS_TRANSLATED, "Alongamento de Ombros Circular"),
    "Behind_Head_Chest_Stretch":   (STATUS_TRANSLATED, "Alongamento de Peito com Mãos Atrás da Cabeça"),

    # Misc clear cases
    "Alternate_Heel_Touchers":   (STATUS_TRANSLATED, "Toque Alternado no Calcanhar"),
    "Alternating_Renegade_Row":  (STATUS_TRANSLATED, "Remada Renegade Alternada"),
    "Anti-Gravity_Press":        (STATUS_UNCHANGED, "Anti-Gravity Press"),
    "Backward_Drag":             (STATUS_TRANSLATED, "Arrastar para Trás"),
    "Dumbbell_Tricep_Extension_-Pronated_Grip": (STATUS_TRANSLATED, "Extensão de Tríceps Pegada Pronada com Halter"),
    "Dumbbell_Lying_Pronation":  (STATUS_TRANSLATED, "Pronação Deitado com Halter"),
    "Dumbbell_Lying_Supination": (STATUS_TRANSLATED, "Supinação Deitado com Halter"),
    "Dumbbell_Scaption":         (STATUS_UNCHANGED, "Scaption com Halter"),
    "Return_Push_from_Stance":   (STATUS_REVIEW,    "Return Push from Stance"),
    "Seated_Leg_Tucks":          (STATUS_TRANSLATED, "Abdominal Sentado com Joelhos no Peito"),
    "See-Saw_Press_Alternating_Side_Press": (STATUS_TRANSLATED, "Desenvolvimento Alternado See-Saw"),  # dup ok
    "Single-Arm_Linear_Jammer":  (STATUS_UNCHANGED, "Single-Arm Linear Jammer"),
    "Squat_with_Plate_Movers":   (STATUS_TRANSLATED, "Agachamento com Movimento de Anilha"),
    "Weighted_Ball_Side_Bend":   (STATUS_TRANSLATED, "Flexão Lateral com Bola Ponderada"),
    "Wide_Stance_Stiff_Legs":    (STATUS_TRANSLATED, "Levantamento Terra Stiff com Posição Aberta"),
    "Straight_Raises_on_Incline_Bench": (STATUS_TRANSLATED, "Elevação no Banco Inclinado com Braços Estendidos"),  # dup
    "Seated_Front_Deltoid":      (STATUS_TRANSLATED, "Elevação Frontal do Deltóide Sentado"),
    "Seated_Biceps":             (STATUS_TRANSLATED, "Rosca de Bíceps Sentado"),
    "Seated_Glute":              (STATUS_TRANSLATED, "Ativação de Glúteos Sentado"),
    "Lying_Glute":               (STATUS_TRANSLATED, "Ativação de Glúteos Deitado"),
    "Lying_Hamstring":           (STATUS_TRANSLATED, "Alongamento de Posteriores Deitado"),
    "Overhead_Triceps":          (STATUS_TRANSLATED, "Extensão de Tríceps Acima da Cabeça"),
    "Seated_Biceps":             (STATUS_TRANSLATED, "Rosca de Bíceps Sentado"),  # dup
    "Butt_Lift_Bridge":          (STATUS_TRANSLATED, "Elevação de Quadril (Bridge)"),
    "Wrist_Circles":             (STATUS_TRANSLATED, "Círculos de Pulso"),
    "Wrist_Roller":              (STATUS_TRANSLATED, "Rolo de Pulso"),
    "Wrist_Rotations_with_Straight_Bar": (STATUS_TRANSLATED, "Rotações de Pulso com Barra Reta"),
    "Finger_Curls":              (STATUS_TRANSLATED, "Rosca de Dedos"),  # dup
    "Cocoons":                   (STATUS_TRANSLATED, "Abdominal Casulo"),
    "Elbow_to_Knee":             (STATUS_TRANSLATED, "Cotovelo ao Joelho"),
    "Side_Jackknife":            (STATUS_TRANSLATED, "Abdominal Lateral com Flexão"),
    "Toe_Touchers":              (STATUS_TRANSLATED, "Alcançar os Dedos dos Pés"),
    "Standing_Cable_Lift":       (STATUS_TRANSLATED, "Elevação com Cabo em Pé"),
    "Standing_Cable_Wood_Chop":  (STATUS_TRANSLATED, "Corte de Madeira com Cabo em Pé"),
    "Cross_Over_-_With_Bands":   (STATUS_TRANSLATED, "Crossover com Faixas Elásticas"),
    "Cable_Russian_Twists":      (STATUS_TRANSLATED, "Rotação de Tronco com Cabo"),
    "Crunch_-_Hands_Overhead":   (STATUS_TRANSLATED, "Abdominal com Mãos Acima da Cabeça"),  # dup ok
    "Chest_Push_multiple_response": (STATUS_TRANSLATED, "Empurrada de Peito (Múltiplas Respostas)"),
    "Chest_Push_single_response":   (STATUS_TRANSLATED, "Empurrada de Peito (Resposta Simples)"),
    "Chest_Push_from_3_point_stance":(STATUS_TRANSLATED, "Empurrada de Peito de 3 Pontos"),
    "Chest_Push_with_Run_Release":   (STATUS_TRANSLATED, "Empurrada de Peito com Largada"),
    "Drop_Push":                 (STATUS_TRANSLATED, "Empurrada com Queda"),
    "T-Bar_Row_with_Handle":     (STATUS_TRANSLATED, "Remada T-Bar com Handle"),  # dup
    "Cable_Judo_Flip":           (STATUS_UNCHANGED, "Cable Judo Flip"),
    "Cable_Iron_Cross":          (STATUS_UNCHANGED, "Cable Iron Cross"),
    "Forward_Drag_with_Press":   (STATUS_TRANSLATED, "Arrasto à Frente com Supino"),
    "Body-Up":                   (STATUS_TRANSLATED, "Body-Up"),
    "Bottoms_Up":                (STATUS_UNCHANGED, "Bottoms Up"),
    "Pushups":                   (STATUS_TRANSLATED, "Flexões de Braços"),
    "Pushups_Close_and_Wide_Hand_Positions": (STATUS_TRANSLATED, "Flexões de Braços (Pegadas Fechada e Aberta)"),
    "V-Bar_Pullup":              (STATUS_TRANSLATED, "Barra Fixa com Barra V"),
    "Down_Dog_Pose":             (STATUS_REVIEW, "Down Dog Pose"),
    "Downward_Facing_Balance":   (STATUS_REVIEW, "Downward Facing Balance"),
    "Wind_Sprints":              (STATUS_TRANSLATED, "Tiros de Velocidade"),
    "Fast_Skipping":             (STATUS_TRANSLATED, "Skipping Rápido"),
    "Skating":                   (STATUS_UNCHANGED, "Patinação"),
    "Spell_Caster":              (STATUS_UNCHANGED, "Spell Caster"),
    "Butterfly":                 (STATUS_TRANSLATED, "Borboleta (Polia)"),
    "Crucifix":                  (STATUS_TRANSLATED, "Crucifixo"),
    "Around_The_Worlds":         (STATUS_UNCHANGED, "Around The Worlds"),
    "Balance_Board":             (STATUS_UNCHANGED, "Balance Board"),
    "Car_Drivers":               (STATUS_TRANSLATED, "Car Drivers"),
    "Butt-Ups":                  (STATUS_TRANSLATED, "Abdominal com Elevação de Quadril"),
    "Pyramid":                   (STATUS_REVIEW, "Pyramid"),
    "Rack_Delivery":             (STATUS_UNCHANGED, "Rack Delivery"),
    "Frankenstein_Squat":        (STATUS_TRANSLATED, "Agachamento Frankenstein"),
    "Quick_Leap":                (STATUS_TRANSLATED, "Salto Rápido"),
    "Spider_Crawl":              (STATUS_TRANSLATED, "Arrastar de Aranha"),
    "Push-Ups_With_Feet_On_An_Exercise_Ball": (STATUS_TRANSLATED, "Flexão de Braços com Pés na Bola de Exercícios"),
    "Standing_Inner-Biceps_Curl":(STATUS_TRANSLATED, "Rosca Interna de Bíceps em Pé"),
    "Dumbbell_Bench_Press_with_Neutral_Grip": (STATUS_TRANSLATED, "Supino com Halteres Pegada Neutra"),  # dup
    "Scissor_Kick":              (STATUS_TRANSLATED, "Chute Tesoura"),
    "Speed_Band_Overhead_Triceps":(STATUS_TRANSLATED, "Extensão de Tríceps Acima da Cabeça com Faixa de Velocidade"),
    "Seated_Flat_Bench_Leg_Pull-In": (STATUS_TRANSLATED, "Pull-In com Pernas no Banco Reto Sentado"),
    "Bent_Over_Dumbbell_Rear_Delt_Raise_With_Head_On_Bench": (STATUS_TRANSLATED, "Elevação do Deltóide Posterior Curvado com Halteres (Cabeça no Banco)"),  # dup
}


# ---------------------------------------------------------------------------
# Known technical English terms that should remain in English (UNCHANGED).
# These are fitness terms used as-is in Brazilian Portuguese.
# ---------------------------------------------------------------------------

UNCHANGED_TERMS: frozenset[str] = frozenset({
    # Olympic lifts (technically reviewed; universally used in English in Brazil)
    "clean", "snatch", "jerk",
    "power clean", "hang clean", "hang snatch", "power snatch",
    "clean and jerk", "clean deadlift", "clean pull",
    # Movements kept in English by glossary policy
    "pulldown", "pulldowns",
    "face pull", "face pulls",
    "good morning", "good mornings",
    "pullover", "pullovers",
    "swing", "swings",
    "leg press",
    "skull crusher", "skull crushers",
    "pull through",
    "pull apart",
    "rack pull", "rack pulls",
    "rollout", "rollouts",
    "crossover", "crossovers",
    "box jump", "box jumps",
    "sled push", "sled row",
    "glute ham raise",
})

# Regex for technical English terms detection in the suggested name
_UNCHANGED_PHRASES: list[re.Pattern[str]] = [
    re.compile(r"\b(power clean|hang clean|hang snatch|power snatch)\b", re.I),
    re.compile(r"\bclean and jerk\b", re.I),
    re.compile(r"\b(clean deadlift|clean pull)\b", re.I),
    re.compile(r"\bclean\b", re.I),
    re.compile(r"\bsnatch\b", re.I),
    re.compile(r"\bjerk\b", re.I),
    re.compile(r"\bpulldown\b", re.I),
    re.compile(r"\bface pull\b", re.I),
    re.compile(r"\bgood morning\b", re.I),
    re.compile(r"\bpullover\b", re.I),
    re.compile(r"\bswing\b", re.I),
    re.compile(r"\bleg press\b", re.I),
    re.compile(r"\bskull crusher\b", re.I),
    re.compile(r"\bpull through\b", re.I),
    re.compile(r"\bpull apart\b", re.I),
    re.compile(r"\brac?k pull\b", re.I),
    re.compile(r"\brollout\b", re.I),
    re.compile(r"\bcrossover\b", re.I),
    re.compile(r"\bbox jump\b", re.I),
    re.compile(r"\bsled (push|row|drag)\b", re.I),
    re.compile(r"\bglute ham raise\b", re.I),
    re.compile(r"\bsmr\b", re.I),
    re.compile(r"\batlas stone\b", re.I),
    re.compile(r"\bwindsmill\b", re.I),
]


# ---------------------------------------------------------------------------
# Olympic lift and other REVIEW-core terms that should remain in English
# (matched against audit matchedGlossaryTerms)
# ---------------------------------------------------------------------------

_OLYMPIC_TERMS: frozenset[str] = frozenset({
    "clean", "snatch", "jerk",
    "hang clean", "hang snatch",
    "power clean", "power snatch",
    "clean and jerk",
})


def _has_olympic_core(audit: dict) -> bool:
    matched_lower = {t.lower() for t in audit.get("matchedGlossaryTerms", [])}
    return bool(matched_lower & _OLYMPIC_TERMS)


def _is_technically_unchanged(original_name: str, suggested_name: str) -> bool:
    """Return True if the suggested name is the same (or very close) as the
    original English name, indicating the term is kept in English by policy."""
    orig_clean = original_name.strip().lower()
    sugg_clean = re.sub(r"\s*\[.*?\]", "", suggested_name).strip().lower()
    # Direct equality (case-insensitive)
    if orig_clean == sugg_clean:
        return True
    # Remove hyphens/underscores difference
    if orig_clean.replace("-", " ").replace("_", " ") == sugg_clean.replace("-", " ").replace("_", " "):
        return True
    return False


def _clean_suggested_name(suggested: str) -> str:
    """Remove the [uncovered token] brackets appended by the audit tool."""
    cleaned = re.sub(r"\s*\[.*?\]", "", suggested).strip()
    return cleaned if cleaned else suggested.strip()


_EQUIPMENT_ONLY_PREFIXES = (
    "com ", "na ", "no ", "de ", "do ", "da ", "dos ", "das ",
    "Acima ", "Abaixo ", "por ",
)


def _name_is_equipment_only(name: str) -> bool:
    """Return True if the name starts with a preposition/equipment phrase
    and appears to have no Portuguese core movement word.
    Used to detect cases where the translation is incomplete (e.g. 'com Kettlebell').
    """
    return any(name.startswith(prefix) for prefix in _EQUIPMENT_ONLY_PREFIXES)


def _determine_status_and_name(
    original_name: str,
    audit: dict,
) -> tuple[str, str]:
    """Return (status, translated_name) for an exercise.

    Logic:
      DIRECT / CONTEXTUAL audit:
        - If suggested name ≈ original → UNCHANGED (technical English term kept)
        - Else → TRANSLATED
      REVIEW audit with no uncovered tokens:
        - If matched terms include Olympic lifts (clean/snatch/jerk/…)
          → UNCHANGED (technical English term used in Brazil)
        - If matched terms include 'push' + 'press' (Push Press variants)
          → UNCHANGED with original name (Push Press is used as-is in Brazil)
        - Else → REVIEW with original name (no meaningful core movement found;
          partial Portuguese name would be misleading)
      REVIEW audit with uncovered tokens:
        - REVIEW, use cleaned suggested name if different from original
    """
    classification = audit["classification"]
    suggested = audit["suggestedName"]
    uncovered = audit.get("uncoveredTokens", [])
    matched_lower = {t.lower() for t in audit.get("matchedGlossaryTerms", [])}
    name_clean = _clean_suggested_name(suggested)

    if classification in ("DIRECT", "CONTEXTUAL"):
        if _is_technically_unchanged(original_name, name_clean):
            return STATUS_UNCHANGED, name_clean
        return STATUS_TRANSLATED, name_clean

    # REVIEW case
    if not uncovered:
        if _has_olympic_core(audit):
            # Olympic lifts — technical English terms kept as-is in Brazil
            return STATUS_UNCHANGED, name_clean
        if "push" in matched_lower and "press" in matched_lower:
            # Push Press variants — widely used in English in Brazilian fitness
            return STATUS_UNCHANGED, original_name.strip()
        # No meaningful core movement — partial Portuguese name would mislead.
        # Keep original name and mark REVIEW for human decision.
        return STATUS_REVIEW, original_name.strip()

    # REVIEW with uncovered tokens — partial translation available
    if name_clean and name_clean.lower() != original_name.strip().lower():
        # If the name looks like only equipment/preposition (no core movement),
        # fall back to original to avoid a misleading incomplete name.
        if _name_is_equipment_only(name_clean):
            return STATUS_REVIEW, original_name.strip()
        # We have SOME translation; present it as REVIEW for human validation
        return STATUS_REVIEW, name_clean

    # Completely untranslatable — keep original
    return STATUS_REVIEW, original_name.strip()


def _load_english_original_names() -> dict[str, str]:
    """English presentation names from the Free Exercise DB raw dataset.

    After Etapa 4.8 the normalized catalog may already contain pt-BR names.
    Translation generation must still key off the English originals.
    """
    if not RAW_SOURCE.exists():
        return {}
    rows = json.loads(RAW_SOURCE.read_text(encoding="utf-8"))
    out: dict[str, str] = {}
    for row in rows:
        if not isinstance(row, dict):
            continue
        eid = row.get("id")
        name = row.get("name")
        if isinstance(eid, str) and eid.strip() and isinstance(name, str) and name.strip():
            out[eid] = name
    return out


def _build_entry(
    exercise: dict,
    audit: dict,
    english_names: dict[str, str] | None = None,
) -> OrderedDict:
    """Build one translations.json entry for a single exercise."""
    external_id: str = exercise["externalId"]
    source: str = exercise.get("source") or "free-exercise-db"
    originals = english_names or {}
    original_name: str = originals.get(external_id) or exercise["name"]

    if external_id in EXERCISE_OVERRIDES:
        status, translated_name = EXERCISE_OVERRIDES[external_id]
    else:
        status, translated_name = _determine_status_and_name(original_name, audit)

    raw_instructions: list[str] = (
        exercise.get("sourceData", {}).get("instructions") or []
    )

    if not raw_instructions:
        instructions: list[str] = []
        instr_status = STATUS_NOT_APPLICABLE
    else:
        instructions, _all_exact = translate_instructions(raw_instructions)
        instr_status = STATUS_TRANSLATED  # best-effort always produced

    # The overall status is driven by the NAME translation quality.
    # Instructions are always translated best-effort.
    final_status = status

    return OrderedDict(
        [
            ("source", source),
            ("externalId", external_id),
            ("status", final_status),
            ("originalName", original_name),
            ("name", translated_name),
            ("muscleGroup", exercise.get("muscleGroup") or ""),
            ("equipmentType", exercise.get("equipmentType") or ""),
            ("instructions", instructions),
        ]
    )


def _importable_rows(catalog: list[dict]) -> list[dict]:
    rows = [r for r in catalog if classify_importability(r) in IMPORTABLE_LABELS]
    rows.sort(key=lambda r: (r.get("source") or "", r.get("externalId") or ""))
    return rows


def build_translations(catalog: list[dict], audit_data: dict) -> dict:
    """Build the full translations payload."""
    rows = _importable_rows(catalog)
    english_names = _load_english_original_names()

    # Index audit by (source, externalId)
    audit_index: dict[tuple[str, str], dict] = {
        (e["source"], e["externalId"]): e
        for e in audit_data["exercises"]
    }

    entries: list[OrderedDict] = []
    status_counts: dict[str, int] = {
        STATUS_TRANSLATED: 0,
        STATUS_UNCHANGED: 0,
        STATUS_REVIEW: 0,
        STATUS_NOT_APPLICABLE: 0,
    }

    for row in rows:
        key = (row.get("source") or "free-exercise-db", row["externalId"])
        audit = audit_index[key]
        entry = _build_entry(row, audit, english_names=english_names)
        entries.append(entry)
        status_counts[entry["status"]] = status_counts.get(entry["status"], 0) + 1

    payload = OrderedDict(
        [
            ("source", "free-exercise-db"),
            ("version", VERSION),
            ("total", len(entries)),
            (
                "statusCounts",
                OrderedDict(
                    [
                        (STATUS_TRANSLATED, status_counts[STATUS_TRANSLATED]),
                        (STATUS_UNCHANGED, status_counts[STATUS_UNCHANGED]),
                        (STATUS_REVIEW, status_counts[STATUS_REVIEW]),
                        (STATUS_NOT_APPLICABLE, status_counts[STATUS_NOT_APPLICABLE]),
                    ]
                ),
            ),
            ("translations", entries),
        ]
    )
    return payload


def validate(payload: dict) -> None:
    """Validate the generated payload."""
    translations = payload["translations"]
    total = payload["total"]

    if total != 777:
        raise SystemExit(f"Expected 777 entries, got {total}")
    if len(translations) != 777:
        raise SystemExit(f"Expected 777 translations, got {len(translations)}")

    seen: set[tuple[str, str]] = set()
    for t in translations:
        if not t.get("source"):
            raise SystemExit(f"Missing source: {t}")
        if not t.get("externalId"):
            raise SystemExit(f"Missing externalId: {t}")
        key = (t["source"], t["externalId"])
        if key in seen:
            raise SystemExit(f"Duplicate identity: {key}")
        seen.add(key)

        if t.get("status") not in {
            STATUS_TRANSLATED, STATUS_UNCHANGED, STATUS_REVIEW, STATUS_NOT_APPLICABLE
        }:
            raise SystemExit(f"Invalid status: {t['status']} for {key}")

        if not t.get("name"):
            raise SystemExit(f"Empty name for {key}")
        if not t.get("originalName"):
            raise SystemExit(f"Empty originalName for {key}")

        # Pulldown must not become Puxada
        if re.search(r"\bpulldown\b", t["originalName"], re.I) and re.search(
            r"\bpuxada\b", t["name"], re.I
        ):
            raise SystemExit(f"Pulldown became Puxada: {key}")

    # Check ordering
    ids = [(t["source"], t["externalId"]) for t in translations]
    if ids != sorted(ids):
        raise SystemExit("translations not sorted by (source, externalId)")

    # Count check
    counts = payload["statusCounts"]
    total_counted = sum(counts.values())
    if total_counted != 777:
        raise SystemExit(f"Status counts sum to {total_counted}, not 777")


def dumps_deterministic(payload: dict) -> str:
    return json.dumps(payload, ensure_ascii=False, indent=2) + "\n"


def main() -> None:
    catalog = json.loads(NORMALIZED.read_text(encoding="utf-8"))
    audit_data = json.loads(AUDIT_JSON.read_text(encoding="utf-8"))

    payload = build_translations(catalog, audit_data)
    validate(payload)

    text = dumps_deterministic(payload)
    OUT_FILE.write_bytes(text.encode("utf-8"))

    counts = payload["statusCounts"]
    print(
        json.dumps(
            {
                "total": payload["total"],
                "TRANSLATED": counts[STATUS_TRANSLATED],
                "UNCHANGED": counts[STATUS_UNCHANGED],
                "REVIEW": counts[STATUS_REVIEW],
                "NOT_APPLICABLE": counts[STATUS_NOT_APPLICABLE],
            },
            ensure_ascii=False,
        )
    )


if __name__ == "__main__":
    main()
