package com.eduardo.financialcontrol.fornecedor.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ItemCompraFornecedorRequest(
        @NotNull Long produtoId,
        @NotNull @Positive BigDecimal quantidade,
        @NotNull @Positive BigDecimal custoUnitario
) {}
