package com.eduardo.financialcontrol.fornecedor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LancamentoFornecedorRepository extends JpaRepository<LancamentoFornecedor, Long> {

    Optional<LancamentoFornecedor> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<LancamentoFornecedor> findByEstornoDeAndUsuarioId(LancamentoFornecedor lancamento, Long usuarioId);

    List<LancamentoFornecedor> findByDataCompetenciaAndUsuarioIdOrderByIdAsc(
            LocalDate dataCompetencia, Long usuarioId);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN l.natureza = com.eduardo.financialcontrol.lancamento.Natureza.DEBITO
                                     THEN l.valor ELSE -l.valor END), 0)
            FROM LancamentoFornecedor l
            WHERE l.fornecedor.id = :fornecedorId
            AND l.usuario.id = :usuarioId
            """)
    BigDecimal calcularSaldoAPagar(@Param("fornecedorId") Long fornecedorId, @Param("usuarioId") Long usuarioId);

    Page<LancamentoFornecedor> findByFornecedorIdAndUsuarioIdOrderByDataCompetenciaAscIdAsc(
            Long fornecedorId, Long usuarioId, Pageable pageable);
}
