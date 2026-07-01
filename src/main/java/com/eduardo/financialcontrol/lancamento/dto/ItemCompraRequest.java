package com.eduardo.financialcontrol.lancamento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemCompraRequest(
        @NotNull Long produtoId,
        @NotNull @Positive BigDecimal quantidade,
        @Positive BigDecimal precoUnitario
) {}
