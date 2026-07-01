-- V14__add_usuario_id_multi_tenant.sql

-- clientes
ALTER TABLE clientes ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE clientes ADD CONSTRAINT fk_clientes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
CREATE INDEX idx_clientes_usuario ON clientes (usuario_id);
DROP INDEX uk_clientes_documento;
CREATE UNIQUE INDEX uk_clientes_documento ON clientes (usuario_id, documento) WHERE documento IS NOT NULL;

-- lancamentos
ALTER TABLE lancamentos ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE lancamentos ADD CONSTRAINT fk_lancamentos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
CREATE INDEX idx_lancamentos_usuario ON lancamentos (usuario_id);

-- fornecedores
ALTER TABLE fornecedores ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE fornecedores ADD CONSTRAINT fk_fornecedores_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
CREATE INDEX idx_fornecedores_usuario ON fornecedores (usuario_id);
DROP INDEX uk_fornecedores_documento;
CREATE UNIQUE INDEX uk_fornecedores_documento ON fornecedores (usuario_id, documento) WHERE documento IS NOT NULL;

-- lancamentos_fornecedor
ALTER TABLE lancamentos_fornecedor ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE lancamentos_fornecedor ADD CONSTRAINT fk_lancfor_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
CREATE INDEX idx_lancfor_usuario ON lancamentos_fornecedor (usuario_id);

-- produtos
ALTER TABLE produtos ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE produtos ADD CONSTRAINT fk_produtos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
CREATE INDEX idx_produtos_usuario ON produtos (usuario_id);

-- movimentacao_estoque
ALTER TABLE movimentacao_estoque ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE movimentacao_estoque ADD CONSTRAINT fk_mov_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
CREATE INDEX idx_mov_usuario ON movimentacao_estoque (usuario_id);

-- contas_pessoais
ALTER TABLE contas_pessoais ADD COLUMN usuario_id BIGINT NOT NULL;
ALTER TABLE contas_pessoais ADD CONSTRAINT fk_contas_pessoais_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id);
CREATE INDEX idx_contas_pessoais_usuario ON contas_pessoais (usuario_id);

-- recriar view com usuario_id
DROP VIEW vw_saldo_cliente;
CREATE VIEW vw_saldo_cliente AS
SELECT c.usuario_id,
       c.id AS cliente_id,
       c.nome,
       COALESCE(SUM(l.valor) FILTER (WHERE l.natureza = 'DEBITO'),  0)
     - COALESCE(SUM(l.valor) FILTER (WHERE l.natureza = 'CREDITO'), 0) AS saldo_devedor
FROM clientes c
LEFT JOIN lancamentos l ON l.cliente_id = c.id
GROUP BY c.usuario_id, c.id, c.nome;