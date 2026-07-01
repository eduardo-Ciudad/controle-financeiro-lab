CREATE TABLE lancamentos (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cliente_id        BIGINT        NOT NULL,
    natureza          VARCHAR(10)   NOT NULL,
    categoria         VARCHAR(20)   NOT NULL,
    valor             NUMERIC(15,2) NOT NULL,
    data_competencia  DATE          NOT NULL,
    descricao         VARCHAR(255),
    forma_pagamento   VARCHAR(20),
    estorno_de_id     BIGINT,
    criado_em         TIMESTAMPTZ   NOT NULL DEFAULT now(),

    CONSTRAINT fk_lanc_cliente  FOREIGN KEY (cliente_id)    REFERENCES clientes (id),
    CONSTRAINT fk_lanc_estorno  FOREIGN KEY (estorno_de_id) REFERENCES lancamentos (id),
    CONSTRAINT chk_valor_pos    CHECK (valor > 0),
    CONSTRAINT chk_natureza     CHECK (natureza  IN ('DEBITO','CREDITO')),
    CONSTRAINT chk_categoria    CHECK (categoria IN ('COMPRA','PAGAMENTO','ESTORNO','AJUSTE')),
    CONSTRAINT chk_nao_auto_est CHECK (estorno_de_id IS NULL OR estorno_de_id <> id)
);
CREATE INDEX idx_lanc_cliente ON lancamentos (cliente_id, data_competencia);
CREATE UNIQUE INDEX uk_lanc_estorno
    ON lancamentos (estorno_de_id) WHERE estorno_de_id IS NOT NULL;
