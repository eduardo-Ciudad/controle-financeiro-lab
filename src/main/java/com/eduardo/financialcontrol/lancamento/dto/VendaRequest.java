package com.eduardo.financialcontrol.lancamento.dto;


import com.eduardo.financialcontrol.lancamento.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record VendaRequest(
        @NotNull Long clienteId,
        @NotNull Long produtoId,
        @NotNull @Positive BigDecimal quantidade,
        @Positive BigDecimal precoUnitario,
        FormaPagamento formaPagamento,
        @NotNull LocalDate dataCompetencia
) {}