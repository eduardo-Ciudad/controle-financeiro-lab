package com.eduardo.financialcontrol.saldo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record ResumoMensalResponse(
        String mes,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal entradas,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal saidas
) {}
