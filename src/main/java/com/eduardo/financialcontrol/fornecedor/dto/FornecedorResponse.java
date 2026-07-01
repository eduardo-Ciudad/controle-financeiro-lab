package com.eduardo.financialcontrol.fornecedor.dto;

import com.eduardo.financialcontrol.fornecedor.Fornecedor;

import java.time.OffsetDateTime;

public record FornecedorResponse(
        Long id,
        String nome,
        String documento,
        String telefone,
        String email,
        String observacao,
        Boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm
) {
    public static FornecedorResponse de(Fornecedor f) {
        return new FornecedorResponse(
                f.getId(), f.getNome(), f.getDocumento(), f.getTelefone(),
                f.getEmail(), f.getObservacao(), f.getAtivo(),
                f.getCriadoEm(), f.getAtualizadoEm()
        );
    }
}
