# HojeTemBola — Ficheiro de Contexto Completo
# Projeto de Computação Móvel — Engenharia Informática 2025/2026 — IPVC
# Última atualização: Junho 2026

---

## 1. IDENTIDADE DO PROJETO

| Campo | Valor |
|---|---|
| Nome da app | HojeTemBola |
| Subtítulo | Gestão de Eventos Desportivos |
| Tema UC | Tema 2 — Gestão de Eventos Desportivos |
| UC | Computação Móvel — Engenharia Informática 2025/2026 |
| Instituição | IPVC — Instituto Politécnico de Viana do Castelo |
| Plataforma | Android (Kotlin + Android Studio) |
| Package name | com.hojetembola.app |
| Minimum SDK | API 26 (Android 8.0) |
| Build config | Kotlin DSL (build.gradle.kts) |

### Grupo
| Nome | Número |
|---|---|
| Gonçalo Teixeira | 31396 |
| Diogo Sá | 31378 |
| Afonso Araújo | 31416 |
| Diogo Monteiro | 32428 |

### Repositório e Serviços
- **GitHub:** https://github.com/maiatoscastor/Hojetembola
- **Supabase URL:** https://xudejgyoknaampmt gdof.supabase.co
- **Supabase Anon Key:** eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh1ZGVqZ3lva25hYW1wbXRnZG9mIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzk4ODY0ODcsImV4cCI6MjA5NTQ2MjQ4N30.2s4AE7YT_wCqcXUOwKR05zJBEHgCAVHpRuVyzL4B-vA
- **Firebase:** google-services.json já está em app/

---

## 2. STACK TECNOLÓGICA FINAL

| Tecnologia | Versão | Para quê |
|---|---|---|
| Kotlin | 2.0.21 | Linguagem principal |
| Android Studio | Ladybug+ | IDE |
| AGP | 9.0.1 | Android Gradle Plugin |
| Gradle | 9.1.0 | Build system |
| XML Layouts + View Binding | nativo | UI |
| Navigation Component | 2.7.7 | Navegação entre ecrãs |
| ViewModel + LiveData | Lifecycle 2.7.0 | Gestão de estado |
| Hilt | 2.59.2 | Injeção de dependências (mínimo compatível com AGP 9.0) |
| KSP | 2.0.21-1.0.28 | Substitui KAPT (Room + Hilt) |
| Supabase BOM | 3.0.0 | Backend — BD, Auth, Realtime, Storage |
| Ktor Android | 3.0.0 | Cliente HTTP do Supabase SDK |
| Room | 2.6.1 | Base de dados local (offline) |
| WorkManager | 2.9.0 | Sync automática offline |
| Firebase BOM | 33.0.0 | Firebase Cloud Messaging |
| Firebase Messaging | via BOM | Notificações push |
| Glide | 4.16.0 | Carregar imagens |
| Coroutines | 1.7.3 | Operações assíncronas |
| JUnit 4 | 4.13.2 | Testes unitários |
| MockK | 1.13.10 | Mocking em Kotlin |
| Espresso | 3.5.1 | Testes de interface |

### Notas importantes de compatibilidade (já resolvidas)
- AGP 9.0.1 inclui Kotlin internamente — NÃO adicionar plugin kotlin.android separadamente
- Hilt mínimo compatível com AGP 9.0 é 2.59.2 (não usar 2.51)
- Usar `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }` em vez de `kotlinOptions`
- Usar KSP em vez de KAPT
- JVM toolchain NÃO usar (causa problemas se JDK 11 não instalado)

---

## 3. DESIGN SYSTEM COMPLETO

### Cores
```xml
<!-- Backgrounds -->
bg_primary     = #0D1B3E   <!-- Fundo principal -->
bg_surface     = #162448   <!-- Cards, inputs, superfícies -->
bg_border      = #1E3260   <!-- Bordas, separadores -->

<!-- Brand -->
orange         = #F57C00   <!-- Botões, badges live, ações -->
green          = #4CAF50   <!-- Sucesso, inscrições abertas -->

<!-- Texto -->
text_primary   = #FFFFFF
text_secondary = #8A9BB8
text_muted     = #4A5C7A   <!-- Placeholders -->

<!-- Estados -->
color_live     = #F57C00
color_open     = #4CAF50
color_done     = #8A9BB8
```

### Tipografia
- **Fonte:** Poppins (Google Fonts — Downloadable Fonts)
- Pesos usados: 400 (regular), 600 (semibold), 700 (bold)
- Import: `@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600;700&display=swap')`

### Componentes base
- **Cards:** background `#162448`, border-radius 12dp, border 1.5dp `#1E3260`
- **Card destacado (ao vivo):** border-color `#F57C00`
- **Inputs:** background `#162448`, border 1.5dp `#1E3260`, focus border `#F57C00`, placeholder `#4A5C7A`
- **Botão principal:** background `#F57C00`, border-radius 12dp, sempre visível (NÃO depender de hover)
- **Botão secundário:** background `#162448`, border `#1E3260`
- **Bottom Nav:** 4 itens (Início, Torneios, Rankings, Perfil), ativo em `#F57C00`, inativo em `#4A5C7A`
- **Badges:** font 9-11dp, border-radius 20dp
- **Barras de progresso:** height 3dp, background `#0D1B3E`
- **Avatares de equipas:** máx 4 visíveis + "+N" para as restantes
- **Toggles:** ON `#F57C00`, OFF `#1E3260`

### Regras de design
- NUNCA usar hover para mostrar cor de botão (app mobile)
- Scroll interno com scrollbar 3dp, thumb `#1E3260`
- Todos os textos em strings.xml (PT e EN)
- Layouts portrait E landscape onde faz sentido
- Status bar sempre presente: "9:41" + "●●●"
- Bottom nav em todos os ecrãs principais (não em modais)

---

## 4. ARQUITETURA MVVM

```
UI Layer (Activities/Fragments/XML)
        ↕ LiveData/StateFlow
ViewModel Layer (lógica de apresentação, Hilt)
        ↓
Repository Layer (decide local vs remoto)
        ↓              ↓
Room (offline)    Supabase SDK
WorkManager sync  Auth+Realtime+Storage
                        ↓
                  Firebase FCM
                  Google Maps Intent
                  Glide (imagens)
```

### Estrutura de pastas
```
app/src/main/java/com/hojetembola/app/
├── data/
│   ├── local/
│   │   ├── dao/              (15 DAOs — um por tabela)
│   │   ├── entity/           (15 entidades Room)
│   │   └── AppDatabase.kt
│   ├── remote/
│   │   └── SupabaseClient.kt
│   └── repository/           (TorneioRepository, JogoRepository, etc.)
├── domain/
│   ├── model/                (modelos de negócio puros)
│   └── usecase/              (CriarTorneioUseCase, VotarMVPUseCase, etc.)
├── ui/
│   ├── auth/                 (Splash, Sliders, Login, Registo)
│   ├── dashboard/
│   ├── torneios/             (Lista, Criar, Detalhe)
│   ├── jogos/                (Ao vivo, Registar evento)
│   ├── equipas/              (Perfil da equipa)
│   ├── rankings/
│   ├── mvp/                  (Votação MVP)
│   ├── perfil/
│   └── notificacoes/
├── utils/
│   ├── NetworkUtils.kt
│   ├── SyncWorker.kt
│   ├── Extensions.kt
│   └── HojeTemBolaFirebaseMessagingService.kt
└── di/
    └── AppModule.kt
```

---

## 5. BASE DE DADOS — SUPABASE

### Tabelas criadas (15)
As tabelas já estão criadas no Supabase. ENUMs e constraints já aplicados.

| Tabela | Descrição |
|---|---|
| utilizador | Contas de utilizador com perfil |
| equipa | Equipas com escudo, cor, capitão |
| membro_equipa | Relação N:M utilizador-equipa com campo ativo |
| convite | Convites para equipas (expiram 48h) |
| torneio | Torneios com todas as configurações |
| inscricao_equipa | Relação N:M equipa-torneio com estado e grupo |
| jogador_inscricao | Plantel selecionado para cada torneio (RF16) |
| jornada | Jornadas de um torneio |
| jogo | Jogos com localização e sync offline |
| convocatoria_jogo | Quem jogou e quantos minutos |
| evento_jogo | Golos, cartões, substituições, faltas |
| suspensao | Suspensões automáticas por cartões |
| classificacao | Tabela classificativa com posição anterior |
| voto_mvp | Votos no MVP com tipo_votante |
| notificacao | Notificações push com entidade_tipo/id |

### ENUMs definidos
- tipo_perfil: Administrador, Organizador, Capitao, Jogador, Espectador
- modalidade_tipo: Fut5, Fut7, Fut11, Personalizado
- formato_tipo: Liga, Eliminatorias, GruposEliminatorias, TodosContraTodos
- criterio_desempate: Prolongamento, Penalidades, GoloDeOuro
- visibilidade_tipo: Publico, Privado
- estado_torneio: Criado, InscricoesAbertas, InscricoesFechadas, ADecorrer, Terminado
- estado_inscricao: Pendente, Confirmada, Desistente, Rejeitada
- estado_jogo: Agendado, AoVivo, Terminado, Cancelado
- periodo_tipo: PrimeiroTempo, SegundoTempo, Prolongamento, Penalidades
- tipo_evento: Golo, CartaoAmarelo, CartaoVermelho, Substituicao, Falta
- estado_suspensao: Ativa, Cumprida, Cancelada
- tipo_votante: Jogador, Publico, Organizador
- tipo_notificacao: JogoMarcado, ResultadoAtualizado, ConviteRecebido, ConviteExpirado, Suspensao, AlteracaoCalendario
- estado_jornada: Agendada, ADecorrer, Terminada

### Campos de sync offline (importantes)
- JOGO: `sincronizado`, `pendente_sincronizacao`
- EVENTO_JOGO: `sincronizado`, `pendente_sincronizacao`
- SUSPENSAO: `sincronizado`
- TORNEIO: `sincronizado`

---

## 6. PERFIS DE UTILIZADOR E PERMISSÕES

| Perfil | Pode fazer |
|---|---|
| Administrador | Gerir todas as contas e torneios |
| Organizador | Criar torneios, registar jogos, eventos, resultados, cartões |
| Capitão | Criar/gerir equipa, inscrever em torneios, convidar jogadores |
| Jogador | Ver jogos, estatísticas, votar no MVP |
| Espectador | Ver resultados ao vivo, votar no MVP (sem equipa) |

---

## 7. FUNCIONALIDADES COMPLETAS

### 7.1 Autenticação (RF01-RF05)
- Splash screen com logo e loader animado
- 4 sliders introdutórios (mostrar só na primeira vez — SharedPreferences)
- Registo: nome, email, password, escolha de perfil
- Login: email + password OU Google OAuth 2.0
- "Esqueci a password" visível
- JWT tokens via Supabase Auth

### 7.2 Gestão de Equipas (RF06-RF10, RN01)
- Criar equipa: nome + cor/escudo
- Convidar jogadores via link (válido 48 horas — expira automaticamente)
- Transferir capitania para outro jogador
- Historial de torneios e estatísticas acumuladas da equipa
- Uma equipa pode estar em vários torneios em simultâneo
- Se equipa tem mais jogadores que limite → capitão seleciona quem vai (RF16)
- Quando jogador sai: registo NÃO é apagado (ativo = FALSE) — mantém historial

### 7.3 Torneios (RF11-RF22, RN02, RN03)
#### Formatos disponíveis
- Liga
- Eliminatórias
- Grupos + Eliminatórias
- Todos contra todos

#### Modalidades
- Fut5, Fut7, Fut11, Personalizado (número à escolha)

#### Campos completos do torneio
- Nome
- Modalidade + nº personalizado se aplicável
- Formato
- Nº máximo de equipas (min 2, max 32)
- Nº máximo de jogadores por equipa/plantel (min 5, max 25)
- Data início inscrições
- Data fim inscrições
- Data início torneio
- Data fim prevista
- Localização: nome + morada OU link Google Maps
- Critério de desempate (eliminatórias): prolongamento / penalidades / golo de ouro
- Nº amarelos para suspensão automática (1-5, default 3, configurável)
- Visibilidade: público / privado
- Toggle: permitir espectadores
- Toggle: votação MVP ativa
- Regulamento (texto livre)

#### Estados automáticos do torneio
- "Inscrições fechadas" → automático quando vagas esgotadas
- "A decorrer" → automático quando primeiro jogo começa
- "Terminado" → automático quando último jogo concluído
- "Criado" e "Inscrições abertas" → manuais

#### Inscrição de equipas
- Organizador cria equipas manualmente OU
- Capitão inscreve equipa existente do perfil OU
- Capitão cria equipa nova para o torneio
- Torneios públicos aparecem na pesquisa com filtros
- Torneios privados só por convite/link

#### Desistência
- Jogo por realizar = 3-0 para adversário
- Equipa fica marcada como "Desistente"

#### Fim do torneio
- Top 3 guardado no historial das equipas
- Sem ecrã especial de cerimónia

### 7.4 Jogos (RF21-RF25, RN04)
- Registar jogo: equipas, data, hora, localização
- Localização: nome + morada OU link Google Maps → abre Google Maps via Intent
- Registo em tempo real pelo Organizador: golos, faltas, substituições
- Cartões: podem ser adicionados durante OU no fim (com minuto + jogador)
- Suspensões automáticas:
  - Acumulação de X amarelos (configurado no torneio) → suspensão jogo seguinte
  - Vermelho direto → suspensão jogo seguinte
- Modo offline: organizador regista sem internet, sincroniza com WorkManager

### 7.5 Estatísticas e Rankings (RF26-RF29, RN05)
- Tabela classificativa atualizada automaticamente após cada jogo
- Estatísticas: golos e jogos disputados por jogador e equipa
- Filtros: por jornada / mês / torneio completo
- Evolução de posição: ▲ subiu N / ▼ desceu N

### 7.6 Votação MVP (RF30-RF31, RN06, RN07)
- MVP = Most Valuable Player (Jogador Mais Valioso)
- Quem vota: Jogadores + Público/Espectadores + Organizador
- 1 voto por pessoa por jogo
- Voto pode ser alterado durante o jogo
- Resultados SEMPRE em percentagem (nunca número absoluto)
- Percentagens separadas por grupo: jogadores / público / organizador
- MVP revelado apenas após fecho da votação
- Em caso de empate: organizador desempata
- Vencedor acumula pontos no ranking de MVPs
- Campo `JOGO.votacao_mvp_ativa` controla quando é visível

### 7.7 Notificações (RN08)
- Firebase Cloud Messaging (FCM) — notificações push reais
- Funcionam mesmo com a app fechada
- Tipos: jogos marcados, resultados, convites, convites expirados, suspensões, alterações calendário
- Canal de notificação criado para API 26+

### 7.8 Outros
- Google Maps: Intent Android (não SDK) — apenas para abrir localização
- Glide: carregar fotos de perfil e escudos do Supabase Storage
- Supabase Realtime: marcador ao vivo + percentagens MVP em tempo real (WebSockets)

---

## 8. ECRÃS DA APP — LISTA COMPLETA

### Portrait (20 ecrãs)
| Nº | Ecrã | Notas |
|---|---|---|
| 01 | Splash Screen | Logo + loader animado + 4 dots |
| 02-05 | Sliders (4) | Só na primeira vez, botão sempre laranja |
| 06 | Login | Tabs Entrar/Registar, Google OAuth |
| 07 | Registo | Nome, email, password, perfil |
| 08 | Dashboard (Jogador) | Stats pessoais, jogo ao vivo, torneios |
| 09 | Dashboard (Organizador) | Torneios geridos, ações rápidas |
| 10 | Lista de Torneios | Filtros, pesquisa, +N equipas |
| 11 | Criar Torneio | Formulário completo em scroll |
| 12 | Detalhe de Torneio | Tabs: Classificação/Jornadas/Equipas/Info |
| 13 | Perfil da Equipa | Tabs: Jogadores/Torneios/Estatísticas |
| 14 | Jogo Ao Vivo | Marcador, timeline eventos, botões org. |
| 15 | Registar Evento | Bottom sheet: golo/cartão/substituição |
| 16 | Rankings | Pódio top 3, lista com evolução |
| 17 | Votação MVP | Percentagens por grupo, botão votar |
| 18 | Perfil do Utilizador | Stats, equipas, historial, settings |
| 19 | Notificações | Lista com ícones e tempo |
| 20 | Pesquisa Torneios | Filtros: zona, modalidade, formato, estado |

### Landscape (2 ecrãs — obrigatório mostrar suporte)
| Nº | Ecrã | Layout |
|---|---|---|
| L1 | Jogo Ao Vivo | Marcador à esquerda, timeline à direita |
| L2 | Rankings | Pódio à esquerda, tabela completa à direita |

---

## 9. DADOS DE EXEMPLO CONSISTENTES

Usar SEMPRE os mesmos dados fictícios em todos os mockups/testes:

- **Utilizador logado:** Gonçalo Teixeira
- **Equipas:**
  - FC Malcata (#E84C3D)
  - Os Putos (#2ECC71)
  - Turbo FC (#3498DB)
  - Super Raia (#E67E22)
- **Torneio ativo:** "Torneio Verão 2025" — Liga, Fut7, Jornada 4/7, 8 equipas
- **Jogo ao vivo:** FC Malcata 2—1 Os Putos, 47', Campo da Várzea, Viana do Castelo

---

## 10. REQUISITOS FUNCIONAIS (resumo)

| Código | Tipo | Resumo |
|---|---|---|
| RF01 | User Story | Administrador cria/edita/remove contas |
| RF02 | User Story | Utilizador faz registo com perfil |
| RF03 | User Story | Autenticação email/Google OAuth |
| RF04 | User Story | Sliders introdutórios na 1ª vez |
| RF05 | User Story | Editar perfil |
| RF06 | User Story | Capitão cria equipa |
| RF07 | User Story | Capitão convida jogadores (link 48h) |
| RF08 | User Story | Capitão transfere capitania |
| RF09 | User Story | Historial e stats da equipa |
| RF10 | User Story | Jogador notificado de convite |
| RN01 | Regra negócio | Sistema expira convites após 48h |
| RF11 | User Story | Organizador cria torneio completo |
| RF12 | User Story | Definir formato do torneio |
| RF13 | User Story | Torneio público ou privado |
| RF14 | User Story | Organizador cria equipas manualmente |
| RF15 | User Story | Capitão inscreve equipa em torneio |
| RF16 | User Story | Capitão seleciona jogadores (plantel > limite) |
| RF17 | User Story | Pesquisar torneios públicos com filtros |
| RF18 | User Story | Organizador gere calendário de jogos |
| RF19 | User Story | Marcar equipa como desistente (3-0) |
| RF20 | User Story | Definir critério de desempate |
| RN02 | Regra negócio | Inscrições fecham automaticamente |
| RN03 | Regra negócio | Estado do torneio atualiza automaticamente |
| RF21 | User Story | Registar jogo com local |
| RF22 | User Story | Registar resultados/golos/faltas/subs |
| RF23 | User Story | Registar cartões com minuto |
| RF24 | User Story | Abrir localização no Google Maps |
| RF25 | User Story | Registar offline |
| RN04 | Regra negócio | Suspensões automáticas por cartões |
| RF26 | User Story | Consultar classificação |
| RF27 | User Story | Historial de resultados |
| RF28 | User Story | Estatísticas por jogador/equipa |
| RF29 | User Story | Filtrar estatísticas por período |
| RN05 | Regra negócio | Classificações atualizadas automaticamente |
| RF30 | User Story | Votar no MVP (Most Valuable Player) |
| RF31 | User Story | Alterar voto durante o jogo |
| RN06 | Regra negócio | Regras votação MVP (1 voto, sem auto-voto, % apenas) |
| RN07 | Regra negócio | Desempate MVP pelo organizador |
| RN08 | Regra negócio | Notificações automáticas |

### Requisitos Não Funcionais
| Código | Área | Resumo |
|---|---|---|
| RNF01 | Segurança | Passwords com bcrypt (fator mínimo 12) |
| RNF02 | Segurança | JWT tokens (24h validade) + refresh (30 dias) |
| RNF03 | Segurança | HTTPS (TLS 1.2+) em toda a comunicação |
| RNF04 | Segurança | Google OAuth 2.0 com fluxo PKCE |
| RNF05 | Integridade | Dados offline com SQLite, sync por timestamp |
| RNF06 | Integridade | Transações ACID na base de dados |
| RNF07 | Backups | Backups automáticos diários (30 dias retenção) |
| RNF08 | Desempenho | Resposta API < 2 segundos (95% operações) |
| RNF09 | Disponibilidade | Disponibilidade mínima 99% |
| RNF10 | Usabilidade | 10 heurísticas de Nielsen |
| RNF11 | Usabilidade | Elementos interativos mínimo 44x44dp |
| RNF12 | Usabilidade | Suporte PT e EN (strings.xml) |
| RNF13 | Usabilidade | Portrait e landscape em todos os layouts |
| RNF14 | Compatibilidade | Android 8.0+ (API 26+) |
| RNF15 | Auditoria | Logs de operações críticas |
| RNF16 | Manutenibilidade | Kotlin Style Guide + MVVM + 60% cobertura testes |

---

## 11. ESTADO ATUAL DA IMPLEMENTAÇÃO

### ✅ Concluído

- Repositório GitHub criado e configurado
- Projeto Android Studio criado (package: com.hojetembola.app, API 26, Kotlin DSL)
- Firebase configurado (google-services.json em app/)
- Supabase criado com todas as 15 tabelas e ENUMs

- **Passo 1 — Fundação técnica COMPLETA:**
  - libs.versions.toml com todas as dependências
  - build.gradle.kts (projeto e app) configurados e funcionais
  - Compatibilidade AGP 9.0.1 + Kotlin 2.0.21 resolvida (ver secção 15)
  - HojeTemBolaApp.kt, SupabaseClient.kt, AppDatabase.kt (Room v2, 15 entidades)
  - 15 DAOs + AppModule.kt (Hilt) + NetworkUtils + SyncWorker + Extensions + FCM
  - colors.xml, themes.xml, dimens.xml, strings.xml (~130 PT + EN)
  - Poppins via Downloadable Fonts
  - **BUILD SUCCESSFUL** ✅

- **Passo 2 — Autenticação COMPLETA:**
  - SplashActivity com logo real (logo_htb.png) e loader animado
  - 4 SliderFragments (mostrados só na 1ª vez via SharedPreferences)
  - LoginFragment: tabs Entrar/Registar, email+password, Google OAuth (stub), recuperação de password
  - RegistoFragment: nome, email, password, confirmação, escolha de perfil
  - AuthViewModel (StateFlow: Idle/Loading/Success/EmailSent/Error)
  - AuthRepository (Supabase Auth: login, register, forgotPassword, signOut)
  - Layouts portrait e landscape para todos os ecrãs de auth
  - **Fluxo de navegação auth → main:** após login bem-sucedido navega para MainActivity e faz finish() da SplashActivity
  - **Após registo:** navega imediatamente para o Login (não espera pelo Snackbar) e passa mensagem de sucesso via savedStateHandle

- **Passo 3 — Navegação principal COMPLETA:**
  - MainActivity com BottomNavigationView (4 tabs: Início, Torneios, Rankings, Perfil)
  - nav_auth.xml (Splash → Sliders → Login/Registo)
  - nav_main.xml (Home, Torneios, CriarTorneio, Rankings, Perfil)
  - Dois NavGraphs separados — auth em SplashActivity, main em MainActivity

- **Passo 5 (parcial) — Torneios:**
  - TorneiosFragment: lista com RecyclerView multi-tipo (Header/Meu/Público), pesquisa reativa, chips de filtro (Todos/A decorrer/Inscrições/Terminados), estados loading/empty/error
  - CriarTorneioFragment: formulário completo em 6 secções (Informações, Equipas, Datas, Localização, Regras, Visibilidade), ChipGroups, steppers −/+, DatePickers via MaterialDatePicker, SwitchMaterial, validação, offline-first
  - TorneioAdapter: ListAdapter com 3 view types + DiffUtil (TorneioListItem sealed class)
  - TorneiosViewModel: combine() de 3 StateFlows (filtro, pesquisa, zona) + dados Room
  - CriarTorneioViewModel: CriarTorneioForm data class, validate(), criarTorneio() com offline-first
  - TorneioRepository: getMeusTorneios (UNION query organizer+membro), getTorneiosPublicos, syncTorneios, criarTorneio
  - TorneioDto: @Serializable com @SerialName, toEntity() / toDto()
  - Drawables: bg_badge_live, bg_badge_open, bg_badge_done, ic_back
  - Layouts: item_lista_header, item_torneio_meu (com barra de progresso), item_torneio_publico (com botão inscrever)
  - ❌ **DetalheTorneioFragment ainda não implementado**

- **Passo 10 (parcial) — Perfil:**
  - PerfilFragment completo: avatar, nome, perfil, estatísticas, equipas, conquistas, terminar sessão
  - PerfilViewModel: StateFlow (Loading/Success/Error) + SharedFlow para evento signOut
  - UserRepository: getCurrentUser (cache-first + fallback rede), signOut
  - ❌ **NotificacoesFragment ainda não implementado**

- **Launcher e branding:**
  - Ícone launcher: bola de futebol calculada matematicamente (1 pentágono central + 5 periféricos)
  - Logo real da app (logo_htb.png) usada em SplashActivity e LoginFragment

### ⚠️ Implementado mas com pendências conhecidas

Ver **Secção 17 — Melhorias e Correções Pendentes** para lista completa e detalhada.

### ⏳ Por implementar (por ordem de prioridade)

**Passo 4 — Dashboard/Home**
- HomeFragment: vista diferente para Organizador vs. Utilizador
- Stats pessoais (golos, MVPs, partidas), próximo jogo, torneios ativos
- HomeViewModel + HomeRepository

**Passo 5 (continuação) — Detalhe de Torneio**
- DetalheTorneioFragment com 4 tabs: Classificação / Jornadas / Equipas / Info
- TorneioDetalheViewModel

**Passo 6 — Equipas**
- PerfilEquipaFragment (tabs: Jogadores / Torneios / Estatísticas)
- EquipaViewModel + EquipaRepository
- Gestão de convites (link 48h, expiração automática)
- Transferência de capitania

**Passo 7 — Jogos**
- JogoAoVivoFragment (portrait + landscape L1 obrigatório)
- RegistarEventoFragment (bottom sheet: golo / cartão / substituição)
- JogoViewModel + JogoRepository
- Supabase Realtime para marcador ao vivo (WebSocket já instalado)

**Passo 8 — Rankings**
- RankingsFragment (portrait + landscape L2 obrigatório)
- Pódio visual top 3 (cores: gold #EF9F27, silver #378ADD, bronze #D85A30)
- Lista com indicador de evolução ▲N / ▼N
- RankingsViewModel + RankingsRepository

**Passo 9 — MVP**
- VotacaoMVPFragment com percentagens por grupo (Jogadores / Público / Organizador)
- Percentagens em tempo real via Supabase Realtime
- MVPViewModel + MVPRepository
- Resultado escondido até fechar votação

**Passo 10 (continuação) — Notificações**
- NotificacoesFragment: lista com ícone por tipo + tempo relativo
- NotificacoesViewModel
- Integração com FCM (base já existe em HojeTemBolaFirebaseMessagingService)

**Passo 11 — Pesquisa avançada**
- PesquisaTorneiosFragment com filtros: zona, modalidade, formato, estado

**Passo 12 — Offline e sync**
- Completar SyncWorker (syncJogos e syncEventos têm TODO — atualmente apenas marcam como sincronizados sem enviar para o Supabase)
- Testar modo offline em todas as operações críticas

**Passo 13 — Testes**
- Testes unitários (JUnit 4 + MockK), mínimo 60% cobertura
- Testes de interface (Espresso)

**Passo 14 — Entrega final**
- APK assinado
- Documentação técnica completa
- Apresentação em slides

---

## 12. REGRAS IMPORTANTES PARA IMPLEMENTAÇÃO

1. **Botões sempre visíveis** — cor `#F57C00` sempre aplicada, NUNCA depender de hover
2. **MVVM estrito** — UI nunca acede diretamente ao Repository
3. **StateFlow** para estados da UI no ViewModel (não LiveData para novos ficheiros)
4. **Coroutines** para tudo assíncrono — nunca callbacks
5. **Strings em resources** — nunca hardcoded
6. **Cores em colors.xml** — nunca hardcoded
7. **Dimensões em dimens.xml** — nunca hardcoded
8. **Portrait E landscape** — criar layout-land/ onde necessário
9. **PT e EN** — todos os textos em strings.xml e values-en/strings.xml
10. **Avatares de equipas** — máx 4 visíveis + "+N"
11. **MVP em percentagem** — NUNCA mostrar número absoluto de votos
12. **Google Maps** — usar Intent Android, não SDK
13. **Supabase Realtime** — para marcador ao vivo e percentagens MVP
14. **WorkManager** — para sync offline em background
15. **Tratar erros** — sempre com mensagens adequadas ao utilizador em PT

---

## 13. ENTREGÁVEIS DO PROJETO

| Entregável | Estado |
|---|---|
| Utilização do GIT | ✅ GitHub configurado |
| Plataforma gestão projetos | ✅ Notion configurado |
| Requisitos RF e RNF | ✅ Documento Word v2.0 gerado |
| Mockups (20 portrait + 2 landscape) | ✅ Criados em HTML |
| Modelo de dados (DER + justificação) | ✅ Documento Word gerado |
| Arquitetura e planeamento | ✅ Documento Word gerado |
| Base de dados (Supabase) | ✅ 15 tabelas criadas |
| Código-fonte Android | 🔄 Em progresso (Passos 1, 2, 3, 5 parcial, 10 parcial concluídos) |
| APK assinado | ⏳ Pendente |
| Testes unitários + integração | ⏳ Pendente |
| Documentação técnica | ⏳ Pendente |
| Apresentação slides | ⏳ Pendente |

---

## 14. DECISÕES DE MODELAÇÃO IMPORTANTES

### Porquê EVENTO_JOGO separado
- Necessário N registos por tipo (golos, cartões por minuto e jogador)
- Timeline ao vivo usa ORDER BY minuto trivial
- Modo offline: cada evento é linha atómica, enfileira e sincroniza individualmente

### Relações N:M
- EQUIPA ↔ TORNEIO via INSCRICAO_EQUIPA (tem estado, grupo, data)
- UTILIZADOR ↔ EQUIPA via MEMBRO_EQUIPA (campo ativo — historial preservado)
- UTILIZADOR ↔ TORNEIO via JOGADOR_INSCRICAO (plantel selecionado — RF16)

### Rastreabilidade disciplinar
1. Registo: EVENTO_JOGO com tipo = CartaoAmarelo/CartaoVermelho
2. Lógica: COUNT amarelos no torneio → se >= limite → cria SUSPENSAO
3. SUSPENSAO explícita: estado Ativa/Cumprida/Cancelada — consultável offline sem recalcular

### MVP em percentagem
- VOTO_MVP nunca exposto diretamente
- Cálculo: COUNT(votado_id=X) / COUNT(jogo_id=J) × 100 por tipo_votante
- UNIQUE(jogo_id, votante_id) garante 1 voto por pessoa na BD
- CHECK(votante_id <> votado_id) impede auto-voto na BD

### Modo offline
- Flags `sincronizado` e `pendente_sincronizacao` nas tabelas críticas
- WorkManager envia por ordem cronológica (data_registo)
- Server recalcula CLASSIFICACAO e SUSPENSAO após cada sync

---

## 15. DECISÕES TÉCNICAS E PADRÕES DE IMPLEMENTAÇÃO

### Multi-type RecyclerView (TorneiosFragment)
- Padrão: `sealed class TorneioListItem` com subtypes `Header`, `Meu`, `Publico`
- `ListAdapter<TorneioListItem, RecyclerView.ViewHolder>` com `DiffUtil.ItemCallback` que usa `getItemViewType()` para retornar 0/1/2
- O Fragment constrói a lista combinada (headers + items) antes de chamar `submitList()`
- Nunca misturar lógica de agrupamento no Adapter — fica no Fragment ou ViewModel

### StateFlow reativo com `combine()` (TorneiosViewModel)
```kotlin
val uiState = combine(baseState, _filtro, _pesquisa, _zona) { state, f, p, z ->
    // mapeia para UiState filtrado
}.stateIn(viewModelScope, SharingStarted.Lazily, UiState.Loading)
```
- `SharingStarted.Lazily` — só inicia quando há o primeiro subscriber, não produz trabalho desnecessário
- Cada filtro é um `MutableStateFlow<String>` separado; usar `setValue()` nos setters públicos

### Offline-first — Torneios
1. `CriarTorneioViewModel.criarTorneio()`: insere no Room com `isSynced = false`
2. Tenta enviar para Supabase imediatamente
3. Se sucesso: atualiza `isSynced = true` no Room
4. Se falha: WorkManager irá tentar mais tarde (SyncWorker)
- Nunca bloquear a UI a aguardar resposta do Supabase

### DatePicker com ISO 8601
```kotlin
val millis: Long = selection  // MaterialDatePicker retorna epoch ms
val iso = Instant.ofEpochMilli(millis)
    .atZone(ZoneOffset.UTC)
    .toLocalDate()
    .toString()  // "YYYY-MM-DD"
```
- Sempre guardar datas como ISO 8601 (YYYY-MM-DD) nas entidades e DTOs
- Para display ao utilizador: converter para DD/MM/YYYY com `toDisplayDate()` extension

### `setTextSilently()` — evitar loop TextWatcher
Quando um StateFlow atualiza um campo de texto que tem um TextWatcher registado, o listener re-dispara e pode criar ciclo infinito. Solução:
```kotlin
fun EditText.setTextSilently(watcher: TextWatcher, text: String) {
    removeTextChangedListener(watcher)
    setText(text)
    addTextChangedListener(watcher)
}
```

### ChipGroup — regra crítica de layout
**Todos os `<Chip>` dentro de `<ChipGroup>` DEVEM ter `android:layout_width` e `android:layout_height` explícitos no XML**, mesmo quando a dimensão está definida no style. `ChipGroup.LayoutParams` não herda atributos de layout do style — causa `InflateException` se omitidos. Ver Secção 16 para o bug completo.

### QUERY DAO com UNION (getMeusTorneios)
```sql
SELECT t.* FROM torneio t WHERE t.organizador_id = :userId
UNION
SELECT t.* FROM torneio t
INNER JOIN inscricao_equipa ie ON t.id = ie.torneio_id
INNER JOIN membro_equipa me ON ie.equipa_id = me.equipa_id
WHERE me.utilizador_id = :userId AND me.ativo = 1
```
- Retorna Flow<List<TorneioEntity>> — atualiza automaticamente quando Room muda

### Room version bump
- Versão atual: **2** (15 entidades, esquema estável)
- Sempre incrementar versão em `AppDatabase` quando se adicionam colunas/tabelas
- Usar `fallbackToDestructiveMigration()` durante desenvolvimento (não em produção)
- Próximo bump necessário ao implementar: `codigoAcesso` e `tempoExtraMinutos` na tabela `torneio`

---

## 16. BUGS ENCONTRADOS E CORREÇÕES APLICADAS

### BUG-01 — App fecha ao navegar para aba Torneios
**Data:** Sessão 2 (Junho 2026)
**Sintoma:** `java.lang.RuntimeException` → `InflateException` ao navegar para TorneiosFragment
**Stack trace:** `ChipGroup$LayoutParams.<init>` ao inflar `fragment_torneios.xml` linha 95
**Causa raiz:** Os elementos `<Chip>` dentro de `<ChipGroup>` não tinham `android:layout_width` e `android:layout_height` declarados explicitamente no XML. Ao contrário de outros ViewGroups, `ChipGroup.LayoutParams` exige estes atributos diretamente no elemento — não os herda do `style`.
**Correção:**
- Adicionado `android:layout_width="wrap_content"` e `android:layout_height="wrap_content"` aos 4 chips de `fragment_torneios.xml`
- Adicionado o mesmo aos 13 chips distribuídos pelos 4 ChipGroups de `fragment_criar_torneio.xml`
**Ficheiros alterados:** `fragment_torneios.xml`, `fragment_criar_torneio.xml`
**Prevenção futura:** Sempre incluir `layout_width`/`layout_height` explícitos em Chips dentro de ChipGroups, mesmo com style definido.

### BUG-02 — Compatibilidade AGP 9.0.1 + Kotlin 2.0.21 (Passo 1)
**Sintoma:** Build falha com erros de plugin Kotlin
**Causa:** AGP 9.0.1 inclui Kotlin internamente; usar `kotlin.android` plugin separado causa conflito. KAPT não funciona com KSP configurado em simultâneo.
**Correções aplicadas:**
- Removido plugin `kotlin.android` separado do `build.gradle.kts`
- Migrado de `kotlinOptions { jvmTarget = "11" }` para `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }`
- Substituído KAPT por KSP em todas as anotações (Room + Hilt)
- Hilt versão mínima compatível: 2.59.2 (não usar 2.51 com AGP 9.0)
- NÃO usar JVM toolchain (causa problemas se JDK 11 não instalado globalmente)

---

## 17. MELHORIAS E CORREÇÕES PENDENTES

### MELHORIA-01 — Verificação de permissão na criação de torneios
**Prioridade:** Alta
**Problema:** Qualquer utilizador logado pode ver e clicar em "Criar Torneio", mas só Organizadores devem poder criar.
**Implementação necessária:**
1. Em `TorneiosFragment.kt`: esconder `btnCriar` se `utilizador.perfil != "organizador"`
   ```kotlin
   // No observeState() após obter o utilizador:
   binding.btnCriar.isVisible = utilizador.perfil == "organizador"
   ```
2. Em `CriarTorneioViewModel.criarTorneio()`: validar antes de prosseguir
   ```kotlin
   if (currentUser?.perfil != "organizador") {
       _uiState.value = CriarTorneioUiState.Error("Apenas organizadores podem criar torneios")
       return@launch
   }
   ```
3. `TorneiosViewModel` já tem acesso ao `UserRepository` via Hilt — usar para obter perfil atual

### MELHORIA-02 — Mínimo de jogadores por equipa baseado na modalidade
**Prioridade:** Alta
**Problema:** O stepper de "max jogadores por equipa" não tem mínimo baseado na modalidade. Um torneio Fut11 podia ser criado com mínimo de 5 jogadores por equipa.
**Regras:**
| Modalidade | Mínimo jogadores/equipa |
|---|---|
| Fut5 | 5 |
| Fut7 | 7 |
| Fut11 | 11 |
| Personalizado | Igual ao numJogadoresPersonalizado |
**Implementação necessária em `CriarTorneioViewModel`:**
```kotlin
fun setModalidade(modalidade: String) {
    val minJogadores = when (modalidade) {
        "fut5" -> 5; "fut7" -> 7; "fut11" -> 11
        "personalizado" -> _form.value.numJogadoresPersonalizado ?: 5
        else -> 5
    }
    val currentMax = _form.value.maxJogadoresPorEquipa
    _form.update {
        it.copy(
            modalidade = modalidade,
            maxJogadoresPorEquipa = maxOf(currentMax, minJogadores),
            minJogadoresPorEquipa = minJogadores
        )
    }
}
```
- Adicionar campo `minJogadoresPorEquipa: Int = 5` ao `CriarTorneioForm`
- Atualizar `validate()` para verificar `maxJogadoresPorEquipa >= minJogadoresPorEquipa`
- No Fragment: stepper decrementar deve ter limite mínimo dinâmico

### MELHORIA-03 — Validação de datas
**Prioridade:** Alta
**Problema:** Não há validação para impedir datas no passado ou ordenação incoerente.
**Regras a implementar em `validate()`:**
```kotlin
val today = LocalDate.now()
val inicioInscricoes = LocalDate.parse(form.dataInicioInscricoes)
val fimInscricoes = LocalDate.parse(form.dataFimInscricoes)
val inicioTorneio = LocalDate.parse(form.dataInicio)

if (inicioInscricoes < today)
    return ValidationError("Data de início de inscrições não pode ser no passado")
if (fimInscricoes <= inicioInscricoes)
    return ValidationError("Data de fim de inscrições deve ser posterior ao início")
if (inicioTorneio <= fimInscricoes)
    return ValidationError("Data de início do torneio deve ser posterior ao fim das inscrições")
if (form.dataFimPrevista.isNotEmpty()) {
    val fimPrevista = LocalDate.parse(form.dataFimPrevista)
    if (fimPrevista <= inicioTorneio)
        return ValidationError("Data de fim prevista deve ser posterior ao início")
}
```

### MELHORIA-04 — Campo de duração do prolongamento
**Prioridade:** Média
**Problema:** Quando se seleciona "Prolongamento" como critério de desempate, não existe campo para especificar quantos minutos tem cada parte do prolongamento.
**Mudanças necessárias (requerem Room bump v2 → v3 e coluna Supabase):**
1. **`TorneioEntity`**: adicionar `val tempoExtraMinutos: Int = 10`
2. **`TorneioDto`**: adicionar `@SerialName("tempo_extra_minutos") val tempoExtraMinutos: Int = 10`
3. **`AppDatabase`**: versão 2 → 3, adicionar migration ou `fallbackToDestructiveMigration()`
4. **`fragment_criar_torneio.xml`**: adicionar stepper `tvTempoExtra` (visível só quando chipProlongamento checked)
5. **`CriarTorneioForm`**: adicionar `tempoExtraMinutos: Int = 10`
6. **`CriarTorneioFragment`**: observar `cgCriterio` e mostrar/esconder stepper
7. **Supabase**: `ALTER TABLE torneio ADD COLUMN tempo_extra_minutos INTEGER DEFAULT 10`

### MELHORIA-05 — Código de acesso para torneios privados
**Prioridade:** Média
**Problema:** Torneios privados não têm mecanismo para controlar quem pode inscrever-se/participar.
**Decisão tomada:** Código de acesso (4-6 dígitos/letras) que o Capitão insere ao tentar inscrever-se — mais simples que sistema de convites e não requer UI adicional para o Organizador.
**Mudanças necessárias (requerem Room bump v2 → v3 e coluna Supabase):**
1. **`TorneioEntity`**: adicionar `val codigoAcesso: String? = null`
2. **`TorneioDto`**: adicionar `@SerialName("codigo_acesso") val codigoAcesso: String? = null`
3. **`AppDatabase`**: versão 2 → 3 (juntar com MELHORIA-04 no mesmo bump)
4. **`fragment_criar_torneio.xml`**: adicionar `tilCodigoAcesso` (TextInputLayout, visível só quando chipPrivado checked)
5. **`CriarTorneioForm`**: adicionar `codigoAcesso: String? = null`
6. **`CriarTorneioFragment`**: observar `cgVisibilidade` e mostrar/esconder campo
7. **Supabase**: `ALTER TABLE torneio ADD COLUMN codigo_acesso TEXT`
8. **Futura tela de inscrição**: verificar `codigoAcesso` antes de confirmar inscrição

### MELHORIA-06 — Renomear "Todos vs todos" para "Round Robin"
**Prioridade:** Baixa
**Problema:** O texto "Todos vs todos" não transmite claramente o formato para utilizadores familiarizados com terminologia desportiva.
**Round Robin = cada equipa joga contra todas as outras exatamente 1 vez (liga simples); Liga = home+away (2 voltas).**
**Implementação:** Apenas alterar `strings.xml`:
```xml
<!-- Antes -->
<string name="formato_todos_vs_todos">Todos vs todos</string>
<!-- Depois -->
<string name="formato_todos_vs_todos">Round Robin (1 volta)</string>
```
Sem alterações de lógica — apenas visual/textual.

### NOTA — Melhorias 04 e 05 em conjunto
As MELHORIA-04 e MELHORIA-05 ambas requerem alterações ao `TorneioEntity`, `TorneioDto` e um Room version bump. Devem ser implementadas **na mesma sessão** para minimizar o número de migrations necessárias (bump único v2 → v3).

---

*Ficheiro atualizado em Junho 2026 — HojeTemBola, Computação Móvel 2025/2026, IPVC*
*Usar este ficheiro como contexto principal em todas as sessões do Claude Code*
