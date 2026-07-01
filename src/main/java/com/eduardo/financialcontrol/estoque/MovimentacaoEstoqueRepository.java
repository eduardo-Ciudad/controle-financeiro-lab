package com.eduardo.financialcontrol.estoque;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN m.tipo = com.eduardo.financialcontrol.estoque.TipoMovimentacao.ENTRADA
                                     THEN m.quantidade ELSE -m.quantidade END), 0)
            FROM MovimentacaoEstoque m
            WHERE m.produto.id = :produtoId
            """)
    BigDecimal calcularEstoque(@Param("produtoId") Long produtoId);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.lancamentoClienteId = :lancamentoClienteId
              AND m.origem = com.eduardo.financialcontrol.estoque.OrigemMovimentacao.VENDA
            ORDER BY m.id
            """)
    List<MovimentacaoEstoque> buscarItensVenda(@Param("lancamentoClienteId") Long lancamentoClienteId);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.lancamentoFornecedorId = :lancamentoFornecedorId
              AND m.origem = com.eduardo.financialcontrol.estoque.OrigemMovimentacao.COMPRA
            ORDER BY m.id
            """)
    List<MovimentacaoEstoque> buscarItensCompra(@Param("lancamentoFornecedorId") Long lancamentoFornecedorId);

    List<MovimentacaoEstoque> findByLancamentoClienteId(Long lancamentoClienteId);
}
