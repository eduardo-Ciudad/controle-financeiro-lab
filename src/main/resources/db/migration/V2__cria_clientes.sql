CREATE TABLE clientes (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    documento     VARCHAR(18),
    telefone      VARCHAR(20),
    email         VARCHAR(160),
    observacao    TEXT,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uk_clientes_documento
    ON clientes (documento) WHERE documento IS NOT NULL;
