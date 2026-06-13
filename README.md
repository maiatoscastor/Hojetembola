# HojeTemBola

Aplicação Android para **gestão de eventos desportivos**, focada em torneios de futebol (Fut5, Fut7, Fut11 e formato personalizado).

O objetivo é simples: organizar torneios de ponta a ponta, acompanhar jogos em direto, manter rankings atualizados e permitir votação MVP de forma clara para todos os participantes.

---

## Funcionalidades principais

- Criação e gestão de torneios nos formatos **liga**, **eliminatórias** e **grupos + eliminatórias**
- Inscrição de equipas com controlo de plantel por torneio
- Registo de eventos de jogo em tempo real (golos, cartões, substituições)
- Classificações automáticas, rankings e estatísticas individuais e coletivas
- Sistema de suspensões automáticas por acumulação de cartões
- Votação MVP por jogo
- **Modo offline** — regista eventos sem internet, sincroniza automaticamente ao restaurar ligação
- Interface disponível em **Português** e **Inglês**, com suporte a portrait e landscape

---

## Perfis de utilizador

| Perfil | Permissões principais |
|---|---|
| **Organizador** | Cria torneios, define regras, gere jogos e resultados |
| **Capitão** | Cria e gere equipa, convida jogadores, inscreve em torneios |
| **Jogador** | Acompanha jogos, estatísticas e participa na votação MVP |

---

## Como funciona

### 1. Autenticação
- Onboarding inicial com apresentação das funcionalidades
- Registo e login por email/password
- Perfil adaptado às permissões de cada utilizador

### 2. Gestão de equipas
- Capitão cria equipa com nome, iniciais e cor
- Jogadores são convidados por email
- Equipa pode participar em vários torneios simultaneamente

### 3. Torneios
- Organizador define nome, formato, modalidade, datas, local e limites de participantes
- Regras de desempate e limite de amarelos para suspensão configuráveis por torneio
- Torneio pode ser público ou privado (acesso por código de 4 dígitos)
- Estados: criado → inscrições abertas → inscrições fechadas → a decorrer → terminado

### 4. Jogos ao vivo
- Ecrã live com marcador, minuto, local e timeline de eventos
- Registo de golos, cartões amarelos/vermelhos e substituições
- Vista de titulares com campo visual interativo
- Placar e classificação atualizados automaticamente

### 5. Disciplina e automatismos
- Acumulação de amarelos gera suspensão automática para o jogo seguinte
- Vermelho direto resulta em suspensão imediata
- Regras configuradas pelo organizador no momento da criação do torneio

### 6. Rankings e estatísticas
- Rankings globais de golos, assistências, cartões e MVP
- Classificação por equipa com pontos, vitórias, empates, derrotas e diferença de golos
- Histórico de resultados por torneio e por equipa

### 7. Modo offline
- Eventos registados sem internet são guardados localmente
- SyncWorker sincroniza automaticamente com o Supabase ao restaurar a ligação
- Sincronização imediata assim que a rede é detetada

---

## Stack tecnológico

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| Base de dados local | Room (SQLite) |
| Backend / BD remota | Supabase (PostgreSQL + Auth + RLS) |
| Injeção de dependências | Hilt (Dagger) |
| Sincronização offline | WorkManager |
| Notificações push | Firebase Cloud Messaging (FCM) |
| Navegação | Navigation Component |
| UI | Material Design 3 |

---

## Fluxo principal

1. Utilizador cria conta e entra na app
2. Capitão cria equipa e convida jogadores
3. Organizador cria torneio e abre inscrições
4. Equipas inscrevem-se e o calendário é definido
5. Jogos decorrem com registo live de eventos
6. Sistema atualiza classificações e rankings automaticamente
7. Comunidade vota MVP e o torneio fecha com histórico consolidado

---

## Requisitos

- Android 8.0 (API 26) ou superior
- Ligação à internet para sincronização (funciona offline com sincronização posterior)
