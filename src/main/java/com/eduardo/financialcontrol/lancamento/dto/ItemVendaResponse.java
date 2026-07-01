package com.eduardo.financialcontrol.lancamento.dto;

import com.eduardo.financialcontrol.estoque.MovimentacaoEstoque;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ItemVendaResponse(
        Long produtoId,
        String nomeProduto,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal quantidade,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precoUnitario,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precoCusto,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal subtotal
) {
    public static ItemVendaResponse de(MovimentacaoEstoque m) {
        BigDecimal subtotal = m.getQuantidade().multiply(m.getPrecoUnitario()).setScale(2, RoundingMode.HALF_UP);
        return new ItemVendaResponse(
                m.getProduto().getId(),
                m.getProduto().getNome(),
                m.getQuantidade(),
                m.getPrecoUnitario(),
                m.getProduto().getPrecoCusto(),
                subtotal
        );
    }
}
