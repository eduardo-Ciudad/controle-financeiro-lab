package com.eduardo.financialcontrol.saldo;

import com.eduardo.financialcontrol.saldo.dto.ResumoMensalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ResumoController {

    private final SaldoService saldoService;

    @GetMapping("/lancamentos/resumo-mensal")
    public List<ResumoMensalResponse> resumoMensal() {
        return saldoService.resumoMensal();
    }
}
