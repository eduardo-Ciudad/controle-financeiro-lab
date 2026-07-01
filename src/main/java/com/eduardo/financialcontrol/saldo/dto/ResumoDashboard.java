package com.eduardo.financialcontrol.saldo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.util.List;

public record ResumoDashboard(
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalAReceber,
        long qtdDevedores,
        List<DevedorItem> topDevedores
) {
    public record DevedorItem(
            Long clienteId,
            String nome,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal saldo
    ) {}
}
