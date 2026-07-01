package com.eduardo.financialcontrol.fornecedor.dto;

import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record EstornoFornecedorRequest(
        @PastOrPresent LocalDate dataCompetencia,
        String descricao
) {}
