ALTER TABLE movimentacao_estoque ADD COLUMN lancamento_fornecedor_id BIGINT;
ALTER TABLE movimentacao_estoque
    ADD CONSTRAINT fk_mov_lanc_fornecedor
    FOREIGN KEY (lancamento_fornecedor_id) REFERENCES lancamentos_fornecedor(id);
CREATE INDEX idx_mov_lanc_fornecedor ON movimentacao_estoque (lancamento_fornecedor_id);
