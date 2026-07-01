package com.eduardo.financialcontrol.saldo;

import com.eduardo.financialcontrol.saldo.dto.SaldoClienteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class SaldoController {

    private final SaldoService saldoService;

    @GetMapping("/{id}/saldo")
    public SaldoClienteResponse saldo(@PathVariable Long id) {
        return saldoService.saldoCliente(id);
    }
}
