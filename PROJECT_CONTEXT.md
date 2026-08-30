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

* Room versão **5**.
* SQLite.
* Nenhuma migration pendente.
* Não existe tabela específica para estatísticas.
* Não existe tabela específica para PRs.
* Métricas são derivadas dos dados existentes.

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
7424e70 feat: add exercise performance navigation
3ac7d38 fix: add exercise performance strings
1ead7de feat: implement exercise performance history
e5094be feat: implement dashboard statistics
9dda2ce feat: implement workout history
8dc5089 feat: implement workout execution flow
```

Após a implementação da 7.3, o working tree foi limpo e a Fase 7 foi consolidada.

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

O modelo possui:

```text
Exercise(
    id,
    name,
    muscleGroup,
    equipmentType
)
```

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

O fluxo de execução está integrado ao aplicativo.

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

Auditoria UX realizada. **Escopo aprovado; implementação ainda não iniciada.**

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

Melhorar:

* Snackbar;
* confirmação de ações;
* sucesso;
* erro;
* loading;
* feedback de operações.

Não criar feedback redundante.

---

## 8.3 — Acessibilidade

Avaliar:

* `contentDescription`;
* semântica Compose;
* tamanho mínimo de toque;
* contraste;
* ordem de navegação;
* textos;
* componentes interativos.

Não alterar comportamento funcional sem necessidade.

---

## 8.4 — Animações

Adicionar animações somente onde agregarem valor.

Possíveis áreas:

* mudanças de estado;
* entrada/saída de elementos;
* transições;
* timer;
* listas.

Não adicionar animações indiscriminadamente.

---

## 8.5 — Performance

Auditar:

* recomposições desnecessárias;
* coleta de Flows;
* listas;
* consultas;
* N+1;
* operações no Main Thread;
* uso de memória.

Não criar otimizações especulativas.

---

## 8.6 — Tratamento de erros

Revisar:

* repositories;
* ViewModels;
* execução;
* histórico;
* dashboard;
* estatísticas;
* operações de persistência.

Erros técnicos devem ser convertidos em estados compreensíveis para o usuário quando apropriado.

---

## 8.7 — Auditoria final de UX

Executar o fluxo completo:

```text
Exercícios
    ↓
Treinos
    ↓
Execução
    ↓
Timer
    ↓
Finalização
    ↓
Histórico
    ↓
Dashboard
    ↓
Desempenho do exercício
```

### Estado

**NÃO INICIADA.**

---

# Fase 9 — Hardening e testes finais

A Fase 9 não deve simplesmente adicionar testes indiscriminadamente.

Grande parte da aplicação já possui testes unitários, ViewModel, Compose e navegação.

O objetivo agora será identificar lacunas.

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

### Estado

**NÃO INICIADA.**

---

# Fase 10 — IA

Somente após:

* aplicativo offline estável;
* Fase 8 concluída;
* Fase 9 concluída;
* MVP validado.

Possíveis funcionalidades futuras:

* análise de desempenho;
* sugestões de treino;
* recomendações;
* análise de progressão.

Não implementar antes da estabilidade do aplicativo base.

### Estado

**FUTURA.**

---

# Fase 11 — Backend

Somente após a versão offline estar estável.

Possíveis componentes:

* API;
* autenticação;
* sincronização;
* backup;
* conta do usuário;
* sincronização entre dispositivos.

Não adicionar backend antecipadamente.

### Estado

**FUTURA.**

---

# 8. Estado funcional atual

O aplicativo atualmente possui:

```text
Exercícios
├── listar
├── pesquisar
├── criar
├── editar
├── excluir
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
├── iniciar treino
├── registrar carga
├── registrar repetições
├── concluir séries
├── progresso
├── timer
└── finalizar treino

Histórico
├── sessões COMPLETED
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

# 9. Regras de dados importantes

## Sessões

Métricas e histórico consideram somente:

```text
COMPLETED
```

`IN_PROGRESS` não deve aparecer em estatísticas históricas.

## Exercícios

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
16d7533 feat: resume in-progress workout from Home

Working tree:
limpo

Últimos commits relevantes:
16d7533 — 8.S.4
1deb038 — 8.S.3
1c4880f — 8.S.2
6fdedb8 — 8.S.1

8.S.1–8.S.4: concluídas.
8.0: concluída.
8.1: escopo aprovado, ainda sem implementação.
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
8.1 — Auditoria UX                  escopo aprovado; implementação ainda não iniciada
   ↓
8.2 — Feedback visual               não iniciada
   ↓
8.3 — Acessibilidade                não iniciada
   ↓
8.4 — Animações                     não iniciada
   ↓
8.5 — Performance                   não iniciada
   ↓
8.6 — Tratamento de erros           não iniciada
   ↓
8.7 — Auditoria final UX            não iniciada
   ↓
Fase 9 — Hardening e testes finais
   ↓
MVP offline estável
   ↓
Fase 10 — IA
   ↓
Fase 11 — Backend
```

Não avançar para uma etapa posterior sem concluir e auditar a anterior.

**Próxima etapa: implementar o escopo aprovado da 8.1.**

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
Fase 4  — Execução              IMPLEMENTADA / 8.S concluída
Fase 5  — Timer                 IMPLEMENTADA / integrada à execução
Fase 6  — Histórico/Dashboard   CONCLUÍDA
Fase 7  — Estatísticas          CONCLUÍDA
Fase 8  — Polimento             8.0 concluída; 8.1 escopo aprovado (não iniciada)
Fase 9  — Hardening/Testes      FUTURA
Fase 10 — IA                    FUTURA
Fase 11 — Backend               FUTURA
```

**Próxima etapa: implementar o escopo aprovado da 8.1.**
