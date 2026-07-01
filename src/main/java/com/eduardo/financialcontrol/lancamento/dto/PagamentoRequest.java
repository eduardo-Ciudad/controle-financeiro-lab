package com.eduardo.financialcontrol.lancamento.dto;

import com.eduardo.financialcontrol.lancamento.FormaPagamento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PagamentoRequest(
        @NotNull @Positive BigDecimal valor,
        @NotNull @PastOrPresent LocalDate dataCompetencia,
        FormaPagamento formaPagamento,
        String descricao
) {}
