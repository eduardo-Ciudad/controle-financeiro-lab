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
            AND m.usuario.id = :usuarioId
            """)
    BigDecimal calcularEstoque(@Param("produtoId") Long produtoId, @Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.lancamentoClienteId = :lancamentoClienteId
              AND m.origem = com.eduardo.financialcontrol.estoque.OrigemMovimentacao.VENDA
              AND m.usuario.id = :usuarioId
            ORDER BY m.id
            """)
    List<MovimentacaoEstoque> buscarItensVenda(@Param("lancamentoClienteId") Long lancamentoClienteId,
                                               @Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT m FROM MovimentacaoEstoque m
            WHERE m.lancamentoFornecedorId = :lancamentoFornecedorId
              AND m.origem = com.eduardo.financialcontrol.estoque.OrigemMovimentacao.COMPRA
              AND m.usuario.id = :usuarioId
            ORDER BY m.id
            """)
    List<MovimentacaoEstoque> buscarItensCompra(@Param("lancamentoFornecedorId") Long lancamentoFornecedorId,
                                                @Param("usuarioId") Long usuarioId);


    List<MovimentacaoEstoque> findByLancamentoClienteIdAndUsuarioId(Long lancamentoClienteId, Long usuarioId);
}
