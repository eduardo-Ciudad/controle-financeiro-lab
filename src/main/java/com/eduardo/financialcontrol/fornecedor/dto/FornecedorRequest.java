package com.eduardo.financialcontrol.fornecedor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FornecedorRequest(
        @NotBlank @Size(max = 120) String nome,
        @Size(max = 18) String documento,
        @Size(max = 20) String telefone,
        @Size(max = 160) @Email String email,
        @Size(max = 500) String observacao) {}
