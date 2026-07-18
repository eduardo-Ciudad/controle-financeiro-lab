# Financial Control Lab — Backend Multi-Tenant

Sistema de gestão financeira multi-tenant — evolução do projeto freelance original. Cada usuário se cadastra, verifica seu email e tem acesso isolado aos seus próprios dados. Controla lançamentos de clientes e fornecedores, estoque de produtos, extratos mensais e dashboard de vendas com autenticação JWT, verificação de email, fluxos completos de alteração e recuperação de senha, rate limiting e arquitetura append-only para integridade dos dados.

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **Spring Security** + JWT
- **PostgreSQL 16**
- **Flyway** — 16 migrações versionadas
- **Docker** — multi-stage build (`eclipse-temurin:17`, non-root user)
- **Nginx** — reverse proxy com SSL
- **Let's Encrypt / Certbot** — certificado HTTPS via DuckDNS
- **Gmail SMTP** — verificação de email e notificações de senha assíncronas (`@Async`)
- **Caffeine Cache** — rate limiting por IP
- **GitHub Actions** — CI automatizado
- **Swagger / OpenAPI** — documentação interativa

## Arquitetura

```
com.eduardo.financialcontrol
├── auth/            # Registro, login, verificação de email, alteração e reset de senha
├── cliente/         # CRUD de clientes + lançamentos (conta-corrente)
├── fornecedor/      # CRUD de fornecedores + lançamentos de compra
├── lancamento/      # Lançamentos gerais (categoria, natureza, forma de pagamento)
├── produto/         # Catálogo com preço de custo / venda (margem 30%)
├── estoque/         # Movimentações de entrada/saída, decremento integrado em vendas
├── pessoal/         # Contas pessoais com parcelamento
├── saldo/           # Dashboard, extrato mensal, resumo e cálculo de saldo
├── email/           # EmailService — envio assíncrono (verificação, alteração e reset de senha)
├── config/          # CORS, OpenAPI, RateLimitConfig (Caffeine)
├── security/        # JwtFilter, JwtService, SecurityConfig, UsuarioAutenticadoService
└── shared/          # GlobalExceptionHandler, exceções customizadas, ApiError DTO
```

### Decisões técnicas

- **Multi-tenant por usuário** — isolamento completo de dados via `usuario_id` como FK em todas as tabelas (migração V14). Todas as queries filtram pelo usuário autenticado via `UsuarioAutenticadoService`, garantindo que nenhum usuário acesse dados de outro. Entidades usam atribuição de ownership por construtor (não setter) para prevenir transferência acidental.
- **Append-only ledger** — lançamentos nunca são editados ou deletados; correções são feitas via estorno (novo lançamento inverso), garantindo rastreabilidade completa.
- **Saldo computado** — o saldo é calculado em tempo real via `SUM` sobre os lançamentos, nunca armazenado diretamente. Isso elimina inconsistências entre saldo e histórico.
- **Package-by-feature** — cada domínio (cliente, fornecedor, produto, etc.) é um pacote isolado com sua própria camada de controller, service, repository e DTOs.
- **Rate limiting com Caffeine** — proteção contra abuso de requisições por IP, substituindo `ConcurrentHashMap` por Caffeine Cache com `expireAfterAccess` de 1 hora e `maximumSize` de 10.000 entradas.
- **Verificação de email** — registro exige confirmação via link enviado por Gmail SMTP. Token UUID com expiração de 24h, envio assíncrono via `@Async`/`@EnableAsync`, endpoint de reenvio disponível.
- **Tabela separada para tokens de senha** — em vez de adicionar colunas na tabela `usuarios` (que já tem tokens de verificação de email), os tokens de alteração e reset de senha usam a tabela `password_tokens` com enum `TipoTokenSenha` (ALTERACAO/RESET). Evita conflitos e escala melhor.
- **Prevenção de enumeração de contas** — o endpoint `POST /auth/esqueci-senha` sempre retorna 200 independente do email existir ou não, impedindo que um atacante descubra quais emails estão cadastrados.

## Funcionalidades

- **Autenticação** — registro com verificação de email obrigatória e login com JWT. Usuários inativos (não verificados) não conseguem autenticar.
- **Verificação de email** — ao registrar, o usuário recebe um email com link de verificação (token UUID, expiração 24h). Endpoints públicos para verificar (`GET /auth/verificar`) e reenviar (`POST /auth/reenviar-verificacao`).
- **Alteração de senha** — usuário logado envia senha atual + nova senha; backend valida, hasheia a nova senha em staging na tabela `password_tokens` e envia email de confirmação. A senha só é aplicada quando o usuário clica no link do email (`GET /auth/confirmar-alteracao-senha`).
- **Recuperação de senha** — usuário deslogado informa seu email; backend gera token com expiração de 1 hora e envia link por email. O usuário clica, define uma nova senha e envia via `POST /auth/resetar-senha`.
- **Rate limiting** — proteção por IP via Caffeine Cache em todos os endpoints de autenticação, evitando abuso.
- **Clientes** — cadastro completo com lançamentos no modelo conta-corrente; extrato agrupado por mês; coluna de saldo com indicador visual.
- **Fornecedores** — cadastro + lançamentos de compra com preço unitário customizado por operação.
- **Produtos** — catálogo com `precoCusto` e `precoVenda`, fórmula de margem de 30%.
- **Estoque** — movimentações de entrada e saída; decremento automático integrado ao registrar vendas; enums `TipoMovimentacao` e `OrigemMovimentacao` para rastreabilidade.
- **Contas pessoais** — CRUD com parcelamento automático e resumo mensal.
- **Dashboard** — painel de vendas mensais com navegação por setas (mês anterior / próximo); resumo financeiro; extrato detalhado.
- **Tratamento de erros** — `GlobalExceptionHandler` com `@RestControllerAdvice`, exceções customizadas e resposta padronizada via `ApiError`.
- **Documentação** — Swagger/OpenAPI configurado via `OpenApiConfig`.

## Segurança

- JWT com role `ROLE_USER` hardcoded (todos os usuários são iguais, sem RBAC)
- Senhas hasheadas com BCrypt
- Credenciais externalizadas via variáveis de ambiente com fallback para dev
- Bean Validation em DTOs (`@Email`, `@Size`, `@Positive`, `@NotBlank`)
- Rate limiting por IP com Caffeine Cache em endpoints sensíveis
- Container Docker roda como non-root user (`appuser`)
- Verificação de email obrigatória no registro
- Tokens de senha com expiração de 1 hora e flag `utilizado` para uso único
- Prevenção de enumeração de contas no fluxo de recuperação de senha

## Testes

Testes unitários com JUnit 5, Mockito e AssertJ cobrindo:

- **AuthService** — login (credenciais válidas, inválidas, usuário inativo, email não cadastrado), solicitação de alteração de senha (senha correta, incorreta, nova igual à atual), confirmação de alteração (token válido, inválido, expirado), esqueci senha (email existente, inexistente), reset de senha (token válido, inválido, expirado)
- **ClienteService, ProdutoService, EstoqueService, LancamentoService, ContaPessoalService, SaldoService** — operações de CRUD, validações de negócio e edge cases

## Configuração

Todas as configurações são feitas via variáveis de ambiente:

| Variável | Descrição |
|---|---|
| `SPRING_DATASOURCE_URL` | URL de conexão do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta para assinatura dos tokens (mín. 32 caracteres) |
| `CORS_ORIGINS` | Origem(ns) permitida(s) para CORS (separadas por vírgula) |
| `MAIL_USERNAME` | Email Gmail para envio de verificação |
| `MAIL_PASSWORD` | Senha de app do Gmail (16 caracteres) |
| `APP_FRONTEND_URL` | URL do frontend (para links de verificação e reset de senha) |

## Deploy

Hospedado em VPS Linux (Hostinger KVM1, Ubuntu 24.04) com infraestrutura própria:

- **Backend** — Docker container com Spring Boot, atrás de Nginx como reverse proxy
- **Banco de dados** — PostgreSQL 16 em container Docker com volume persistente
- **SSL** — Certificado HTTPS via Let's Encrypt (Certbot) com subdomínio DuckDNS (`controlefinanceirolab.duckdns.org`)
- **Frontend** — Vercel (HTML/CSS/JS estático)
- **CI** — GitHub Actions com PostgreSQL 16 service container e JDK 17

### Subindo com Docker Compose

```bash
git clone https://github.com/eduardo-Ciudad/controle-financeiro-lab.git
cd controle-financeiro-lab
# Configure as variáveis de ambiente no docker-compose.yml
docker compose up -d --build
```

### Rodando localmente sem Docker

```bash
# Configure o application.properties com seu banco local
./mvnw spring-boot:run
```

## Endpoints principais

| Módulo | Base path | Operações |
|---|---|---|
| Auth | `/auth` | Registro (`POST /register`), login (`POST /login`), verificação (`GET /verificar`), reenvio (`POST /reenviar-verificacao`), alterar senha (`POST /alterar-senha`), confirmar alteração (`GET /confirmar-alteracao-senha`), esqueci senha (`POST /esqueci-senha`), resetar senha (`POST /resetar-senha`) |
| Clientes | `/clientes` | CRUD completo |
| Fornecedores | `/fornecedores` | CRUD completo |
| Produtos | `/produtos` | CRUD com preço de custo/venda |
| Estoque | `/estoque` | Movimentações de entrada/saída |
| Lançamentos | `/lancamentos` | Lançamentos gerais |
| Lançamentos Fornecedor | `/lancamentos-fornecedor` | Lançamentos de compra |
| Saldo | `/saldo` | Consulta de saldo por cliente |
| Extrato | `/extrato` | Extrato mensal agrupado |
| Dashboard | `/dashboard` | Resumo de vendas mensais |
| Resumo | `/resumo` | Visão geral financeira |

> Documentação interativa disponível em `/swagger-ui.html` após subir a aplicação.

## Autor

**Eduardo Ciudad** — Desenvolvedor Backend Java

- GitHub: [github.com/eduardo-Ciudad](https://github.com/eduardo-Ciudad)
- LinkedIn: [linkedin.com/in/eduardociudadf](https://linkedin.com/in/eduardociudadf/)
- Instagram: [@ciudad_dev](https://instagram.com/ciudad_dev)