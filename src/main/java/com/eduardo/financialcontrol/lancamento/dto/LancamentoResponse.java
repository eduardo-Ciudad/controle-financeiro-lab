package com.eduardo.financialcontrol.lancamento.dto;

import com.eduardo.financialcontrol.lancamento.Categoria;
import com.eduardo.financialcontrol.lancamento.FormaPagamento;
import com.eduardo.financialcontrol.lancamento.Lancamento;
import com.eduardo.financialcontrol.lancamento.Natureza;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record LancamentoResponse(
        Long id,
        Long clienteId,
        Natureza natureza,
        Categoria categoria,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valor,
        LocalDate dataCompetencia,
        String descricao,
        FormaPagamento formaPagamento,
        Long estornoDeId,
        OffsetDateTime criadoEm,
        List<ItemVendaResponse> itens
) {
    public static LancamentoResponse de(Lancamento l, List<ItemVendaResponse> itens) {
        return new LancamentoResponse(
                l.getId(),
                l.getCliente().getId(),
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
