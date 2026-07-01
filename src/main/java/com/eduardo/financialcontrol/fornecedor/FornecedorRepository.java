package com.eduardo.financialcontrol.fornecedor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {

    Optional<Fornecedor> findByDocumento(String documento);

    @Query("""
        SELECT f
        FROM Fornecedor f
        WHERE f.ativo = true
          AND LOWER(f.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
        """)
    Page<Fornecedor> buscarAtivosPorNome(
            @Param("nome") String nome,
            Pageable pageable);

    Page<Fornecedor> findByAtivoTrue(Pageable pageable);
}
