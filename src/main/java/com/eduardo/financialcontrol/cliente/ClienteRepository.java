package com.eduardo.financialcontrol.cliente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Cliente> findByDocumentoAndUsuarioId(String documento, Long usuarioId);

    Page<Cliente> findByAtivoTrueAndUsuarioId(Long usuarioId, Pageable pageable);

    @Query("""
SELECT c
FROM Cliente c
WHERE c.ativo = true
AND c.usuario.id = :usuarioId
AND LOWER(c.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
""")
    Page<Cliente> buscarAtivosPorNome(
            @Param("nome") String nome,
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);

    @Query("""
SELECT c
FROM Cliente c
LEFT JOIN Lancamento l ON l.cliente = c
WHERE c.ativo = true
AND c.usuario.id = :usuarioId
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
    Page<Cliente> buscarAtivosComDivida(
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);

    @Query("""
SELECT c
FROM Cliente c
LEFT JOIN Lancamento l ON l.cliente = c
WHERE c.ativo = true
AND c.usuario.id = :usuarioId
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
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);
}
