# GymTrack

## 1. Visão

GymTrack é um aplicativo Android nativo para gerenciamento, execução e acompanhamento de treinos de musculação.

O objetivo é criar um aplicativo simples, rápido e agradável para ser utilizado durante o treino.

O aplicativo é inicialmente **offline-first**.

---

## 2. Objetivo do MVP

O usuário deverá conseguir:

* visualizar seus treinos;
* criar treinos;
* editar treinos;
* excluir treinos;
* adicionar exercícios aos treinos;
* configurar séries;
* configurar faixa de repetições;
* configurar descanso;
* iniciar um treino;
* registrar carga;
* registrar repetições;
* concluir séries;
* utilizar timer de descanso;
* finalizar o treino;
* consultar histórico;
* consultar desempenho anterior de um exercício;
* visualizar estatísticas básicas.

---

## 3. Stack

### Android

* Kotlin
* Jetpack Compose
* Material 3
* Navigation Compose

### Arquitetura

* MVVM
* Clean Architecture simplificada

### Dependency Injection

* Hilt

### Persistência

* Room
* SQLite

### Assíncrono

* Kotlin Coroutines
* Kotlin Flow

### Build

* Gradle Kotlin DSL

---

## 4. Arquitetura

A aplicação deve seguir:

```text
Presentation
     ↓
Domain
     ↓
Data
```

A camada Presentation não deve acessar diretamente o Room.

A camada Domain não deve depender de Android Framework quando isso não for necessário.

A camada Data é responsável por persistência e implementação dos repositories.

### Regras arquiteturais atuais

* Não criar `DashboardRepository` sem necessidade real.
* Não criar queries específicas para métricas quando os dados existentes forem suficientes.
* Não carregar dados no Composable.
* ViewModels não devem receber `NavController`.
* Navegação pertence ao Navigation Graph.
* Cálculos de métricas devem permanecer no Domain.
* Evitar N+1.
* Preferir os Flows existentes.
* Não persistir métricas derivadas sem necessidade.
* Não introduzir abstrações antecipadamente.

---

## 5. Princípios

Priorizar:

* simplicidade;
* legibilidade;
* manutenibilidade;
* testabilidade;
* baixo acoplamento;
* código Kotlin idiomático.

Não criar abstrações sem necessidade.

Não criar funcionalidades fora do escopo solicitado.

Não implementar funcionalidades futuras antecipadamente.

Antes de alterar a arquitetura, analisar o código existente e explicar o motivo da alteração.

---

## 6. Estado atual do projeto

### Persistência

* Room versão **8** (`GymTrackDatabase`, `exportSchema = true`).
* SQLite (`gymtrack.db`).
* Migrations registradas em `DatabaseModule`: `1→2` … `7→8`. **Não existe** `MIGRATION_8_9` nem coluna `selectedExerciseId`.
* Schemas exportados: `app/schemas/.../7.json` e `8.json` (além das versões anteriores já existentes).
* Não existe tabela específica para estatísticas.
* Não existe tabela específica para PRs.
* Métricas são derivadas dos dados existentes.

Detalhe do catálogo, músculos secundários, `status` por exercício da sessão e ordem livre: ver a seção **Catálogo, status e ordem livre** abaixo.

Exclusão de sessões `COMPLETED` no histórico: ver **Exclusão de sessões do histórico**. Sem migration; Room permanece na versão 8.

### Backend

Ainda não existe backend.

### Autenticação

Ainda não existe autenticação.

### IA

Ainda não existe IA.

### Sincronização

Ainda não existe sincronização em nuvem.

### Git

Branch principal de desenvolvimento atual:

```text
feature/next-step
```

Últimos commits relevantes:

```text
15c03f8 feat: implement free exercise ordering and status flow
2f9f32d fix: preserve workout execution input drafts
af1139d fix: show local workout summary times
b6fad76 fix: return home from execution tab
728688a feat: allow access to exercises from picker
```

HEAD da branch: `15c03f8`. Há um `README.md` na raiz (portfólio GitHub; não substitui este documento).

---

# 7. Roadmap

## Fase 1 — Fundação

### Objetivo

Criar a base técnica do aplicativo.

### Escopo

* projeto Android;
* configuração Gradle;
* Compose;
* Material 3;
* Hilt;
* Room;
* Navigation.

### Estado

**CONCLUÍDA.**

---

## Fase 2 — Exercícios

### Objetivo

Permitir o gerenciamento da biblioteca de exercícios.

### Escopo

* banco de exercícios;
* listagem;
* pesquisa;
* filtros;
* detalhes;
* criação;
* edição;
* exclusão.

### Estado

**CONCLUÍDA.**

A tela atual é `ExercisesScreen`.

O modelo de domínio do **catálogo** (`com.gymtrack.domain.model.Exercise`) possui:

```text
Exercise(
    id,
    name,
    muscleGroup,       // primário (catálogo fechado MUSCLE_GROUPS)
    equipmentType,     // catálogo fechado EQUIPMENT_TYPES (inclui Outro)
    secondaryMuscles   // 0..N, List<String>, nunca inclui o primário
)
```

Isto **não** é o exercício da sessão. O snapshot em execução é `WorkoutSessionExercise` (tabela `workout_session_exercises`), copiado em `startSession` e independente de edições posteriores no catálogo.

O clique no card continua sendo reservado para edição.

Ações adicionais podem ser adicionadas sem alterar esse comportamento.

---

## Fase 3 — Treinos

### Objetivo

Permitir a criação e configuração dos treinos.

### Escopo

* criação;
* edição;
* exclusão;
* exercícios;
* séries;
* repetições;
* descanso;
* ordenação.

### Estado

**CONCLUÍDA.**

---

## Fase 4 — Execução

### Objetivo

Permitir executar um treino e registrar seus resultados.

### Escopo

* iniciar treino;
* registrar séries;
* cargas;
* repetições;
* progresso;
* finalizar treino.

### Estado

**IMPLEMENTADA.**

O fluxo de execução está integrado ao aplicativo. Depois da Fase 8, a branch `feature/next-step` acrescentou skip, status por exercício da sessão, ordem livre (“Fazer agora”) e meta visual de reps — ver secção **Catálogo, status e ordem livre**.

### Pendências técnicas conhecidas

As pendências funcionais abaixo foram tratadas na estabilização **8.S** (ver Fase 8):

* garantia de no máximo uma sessão `IN_PROGRESS` (`inProgressLock`, Room v6);
* `TimeProvider` no ciclo relevante da sessão (`startSession` / `completeSet` e restante do fluxo já alinhado);
* retomada após cold start pela Home (`observeInProgress()` → CTA → sessão existente).

O flake `restEnds_returnsToWorking` permanece classificado como problema de **teste/harness**. Não deve gerar alteração artificial no timer.

Não corrigir essas áreas incidentalmente durante outra fase.

---

## Fase 5 — Timer

### Objetivo

Fornecer timer de descanso durante a execução.

### Escopo original

* descanso;
* pausa;
* continuar;
* adicionar tempo;
* pular.

### Estado real

**IMPLEMENTADO E INTEGRADO AO FLUXO DE EXECUÇÃO.**

O timer não deve ser tratado como uma fase futura independente.

A execução e o timer atualmente fazem parte do fluxo funcional já existente.

Pendências específicas do timer devem ser tratadas na etapa de estabilização, caso necessário.

---

# Fase 6 — Histórico e Dashboard

A Fase 6 foi implementada em quatro etapas:

```text
6.1 Dashboard
6.2 Evolução e frequência
6.3 Recordes
6.4 Estatísticas de período
```

## 6.1 — Dashboard

### Implementado

O Início deixou de ser apenas uma tela de apresentação e passou a apresentar dados reais.

Inclui:

* treino recente;
* totais da semana;
* volume;
* tempo;
* exercícios distintos;
* estados empty/loading/erro;
* navegação para o resumo da sessão.

Somente sessões `COMPLETED` são consideradas.

### Estado

**CONCLUÍDA.**

---

## 6.2 — Evolução e frequência

### Implementado

* frequência por dias treinados;
* tendência de 7 dias;
* timezone local;
* domínio puro;
* sem N+1;
* sem biblioteca de gráficos.

### Estado

**CONCLUÍDA.**

---

## 6.3 — Recordes

### Implementado

O Início apresenta recordes all-time agrupados por `exerciseName`.

São derivados de:

```text
personalRecords()
```

Os critérios existentes da Fase 5.4 são reutilizados.

Inclui:

* melhor carga;
* melhores repetições;
* melhor volume.

O peso `0` continua sendo válido.

Os registros utilizam o snapshot de `exerciseName`.

Não existe persistência específica de PR.

### Estado

**CONCLUÍDA.**

---

## 6.4 — Estatísticas de período

### Implementado

O card de totais possui seletor:

```text
Semana | Mês | Tudo
```

O seletor altera:

* totais;
* frequência.

Não altera:

* treino recente;
* tendência de 7 dias;
* recordes;
* acesso rápido.

### Períodos

#### Semana

Utiliza `isoWeekBounds`.

#### Mês

Utiliza:

```text
isoMonthBounds
```

com:

* `ZoneId.systemDefault()`;
* `atStartOfDay`;
* intervalo `[start, end)`.

#### Tudo

Não utiliza timestamp artificial.

O período `ALL` utiliza todas as sessões `COMPLETED`.

### Estado

**CONCLUÍDA.**

### Arquitetura

O dashboard utiliza:

```text
observeCompletedSessions()
        +
observeCompletedSetHistory()
        +
selectedPeriod
        ↓
HomeViewModel
        ↓
Domain
        ↓
HomeUiState
        ↓
HomeScreen
```

O domínio utiliza:

```text
PeriodDashboardStats
dashboardPeriodStats
DashboardPeriod
```

Não existe:

* DashboardRepository;
* query específica de dashboard;
* migration;
* tabela de estatísticas;
* N+1.

### Critério final da Fase 6

A Fase 6 está **CONCLUÍDA**.

---

# Fase 7 — Estatísticas e desempenho por exercício

A Fase 7 foi dividida em:

```text
7.1 Domain
7.2 Tela e navegação pelo Home
7.3 Entrada pelo catálogo de Exercícios
```

## 7.1 — Histórico de desempenho por exercício

### Implementado

Foi criado:

```text
ExercisePerformancePoint
```

e:

```text
exercisePerformanceHistory(...)
```

A função:

* filtra por `exerciseName`;
* utiliza somente dados COMPLETED;
* reutiliza `sessionExerciseStats`;
* gera um ponto por sessão;
* calcula melhor carga;
* calcula melhores repetições;
* calcula volume;
* ordena cronologicamente;
* utiliza `sessionId` como desempate.

Não existe nova regra de PR.

`personalRecords()` permanece responsável pelas regras de PR.

### Identidade

A identidade histórica continua sendo:

```text
exerciseName
```

O histórico utiliza snapshot.

Não existe `exerciseId` em `CompletedSetRecord`.

### Estado

**CONCLUÍDA.**

---

## 7.2 — Tela de desempenho por exercício

### Implementado

Nova rota:

```text
exercise_stats/{exerciseName}
```

A navegação utiliza encoding do nome.

A tela apresenta:

* PR all-time;
* carga;
* repetições;
* volume;
* histórico cronológico;
* data/hora local;
* loading;
* empty;
* erro.

Não existe gráfico nesta versão.

### Origem dos dados

```text
observeCompletedSetHistory()
        ↓
exercisePerformanceHistory()
        +
personalRecords()
```

O ViewModel não possui `NavController`.

O ViewModel recebe o `exerciseName` através de `SavedStateHandle`.

### Empty

Quando não há histórico:

```text
Sem histórico para este exercício
```

Não são exibidos zeros artificiais.

### Estado

**CONCLUÍDA.**

---

## 7.3 — Acesso pelo catálogo de Exercícios

### Implementado

A tela `ExercisesScreen` possui uma ação secundária:

```text
Ver desempenho
```

Fluxo:

```text
Exercícios
    ↓
Ver desempenho
    ↓
onExerciseStatsClick(exercise.name)
    ↓
GymTrackNavGraph
    ↓
exercise_stats/{Uri.encode(exerciseName)}
    ↓
ExerciseStatsViewModel
```

### Regras

O clique no card continua abrindo a edição.

A lixeira continua excluindo.

A ação de desempenho é independente dessas ações.

Nenhuma lógica de navegação foi adicionada ao `ExerciseViewModel`.

### Estado

**CONCLUÍDA.**

---

## Critério final da Fase 7

A Fase 7 está **CONCLUÍDA em 7.1–7.3**.

O MVP agora possui:

* estatísticas básicas;
* volume;
* frequência;
* PRs;
* tendência;
* desempenho histórico por exercício;
* acesso ao desempenho pelo Home;
* acesso ao desempenho pelo catálogo de Exercícios.

Não criar uma segunda tela de estatísticas sem necessidade.

Não duplicar o dashboard.

---

# Fase 8 — Polimento e estabilização

A Fase 8 é incremental: primeiro estabilização técnica, depois polimento UX.

## 8.S — Estabilização da execução

**CONCLUÍDA** em quatro commits:

```text
6fdedb8  fix: use TimeProvider for session timestamps
1c4880f  fix: enforce single in-progress workout session
1deb038  fix: resume or block when starting workout with active session
16d7533  feat: resume in-progress workout from Home
```

* **8.S.1** — `TimeProvider` aplicado ao ciclo relevante da sessão (`startSession` / `completeSet`; descanso e finalização já o usavam).
* **8.S.2** — no máximo uma sessão `IN_PROGRESS`, via `inProgressLock` e Room **v6** (`MIGRATION_5_6`).
* **8.S.3** — `startSession` distingue `Created`, `Resumed` e `BlockedOtherWorkout`; o detalhe do treino mostra diálogo no caso bloqueado.
* **8.S.4** — a Home observa `observeInProgress()`, mostra CTA para a sessão existente e navega para `workout_execution/{sessionId}` **sem** chamar `startSession()`.

```text
8.S.1–8.S.4: concluídas e commitadas.
8.S.5: não implementar como nova feature; o item original relacionado ao timer foi classificado como fora de escopo / sem bug de produto evidenciado.
```

### Process death / cold start

A retomada após cold start foi resolvida pela **8.S.4**:

```text
Room → observeInProgress() → HomeUiState.inProgressSession
→ CTA "Continuar treino"
→ workout_execution/{sessionId}
```

A sessão existente é reutilizada. Nenhum `startSession()` novo é chamado a partir da Home. **Não** há auto-navegação no cold start: isso é intencional e **não** deve ser tratado como bug.

---

## 8.0 — Estabilização pré-polimento

### Objetivo original

Antes de adicionar efeitos visuais ou alterações de UX, auditar:

* pendências técnicas da Fase 4;
* `IN_PROGRESS`;
* `TimeProvider`;
* process death;
* timer;
* flake `restEnds_returnsToWorking`;
* tratamento de erros existente;
* inconsistências de estado.

Esta etapa deve separar problemas funcionais reais de problemas exclusivamente de teste.

Não corrigir automaticamente problemas apenas porque foram encontrados durante a auditoria.

### Estado

**CONCLUÍDA.** Auditoria técnica encerrada.

As pendências funcionais relevantes da Fase 4 foram auditadas e os problemas reais identificados foram resolvidos pela sequência **8.S.1–8.S.4**.

Não há bug funcional conhecido da 8.0 aguardando implementação.

O flake `restEnds_returnsToWorking` permanece classificado como problema de teste/harness e não deve gerar alteração artificial no timer.

As falhas instrumentadas envolvendo Activity em estado `PAUSED` / teardown e crash do processo permanecem classificadas como harness, não como regressão funcional da aplicação.

Não alterar produção para mascarar esses problemas.

---

## 8.1 — Auditoria UX

Avaliar todas as telas existentes:

* Exercícios;
* Treinos;
* Execução;
* Timer;
* Histórico;
* Resumo da sessão;
* Início;
* Estatísticas por exercício.

Verificar:

* hierarquia visual;
* textos;
* botões;
* ações destrutivas;
* estados vazios;
* loading;
* erros;
* estados disabled;
* consistência entre telas;
* navegação.

Primeiro auditar.

Depois implementar somente os problemas aprovados.

### Estado

**CONCLUÍDA.** Auditoria UX realizada; escopo aprovado implementado (`f79cc82 feat: polish workout screens UX`).

A 8.1 é polimento visual e consistência **sem** mudar comportamento funcional.

### Escopo aprovado da 8.1

1. **Exercícios** — alinhar o padding interno do card/list item com o padrão das demais listas; não remover nem alterar as ações existentes.
2. **Execução** — aplicar `titleLarge` à TopAppBar; manter exatamente o texto `"Treino em andamento"`; não alterar timer, descanso, ações ou `WorkoutExecutionViewModel`.
3. **Resumo da sessão** — estado `session_not_found` no padrão visual de empty (ícone + título + eventualmente hint); manter a regra funcional de sessão não encontrada; aplicar `titleLarge` à TopAppBar.
4. **Stats por exercício** — `empty_exercise_stats` no padrão visual de empty (ícone + título + hint); manter o título existente; aplicar `titleLarge` à TopAppBar.

### Telas sem alteração na 8.1

* Home — nenhuma alteração de UX aprovada. O comportamento da **8.S.4** (CTA de retomada de `IN_PROGRESS`) permanece.
* Treinos — nenhuma alteração de UX aprovada.
* Histórico — nenhuma alteração de UX aprovada.

### Fora da 8.1

* Snackbar / feedback visual adicional → 8.2
* acessibilidade profunda / `contentDescription` / semântica → 8.3
* animações e transições → 8.4
* performance → 8.5
* tratamento de erros técnicos como estados de utilizador → 8.6
* auditoria final completa → 8.7
* redesign da Settings
* alteração do timer/rest
* alteração do `WorkoutExecutionViewModel`
* alteração de `startSession`
* alteração do Room/schema
* alteração do CTA da Home
* auto-navegação após process death
* correção artificial dos flakes/harness

---

## 8.2 — Feedback visual

Objetivo original: melhorar Snackbar, confirmação de ações, sucesso, erro, loading e feedback de operações. Não criar feedback redundante.

### Estado

**CONCLUÍDA — SEM IMPLEMENTAÇÃO**

A auditoria de produto verificou:

* Snackbars de erro já existentes;
* confirmações das ações destrutivas já existentes;
* feedback visual decorrente da atualização das listas;
* feedback da conclusão de séries (progresso / descanso na execução);
* feedback de finalização através do resumo;
* loading inicial das telas;
* operações locais via Room com resposta rápida.

Não foi identificado um problema real de feedback visual que justifique alteração de código.

### Snackbars de sucesso

Não foram adicionados para salvar/excluir treino, criar/excluir exercício, adicionar/remover exercício do treino, concluir série ou finalizar treino.

O próprio fluxo já fornece feedback imediato; um Snackbar acrescentaria informação redundante. Isto **não** é um bug corrigido — é decisão de não duplicar feedback.

### Loading de operações

Não foi criado `isSaving`, `isDeleting` ou estados equivalentes para CRUD.

Operações são locais; a UI já atualiza rapidamente; não há evidência de problema de produto. Estado só para impedir duplo clique seria mudança comportamental desnecessária. Concorrência real futura seria requisito separado.

### Confirmações

Auditadas e consideradas suficientes: excluir treino, excluir exercício, remover exercício do treino, finalizar treino. Os diálogos **não** foram alterados.

### Erros

**8.2:** feedback visual de erro existente (Snackbar).

**8.6 (fora da 8.2):** transformar `Exception.message` em mensagens amigáveis; novos estados de erro; retry; tratamento específico de exceções; mudanças arquiteturais no fluxo de erro. Não antecipar 8.6.

### Execução e 8.S

Execução, timer e descanso **não** receberam alterações na 8.2. Permanecem intactos: `WorkoutExecutionViewModel`, ticker, `maybeExpireRest`, persistência do descanso, `TimeProvider`, comportamento de `IN_PROGRESS`.

Home / 8.S **não** reabertas **nessa fase**: `observeInProgress`, `HomeViewModel`, CTA `IN_PROGRESS` da **sessão**, `StartSessionResult`, `startSession`, rota `workout_execution/{sessionId}`.

Estas proteções descrevem o encerramento da **8.2**. Não significam que execução e Room tenham ficado congelados para sempre: o commit `15c03f8` (e drafts em `2f9f32d`) alterou depois o fluxo de execução e o schema (Room 7 e 8). `IN_PROGRESS` na 8.S continua a referir-se ao **status da sessão**, não ao status do exercício da sessão.

Flakes de harness (`restEnds_returnsToWorking`, Activity `PAUSED`/`DESTROYED`, crash de processo) **não** foram “corrigidos” na 8.2. Sem `@Ignore`, `Thread.sleep` ou produção artificial.

---

## 8.3 — Acessibilidade

### Estado

**CONCLUÍDA.** Auditoria somente leitura; um único item aprovado (Bloco A) implementado e commitado (`1bbf8d6 feat: improve accessibility of exercise search`).

Arquivos do commit:

* `app/src/main/java/com/gymtrack/presentation/exercises/ExercisesScreen.kt`
* `app/src/main/res/values/strings.xml`
* `app/src/androidTest/java/com/gymtrack/exercises/ExercisesScreenTest.kt`

### Problema real identificado

Em `ExercisesScreen`, o botão de limpar pesquisa (`trailingIcon` do campo de busca) era um `IconButton` com `Icons.Filled.Clear` e `contentDescription = null`. Ação interativa só por ícone, sem nome acessível para TalkBack.

### Correção (Bloco A)

```kotlin
contentDescription = stringResource(R.string.clear_search)
```

Nova string: `clear_search` = `"Limpar pesquisa"`.

O comportamento da busca **não** mudou: o clique continua `onSearchQueryChange("")`. O `leadingIcon` de pesquisa permanece decorativo (`contentDescription = null`).

### Teste

Instrumentado `filledSearch_clearButton_hasAccessibleDescription` em `ExercisesScreenTest`: tela com pesquisa preenchida; botão localizado pela semântica; descrição acessível esperada.

Validação:

```text
:app:compileDebugKotlin       PASS
:app:assembleDebug            PASS
ExercisesScreenTest#filledSearch_clearButton_hasAccessibleDescription
Pixel_8 / API 17              PASS
```

### Fora da 8.3 (não implementado)

Itens opcionais B e todos os itens C da auditoria permaneceram fora do escopo.

A 8.3 **não** incluiu:

* alterações na Bottom Navigation;
* `contentDescription` adicional em ícones decorativos;
* descrição adicional em `CircularProgressIndicator`;
* alterações de contraste/tema;
* alterações em Settings;
* redesign;
* alterações em Home, Workouts, History, Summary, Stats ou Execution;
* alterações no timer/rest;
* alterações em ViewModels;
* alterações no Room;
* alterações no Navigation;
* correções de harness/flakes.

---

## 8.4 — Animações

### Estado

**CONCLUÍDA — SEM IMPLEMENTAÇÃO / SEM ALTERAÇÃO NECESSÁRIA**

Auditoria somente leitura. Nenhum problema real de animação/transição. Nenhum código, teste, rota ou ViewModel alterado.

Objetivo original: adicionar animações somente onde agregarem valor. Não adicionar animações indiscriminadamente.

### O que a auditoria verificou

* `GymTrackNavGraph.kt` / `NavHost`;
* Navigation Compose **2.8.5**;
* ausência de `enterTransition`, `exitTransition`, `popEnterTransition` e `popExitTransition`;
* ausência de APIs próprias de animação (`AnimatedVisibility`, `Crossfade`, `AnimatedContent`, `animate*`, `tween`, `spring`, `updateTransition`, `rememberInfiniteTransition`, `animateItem`, `graphicsLayer`, etc.);
* motion apenas dos componentes Material: `CircularProgressIndicator`, `LinearProgressIndicator`, `AlertDialog`, Snackbar;
* loading / empty / conteúdo via `when`/`if`, sem animações customizadas;
* execução `WORKING` / `RESTING` sem animação customizada;
* sem inconsistência, motion duplicado ou UX quebrada que justifique alteração.

### Decisão

* **Nenhum problema real de animação/transição foi identificado.**
* A ausência de animações customizadas **não** é bug.
* O `NavHost` já usa o fade/comportamento padrão da 2.8.5.
* Não foram adicionados fades/slides customizados, `Crossfade`, `AnimatedVisibility`, `animateItem`, shimmer, bounce nem outras animações cosméticas.
* Timer, rest e execução **não** foram alterados.
* Navegação (rotas e comportamento) **não** foi alterada.

### Considerado e não aprovado (preferência / cosmética)

Não são bugs nem requisitos:

* transições customizadas no `NavHost`;
* `Crossfade` entre loading/empty/conteúdo;
* animação entre `WORKING` e `RESTING`;
* animações de listas/cards/FAB;
* animação do timer.

### Proteções (intactos)

A 8.4 **não** reabre 8.S / 8.0. Permanecem intactos: `HomeViewModel`, `HomeUiState`, `observeInProgress`, CTA `IN_PROGRESS`, `StartSessionResult`, `startSession`, `WorkoutExecutionViewModel`, timer/rest, `TimeProvider`, Room / DAO / Repository / migrations, `GymTrackNavGraph` / rotas, testes de navegação, harness/flakes (`PAUSED`, `DESTROYED`, `restEnds_returnsToWorking`).

---

## 8.5 — Performance

### Estado

**CONCLUÍDA — SEM IMPLEMENTAÇÃO / SEM ALTERAÇÃO NECESSÁRIA**

Auditoria somente leitura. Nenhum problema real de performance. Nenhum código, teste, query, Flow, dispatcher ou ViewModel alterado.

Objetivo original: auditar recomposições, coleta de Flows, listas, consultas, N+1, Main Thread e memória. **Não** criar otimizações especulativas.

### O que a auditoria verificou

* `collectAsStateWithLifecycle()` nas telas;
* `StateFlow` nos ViewModels;
* `debounce(300)` + `flatMapLatest` na busca de Exercícios;
* ausência de `collectAsState` sem lifecycle em `src/main`;
* `LazyColumn` com keys estáveis nas listas;
* `Column`/`forEach` somente em conjuntos pequenos e adequados ao modelo atual;
* ausência de N+1 comprovado nas consultas principais;
* Histórico com consulta única/subqueries (`WorkoutHistoryRow`);
* sets concluídos via JOIN (`observeCompletedSetHistory`);
* ausência de `allowMainThreadQueries`;
* operações de Room em `Dispatchers.IO`;
* ticker da execução em `Dispatchers.Default`;
* ausência de `runBlocking` / `GlobalScope` em produção;
* recomposições do timer a cada segundo como contrato esperado da execução;
* ausência de ANR, jank, problema de memória ou gargalo **medido** que justifique otimização.

### Decisão

* **Nenhum problema real de performance foi identificado.**
* Sem evidência que justifique alteração de código.
* Não realizar otimizações especulativas.
* Não alterar queries, Flow, dispatchers, listas ou ViewModels só por hipótese teórica.
* Não tocar na execução / ticker / timer / rest.
* Não reabrir os contratos da 8.S / 8.0.

### Considerado e não aprovado (hipótese / preferência, sem evidência)

Não são bugs nem backlog aprovado; **não** implementadas:

* substituir múltiplos `observeSets` por sessão por um Flow/consulta agregado;
* alterar dispatcher das agregações da Home;
* otimizar consultas de Stats;
* trocar `Column` por `LazyColumn` no Resumo;
* deduplicar `getAll()` no detalhe do treino.

### Fora da 8.5

* 8.3 — acessibilidade;
* 8.4 — animações;
* 8.6 — tratamento/copy de erros;
* 8.7;
* 8.S / 8.0;
* `HomeViewModel`, `observeInProgress`, CTA `IN_PROGRESS`;
* `StartSessionResult`, `startSession`;
* `WorkoutExecutionViewModel`;
* timer / rest / ticker / `maybeExpireRest`;
* `TimeProvider`;
* Room schema / migrations;
* harness/flakes (`PAUSED`, `DESTROYED`, `restEnds_returnsToWorking`).

---

## 8.6 — Tratamento de erros

### Estado

**CONCLUÍDA — SEM IMPLEMENTAÇÃO / SEM ALTERAÇÃO NECESSÁRIA**

Auditoria somente leitura. Nenhum problema real de tratamento de erros. **Nenhum arquivo de código ou teste deve ser alterado como resultado da 8.6.** Nenhum código, teste ou contrato de UI foi alterado.

### O que a auditoria verificou

* Snackbar de erro já existe onde necessário;
* validações existentes continuam fornecendo feedback;
* diálogos existentes cobrem as confirmações necessárias;
* o fluxo 8.S possui o diálogo/feedback necessário;
* não existe fluxo crítico sem feedback comprovado;
* não foi identificado erro de produto que exija transformar exceções em novos estados de UI;
* não há evidência suficiente para justificar novos estados de erro, retry ou mudanças de arquitetura;
* `e.message` / copy técnica, quando aplicável, permanece separada de qualquer futura decisão de UX/copy;
* não deve ser criado tratamento adicional só para preencher a fase.

### Decisão

* **Nenhum problema real de tratamento de erros foi identificado.**
* Não implementar novos estados de erro.
* Não adicionar retry.
* Não alterar contratos existentes de UI.
* Não alterar ViewModels só para introduzir estados hipotéticos.
* Não alterar o fluxo da execução.
* Não alterar o comportamento estabilizado da 8.S / 8.0.

### Testes

* Nenhum teste novo é necessário para esta conclusão.
* Testes **não** foram alterados.
* Qualquer futura alteração de copy/tratamento de erro deverá ter testes de ViewModel/Compose correspondentes.
* Testes de harness **não** devem ser alterados nem mascarados para acomodar UX.

### Fora da 8.6

* 8.3 — acessibilidade;
* 8.4 — animações;
* 8.5 — performance;
* 8.7;
* 8.S / 8.0;
* `StartSessionResult`;
* `startSession`;
* CTA `IN_PROGRESS`;
* `observeInProgress`;
* `TimeProvider`;
* ticker;
* `maybeExpireRest`;
* Room e migrations;
* timer/rest;
* falhas de `skipRest` no expiry (contrato da execução);
* harness/flakes (`PAUSED`, `DESTROYED`, `restEnds_returnsToWorking`).

---

## 8.7 — Auditoria final de UX

### Estado

**CONCLUÍDA — Bloco A implementado** (`dbdfe1e feat: improve exercise search empty state`).

Auditoria somente leitura do fluxo:

```text
Exercícios → Treinos → Execução → Timer → Finalização → Histórico → Dashboard → Desempenho do exercício
```

Um único problema real (Bloco A) foi aprovado, implementado e commitado. Itens B/C **não** foram implementados.

### Problema encontrado

Na tela de Exercícios, pesquisa preenchida sem correspondências reutilizava o empty de catálogo (`empty_exercises` / `empty_exercises_hint`). Copy incorreta: o catálogo não estava necessariamente vazio.

### Correção (Bloco A)

Diferenciar **catálogo vazio** de **busca sem resultados**.

Quando `searchQuery.isNotEmpty()` e a lista filtrada está vazia:

* `empty_exercises_search` — Nenhum exercício encontrado
* `empty_exercises_search_hint` — Tente outro termo ou limpe a pesquisa.

Catálogo vazio (`searchQuery` vazio + lista vazia) mantém `empty_exercises` / `empty_exercises_hint`.

Busca, debounce, filtro, FAB, ViewModel e `clear_search` **permaneceram intactos**.

Arquivos do commit:

* `app/src/main/java/com/gymtrack/presentation/exercises/ExercisesScreen.kt`
* `app/src/main/res/values/strings.xml`
* `app/src/androidTest/java/com/gymtrack/exercises/ExercisesScreenTest.kt`

### Testes

* `emptyCatalog_showsCatalogEmptyMessage`
* `filledSearch_noResults_showsSearchEmptyNotCatalogEmpty`
* `filledSearch_clearButton_hasAccessibleDescription`

```text
:app:compileDebugKotlin       PASS
:app:assembleDebug            PASS
```

Testes instrumentados alvo: comportamento da correção validado. Falhas `edited == null`, crash de processo e `PAUSED`/`DESTROYED` = harness conhecido. Sem `@Ignore`, `Thread.sleep` ou workaround de harness.

Nota posterior (`d74fcc1`): `edited == null` em `clickingCard_opensEditDialog` **não** era harness; ver a correção de teste pós-8.7 abaixo. Crash de processo e `PAUSED`/`DESTROYED` continuam classificados como harness.

### Escopo preservado (não alterado)

ViewModels; debounce; filtro; Navigation; Room / DAO / Repository; Home; Treinos; Histórico; Resumo; Stats; Execution; timer/rest; `WorkoutExecutionViewModel`; `TimeProvider`; CTA `IN_PROGRESS`; contratos 8.S / 8.0; itens B/C da auditoria 8.7.

---

## Correção de teste pós-8.7 — `clickingCard_opensEditDialog`

Não é fase nova nem backlog. Correção de matcher identificada na validação após a 8.7.

### Estado

**CONCLUÍDA** (`d74fcc1 test: fix exercise edit click target`).

Arquivo do commit:

* `app/src/androidTest/java/com/gymtrack/exercises/ExercisesScreenTest.kt`

Nenhum código de produção foi alterado (`ExercisesScreen`, `ExerciseItem`, ViewModel, Navigation, Room, Home, execução/timer).

### Problema

`ExercisesScreenTest.clickingCard_opensEditDialog` falhava com `edited == null` após `performClick()` no Card:

```text
onNodeWithTag("exercise_card_Supino reto").performClick()
```

Não era bug de produto: o `Card` continua com `onClick` → `onExerciseClick` → diálogo de edição. Não era harness (`PAUSED` / `DESTROYED` / crash).

### Causa

`performClick()` injeta o toque no **centro geométrico** do nó do Card. Nesse layout, o centro pode cair sobre o `TextButton` filho **Ver desempenho**, que intercepta o hit-test e dispara `onExerciseStatsClick`. `onExerciseClick` não executa; `edited` permanece `null`.

### Correção mínima

Somente o teste passou a clicar o texto do nome na árvore não mesclada:

```text
onNodeWithText("Supino reto", useUnmergedTree = true).performClick()
```

Asserções preservadas: `assertEquals(exercise, edited)`, `assertNull(statsName)`, `"Editar exercício"`. Testes de desempenho e exclusão **não** foram alterados. Sem `@Ignore`, `Thread.sleep` ou nova tag na UI.

### Testes

```text
ExercisesScreenTest#clickingCard_opensEditDialog   PASS (isolado)
ExercisesScreenTest                                 10/10 PASS
git diff --check                                    limpo
```

---

# Fase 9 — Hardening e testes finais

### Estado

**CONCLUÍDA — SEM IMPLEMENTAÇÃO / SEM ALTERAÇÃO NECESSÁRIA**

Auditoria somente leitura dos temas 9.1–9.6. Nenhum código, teste ou configuração foi alterado.

A Fase 9 não deve simplesmente adicionar testes indiscriminadamente. Grande parte da aplicação já possui testes unitários, ViewModel, Compose e navegação. O objetivo era identificar **lacunas reais**.

A cobertura existente foi considerada suficiente para o estado atual do produto:

* não foi encontrada lacuna real comprovada;
* não existe regra nova sem cobertura que justifique teste;
* o fluxo crítico possui cobertura **distribuída** entre testes existentes;
* ausência de um único teste E2E **não** é considerada problema;
* não foram adicionados testes indiscriminadamente.

### Bloco A

**Nenhum item recomendado.** Sem implementação: não havia alteração necessária.

### Bloco B (não aprovado / não implementado)

Não constituem backlog aprovado:

* teste instrumentado único do fluxo criar → iniciar → série → finalizar → histórico → dashboard → desempenho;
* casos adicionais de borda de domínio se alguma regra **nova** ficar sem cobertura.

### Testes

Nenhum teste foi alterado. A suíte existente foi auditada. Falhas de harness continuam separadas de problemas de produto. Sem `@Ignore`, `Thread.sleep` ou alteração de teardown.

### Fora de escopo (não reabrir)

8.S–8.7; `StartSessionResult`; `startSession`; CTA `IN_PROGRESS`; `observeInProgress`; `HomeViewModel`; `WorkoutExecutionViewModel`; `TimeProvider`; ticker; `maybeExpireRest`; timer/rest; Room/migrations; Navigation; harness (`PAUSED`, `DESTROYED`, crash, `restEnds_returnsToWorking`); Fases 10 e 11.

Checklist auditado (9.1–9.6), sem novos requisitos:

## 9.1 — Domain

Revisar:

* casos de borda;
* regras de cálculo;
* datas;
* timezone;
* empates;
* valores zero;
* listas vazias.

## 9.2 — DAO / Room

Adicionar testes onde houver lacunas reais:

* persistência;
* consultas;
* estados;
* relações;
* constraints.

Não criar testes duplicados sem valor.

## 9.3 — Repositories

Verificar:

* sucesso;
* erro;
* persistência;
* recuperação;
* comportamento dos Flows.

## 9.4 — ViewModels

Revisar:

* loading;
* conteúdo;
* empty;
* erro;
* reemissão dos Flows;
* eventos;
* estados de transição.

## 9.5 — Fluxos críticos

Testar principalmente:

```text
criar treino
→ iniciar
→ registrar série
→ finalizar
→ histórico
→ dashboard
→ desempenho
```

## 9.6 — Regressão

Garantir que alterações de polimento não quebrem:

* execução;
* timer;
* histórico;
* dashboard;
* estatísticas;
* navegação.

---

# Fase 10 — IA

Ainda **FUTURA** no sentido de produto: **não** implementar. Não inventar backlog.

Auditoria somente leitura: **ESCOPO INSUFICIENTEMENTE DEFINIDO**. Sem 10.1, sem critérios de aceitação. As linhas abaixo são *possíveis* funcionalidades, **não** requisitos aprovados. **Bloco A vazio.** Nenhuma implementação de IA.

Somente após:

* aplicativo offline estável;
* Fase 8 concluída;
* Fase 9 concluída;
* MVP validado.

Possíveis funcionalidades futuras (não aprovadas / não backlog):

* análise de desempenho;
* sugestões de treino;
* recomendações;
* análise de progressão.

Não implementar antes da estabilidade do aplicativo base.

### Estado

**AUDITADA — ESCOPO INSUFICIENTEMENTE DEFINIDO.** Sem implementação. Chatbot/LLM e provedores externos **não** estão definidos neste documento.

---

# Fase 11 — Backend

### Estado

**CONCLUÍDA — SEM IMPLEMENTAÇÃO / ESCOPO INSUFICIENTEMENTE DEFINIDO**

Auditoria somente leitura. Nenhum código, teste, Gradle, Manifest, Room ou Navigation foi alterado. **Bloco A vazio.** Nenhum teste novo.

A Fase 11 permanece **futura** e depende da estabilidade da versão **offline**. Não há 11.1 nem critérios de aceitação.

API, autenticação, sincronização, backup em nuvem, conta e multi-dispositivo aparecem só como **possibilidades**, **não** como requisitos e **não** constituem backlog aprovado.

O projeto continua deliberadamente offline (Room/SQLite local). Não há cliente HTTP, API, autenticação, sincronização nem conta em nuvem. `android:allowBackup="true"` **não** é a implementação de backup da Fase 11.

Não existe lacuna de produto implementável enquanto não houver definição do que o backend deve fazer e quais são os critérios de aceitação. *Não adicionar backend antecipadamente.*

Itens (não backlog):

* API — não implementada; escopo insuficiente.
* Autenticação — não implementada; escopo insuficiente.
* Sincronização / multi-dispositivo — não implementada; escopo insuficiente.
* Backup em nuvem / conta — não implementado; escopo insuficiente.
* Chatbot / LLM — **fora** da Fase 11; pertence à Fase 10 (também sem escopo suficiente).

### Proteções (intactas)

8.S / 8.0–8.7; Fase 9; Fase 10; timer/rest; Room/migrations; Navigation; ViewModels protegidos; harness (`PAUSED` / `DESTROYED` / crashes / `restEnds_returnsToWorking`); demais contratos do documento. Não reabrir.

Possíveis componentes (não requisitos):

* API;
* autenticação;
* sincronização;
* backup;
* conta do usuário;
* sincronização entre dispositivos.

---

# Auditoria exploratória do MVP (pós-Fase 11)

**Não é uma nova fase.** Não é Fase 12. Não constitui backlog. Somente leitura; **nenhum código, teste ou Gradle foi alterado.**

### Estado

**CONCLUÍDA — NENHUM PROBLEMA REAL ENCONTRADO.** Sem Bloco A. Sem implementação aprovada.

HEAD no momento da auditoria: `d74fcc1 test: fix exercise edit click target`. Produto mais recente: `7f33ed3 feat: allow adding first exercise from empty picker`.

### Fluxos revisados

Instalação limpa; Home empty; criar treino; detalhe; biblioteca de exercícios vazia; **Ir para exercícios**; criar exercício; voltar ao treino; selecionar exercício; configurar exercício; iniciar treino; executar série; descanso; finalizar; resumo; histórico; dashboard; desempenho/stats; CRUD de exercícios; CRUD de treinos; remoção de exercícios; conflito `IN_PROGRESS`; retomada de sessão; navegação Voltar e bottom bar.

### Resultado

**NENHUM PROBLEMA REAL ENCONTRADO.** Nenhum bug novo de produto após os fluxos completos.

O impasse da biblioteca vazia no primeiro treino **já estava corrigido**:

```text
7f33ed3 feat: allow adding first exercise from empty picker
```

A falha `ExercisesScreenTest.clickingCard_opensEditDialog` (`edited == null`) foi classificada como matcher/hit-test do teste (não produto, não harness) e corrigida em:

```text
d74fcc1 test: fix exercise edit click target
```

### Melhorias opcionais (B — não são bugs, não são backlog)

Identificadas e **não** transformadas em requisitos:

* acesso a Exercícios na Home empty;
* abrir automaticamente o detalhe após criar treino;
* auto-seleção após criar exercício;
* botão no histórico vazio;
* impedir repetição de exercício no treino;
* aviso sobre efeito da exclusão do exercício nos treinos;
* Settings ainda placeholder.

B ≠ bug. Sem Bloco A. Sem implementação aprovada. Sem nova fase.

### Proteções (intactas)

8.S–8.7; Fase 9; Fase 10; Fase 11; timer/rest; Navigation; harness (`PAUSED` / `DESTROYED` / crash / `restEnds_returnsToWorking`). Não reabrir essas fases. O schema **atual** é Room 8 (trabalho posterior a esta auditoria).

---

# Catálogo, status e ordem livre (`feature/next-step`)

Documentação técnica do estado **atual** do código (HEAD `15c03f8`, drafts `2f9f32d`). Substitui qualquer leitura antiga de “ordem rígida só pela `position`”, “campo extra de seleção” ou “migration 8→9”.

Não há `selectedExerciseId`. A seleção persistida é o `status` `IN_PROGRESS` em `workout_session_exercises`.

`resumeSessionExercise` **não** usa `database.withTransaction`. São duas escritas DAO em sequência (`clearInProgress` + `updateStatus`) em `Dispatchers.IO`.

Ao criar a sessão, os snapshots nascem com `status` default `PENDING`. O primeiro exercício apresentado **não** é gravado como `IN_PROGRESS` até o usuário usar “Fazer agora” / `resumeSessionExercise`. A fila automática usa `resolveCurrentExerciseIndex` sobre incompletos não `SKIPPED`.

---

## Catálogo vs snapshot da sessão

| Conceito | Tipo / tabela | Papel |
| --- | --- | --- |
| Catálogo | `Exercise` / `exercises` + `exercise_secondary_muscles` | Biblioteca editável |
| Item do treino (template) | `WorkoutExercise` / `workout_exercises` | Séries, faixa de reps, carga, descanso, `position` |
| Exercício da sessão | `WorkoutSessionExercise` / `workout_session_exercises` | Snapshot após `startSession`; `status`; séries em `workout_sets` |

`ExerciseRepositoryImpl.save`: insert se `id == 0`; senão `ExerciseDao.update` (não `INSERT OR REPLACE` da linha do exercício), depois `deleteByExerciseId` + `insertAll` dos secundários, em `database.withTransaction`. O `update` evita apagar a PK e disparar CASCADE em `workout_exercises`.

`startSession` copia para o snapshot: nome, `muscleGroup`, `equipmentType`, `plannedSets`, `minRepetitions`, `maxRepetitions`, `plannedWeight`, `restSeconds`, `notes`, `secondaryMuscles` via `serializeSecondaryMuscles`. Edição posterior do catálogo **não** altera sessões já criadas.

`ExerciseViewModel.saveExercise` exige nome, grupo e equipamento não vazios; aplica `sanitizedSecondaryMuscles`. A UI (`ExercisesScreen`) escolhe primário e equipamento em `MUSCLE_GROUPS` / `EQUIPMENT_TYPES` (valores fora do catálogo só entram na lista se já existirem na linha, para não perder dados migrados).

---

## Músculos secundários

* Entidade: `ExerciseSecondaryMuscleEntity` (`exerciseId`, `muscle`).
* Tabela: `exercise_secondary_muscles`, PK composta `(exerciseId, muscle)`, `FOREIGN KEY` para `exercises(id)` **`ON DELETE CASCADE`**, índice em `exerciseId`.
* DAO: `ExerciseSecondaryMuscleDao` (`getAll`, `getMuscles`, `deleteByExerciseId`, `insertAll` com `OnConflictStrategy.REPLACE`).
* Cardinalidade: **N:N** exercício ↔ músculo (um exercício, `0..N` músculos; o mesmo rótulo não repete por causa da PK).
* `sanitizedSecondaryMuscles(primary, secondaries)`: trim, descarta vazio, **remove o primário**, `distinct`, `sorted`.
* Snapshot da sessão: coluna TEXT `secondaryMuscles` (`serializeSecondaryMuscles` / `deserializeSecondaryMuscles`, CSV ordenado). Não é tabela N:N no snapshot.

`MIGRATION_6_7` **não** mapeia valores desconhecidos (ex. `Legs`) para o catálogo; só sinónimos listados no SQL.

---

## Faixa de repetições (meta visual)

Campos no template e no snapshot: `minRepetitions`, `maxRepetitions`.

`formatRepetitionTarget` (`RepetitionTarget.kt`):

* inválido (`< 1`, ou `max < min`) → `null` (a UI omite a linha);
* `min == max` → string do valor único (ex. `"8"`);
* caso contrário → `"8–12"` (en-dash).

Na execução, `WorkoutExecutionScreen` mostra `execution_reps_target` só se o formatter não for `null`.

`WorkoutExecutionViewModel.completeCurrentSet` / `WorkoutSessionRepositoryImpl.completeSet` **não** validam a faixa. Reps persistidas: inteiro **≥ 1**. Carga ≥ 0.

---

## Status do exercício da sessão

Enum `WorkoutSessionExerciseStatus` / constantes em `WorkoutSessionExerciseEntity`. Isto **não** é o `status` da sessão (`WorkoutSessionStatus`).

| Status | Quando |
| --- | --- |
| `PENDING` | Default na criação e após `clearInProgress`; incompleto e não escolhido agora |
| `IN_PROGRESS` | Escolhido via `resumeSessionExercise` (“Fazer agora”). Não há unique index de exercício `IN_PROGRESS` |
| `COMPLETED` | `syncExerciseCompletionStatus` quando `sets.size >= plannedSets` (`isSessionExerciseComplete`) |
| `SKIPPED` | `skipSessionExercise` se ainda incompleto. **Não** cria nem apaga `workout_sets` |

`parseWorkoutSessionExerciseStatus`: valor desconhecido → `PENDING`.

`finishSession` atualiza só a **sessão** (`COMPLETED` + `endedAtMillis`). **Não** reescreve status dos exercícios.

---

## `resolveCurrentExerciseIndex`

Lista ordenada por `position` (DAO `ORDER BY position ASC`).

1. Primeiro exercício com `status == IN_PROGRESS` **e** ainda incompleto;
2. senão, primeiro incompleto com `status != SKIPPED`;
3. senão **-1**. A UI trata `currentExercise == null` (`PendingExercisesPanel` se `hasIncompleteExercises`). **Não crasha.**

Completar o exercício escolhido fora de ordem faz o passo 1 falhar e o passo 2 voltar ao primeiro incompleto não pulado (menor `position` incompleto não `SKIPPED`).

---

## Ordem livre — Pular ≠ Fazer agora

A `position` **continua** a ordenar a lista. A ordem de **execução** pode divergir.

**Pular** (`requestSkip` / `confirmSkip` → `skipSessionExercise`): marca `SKIPPED`. Não chama `resumeSessionExercise`. O exercício não regressa sozinho à fila.

**Fazer agora** (lista → `resumeExercise` → `resumeSessionExercise`):

* **não** chama `skipSessionExercise`;
* `resumeExercise` recusa se `phase != WORKING`, se o alvo não existir, se `completedSets >= plannedSets`, ou se já for o `currentExercise`;
* `resumeSessionExercise` recusa se já estiver completo; depois `clearInProgress(sessionId)` (todo `IN_PROGRESS` da sessão → `PENDING`) e `updateStatus(alvo, IN_PROGRESS)`;
* séries do exercício anterior **mantêm-se**; no ViewModel `currentSetIndex = sets.size` do exercício atual;
* só incompletos são selecionáveis (`SessionExerciseRow.canSelectNow`: `!isComplete && !isCurrent`); COMPLETED **não** tem ação de seleção.

Cadeia A→C→B→A: só troca IN_PROGRESS/PENDING; não cria séries nem SKIPPED.

---

## Fluxo da UI de execução (`WORKING`)

```text
WORKING
  → exercício atual (resolveCurrentExerciseIndex)
  → botão Exercícios (ActiveSetContent)
  → lista de todos os da sessão (SessionExercisesList / sessionExerciseRows)
  → rótulos: Pendente / Em andamento / Pulado / Concluído (SessionExerciseListStatus)
  → “Fazer agora” (`do_exercise_now`) nos incompletos que não são o atual
```

Em `RESTING` a lista/seleção **não** está disponível (`showSessionExercises && phase == WORKING`). `resumeExercise` rejeita fora de `WORKING`.

Se não há current (só COMPLETED/SKIPPED, ou todos skipped): `PendingExercisesPanel` com a mesma lista. Finalizar permanece disponível.

---

## Drafts (`WorkoutExecutionDraftStore`)

* Escopo: `@ActivityRetainedScoped` + `ConcurrentHashMap` em memória.
* Chave: `DraftKey(sessionId, exerciseId)` — o segundo Long é **`WorkoutSessionExercise.id`** (linha da sessão), **não** o `exerciseId` do catálogo. O parâmetro do store chama-se `exerciseId`, mas o ViewModel passa `currentExercise.id`.
* Trocar de exercício **não** faz `clear` do draft anterior; ao voltar, `applyPersistedProgress` faz `get(sessionId, currentExercise.id)` quando o current muda em `WORKING`.
* `clear(sessionId, exercise.id)`: após `completeCurrentSet` bem-sucedido.
* `clearSession(sessionId)`: após `confirmFinish` bem-sucedido.
* Skip **não** limpa o draft desse exercício.
* **Não** há persistência Room/DataStore: process death perde drafts. Séries e `status` vêm do Room.

---

## Finalização e resumo

`hasIncompleteExercises`: existe exercício com `sets.size < plannedSets` (PENDING e SKIPPED incompletos **contam**). `requestFinish` usa isso em `finishHasPendingExercises`.

`finishSession` não altera status dos exercícios.

Resumo (`WorkoutSessionSummaryUiState.exerciseSummaries` / `WorkoutSessionSummaryScreen`):

* **Concluído** se `sets.size >= plannedSets` (`isComplete`);
* **Pulado** se `status == SKIPPED` **e** ainda incompleto (`isSkipped`);
* **Pendente** nos restantes incompletos.

---

## Room — versões 7 e 8

| Migration | Efeito |
| --- | --- |
| **6→7** | Cria `exercise_secondary_muscles` (CASCADE); adiciona `secondaryMuscles TEXT NOT NULL DEFAULT ''` no snapshot; normaliza sinónimos conhecidos de `muscleGroup` / `equipmentType` em `exercises`. Não mexe em `workout_sets`. Não mapeia Legs. |
| **7→8** | `ALTER TABLE workout_session_exercises ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'`. Não altera `workout_sets`. Não adiciona coluna de seleção. |

Não existe migration **8→9**.

Há `README.md` na raiz (portfólio GitHub). Este arquivo continua a ser a documentação técnica interna.

---

## Testes (cobertura real desta funcionalidade)

Não se inventa um total da suíte. Classes e cenários relevantes:

**Domínio (JVM):** `WorkoutSessionExerciseStatusTest` (IN_PROGRESS tem prioridade; fila ignora COMPLETED/SKIPPED; `-1` só COMPLETED/SKIPPED; unknown → PENDING); `RepetitionTargetTest`; `ExerciseCatalogTest`.

**ViewModel (JVM):** `WorkoutExecutionViewModelTest` — seleção de PENDING posterior; cadeia A→C→B→A sem skip/sets; preservação de séries parciais e de draft; COMPLETED sem ação; SKIPPED → IN_PROGRESS → COMPLETED; skip do current; finish com pendentes; `confirmFinish` limpa drafts da sessão; faixa de reps aceite fora do intervalo. `WorkoutSessionSummaryViewModelTest.mixedPendingCompletedSkipped_mapToDistinctSummaryFlags`. `ExerciseViewModelTest` (secundários).

**Repository / Room (instrumentado):** `WorkoutSessionRepositoryImplTest` — skip sem criar série; resume a partir de PENDING sem skip dos outros; persistência de IN_PROGRESS em nova instância do repository; `finishSession_doesNotRewriteMixedExerciseStatuses`; snapshot de secundários. `ExerciseRepositoryImplTest`. `ExerciseMigrationTest` (`migration6To7_*`, Legs preservado). `WorkoutSessionMigrationTest` (`migration7To8_*`, default PENDING).

**UI Compose (instrumentado):** `WorkoutExecutionScreenTest` — seleção de PENDING posterior + draft + COMPLETED sem “Fazer agora”; `openingScreen_usesPersistedInProgressFromRoom`; skip do último / de todos sem crash; meta `8–12` / valor único; completeSet fora da faixa. `ExercisesScreenTest` (secundários no diálogo).

---

# 8. Estado funcional atual

O aplicativo atualmente possui:

```text
Exercícios (catálogo)
├── listar / pesquisar
├── criar / editar / excluir
├── músculo primário + secundários 0..N
├── equipamento (inclui Outro)
└── ver desempenho

Treinos
├── criar
├── editar
├── excluir
├── configurar exercícios
├── configurar séries
├── configurar repetições
├── configurar descanso
└── ordenar

Execução
├── iniciar treino (snapshot; exercícios PENDING)
├── registrar carga / reps (faixa só visual)
├── concluir séries
├── skip (SKIPPED) ≠ Fazer agora (IN_PROGRESS)
├── lista da sessão / ordem livre
├── drafts em memória por WorkoutSessionExercise.id
├── progresso / timer de descanso
└── finalizar treino + resumo (Concluído / Pulado / Pendente)

Histórico
├── sessões COMPLETED
├── exclusão com diálogo (só COMPLETED)
├── exercícios
├── séries
├── cargas
├── repetições
├── volume
└── resumo da sessão

Dashboard
├── treino recente
├── Semana
├── Mês
├── Tudo
├── frequência
├── tendência 7 dias
├── PRs all-time
└── acesso a Exercícios

Desempenho
├── PR all-time
├── histórico por exercício
├── evolução por sessão
└── acesso pelo Home ou Exercícios
```

---

## Exclusão de sessões do histórico

Implementado na branch (`2083881`). **Não** altera a versão do Room (continua **8**) e **não** cria migration.

* `HistoryScreen`: ícone de lixeira por item (`history_delete_{sessionId}`); o toque no card continua a abrir o resumo.
* Diálogo (`delete_history_session` / `delete_history_session_message`): **Cancelar** chama `dismissDeleteConfirmation()` e **não** apaga; **Excluir** chama `HistoryViewModel.confirmDelete()`.
* `confirmDelete()` usa `sessionToDelete` e chama `WorkoutSessionRepository.deleteCompletedSession(sessionId)`.
* `deleteCompletedSession`: lê a sessão; se não existir ou `status != COMPLETED`, retorna sem escrever. Só então `WorkoutSessionDao.deleteById`.
* Sessão `IN_PROGRESS` permanece intacta.
* `workout_session_exercises` (`ON DELETE CASCADE` da sessão) e `workout_sets` (`ON DELETE CASCADE` do exercício da sessão) são removidos pelo schema **já existente**.
* Catálogo (`exercises`), músculos secundários, template do treino (`workouts` / `workout_exercises`) e outras sessões não são apagados.
* A lista observa `observeCompletedSessions()`; após o DELETE o `Flow` reemite (incluindo empty).

Testes (sem inventar totais):

* JVM: `HistoryViewModelTest` — `confirmDelete_removesOnlySelectedSession`, `dismissDeleteConfirmation_doesNotDelete`, `confirmDelete_lastSession_leavesEmptyList`, `confirmDelete_withoutSelection_doesNothing`.
* UI: `HistoryScreenTest` — `list_showsDeleteAction`, `deleteDialog_cancelKeepsItem_confirmRemovesSession`.
* Repository/Room: `WorkoutSessionRepositoryImplTest` — `deleteCompletedSession_removesOnlyThatSessionAndDependents` (CASCADE, outra sessão e catálogo intactos), `deleteCompletedSession_doesNotDeleteInProgressSession`.

---

# 9. Regras de dados importantes

## Sessões

Métricas e histórico consideram somente sessões:

```text
COMPLETED
```

Sessão `IN_PROGRESS` não deve aparecer em estatísticas históricas.

A exclusão pelo histórico (`deleteCompletedSession`) aplica-se **somente** a sessões `COMPLETED`. Uma sessão `IN_PROGRESS` **não** é removida por essa API.

Isto é o `WorkoutSession.status`. O `IN_PROGRESS` de **`WorkoutSessionExercise`** é só o exercício escolhido na execução e **não** entra sozinho nas métricas de histórico.

## Exercícios da sessão vs catálogo

A identidade histórica dos sets é:

```text
exerciseName
```

Não introduzir `exerciseId` retroativamente sem decisão arquitetural explícita.

## Datas

### Histórico

Mantém sua regra existente.

### Dashboard / estatísticas

Métricas de período utilizam timezone local explícito:

```text
ZoneId.systemDefault()
```

## PRs

PRs continuam sendo derivados.

Não persistir PR sem necessidade.

## Peso

Peso `0` é válido.

Não tratar `0` como ausência de valor.

---

# 10. Regras de performance

* Evitar N+1.
* Reutilizar Flows existentes.
* Não carregar dados no Composable.
* Não criar query apenas para evitar cálculos pequenos em memória.
* Não adicionar índices especulativos.
* Não paginar sem necessidade real.
* Não criar repositories intermediários apenas por organização.
* Se o volume de dados crescer significativamente, reavaliar as consultas com base em evidência.

---

# 11. Regras de testes

Todos os testes devem:

* evitar `Thread.sleep`;
* evitar `@Ignore` para esconder flakiness;
* preferir `waitUntil` nos testes Compose;
* testar comportamento, não implementação;
* cobrir casos de borda relevantes.

Um teste flaky não deve ser simplesmente ignorado.

Quando um flake conhecido impedir a execução da suíte completa, registrar explicitamente o fato e não inventar resultados.

Testes acrescentados em `15c03f8` / ordem livre: ver a seção **Catálogo, status e ordem livre** (não inventar totais da suíte).

Exclusão do histórico: `HistoryViewModelTest` (confirmação, cancelamento, última sessão), `HistoryScreenTest` (diálogo cancelar/confirmar), `WorkoutSessionRepositoryImplTest` (CASCADE e proteção de `IN_PROGRESS`).

---

# 12. Regras de Git

Cada fase deve ser isolada.

Antes de commit:

```text
git status
git diff
git diff --cached
```

Verificar:

* arquivos esperados;
* arquivos acidentais;
* testes;
* alterações fora de escopo.

Não misturar fases sem decisão explícita.

### Estado atual

```text
Branch: feature/next-step

HEAD:
2083881 feat: allow deleting completed sessions from history

Working tree: documentação (`README.md` / `PROJECT_CONTEXT.md`) pode estar à frente do último commit de código.

Últimos commits relevantes:
2083881 — exclusão de sessões COMPLETED no histórico (CASCADE existente; sem migration)
15c03f8 — catálogo secundários, Room 7–8, status, ordem livre, faixa visual, testes
2f9f32d — drafts da execução (chave sessão + id da linha da sessão)
af1139d — horários locais no resumo
b6fad76 — voltar Home a partir da execução
728688a — acesso a Exercícios a partir do picker
d74fcc1 — correção de teste pós-8.7
7f33ed3 — picker vazio → Exercícios

8.S–8.7 e Fases 9–11: histórico das fases (já auditadas).
Pós-MVP nesta branch: catálogo fechado + secundários, status de exercício, skip, ordem livre, exclusão no histórico.
Não documentar lacunas (ex. ausência de transação em resumeSessionExercise) como features feitas.
```

Não reabrir 7.1–7.3 nem 8.S.1–8.S.4 sem necessidade.

---

# 13. Ordem obrigatória das próximas etapas

```text
Fase 7
   ↓
7.1 + 7.2 + 7.3
   ↓
CONCLUÍDA
   ↓
8.S — Estabilização da execução     CONCLUÍDA (8.S.1–8.S.4)
   ↓
8.0 — Estabilização pré-polimento   CONCLUÍDA
   ↓
8.1 — Auditoria UX                  CONCLUÍDA
   ↓
8.2 — Feedback visual               CONCLUÍDA, SEM IMPLEMENTAÇÃO
   ↓
8.3 — Acessibilidade                CONCLUÍDA
   ↓
8.4 — Animações                     CONCLUÍDA, SEM IMPLEMENTAÇÃO
   ↓
8.5 — Performance                   CONCLUÍDA, SEM IMPLEMENTAÇÃO
   ↓
8.6 — Tratamento de erros           CONCLUÍDA, SEM IMPLEMENTAÇÃO
   ↓
8.7 — Auditoria final UX            CONCLUÍDA — Bloco A implementado
   (pós-8.7: correção de teste clickingCard_opensEditDialog — d74fcc1)
   ↓
Fase 9 — Hardening e testes finais  CONCLUÍDA, SEM IMPLEMENTAÇÃO
   ↓
MVP offline estável
   ↓
Fase 10 — IA                        AUDITADA — ESCOPO INSUFICIENTEMENTE DEFINIDO
   ↓
Fase 11 — Backend                   AUDITADA — ESCOPO INSUFICIENTEMENTE DEFINIDO
   (pós-Fase 11: auditoria exploratória do MVP — CONCLUÍDA; não é fase nova;
    nenhum problema real; sem Bloco A)
   ↓
feature/next-step (implementado; não é Fase 12 numerada):
   catálogo / secundários / Room 7–8 / status / skip / ordem livre / drafts /
   exclusão de sessões COMPLETED no histórico
```

Não avançar para uma etapa posterior sem concluir e auditar a anterior.

Fases 10 e 11 não são backlog de implementação. A auditoria exploratória pós-Fase 11 **não** abre uma Fase 12. O trabalho de `15c03f8` está na seção **Catálogo, status e ordem livre**.

---

# 14. Regra fundamental

Cada etapa deve ser implementada e testada antes da próxima.

Não implementar o aplicativo inteiro de uma vez.

Não adicionar tecnologias que não estejam definidas neste documento.

Não antecipar funcionalidades futuras.

Não corrigir uma fase anterior incidentalmente durante outra fase sem registrar e aprovar a alteração.

Quando uma decisão arquitetural for necessária:

1. auditar o código existente;
2. identificar o problema;
3. explicar o motivo da alteração;
4. definir o menor escopo possível;
5. implementar;
6. testar;
7. auditar o diff;
8. somente então fazer commit.

---

# 15. Estado do roadmap

```text
Fase 1  — Fundação              CONCLUÍDA
Fase 2  — Exercícios            CONCLUÍDA
Fase 3  — Treinos               CONCLUÍDA
Fase 4  — Execução              IMPLEMENTADA / 8.S + ordem livre/status (15c03f8)
Fase 5  — Timer                 IMPLEMENTADA / integrada à execução
Fase 6  — Histórico/Dashboard   CONCLUÍDA
Fase 7  — Estatísticas          CONCLUÍDA
Fase 8  — Polimento             8.S–8.7 concluídas
Fase 9  — Hardening/Testes      CONCLUÍDA, SEM IMPLEMENTAÇÃO (suíte alargada depois em 15c03f8)
Fase 10 — IA                    AUDITADA — ESCOPO INSUFICIENTEMENTE DEFINIDO
Fase 11 — Backend               AUDITADA — ESCOPO INSUFICIENTEMENTE DEFINIDO
Room    — versão 8 (migrations até 7→8; sem 8→9)
```

Auditoria exploratória do MVP (pós-Fase 11): **CONCLUÍDA** — nenhum problema real; não é fase nova; sem Bloco A.

Trabalho posterior na mesma branch: catálogo de músculos/equipamentos, secundários N:N, status de exercício, skip, “Fazer agora”, drafts, exclusão de sessões concluídas no histórico. `README.md` é portfólio público (ficheiro separado).

Fases 10 e 11 não são backlog de implementação.
