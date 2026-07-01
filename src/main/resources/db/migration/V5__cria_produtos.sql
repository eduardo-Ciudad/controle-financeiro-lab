CREATE TABLE produtos (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nome          VARCHAR(160) NOT NULL,
    descricao     VARCHAR(255),
    preco_venda   NUMERIC(15,2) NOT NULL,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMPTZ NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_produto_preco CHECK (preco_venda >= 0)
);