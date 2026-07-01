package com.eduardo.financialcontrol.produto.dto;

import com.eduardo.financialcontrol.produto.Produto;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precoVenda,
        Boolean ativo,
        OffsetDateTime criadoEm,
        OffsetDateTime atualizadoEm,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal quantidadeAtual,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorVendaEmEstoque,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valorCompraEmEstoque,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal precoCusto,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal margemLucro,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal lucroPorUnidade
) {
    public static ProdutoResponse de(Produto p, BigDecimal quantidadeAtual) {
        BigDecimal precoCusto = p.getPrecoCusto();
        BigDecimal precoVenda = p.getPrecoVenda();
        BigDecimal qtd = quantidadeAtual != null ? quantidadeAtual : BigDecimal.ZERO;

        BigDecimal valorVendaEmEstoque = (precoVenda != null)
                ? precoVenda.multiply(qtd).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal valorCompraEmEstoque = (precoCusto != null)
                ? precoCusto.multiply(qtd).setScale(2, RoundingMode.HALF_UP)
                : null;

        BigDecimal lucroPorUnidade = null;
        BigDecimal margemLucro = null;
        if (precoCusto != null && precoVenda != null) {   // <- guard extra no precoVenda
            lucroPorUnidade = precoVenda.subtract(precoCusto);
            if (precoVenda.compareTo(BigDecimal.ZERO) != 0) {
                margemLucro = lucroPorUnidade
                        .divide(precoVenda, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
        }

        return new ProdutoResponse(
                p.getId(), p.getNome(), p.getDescricao(), precoVenda,
                p.getAtivo(), p.getCriadoEm(), p.getAtualizadoEm(),
                qtd, valorVendaEmEstoque, valorCompraEmEstoque,
                precoCusto, margemLucro, lucroPorUnidade
        );
    }
}