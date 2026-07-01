package com.eduardo.financialcontrol.lancamento.dto;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record EstornoRequest(
        @PastOrPresent LocalDate dataCompetencia,
        String descricao
) {}
