package com.eduardo.financialcontrol.saldo.dto;


import com.eduardo.financialcontrol.lancamento.Categoria;
import com.eduardo.financialcontrol.lancamento.FormaPagamento;
import com.eduardo.financialcontrol.lancamento.Natureza;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumoDiario(
        LocalDate data,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalVendido,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal totalRecebido,
        int quantidadeVendas,
        int quantidadePagamentos,
        List<LancamentoDiarioItem> lancamentosClientes,
        List<LancamentoDiarioItem> lancamentosFornecedores
) {
    public record LancamentoDiarioItem(
            Long id,
            String nome,
            Natureza natureza,
            Categoria categoria,
            @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valor,
            String descricao,
            FormaPagamento formaPagamento
    ) {}
}