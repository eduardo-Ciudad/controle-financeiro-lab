package com.eduardo.financialcontrol.fornecedor.dto;

import com.eduardo.financialcontrol.saldo.SituacaoSaldo;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record SaldoFornecedorResponse(
        Long fornecedorId,
        String nome,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal saldo,
        SituacaoSaldo situacao,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorAbsoluto
) {}
