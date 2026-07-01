-- Adiciona coluna role à tabela usuarios
-- Usuários existentes recebem 'ADMIN' (o bootstrap só cria admin)
ALTER TABLE usuarios ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Atualiza o admin existente
UPDATE usuarios SET role = 'ADMIN' WHERE id = 1;
UPDATE usuarios SET role = 'ADMIN' WHERE id = 2;