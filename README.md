# HojeTemBola

App mobile para **gestao de eventos desportivos**, focada em torneios de futebol (Fut5, Fut7, Fut11 e formato personalizado).

O objetivo da app e simples: organizar torneios de ponta a ponta, acompanhar jogos em direto, manter rankings atualizados e permitir votacao MVP de forma clara para todos os participantes.

## O que a app resolve

- Centraliza criacao e gestao de torneios num unico sistema.
- Facilita a inscricao de equipas e o controlo de plantel por torneio.
- Permite registo de eventos de jogo em tempo real (golos, cartoes, substituicoes).
- Mostra classificacoes, estatisticas e evolucao de desempenho.
- Ativa participacao da comunidade com votacao MVP e notificacoes automaticas.

## Perfis de utilizador

- **Administrador**: gere contas e supervisao global da plataforma.
- **Organizador**: cria torneios, define regras e gere jogos/resultados.
- **Capitao**: cria/gera equipa, convida jogadores e inscreve em torneios.
- **Jogador**: acompanha jogos, estatisticas e participa na votacao MVP.
- **Espectador**: segue torneios publicos e participa na votacao quando permitido.

## Como o sistema funciona

### 1) Entrada e autenticacao
- Onboarding inicial para apresentar as funcionalidades principais.
- Login por email/password e opcao Google.
- Registo com escolha de perfil para adaptar permissoes e experiencia.

### 2) Gestao de equipas
- Um capitao cria equipa com identidade visual (nome/escudo/cor).
- Convites de jogadores sao enviados por link com validade limitada.
- Equipa pode participar em varios torneios, com selecao de jogadores por prova quando necessario.

### 3) Criacao e configuracao de torneios
- Organizador define nome, formato, modalidade, datas, local e limites de participantes.
- Regras de desempate e disciplina (ex.: limite de amarelos para suspensao) sao configuraveis.
- Torneio pode ser publico ou privado, com controlo de acesso por convite.

### 4) Ciclo de vida do torneio
- Estados de funcionamento: criado, inscricoes abertas, inscricoes fechadas, a decorrer e terminado.
- O sistema fecha inscricoes quando vagas esgotam e progride o estado conforme os jogos avancam.
- Resultado final alimenta historico das equipas e destaque de desempenho.

### 5) Jogos ao vivo
- Ecran live com marcador, minuto, local e timeline de eventos.
- Organizador regista eventos em tempo real:
  - golos;
  - cartoes amarelos/vermelhos;
  - substituicoes.
- O sistema atualiza placar e impacto competitivo quase em tempo real.

### 6) Disciplina e automatismos
- Acumulacao de amarelos e vermelho direto podem gerar suspensao automatica.
- Notificacoes disciplinares sao enviadas aos utilizadores impactados.
- Regras respeitam configuracao definida pelo organizador para cada torneio.

### 7) Rankings e estatisticas
- Rankings por golos e jogos disputados.
- Classificacao por equipa com pontos, vitorias, empates e derrotas.
- Filtros por periodo (jornada, mes, epoca) para leitura rapida da evolucao.

### 8) Votacao MVP
- Voto por jogo, com possibilidade de alteracao enquanto a votacao estiver aberta.
- Participacao por segmentos (jogadores, publico e organizador).
- Resultados apresentados em percentagem para maior clareza.
- MVP final revelado no fecho da votacao.

### 9) Notificacoes automaticas
- Convites, alteracoes de calendario, resultados, suspensoes e atualizacoes relevantes.
- Objetivo: manter todos os perfis sincronizados sem depender de consulta manual constante.

## Experiencia da app

- Interface pensada para mobile, com navegacao por tabs e acoes rapidas.
- Priorizacao de dados chave: estado de torneio, proximo jogo, resultados e alertas.
- Suporte a modo portrait e ecras especificos em landscape para leitura mais confortavel de live/rankings.

## Fluxo principal (resumo)

1. Utilizador cria conta e entra na app.
2. Capitao cria equipa e convida jogadores.
3. Organizador cria torneio e abre inscricoes.
4. Equipas inscrevem-se e calendario e definido.
5. Jogos decorrem com registo live de eventos.
6. Sistema atualiza classificacoes, rankings e notificacoes.
7. Comunidade vota MVP e o torneio fecha com historico consolidado.

## Visao do produto

A HojeTemBola combina **organizacao**, **acompanhamento em direto** e **engagement da comunidade** numa unica experiencia.  
Foi desenhada para reduzir trabalho manual dos organizadores e, ao mesmo tempo, dar mais transparencia e envolvimento a equipas, jogadores e adeptos.

