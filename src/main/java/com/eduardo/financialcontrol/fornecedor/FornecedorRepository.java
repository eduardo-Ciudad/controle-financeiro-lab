package com.eduardo.financialcontrol.fornecedor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Optional<Fornecedor> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<Fornecedor> findByDocumentoAndUsuarioId(String documento, Long usuarioId);

    @Query("""
        SELECT f
        FROM Fornecedor f
        WHERE f.ativo = true
          AND f.usuario.id = :usuarioId
          AND LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
        """)
    Page<Fornecedor> buscarAtivosPorNome(
            @Param("nome") String nome,
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);

    Page<Fornecedor> findByAtivoTrueAndUsuarioId(Long usuarioId, Pageable pageable);
}
