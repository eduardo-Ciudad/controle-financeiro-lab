package com.eduardo.financialcontrol.fornecedor.dto;

import com.eduardo.financialcontrol.fornecedor.LancamentoFornecedor;
import com.eduardo.financialcontrol.lancamento.Categoria;
import com.eduardo.financialcontrol.lancamento.FormaPagamento;
import com.eduardo.financialcontrol.lancamento.Natureza;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record LancamentoFornecedorResponse(
        Long id,
        Long fornecedorId,
        Natureza natureza,
        Categoria categoria,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valor,
        LocalDate dataCompetencia,
        String descricao,
        FormaPagamento formaPagamento,
        Long estornoDeId,
        OffsetDateTime criadoEm,
        List<ItemCompraFornecedorResponse> itens
) {
    public static LancamentoFornecedorResponse de(LancamentoFornecedor l, List<ItemCompraFornecedorResponse> itens) {
        return new LancamentoFornecedorResponse(
                l.getId(),
                l.getFornecedor().getId(),
                l.getNatureza(),
                l.getCategoria(),
                l.getValor(),
                l.getDataCompetencia(),
                l.getDescricao(),
                l.getFormaPagamento(),
                l.getEstornoDe() != null ? l.getEstornoDe().getId() : null,
                l.getCriadoEm(),
                itens
        );
    }
}
