package com.eduardo.financialcontrol.pessoal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ContaPessoalRepository extends JpaRepository<ContaPessoal, Long> {

    List<ContaPessoal> findByStatusOrderByDataVencimentoAsc(StatusConta status);

    List<ContaPessoal> findAllByOrderByDataVencimentoAsc();

    // No ContaPessoalRepository

    List<ContaPessoal> findByDataVencimentoBetweenOrderByDataVencimentoAsc(
            LocalDate inicio, LocalDate fim);

    List<ContaPessoal> findByDataVencimentoBetweenAndStatusOrderByDataVencimentoAsc(
            LocalDate inicio, LocalDate fim, StatusConta status);
}
