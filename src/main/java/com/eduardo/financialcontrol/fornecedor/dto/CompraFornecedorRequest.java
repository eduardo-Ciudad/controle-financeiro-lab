package com.eduardo.financialcontrol.fornecedor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.util.List;

public record CompraFornecedorRequest(
        @NotEmpty @Valid List<ItemCompraFornecedorRequest> itens,
        @NotNull @PastOrPresent LocalDate dataCompetencia,
        String descricao
) {}
