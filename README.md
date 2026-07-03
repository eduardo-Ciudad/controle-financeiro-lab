# Financial Control Lab — Backend Multi-Tenant

Sistema de gestão financeira multi-tenant evolução do projeto freelance original. Cada usuário se cadastra e tem acesso isolado aos seus próprios dados. Controla lançamentos de clientes e fornecedores, estoque de produtos, extratos mensais e dashboard de vendas com autenticação JWT e arquitetura append-only para integridade dos dados.

## Tech Stack

- **Java 17** + **Spring Boot 3**
- **Spring Security** + JWT
- **PostgreSQL 16**
- **Flyway** — 14 migrações versionadas
- **Docker** — multi-stage build (`eclipse-temurin:17`)
- **Nginx** — reverse proxy
- **Let's Encrypt** — certificado SSL/TLS
- **Swagger / OpenAPI** — documentação interativa

## Arquitetura

```
com.eduardo.financialcontrol
├── auth/            # Registro, login, JWT
├── cliente/         # CRUD de clientes + lançamentos (conta-corrente)
├── fornecedor/      # CRUD de fornecedores + lançamentos de compra
├── lancamento/      # Lançamentos gerais (categoria, natureza, forma de pagamento)
├── produto/         # Catálogo com preço de custo / venda (margem 30%)
├── estoque/         # Movimentações de entrada/saída, decremento integrado em vendas
├── saldo/           # Dashboard, extrato mensal, resumo e cálculo de saldo
├── config/          # CORS, OpenAPI
├── security/        # JwtFilter, JwtService, SecurityConfig
└── shared/          # GlobalExceptionHandler, exceções customizadas, ApiError DTO
```

### Decisões técnicas

- **Multi-tenant por usuário** — cada usuário tem acesso isolado aos seus dados. Todas as queries filtram por `usuario_id`, garantindo que nenhum usuário acesse dados de outro.
- **Append-only ledger** — lançamentos nunca são editados ou deletados; correções são feitas via estorno (novo lançamento inverso), garantindo rastreabilidade completa.
- **Saldo computado** — o saldo é calculado em tempo real via `SUM` sobre os lançamentos, nunca armazenado diretamente. Isso elimina inconsistências entre saldo e histórico.
- **Package-by-feature** — cada domínio (cliente, fornecedor, produto, etc.) é um pacote isolado com sua própria camada de controller, service, repository e DTOs.

## Funcionalidades

- **Autenticação** — registro e login com JWT. Qualquer pessoa se cadastra e tem seu próprio espaço isolado.
- **Clientes** — cadastro completo com lançamentos no modelo conta-corrente; extrato agrupado por mês; coluna de saldo com indicador visual.
- **Fornecedores** — cadastro + lançamentos de compra com preço unitário customizado por operação.
- **Produtos** — catálogo com `precoCusto` e `precoVenda`, fórmula de margem de 30%.
- **Estoque** — movimentações de entrada e saída; decremento automático integrado ao registrar vendas; enums `TipoMovimentacao` e `OrigemMovimentacao` para rastreabilidade.
- **Dashboard** — painel de vendas mensais com navegação por setas (mês anterior / próximo); resumo financeiro; extrato detalhado.
- **Tratamento de erros** — `GlobalExceptionHandler` com `@RestControllerAdvice`, exceções customizadas e resposta padronizada via `ApiError`.
- **Documentação** — Swagger/OpenAPI configurado via `OpenApiConfig`.

## Configuração

Todas as configurações são feitas via variáveis de ambiente no `docker-compose.yml`:

| Variável | Descrição |
|---|---|
| `SPRING_DATASOURCE_URL` | URL de conexão do PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco |
| `JWT_SECRET` | Chave secreta para assinatura dos tokens |
| `CORS_ORIGINS` | Origem(ns) permitida(s) para CORS (separadas por vírgula) |

## Deploy

Hospedado em VPS Linux (Ubuntu 24.04) com infraestrutura própria:

- **Backend** — Docker container com Spring Boot, atrás de Nginx como reverse proxy
- **Banco de dados** — PostgreSQL 16 em container Docker com volume persistente
- **SSL** — Certificado HTTPS via Let's Encrypt (Certbot)
- **Frontend** — Vercel (HTML/CSS/JS estático)

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
| Auth | `/auth` | Registro (`POST /auth/register`), login (`POST /auth/login`) |
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
- LinkedIn: [linkedin.com/in/eduardociudadf](https://linkedin.com/in/eduardociudadf/)- 