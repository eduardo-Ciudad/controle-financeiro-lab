package com.eduardo.financialcontrol.fornecedor;

import com.eduardo.financialcontrol.fornecedor.dto.LinhaExtratoFornecedor;
import com.eduardo.financialcontrol.fornecedor.dto.SaldoFornecedorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fornecedores")
@RequiredArgsConstructor
public class SaldoFornecedorController {

    private final LancamentoFornecedorService lancamentoFornecedorService;

    @GetMapping("/{id}/saldo")
    public SaldoFornecedorResponse saldo(@PathVariable Long id) {
        return lancamentoFornecedorService.saldoAPagar(id);
    }

    @GetMapping("/{id}/extrato")
    public Page<LinhaExtratoFornecedor> extrato(@PathVariable Long id, Pageable pageable) {
        return lancamentoFornecedorService.extrato(id, pageable);
    }
}
