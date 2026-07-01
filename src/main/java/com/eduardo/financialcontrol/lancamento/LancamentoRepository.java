package com.eduardo.financialcontrol.lancamento;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LancamentoRepository extends JpaRepository<Lancamento, Long> {

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN l.natureza = com.eduardo.financialcontrol.lancamento.Natureza.DEBITO
                                     THEN l.valor ELSE -l.valor END), 0)
            FROM Lancamento l
            WHERE l.cliente.id = :clienteId
            """)
    BigDecimal calcularSaldo(@Param("clienteId") Long clienteId);

    Optional<Lancamento> findByEstornoDe(Lancamento lancamento);

    Page<Lancamento> findByClienteIdOrderByDataCompetenciaAscIdAsc(Long clienteId, Pageable pageable);

    List<Lancamento> findByDataCompetenciaOrderByIdAsc(LocalDate dataCompetencia);

    @Query(value = """
            SELECT to_char(data_competencia, 'YYYY-MM') AS mes,
                   COALESCE(SUM(CASE WHEN natureza = 'CREDITO' THEN valor ELSE 0 END), 0) AS entradas,
                   COALESCE(SUM(CASE WHEN natureza = 'DEBITO' THEN valor ELSE 0 END), 0) AS saidas
            FROM lancamentos
            GROUP BY to_char(data_competencia, 'YYYY-MM')
            ORDER BY mes
            """, nativeQuery = true)
    List<Object[]> resumoMensal();

    List<Lancamento> findByCategoriaAndDataCompetenciaBetween(
            Categoria categoria, LocalDate inicio, LocalDate fim);
}
