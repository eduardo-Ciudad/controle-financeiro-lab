package com.eduardo.financialcontrol.fornecedor.dto;

import com.eduardo.financialcontrol.estoque.MovimentacaoEstoque;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;

public record ItemCompraFornecedorResponse(
        Long produtoId,
        String nomeProduto,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal quantidade,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal custoUnitario,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal subtotal
) {
    public static ItemCompraFornecedorResponse de(MovimentacaoEstoque m) {
        BigDecimal subtotal = m.getQuantidade().multiply(m.getPrecoUnitario()).setScale(2, RoundingMode.HALF_UP);
        return new ItemCompraFornecedorResponse(
                m.getProduto().getId(),
                m.getProduto().getNome(),
                m.getQuantidade(),
                m.getPrecoUnitario(),
                subtotal
        );
    }
}
