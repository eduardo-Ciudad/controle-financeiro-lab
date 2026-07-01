package com.eduardo.financialcontrol.produto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoRequest(
        @NotBlank @Size(max = 160) String nome,
        @Size(max = 255) String descricao,
        @PositiveOrZero BigDecimal precoVenda,
        @PositiveOrZero BigDecimal precoCusto
) {}
