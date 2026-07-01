CREATE TABLE fornecedores (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    documento     VARCHAR(18),
    telefone      VARCHAR(20),
    email         VARCHAR(160),
    observacao    TEXT,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_fornecedores_documento ON fornecedores (documento) WHERE documento IS NOT NULL;

CREATE TABLE lancamentos_fornecedor (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fornecedor_id    BIGINT NOT NULL,
    natureza         VARCHAR(10) NOT NULL,
    categoria        VARCHAR(20) NOT NULL,
    valor            NUMERIC(15,2) NOT NULL,
    data_competencia DATE NOT NULL,
    descricao        VARCHAR(255),
    forma_pagamento  VARCHAR(20),
    estorno_de_id    BIGINT,
    criado_em        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_lancfor_fornecedor FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id),
    CONSTRAINT fk_lancfor_estorno    FOREIGN KEY (estorno_de_id) REFERENCES lancamentos_fornecedor(id),
    CONSTRAINT chk_lancfor_valor     CHECK (valor > 0),
    CONSTRAINT chk_lancfor_natureza  CHECK (natureza  IN ('DEBITO','CREDITO')),
    CONSTRAINT chk_lancfor_categoria CHECK (categoria IN ('COMPRA','PAGAMENTO','ESTORNO','AJUSTE')),
    CONSTRAINT chk_lancfor_auto_est  CHECK (estorno_de_id IS NULL OR estorno_de_id <> id)
);
CREATE INDEX idx_lancfor_fornecedor ON lancamentos_fornecedor (fornecedor_id, data_competencia);
CREATE UNIQUE INDEX uk_lancfor_estorno ON lancamentos_fornecedor (estorno_de_id) WHERE estorno_de_id IS NOT NULL;
