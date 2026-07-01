CREATE TABLE movimentacao_estoque (
    id                    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    produto_id            BIGINT NOT NULL,
    tipo                  VARCHAR(10) NOT NULL,     -- ENTRADA | SAIDA
    quantidade            NUMERIC(15,3) NOT NULL,
    preco_unitario        NUMERIC(15,2) NOT NULL,   -- snapshot (venda na SAÍDA, custo na ENTRADA)
    origem                VARCHAR(20) NOT NULL,     -- VENDA | COMPRA | AJUSTE | ESTORNO
    lancamento_cliente_id BIGINT,
    data_competencia      DATE NOT NULL,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_mov_produto      FOREIGN KEY (produto_id) REFERENCES produtos(id),
    CONSTRAINT fk_mov_lanc_cliente FOREIGN KEY (lancamento_cliente_id) REFERENCES lancamentos(id),
    CONSTRAINT chk_mov_tipo   CHECK (tipo   IN ('ENTRADA','SAIDA')),
    CONSTRAINT chk_mov_origem CHECK (origem IN ('VENDA','COMPRA','AJUSTE','ESTORNO')),
    CONSTRAINT chk_mov_qtd    CHECK (quantidade > 0)
);
CREATE INDEX idx_mov_produto      ON movimentacao_estoque (produto_id);
CREATE INDEX idx_mov_lanc_cliente ON movimentacao_estoque (lancamento_cliente_id);
