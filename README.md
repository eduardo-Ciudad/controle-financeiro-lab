# Financial Control — Backend

Sistema de gestão financeira desenvolvido sob demanda para uma fornecedora de tecidos. Controla lançamentos de clientes e fornecedores, estoque de produtos, extratos mensais e dashboard de vendas — tudo com autenticação JWT e arquitetura append-only para integridade dos dados.

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **Spring Security** + JWT (access & refresh token)
- **PostgreSQL** (Neon / Supabase / Render)
- **Flyway** — migrações versionadas
- **Bucket4j** — rate limiting
- **Swagger / OpenAPI** — documentação interativa
- **Docker** — multi-stage build (`eclipse-temurin:17`)

## Arquitetura

```
com.eduardo.financialcontrol
├── auth/            # Registro, login, JWT, seed do admin (CommandLineRunner)
├── cliente/         # CRUD de clientes + lançamentos (conta-corrente)
├── fornecedor/      # CRUD de fornecedores + lançamentos de compra
├── lancamento/      # Lançamentos gerais (categoria, natureza, forma de pagamento)
├── produto/         # Catálogo com preço de custo / venda (margem 30%)
├── estoque/         # Movimentações de entrada/saída, decremento integrado em vendas
├── saldo/           # Dashboard, extrato mensal, resumo e cálculo de saldo
├── config/          # CORS, OpenAPI, Rate Limit
├── security/        # JwtFilter, JwtService, SecurityConfig
└── shared/          # GlobalExceptionHandler, exceções customizadas, ApiError DTO
```

### Decisões técnicas

- **Append-only ledger** — lançamentos nunca são editados ou deletados; correções são feitas via estorno (novo lançamento inverso), garantindo rastreabilidade completa.
- **Saldo computado** — o saldo é calculado em tempo real via `SUM` sobre os lançamentos, nunca armazenado diretamente. Isso elimina inconsistências entre saldo e histórico.
- **Package-by-feature** — cada domínio (cliente, fornecedor, produto, etc.) é um pacote isolado com sua própria camada de controller, service, repository e DTOs.

## Funcionalidades

- **Autenticação e autorização** — registro, login, JWT com refresh token, seed de admin no primeiro boot via `BootstrapUsuario` (CommandLineRunner).
- **Clientes** — cadastro completo com lançamentos no modelo conta-corrente (Modelo B); extrato agrupado por mês; coluna de saldo com indicador visual (vermelho/azul/verde).
- **Fornecedores** — cadastro + lançamentos de compra com preço unitário customizado por operação.
- **Produtos** — catálogo com `precoCusto` e `precoVenda`, fórmula de margem de 30%.
- **Estoque** — movimentações de entrada e saída; decremento automático integrado ao registrar vendas; enums `TipoMovimentacao` e `OrigemMovimentacao` para rastreabilidade.
- **Dashboard** — painel de vendas mensais com navegação por setas (mês anterior / próximo); resumo financeiro; extrato detalhado.
- **Tratamento de erros** — `GlobalExceptionHandler` com `@RestControllerAdvice`, exceções customizadas (`RecursoNaoEncontradoException`, `RegraDeNegocioException`) e resposta padronizada via `ApiError`.
- **Rate limiting** — proteção contra abuso via Bucket4j.
- **Documentação** — Swagger/OpenAPI configurado via `OpenApiConfig`.

## Pré-requisitos

- Java 17+
- Maven
- PostgreSQL (local ou cloud)
- Docker (opcional, para build containerizado)

## Configuração

Todas as configurações são feitas via variáveis de ambiente:

| Variável | Descrição |
|---|---|
| `DATABASE_URL` | URL de conexão do PostgreSQL |
| `DATABASE_USERNAME` | Usuário do banco |
| `DATABASE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta para assinatura dos tokens |
| `CORS_ORIGINS` | Origem(ns) permitida(s) para CORS (ex: URL do Vercel) |

## Rodando localmente

### Com Docker Compose

```bash
docker-compose up -d
```

### Sem Docker

```bash
# 1. Configure as variáveis de ambiente
export DATABASE_URL=jdbc:postgresql://localhost:5432/financialcontrol
export DATABASE_USERNAME=postgres
export DATABASE_PASSWORD=sua_senha
export JWT_SECRET=sua_chave_secreta
export CORS_ORIGINS=http://localhost:5500

# 2. Rode a aplicação
./mvnw spring-boot:run
```

No primeiro boot, o `BootstrapUsuario` cria automaticamente o usuário administrador.

## Deploy

- **Backend** — Render (Docker, imagem `eclipse-temurin:17`)
- **Frontend** — Vercel (HTML/CSS/JS estático)

JVM configurada para ambientes com memória limitada:

```
JAVA_OPTS=-Xmx512m -Xms256m
```

> Após o deploy, defina `CORS_ORIGINS` com a URL do Vercel para liberar as requisições do frontend.

## Endpoints principais

| Módulo | Base path | Operações |
|---|---|---|
| Auth | `/auth` | Registro, login, refresh token |
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
- 