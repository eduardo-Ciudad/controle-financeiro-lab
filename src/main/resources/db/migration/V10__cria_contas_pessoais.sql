CREATE TABLE contas_pessoais (
    id              BIGSERIAL     PRIMARY KEY,
    descricao       VARCHAR(255)  NOT NULL,
    valor           NUMERIC(15,2) NOT NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'PENDENTE',
    data_vencimento DATE          NOT NULL,
    data_pagamento  DATE,
    criado_em       TIMESTAMPTZ   NOT NULL DEFAULT now()
);