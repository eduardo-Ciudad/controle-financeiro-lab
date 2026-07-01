package com.eduardo.financialcontrol.fornecedor.dto;

import com.eduardo.financialcontrol.lancamento.Categoria;
import com.eduardo.financialcontrol.lancamento.FormaPagamento;
import com.eduardo.financialcontrol.lancamento.Natureza;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record LinhaExtratoFornecedor(
        Long id,
        Natureza natureza,
        Categoria categoria,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valor,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal saldoAcumulado,
        LocalDate dataCompetencia,
        String descricao,
        FormaPagamento formaPagamento,
        OffsetDateTime criadoEm
) {}
