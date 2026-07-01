DROP INDEX uk_clientes_documento;
CREATE UNIQUE INDEX uk_clientes_documento ON clientes (documento) WHERE documento IS NOT NULL AND documento <> '';

DROP INDEX uk_fornecedores_documento;
CREATE UNIQUE INDEX uk_fornecedores_documento ON fornecedores (documento) WHERE documento IS NOT NULL AND documento <> '';