package com.eduardo.financialcontrol.pessoal.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelamentoRequest(
        @NotBlank String descricao,
        @NotNull @Positive BigDecimal valorTotal,
        @NotNull @Min(2) @Max(48) Integer quantidadeParcelas,
        @NotNull LocalDate dataVencimentoPrimeira
) {}
