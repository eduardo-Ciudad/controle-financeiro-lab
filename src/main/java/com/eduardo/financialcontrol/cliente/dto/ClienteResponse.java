package com.eduardo.financialcontrol.cliente.dto;

import com.eduardo.financialcontrol.cliente.Cliente;

import java.time.OffsetDateTime;

public record ClienteResponse(
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
    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(
                c.getId(), c.getNome(), c.getDocumento(), c.getTelefone(),
                c.getEmail(), c.getObservacao(), c.getAtivo(),
                c.getCriadoEm(), c.getAtualizadoEm()
        );
    }
}
