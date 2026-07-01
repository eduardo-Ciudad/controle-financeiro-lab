package com.eduardo.financialcontrol.pessoal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ContaPessoalRepository extends JpaRepository<ContaPessoal, Long> {

    Optional<ContaPessoal> findByIdAndUsuarioId(Long id, Long usuarioId);

    List<ContaPessoal> findByStatusAndUsuarioIdOrderByDataVencimentoAsc(StatusConta status, Long usuarioId);

    List<ContaPessoal> findAllByUsuarioIdOrderByDataVencimentoAsc(Long usuarioId);

    // No ContaPessoalRepository

    List<ContaPessoal> findByDataVencimentoBetweenAndUsuarioIdOrderByDataVencimentoAsc(
            LocalDate inicio, LocalDate fim, Long usuarioId);

    List<ContaPessoal> findByDataVencimentoBetweenAndStatusAndUsuarioIdOrderByDataVencimentoAsc(
            LocalDate inicio, LocalDate fim, StatusConta status, Long usuarioId);
}
