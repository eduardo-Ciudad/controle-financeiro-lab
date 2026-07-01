package com.eduardo.financialcontrol.estoque.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;

public record EstoqueResponse(
        Long produtoId,
        String nome,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal quantidadeAtual,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorEmEstoque
) {}
