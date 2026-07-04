ALTER TABLE usuarios ADD COLUMN email_verificado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN token_verificacao VARCHAR(64);
ALTER TABLE usuarios ADD COLUMN token_verificacao_expira TIMESTAMP WITH TIME ZONE;