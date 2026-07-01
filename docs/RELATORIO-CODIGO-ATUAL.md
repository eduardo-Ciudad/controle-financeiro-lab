# Relatório do Estado Atual do Código
**Data:** 2026-06-15  
**Branch:** main  
**Último commit:** `4f7969e` – docs: mudei as variaveis de ambiente

---

## 1. Árvore de Packages/Classes

```
com.eduardo.financialcontrol
│
├── FinancialcontrolApplication          (entry point)
│
├── auth/
│   ├── AuthController                   (@RestController /auth)
│   ├── AuthService                      (@Service)
│   ├── BootstrapUsuario                 (@Component – cria usuário admin na startup)
│   ├── Usuario                          (@Entity → tabela usuarios)
│   ├── UsuarioRepository                (JpaRepository<Usuario, Long>)
│   └── dto/
│       ├── LoginRequest                 (record)
│       └── TokenResponse                (record)
│
├── cliente/
│   ├── Cliente                          (@Entity → tabela clientes)
│   ├── ClienteController                (@RestController /clientes)
│   ├── ClienteRepository                (JpaRepository + 3 queries custom)
│   ├── ClienteService                   (@Service)
│   └── dto/
│       ├── ClienteRequest               (record)
│       └── ClienteResponse              (record)
│
├── lancamento/
│   ├── Lancamento                       (@Entity → tabela lancamentos)
│   ├── LancamentoController             (@RestController – paths mistos)
│   ├── LancamentoRepository             (JpaRepository + 3 queries custom)
│   ├── LancamentoService                (@Service)
│   ├── Natureza                         (enum: DEBITO, CREDITO)
│   ├── Categoria                        (enum: COMPRA, PAGAMENTO, ESTORNO, AJUSTE)
│   ├── FormaPagamento                   (enum: DINHEIRO, PIX, CARTAO, BOLETO, OUTRO)
│   └── dto/
│       ├── CompraRequest                (record)
│       ├── PagamentoRequest             (record)
│       ├── EstornoRequest               (record)
│       └── LancamentoResponse           (record + factory de(Lancamento))
│
├── saldo/
│   ├── DashboardController              (@RestController /dashboard)
│   ├── ExtratoController                (@RestController /clientes/{id}/extrato)
│   ├── SaldoController                  (@RestController /clientes/{id}/saldo)
│   ├── SaldoService                     (@Service)
│   ├── SituacaoSaldo                    (enum: DEVEDOR, CREDOR, QUITADO)
│   └── dto/
│       ├── LinhaExtrato                 (record)
│       ├── ResumoDashboard              (record + nested DevedorItem)
│       └── SaldoClienteResponse         (record)
│
├── config/
│   ├── CorsConfig                       (@Configuration)
│   ├── OpenApiConfig                    (@Configuration – Swagger)
│   └── RateLimitConfig                  (@Configuration – Bucket4j, rate limit por IP)
│
├── security/
│   ├── JwtFilter                        (OncePerRequestFilter)
│   ├── JwtService                       (@Service – geração/validação JWT)
│   └── SecurityConfig                   (@Configuration – Spring Security stateless)
│
└── shared/
    ├── dto/
    │   └── ApiError                     (record com nested FieldError)
    └── exception/
        ├── GlobalExceptionHandler       (@RestControllerAdvice)
        ├── RecursoNaoEncontradoException (→ HTTP 404)
        └── RegraDeNegocioException      (→ HTTP 422)
```

---

## 2. Contratos dos Endpoints

Todos os endpoints (exceto `/auth/login` e Swagger/Actuator) exigem `Authorization: Bearer <token>`.

### Auth

| Método | Path | Request body | Response body | Status |
|--------|------|-------------|---------------|--------|
| POST | `/auth/login` | `{ email, senha }` | `{ token, expiraEm }` | 200 / 401 / 429 |

**LoginRequest:**
```json
{ "email": "string (@Email, @NotBlank)", "senha": "string (@NotBlank)" }
```
**TokenResponse:**
```json
{ "token": "string", "expiraEm": "OffsetDateTime (ISO-8601)" }
```
Rate limit: 10 req/min por IP (Bucket4j). Excesso → 429.

---

### Clientes

| Método | Path | Params / Body | Response | Status |
|--------|------|--------------|----------|--------|
| POST | `/clientes` | body: ClienteRequest | ClienteResponse | 201 + Location |
| GET | `/clientes` | query: `nome` (opt), `comDivida` (opt bool), paginação Spring | Page\<ClienteResponse\> | 200 |
| GET | `/clientes/{id}` | – | ClienteResponse | 200 / 404 |
| PUT | `/clientes/{id}` | body: ClienteRequest | ClienteResponse | 200 / 404 / 422 |
| DELETE | `/clientes/{id}` | – | (vazio) | 204 / 404 / 422 |

**ClienteRequest:**
```json
{
  "nome":       "string (NotBlank, max 120)",
  "documento":  "string|null (max 18)",
  "telefone":   "string|null (max 20)",
  "email":      "string|null (max 160)",
  "observacao": "string|null"
}
```
**ClienteResponse:**
```json
{
  "id": 1,
  "nome": "string",
  "documento": "string|null",
  "telefone":  "string|null",
  "email":     "string|null",
  "observacao":"string|null",
  "ativo":     true,
  "criadoEm":  "OffsetDateTime",
  "atualizadoEm": "OffsetDateTime"
}
```

---

### Lançamentos

| Método | Path | Request body | Response body | Status |
|--------|------|-------------|---------------|--------|
| POST | `/clientes/{id}/compras` | CompraRequest | LancamentoResponse | 201 + Location |
| POST | `/clientes/{id}/pagamentos` | PagamentoRequest | LancamentoResponse | 201 + Location |
| POST | `/lancamentos/{id}/estornos` | EstornoRequest | LancamentoResponse | 201 + Location |
| GET  | `/lancamentos/{id}` | – | LancamentoResponse | 200 / 404 |

**CompraRequest:**
```json
{
  "valor":           "BigDecimal (NotNull, Positive)",
  "dataCompetencia": "LocalDate (NotNull, PastOrPresent)",
  "descricao":       "string|null"
}
```
**PagamentoRequest:**
```json
{
  "valor":           "BigDecimal (NotNull, Positive)",
  "dataCompetencia": "LocalDate (NotNull, PastOrPresent)",
  "formaPagamento":  "DINHEIRO|PIX|CARTAO|BOLETO|OUTRO|null",
  "descricao":       "string|null"
}
```
**EstornoRequest:**
```json
{
  "dataCompetencia": "LocalDate|null (PastOrPresent)",
  "descricao":       "string|null"
}
```
**LancamentoResponse:**
```json
{
  "id":              1,
  "clienteId":       1,
  "natureza":        "DEBITO|CREDITO",
  "categoria":       "COMPRA|PAGAMENTO|ESTORNO|AJUSTE",
  "valor":           "string numérico",
  "dataCompetencia": "LocalDate",
  "descricao":       "string|null",
  "formaPagamento":  "enum|null",
  "estornoDeId":     "Long|null",
  "criadoEm":        "OffsetDateTime"
}
```

---

### Saldo / Extrato / Dashboard

| Método | Path | Params | Response | Status |
|--------|------|--------|----------|--------|
| GET | `/clientes/{id}/saldo` | – | SaldoClienteResponse | 200 / 404 |
| GET | `/clientes/{id}/extrato` | paginação Spring | Page\<LinhaExtrato\> | 200 / 404 |
| GET | `/dashboard` | – | ResumoDashboard | 200 |

**SaldoClienteResponse:**
```json
{
  "clienteId":    1,
  "nome":         "string",
  "saldo":        "string numérico",
  "situacao":     "DEVEDOR|CREDOR|QUITADO",
  "valorAbsoluto":"string numérico"
}
```
**LinhaExtrato:**
```json
{
  "id":              1,
  "natureza":        "DEBITO|CREDITO",
  "categoria":       "COMPRA|PAGAMENTO|ESTORNO|AJUSTE",
  "valor":           "string numérico",
  "saldoAcumulado":  "string numérico",
  "dataCompetencia": "LocalDate",
  "descricao":       "string|null",
  "formaPagamento":  "enum|null",
  "criadoEm":        "OffsetDateTime"
}
```
**ResumoDashboard:**
```json
{
  "totalAReceber": "string numérico",
  "qtdDevedores":  2,
  "topDevedores": [
    { "clienteId": 1, "nome": "string", "saldo": "string numérico" }
  ]
}
```

---

## 3. Contratos Exatos das Classes Core

### 3.1 Entidade `Cliente` (tabela `clientes`)

| Campo Java | Coluna DB | Tipo Java | Nullable | Default |
|-----------|-----------|-----------|----------|---------|
| `id` | `id` | `Long` | não | GENERATED ALWAYS AS IDENTITY |
| `nome` | `nome` | `String` | não | – |
| `documento` | `documento` | `String` | sim | – |
| `telefone` | `telefone` | `String` | sim | – |
| `email` | `email` | `String` | sim | – |
| `observacao` | `observacao` | `String` | sim | – |
| `ativo` | `ativo` | `Boolean` | não | `true` |
| `criadoEm` | `criado_em` | `OffsetDateTime` | não | `OffsetDateTime.now()` |
| `atualizadoEm` | `atualizado_em` | `OffsetDateTime` | não | `OffsetDateTime.now()` |

`@PreUpdate` atualiza `atualizadoEm` automaticamente.

---

### 3.2 Entidade `Lancamento` (tabela `lancamentos`)

| Campo Java | Coluna DB | Tipo Java | Nullable |
|-----------|-----------|-----------|----------|
| `id` | `id` | `Long` | não |
| `cliente` | `cliente_id` | `Cliente` (@ManyToOne LAZY) | não |
| `natureza` | `natureza` | `Natureza` (enum STRING) | não |
| `categoria` | `categoria` | `Categoria` (enum STRING) | não |
| `valor` | `valor` | `BigDecimal` (15,2) | não |
| `dataCompetencia` | `data_competencia` | `LocalDate` | não |
| `descricao` | `descricao` | `String` | sim |
| `formaPagamento` | `forma_pagamento` | `FormaPagamento` (enum STRING) | sim |
| `estornoDe` | `estorno_de_id` | `Lancamento` (@ManyToOne LAZY, auto-ref) | sim |
| `criadoEm` | `criado_em` | `OffsetDateTime` | não |

---

### 3.3 DTOs

**`CompraRequest`** (record):
```java
record CompraRequest(
    @NotNull @Positive BigDecimal valor,
    @NotNull @PastOrPresent LocalDate dataCompetencia,
    String descricao
)
```

**`PagamentoRequest`** (record):
```java
record PagamentoRequest(
    @NotNull @Positive BigDecimal valor,
    @NotNull @PastOrPresent LocalDate dataCompetencia,
    FormaPagamento formaPagamento,   // nullable, sem @NotNull
    String descricao
)
```

**`EstornoRequest`** (record):
```java
record EstornoRequest(
    @PastOrPresent LocalDate dataCompetencia,  // nullable, sem @NotNull
    String descricao
)
```

**`LancamentoResponse`** (record):
```java
record LancamentoResponse(
    Long id,
    Long clienteId,
    Natureza natureza,
    Categoria categoria,
    @JsonFormat(STRING) BigDecimal valor,
    LocalDate dataCompetencia,
    String descricao,
    FormaPagamento formaPagamento,
    Long estornoDeId,
    OffsetDateTime criadoEm
)
```

---

### 3.4 Métodos de `LancamentoService`

```java
// Cria lançamento DEBITO/COMPRA para o cliente
@Transactional
LancamentoResponse registrarCompra(Long clienteId, CompraRequest request)

// Cria lançamento CREDITO/PAGAMENTO para o cliente
@Transactional
LancamentoResponse registrarPagamento(Long clienteId, PagamentoRequest request)

// Cria lançamento de ESTORNO (natureza oposta ao original, categoria=ESTORNO)
// Guarda ponteiro estornoDe → original
// Valida: não estornar ESTORNO; não estornar se já há estorno
// Se dataCompetencia null → usa LocalDate.now()
// Se descricao null → "Estorno de lançamento #<id>"
@Transactional
LancamentoResponse estornar(Long lancamentoId, EstornoRequest request)

// Busca por id ou lança RecursoNaoEncontradoException
@Transactional(readOnly=true)
LancamentoResponse buscarPorId(Long id)
```

---

### 3.5 Métodos de `SaldoService`

```java
// Retorna saldo devedor do cliente + SituacaoSaldo
// Fórmula: SUM(DEBITO) - SUM(CREDITO) via JPQL
@Transactional(readOnly=true)
SaldoClienteResponse saldoCliente(Long clienteId)

// Retorna Page<LinhaExtrato> ordenado por dataCompetencia ASC, id ASC
// saldoAcumulado calculado linha a linha em memória (não usa a view V4)
@Transactional(readOnly=true)
Page<LinhaExtrato> extrato(Long clienteId, Pageable pageable)

// Retorna totais globais: somente clientes ativos com saldo > 0
// topDevedores ordenado por saldo DESC
@Transactional(readOnly=true)
ResumoDashboard dashboard()
```

---

## 4. Migrations Flyway

| Versão | Arquivo | O que cria |
|--------|---------|------------|
| V1 | `V1__cria_usuarios.sql` | Tabela `usuarios` (id, nome, email, senha_hash, ativo, criado_em) |
| V2 | `V2__cria_clientes.sql` | Tabela `clientes` + índice único parcial em `documento` |
| V3 | `V3__cria_lancamentos.sql` | Tabela `lancamentos` + FK para clientes/auto-ref + CHECK constraints + índices |
| V4 | `V4__cria_view_saldo.sql` | View `vw_saldo_cliente` (saldo_devedor por cliente) |

**Próximo número livre: V5**

Restrições de banco em V3 que merecem atenção:
- `chk_natureza`: apenas `'DEBITO'` ou `'CREDITO'`
- `chk_categoria`: apenas `'COMPRA'`, `'PAGAMENTO'`, `'ESTORNO'`, `'AJUSTE'`
- `chk_valor_pos`: `valor > 0`
- `chk_nao_auto_est`: `estorno_de_id IS NULL OR estorno_de_id <> id`
- `uk_lanc_estorno`: índice único parcial — cada lançamento pode ser estornado no máximo uma vez

---

## 5. Divergências Observadas

### 5.1 View `vw_saldo_cliente` (V4) não é usada pelo código Java
A migration V4 cria a view `vw_saldo_cliente`, mas `SaldoService` e `LancamentoRepository` calculam o saldo via JPQL (`calcularSaldo`), nunca consultando a view. A view existe no banco sem uso pela aplicação.

### 5.2 Categoria `AJUSTE` existe mas não tem endpoint
O enum `Categoria` inclui `AJUSTE` (confirmado também no `chk_categoria` do banco), porém não há controller nem método de serviço para criar lançamentos de ajuste. A categoria é declarada mas sem caminho de entrada via API.

### 5.3 `comDivida=false` lista TODOS os ativos, não "sem dívida"
Em `GET /clientes?comDivida=false`, o código chama `buscarAtivos(nome, pageable)` que retorna todos os clientes ativos (independente de terem dívida ou não). A semântica esperada pelo parâmetro seria "sem dívida", mas o comportamento real é "sem filtro de dívida".

### 5.4 `encontrarOuLancar` rejeita clientes inativos como 404
`ClienteService.encontrarOuLancar` aplica `.filter(c -> Boolean.TRUE.equals(c.getAtivo()))`, então operações de compra/pagamento/estorno sobre um cliente inativo retornam 404 em vez de um erro mais descritivo (ex.: 422 "cliente inativo").

### 5.5 `saldoAcumulado` no extrato usa lógica invertida do esperado
Em `SaldoService.extrato()`:
- DEBITO → `acumulado = acumulado.add(valor)` (aumenta o saldo devedor)
- CREDITO → `acumulado = acumulado.subtract(valor)` (diminui o saldo devedor)

O `saldoAcumulado` da linha representa o **saldo devedor acumulado** até aquele lançamento, não o "saldo do cliente" convencional (que seria o inverso). Isso é consistente com a visão do credor, mas pode ser contraintuitivo para o consumidor da API.

### 5.6 `inativar` bloqueia clientes com saldo CREDOR (negativo)
`ClienteService.inativar` lança exceção quando `saldo != 0`. Isso bloqueia a inativação tanto de devedores (`saldo > 0`) quanto de credores (`saldo < 0`). Pode ser intencional, mas o teste cobre apenas o caso positivo.

---

## 6. Status dos Testes

Todos os testes são **unitários com Mockito** (`@ExtendWith(MockitoExtension.class)`). Não há testes de integração, testes de controller (MockMvc), nem testes de repositório.

### `AuthServiceTest` (4 testes)
| Cenário | Coberto? |
|---------|---------|
| Login com credenciais válidas | ✅ |
| Login com senha errada | ✅ |
| Login com usuário inativo | ✅ |
| Login com e-mail não cadastrado | ✅ |
| Rate limit (Bucket4j) | ❌ não testado |

### `ClienteServiceTest` (5 testes)
| Cenário | Coberto? |
|---------|---------|
| Criar cliente válido | ✅ |
| Criar com documento duplicado | ✅ |
| Buscar por id inexistente | ✅ |
| Inativar com saldo zero | ✅ |
| Inativar com saldo positivo (devedor) | ✅ |
| Atualizar cliente | ❌ não testado |
| Listar com filtros (`nome`, `comDivida`) | ❌ não testado |
| Inativar com saldo negativo (credor) | ❌ não testado |

### `LancamentoServiceTest` (5 testes)
| Cenário | Coberto? |
|---------|---------|
| Registrar compra (DEBITO/COMPRA) | ✅ |
| Registrar pagamento (CREDITO/PAGAMENTO) | ✅ |
| Estornar lançamento já estornado | ✅ |
| Estornar um ESTORNO | ✅ |
| Estornar lançamento válido | ✅ |
| Buscar por id inexistente | ✅ |
| Estorno com `dataCompetencia null` (usa hoje) | ❌ não testado |
| Estorno com `descricao null` (usa default) | ❌ não testado |

### `SaldoServiceTest` (4 testes)
| Cenário | Coberto? |
|---------|---------|
| Saldo positivo → DEVEDOR | ✅ |
| Saldo negativo → CREDOR | ✅ |
| Saldo zero → QUITADO | ✅ |
| Dashboard com múltiplos devedores | ✅ |
| Extrato com saldo acumulado | ✅ |
| Dashboard com clientes inativos excluídos | ❌ não testado |
| Extrato vazio | ❌ não testado |

### `FinancialcontrolApplicationTests`
Apenas smoke test de context load (`@SpringBootTest`).

### Camadas sem nenhum teste
- Controllers (`AuthController`, `ClienteController`, `LancamentoController`, `SaldoController`, `ExtratoController`, `DashboardController`)
- `JwtService` / `JwtFilter`
- `GlobalExceptionHandler`
- `ClienteRepository` (queries custom JPQL)
- `LancamentoRepository` (query JPQL `calcularSaldo`, paginação)
- `BootstrapUsuario`
