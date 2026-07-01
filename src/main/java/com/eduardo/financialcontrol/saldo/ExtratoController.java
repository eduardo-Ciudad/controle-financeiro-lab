package com.eduardo.financialcontrol.saldo;

import com.eduardo.financialcontrol.saldo.dto.LinhaExtrato;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ExtratoController {

    private final SaldoService saldoService;

    @GetMapping("/{id}/extrato")
    public Page<LinhaExtrato> extrato(@PathVariable Long id, Pageable pageable) {
        return saldoService.extrato(id, pageable);
    }
}
