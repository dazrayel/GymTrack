# GymTrack

## 1. Visão

GymTrack é um aplicativo Android nativo para gerenciamento,
execução e acompanhamento de treinos de musculação.

O objetivo é criar um aplicativo simples, rápido e agradável
para ser utilizado durante o treino.

O aplicativo será inicialmente offline-first.

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

Presentation
↓
Domain
↓
Data

A camada Presentation não deve acessar diretamente o Room.

A camada Domain não deve depender de Android Framework quando isso
não for necessário.

A camada Data é responsável por persistência e implementação dos
repositories.

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

---

## 6. Estado atual

O projeto está em fase inicial.

Ainda não existe backend.

Ainda não existe autenticação.

Ainda não existe IA.

Ainda não existe sincronização em nuvem.

---

## 7. Roadmap

### Fase 1 — Fundação

* projeto Android;
* configuração Gradle;
* Compose;
* Material 3;
* Hilt;
* Room;
* Navigation.

### Fase 2 — Exercícios

* banco de exercícios;
* listagem;
* pesquisa;
* filtros;
* detalhes.

### Fase 3 — Treinos

* criação;
* edição;
* exclusão;
* exercícios;
* séries;
* repetições;
* descanso;
* ordenação.

### Fase 4 — Execução

* iniciar treino;
* registrar séries;
* cargas;
* repetições;
* progresso;
* finalizar treino.

### Fase 5 — Timer

* descanso;
* pausa;
* continuar;
* adicionar tempo;
* pular.

### Fase 6 — Histórico

* sessões;
* exercícios;
* séries;
* cargas;
* repetições;
* volume.

### Fase 7 — Estatísticas

* volume;
* frequência;
* PRs;
* evolução.

### Fase 8 — Polimento

* UX;
* animações;
* feedback visual;
* acessibilidade;
* performance;
* tratamento de erros.

### Fase 9 — Testes

* unitários;
* repositories;
* DAO;
* ViewModels;
* UI.

### Fase 10 — IA

Somente após o aplicativo base estar estável.

### Fase 11 — Backend

Somente após a versão offline estar estável.

---

## 8. Regra fundamental

Cada etapa deve ser implementada e testada antes da próxima.

Não implementar o aplicativo inteiro de uma vez.

Não adicionar tecnologias que não estejam definidas neste documento.

Antes de alterar a arquitetura, analisar o código existente e explicar
o motivo da alteração.
