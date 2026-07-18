-- V16__create_password_tokens.sql

CREATE TABLE password_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token VARCHAR(64) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    nova_senha_hash VARCHAR(255),
    expiracao TIMESTAMP WITH TIME ZONE NOT NULL,
    utilizado BOOLEAN DEFAULT FALSE,
    criado_em TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_password_token_usuario
        FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);