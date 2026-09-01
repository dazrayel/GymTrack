# GymTrack

Aplicativo Android nativo para montar treinos de musculação, executá-los na academia e consultar o histórico — **offline**, com persistência local.

O objetivo é oferecer um fluxo simples durante o treino: saber o que fazer agora, registrar séries (reps e carga), pular ou trocar de exercício quando o equipamento estiver ocupado, e ver um resumo ao finalizar.

## 📱 Sobre o projeto

Treinar com um plano no papel ou no bloco de notas costuma gerar dois problemas: perder o contexto no meio da sessão (qual série, qual carga) e não ter um registro confiável do que foi feito.

O GymTrack resolve isso no dispositivo: o catálogo de exercícios e os treinos ficam no telefone (Room). Durante a sessão, o progresso é gravado a cada série; ao sair e voltar, o estado é reconstruído a partir do banco, não de um rascunho só em memória.

A interface está em português. O projeto está em desenvolvimento (versão do app `1.0`, `versionCode` 1).

## ✨ Funcionalidades

### Catálogo e treinos

- Cadastro, edição, exclusão e busca de exercícios (catálogo: campo **Buscar exercícios**)
- Grupo muscular **primário** obrigatório, escolhido de um catálogo fechado
- Músculos **secundários** (`0..N`), do mesmo catálogo, sem duplicar e sem coincidir com o primário
- Tipo de equipamento em catálogo, incluindo **Outro**
- Criação, edição e exclusão de treinos
- Inclusão de exercícios no treino com séries planejadas, faixa de repetições (`minRepetitions` / `maxRepetitions`), carga planejada, descanso e notas
- No detalhe do treino, **Adicionar exercício** abre o seletor **Selecionar exercício**, com busca local por **nome** (case-insensitive, imediata; placeholder **Buscar exercício**; limpar com **Limpar pesquisa**)
- Busca vazia (ou só espaços) mostra o catálogo completo na ordem atual; sem correspondência aparece **Nenhum exercício encontrado** (distinto de biblioteca vazia)
- Exercícios já no treino continuam no seletor e continuam selecionáveis; a escolha segue para **Configurar exercício**
- Reordenação de exercícios no treino
- Campos de texto livre (nome e descrição do treino, nome do exercício, observações) pedem capitalização de frases no teclado; buscas e campos numéricos não

### Execução da sessão

- Início de uma sessão a partir de um treino (snapshot dos exercícios; uma sessão em andamento por vez)
- Continuação de sessão inacabada (Início / Home)
- Registro de séries realizadas (repetições e carga)
- Meta de reps apenas visual (`Meta: 8–12 reps` ou `Meta: 8 reps` se mínimo = máximo)
- Conclusão de série **não** exige ficar dentro da faixa
- Timer de descanso com pausar, retomar e pular (quando o exercício tem `restSeconds` > 0)
- Tempo decorrido da sessão
- Barra de progresso (exercícios e séries)
- **Pular exercício** (adiar): não cria nem apaga séries; o exercício fica `SKIPPED` e sai da fila automática
- **Fazer agora**: escolher qualquer exercício incompleto da sessão (incluindo `PENDING` posterior), sem marcar o anterior como pulado
- Rascunhos de reps/carga isolados por exercício da sessão (`WorkoutExecutionDraftStore`)
- Finalizar sessão com confirmação se ainda houver séries incompletas
- Resumo com duração (horário local), volume, séries por exercício e rótulos Concluído / Pulado / Pendente

### Acompanhamento

- Tela inicial com estatísticas por período, treino em andamento, últimos treinos e recordes
- Histórico de sessões concluídas, com exclusão opcional de cada sessão
- A exclusão exige confirmação num diálogo; **Cancelar** não remove nada
- Só sessões com status `COMPLETED` podem ser excluídas; sessões `IN_PROGRESS` não são apagadas
- Ao confirmar, a linha de `workout_sessions` é removida e os dados da sessão (`workout_session_exercises`, `workout_sets`) saem pelo **CASCADE** já definido no Room — **sem** migration nem mudança de schema
- Estatísticas por exercício (marcas e evolução a partir das séries gravadas)

A aba **Configurações** existe na navegação, mas a tela atual é apenas um título — não há opções implementadas.

## 🏗️ Arquitetura

O app segue uma camada **UI → ViewModel → Repository → DAO → Room**, com modelos de domínio separados das entidades.

| Camada | Papel |
| --- | --- |
| **UI (Jetpack Compose + Material 3)** | Telas e componentes; observa `StateFlow` de `UiState` |
| **ViewModel** | Orquestra casos de uso, validação de campos e eventos de navegação; não acessa Room diretamente |
| **Repository** | Regras de persistência (transações, snapshots da sessão, skip/resume, conclusão) |
| **DAO + Room** | SQL, `Flow` reativo e migrations |
| **Domain** | `Exercise`, `Workout`, `WorkoutSession`, status, catálogos, formatação de meta de reps |
| **Navegação** | `Navigation Compose` + barra inferior (`home`, `workouts`, `history`, `settings`) e rotas de detalhe, execução, resumo e estatísticas |
| **DI** | Hilt (`@HiltViewModel`, módulos de banco) |
| **Tempo** | `TimeProvider` para relógio injetável (sessão, descanso, testes) |

Não há backend, sincronização na nuvem nem API HTTP neste código.

## 🛠️ Tecnologias utilizadas

Confirmado em `gradle/libs.versions.toml` e `app/build.gradle.kts`:

- Kotlin 2.2.21, JDK 17
- Android (`minSdk` 26, `compileSdk` / `targetSdk` 35)
- Jetpack Compose (BOM `2024.12.01`) e Material 3
- Navigation Compose `2.8.5`
- Lifecycle / ViewModel
- Room `2.6.1` (KSP)
- Hilt `2.59.2`
- Kotlin Coroutines `1.9.0` e Flow
- Gradle (Android Gradle Plugin `9.2.0`)
- Testes: JUnit 4, `androidx.arch.core:core-testing`, Coroutines Test, Espresso, Compose UI Test, Hilt Testing, Room Testing

## 🗄️ Banco de dados

Room, versão **8**, com `exportSchema` em `app/schemas/`.

Persistência principal:

- Exercícios e músculos secundários (`exercise_secondary_muscles`, N:N, `ON DELETE CASCADE`)
- Treinos e itens do treino (`workout_exercises`)
- Sessões (`workout_sessions`) e snapshot dos exercícios (`workout_session_exercises`)
- Séries realizadas (`workout_sets`)

Apagar uma sessão `COMPLETED` no histórico remove a linha em `workout_sessions`; `workout_session_exercises` e `workout_sets` saem pelo `ON DELETE CASCADE` já definido. **Não** houve migration nem mudança de versão do Room para essa funcionalidade.

Migrations relevantes para o estado atual:

| Migration | O que faz |
| --- | --- |
| **6 → 7** | Cria `exercise_secondary_muscles`; adiciona `secondaryMuscles` (TEXT) no snapshot da sessão; normaliza sinônimos conhecidos de músculo/equipamento (ex.: Peito/Chest → Peitoral, Back → Costas). Valores desconhecidos (ex. Legs) **não** são forçados para o catálogo. |
| **7 → 8** | Adiciona `status` em `workout_session_exercises` (`TEXT NOT NULL DEFAULT 'PENDING'`). **Não** altera `workout_sets`. |

Atualizar um exercício do catálogo usa `UPDATE` (não `INSERT OR REPLACE` da linha), para não quebrar vínculos de `workout_exercises` por cascade. O snapshot da sessão **não** muda se o catálogo for editado depois.

## 🔄 Execução dos treinos

Cada exercício da sessão tem um status:

| Status | Significado |
| --- | --- |
| `PENDING` | Ainda não escolhido / não concluído |
| `IN_PROGRESS` | Exercício selecionado agora (incluindo “Fazer agora”) |
| `COMPLETED` | Todas as séries **planejadas** foram registadas (`sets.size >= plannedSets`) |
| `SKIPPED` | Adiado pelo utilizador (Pular) |

A fila automática (`resolveCurrentExerciseIndex`) escolhe:

1. o primeiro `IN_PROGRESS` ainda incompleto;
2. senão, o primeiro incompleto que **não** seja `SKIPPED`;
3. senão, nenhum exercício atual (a UI trata isso sem crash).

**Pular** não é o mesmo que **escolher outro**:

- Pular → `SKIPPED`; o exercício não volta sozinho para a fila.
- Fazer agora → o escolhido fica `IN_PROGRESS`; o `IN_PROGRESS` anterior volta a `PENDING` (não a `SKIPPED`). Séries e drafts do exercício anterior permanecem.

Ao concluir o exercício escolhido fora de ordem, o current volta ao primeiro incompleto não pulado (em geral o de menor `position`).

A lista de exercícios da sessão e a troca manual estão disponíveis na fase de trabalho (`WORKING`), não durante o descanso (`RESTING`).

## 🧪 Testes

O projeto tem testes **JVM** (`app/src/test`) e **instrumentados** (`app/src/androidTest`):

- Regras de domínio (catálogo, meta de reps, resolução do exercício atual)
- ViewModels (execução, resumo, exercícios, home, histórico, estatísticas, detalhe do treino)
- Filtro local do seletor de exercícios (`ExercisePickerFilterTest`)
- Repositories (exercícios, sessões, treinos)
- Room e migrations (incluindo 6→7 e 7→8)
- UI Compose (execução, catálogo, resumo, histórico, detalhe do treino / picker, entre outras)

A suíte instrumentada usa Hilt (`HiltTestRunner`) e um emulador/dispositivo. Testes do picker com busca foram **compilados**; a execução instrumentada depende de dispositivo disponível.

## 📂 Estrutura do projeto

```text
GymTrack/
├── app/
│   ├── schemas/                          # JSON do schema Room (versões 7 e 8)
│   └── src/
│       ├── main/java/com/gymtrack/
│       │   ├── data/                     # Room, DAOs, repositórios
│       │   ├── di/                       # Hilt
│       │   ├── domain/                   # modelos e contratos
│       │   └── presentation/             # Compose, ViewModels, navegação
│       ├── test/                         # testes JVM
│       └── androidTest/                  # testes instrumentados
├── gradle/libs.versions.toml
├── gradlew / gradlew.bat
└── README.md
```

## 🚀 Como executar

Requisitos: Android Studio (ou SDK Android + JDK 17) e um emulador ou dispositivo (`minSdk` 26).

```bash
git clone <url-do-repositorio>
cd GymTrack
```

Abrir a pasta no Android Studio e executar o módulo `app`, ou pela linha de comando:

```bash
# Linux / macOS
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest

# Windows
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest
```

Testes instrumentados (emulador ligado):

```bash
./gradlew :app:connectedDebugAndroidTest
```

## 📋 Estado atual

O núcleo de catálogo, montagem de treinos, execução (incluindo skip, ordem livre e rascunhos), persistência Room 8, histórico (incluindo exclusão de sessões concluídas) e estatísticas básicas **está implementado e coberto por testes**.

Ainda é um projeto em evolução: não há conta de utilizador, sincronização remota nem ecrã de definições funcional.

## 🗺️ Roadmap

A aba Configurações é um placeholder. Novas funcionalidades poderão ser adicionadas a partir daí ou de outros fluxos; não há um roadmap formal versionado neste repositório.

## 👨‍💻 Sobre

Projeto pessoal de portfólio em Android (Kotlin, Compose, Room), focado em um produto pequeno e utilizável na academia, com ênfase em persistência correta e regras de domínio testáveis.

## 📄 Licença

Ainda não foi definida uma licença para este repositório.
