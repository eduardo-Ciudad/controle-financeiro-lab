package com.eduardo.financialcontrol.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByDocumento(String documento);

    Page<Cliente> findByAtivoTrue(Pageable pageable);

    @Query("""
SELECT c
FROM Cliente c
WHERE c.ativo = true
AND LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
""")
    Page<Cliente> buscarAtivosPorNome(
            @Param("nome") String nome,
            Pageable pageable);

    @Query("""
SELECT c
FROM Cliente c
LEFT JOIN Lancamento l ON l.cliente = c
WHERE c.ativo = true
GROUP BY c
HAVING COALESCE(
SUM(
CASE
WHEN l.natureza = com.eduardo.financialcontrol.lancamento.Natureza.DEBITO
THEN l.valor
ELSE -l.valor
END
),0) > 0
""")
    Page<Cliente> buscarAtivosComDivida(Pageable pageable);

    @Query("""
SELECT c
FROM Cliente c
LEFT JOIN Lancamento l ON l.cliente = c
WHERE c.ativo = true
AND LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
GROUP BY c
HAVING COALESCE(
SUM(
CASE
WHEN l.natureza = com.eduardo.financialcontrol.lancamento.Natureza.DEBITO
THEN l.valor
ELSE -l.valor
END
),0) > 0
""")
    Page<Cliente> buscarAtivosComDividaPorNome(
            @Param("nome") String nome,
            Pageable pageable);
}