package com.eduardo.financialcontrol.estoque.dto;

import com.eduardo.financialcontrol.estoque.TipoMovimentacao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AjusteEstoqueRequest(
        @NotNull TipoMovimentacao tipo,
        @NotNull @Positive BigDecimal quantidade,
        String motivo
) {}
