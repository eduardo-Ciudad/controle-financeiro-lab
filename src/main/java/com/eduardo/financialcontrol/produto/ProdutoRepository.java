package com.eduardo.financialcontrol.produto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Optional<Produto> findByIdAndUsuarioId(Long id, Long usuarioId);

    @Query("""
                SELECT p
                FROM Produto p
                WHERE p.ativo = true
                  AND p.usuario.id = :usuarioId
                  AND LOWER(p.nome) LIKE LOWER(CONCAT('%', :nome, '%'))
            """)
    Page<Produto> buscarAtivosPorNome(
            @Param("nome") String nome,
            @Param("usuarioId") Long usuarioId,
            Pageable pageable);

    Page<Produto> findByAtivoTrueAndUsuarioId(Long usuarioId, Pageable pageable);

    List<Produto> findAllByAtivoTrueAndUsuarioId(Long usuarioId);
}