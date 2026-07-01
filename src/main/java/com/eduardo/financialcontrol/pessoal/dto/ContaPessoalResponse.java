package com.eduardo.financialcontrol.pessoal.dto;

import com.eduardo.financialcontrol.pessoal.StatusConta;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContaPessoalResponse(
        Long id,
        String descricao,
        @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal valor,
        StatusConta status,
        LocalDate dataVencimento,
        LocalDate dataPagamento
) {}
