# PUBLISHED_CATALOG V2.3 — Wave 2

- Data (UTC): **2026-10-23 17:24:01**
- Base anterior: V2.2 (**172**)
- Adicionados nesta onda: **11**
- **Pack publicado: 183**
- SHA-256: `c8a13447bb65e67bda8cd3a2cf6234dcfb08706c32a85b84e832aad4f6976cb5`
- Asset: `app/src/main/assets/exercises/gymtrack-exercises.json`
- Tools: `output/free-exercise-db/gymtrack-exercises.json`
- Seleção: `analysis/MAIN_CATALOG_V2_3.json`

## Contagens

| Camada | Quantidade |
| ------ | ---------: |
| Free Exercise DB completo | 876 |
| Publicado V2.2 (preservado) | 172 |
| Wave 2 adicionados | 11 |
| Excluído Wave 2 (mídia) | 1 (`High_Cable_Curls`) |
| Pendentes Wave 1 (inalterados) | 4 |
| **Pack publicado V2.3** | **183** |

- Mídia total: 183/183 com `0.jpg`, 183/183 com `1.jpg`
- Novos JPG copiados: **22** (hash match: 22)
- Licença mídia: **Unlicense (public domain)** (https://github.com/yuhonas/free-exercise-db/blob/main/LICENSE.md)
- Staging: `%TEMP%\gymtrack_v22_wave2_media\exercises`

## IDs adicionados (11)

- `Cable_Internal_Rotation` — Rotação interna na polia | Ombros | Cabos
- `Cable_One_Arm_Tricep_Extension` — Extensão unilateral de tríceps na polia | Tríceps | Cabos
- `Front_Cable_Raise` — Elevação frontal na polia | Ombros | Cabos
- `Natural_Glute_Ham_Raise` — Glute-ham raise natural (GHR) | Posteriores | Peso corporal
- `Reverse_Barbell_Curl` — Rosca inversa com barra | Bíceps | Barra
- `Smith_Machine_Incline_Bench_Press` — Supino inclinado no Smith | Peitoral | Máquina
- `Split_Squat_with_Dumbbells` — Agachamento búlgaro com Halteres | Quadríceps | Halteres
- `Standing_Dumbbell_Triceps_Extension` — Extensão de tríceps acima da cabeça com Halter | Tríceps | Halteres
- `Standing_Low-Pulley_Deltoid_Raise` — Elevação lateral unilateral na polia baixa | Ombros | Cabos
- `Standing_Military_Press` — Desenvolvimento militar em pé | Ombros | Barra
- `Zottman_Curl` — Rosca Zottman | Bíceps | Halteres

## Excluídos desta onda

- `High_Cable_Curls` — mídia FEDB = extensão de tríceps deitada (não rosca na polia alta)

## Pendentes Wave 1 (preservados, não publicados)

- `Spider_Curl`
- `Dumbbell_Scaption`
- `Machine_Triceps_Extension`
- `Seated_One-arm_Cable_Pulley_Rows`

## Limitações conhecidas

- **`Zottman_Curl`:** os dois frames não mostram claramente a pronação na fase negativa; início neutro e pico supinado são coerentes, mas a característica completa não foi comprovada visualmente.
- **`Natural_Glute_Ham_Raise`:** a mídia mostra aparelho GHR dedicado (Hoist); as instruções textuais FEDB mencionam improvisação em lat pulldown/preacher. O padrão GHR visual foi aprovado.

## Distribuição (pack completo)

| Grupo | Qtd |
| ----- | --: |
| Abdômen | 20 |
| Antebraço | 9 |
| Bíceps | 15 |
| Costas | 22 |
| Glúteos | 9 |
| Lombar | 5 |
| Ombros | 22 |
| Panturrilhas | 6 |
| Peitoral | 23 |
| Posteriores | 13 |
| Quadríceps | 20 |
| Trapézio | 5 |
| Tríceps | 14 |

## Distribuição (somente Wave 2)

| Grupo | Qtd |
| ----- | --: |
| Bíceps | 2 |
| Ombros | 4 |
| Peitoral | 1 |
| Posteriores | 1 |
| Quadríceps | 1 |
| Tríceps | 2 |

## Notas

- Os 172 registros V2.2 foram preservados (identidade e campos do pack anterior).
- Relatórios V2.1/V2.2 históricos não foram reescritos.
- Importador continua por upsert; ausentes do pack não são apagados.
