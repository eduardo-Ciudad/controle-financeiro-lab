package com.eduardo.financialcontrol.saldo.dto;

import com.eduardo.financialcontrol.saldo.SituacaoSaldo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record SaldoClienteResponse(
        Long clienteId,
        String nome,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal saldo,
        SituacaoSaldo situacao,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorAbsoluto
) {}
