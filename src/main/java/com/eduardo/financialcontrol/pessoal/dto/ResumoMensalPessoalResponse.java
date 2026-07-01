package com.eduardo.financialcontrol.pessoal.dto;

import java.math.BigDecimal;

public record ResumoMensalPessoalResponse(
        String mes,
        BigDecimal totalMes,
        BigDecimal totalPago,
        BigDecimal totalPendente,
        int quantidadeTotal,
        int quantidadePaga,
        int quantidadePendente
) {

}