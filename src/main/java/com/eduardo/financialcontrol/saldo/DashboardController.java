package com.eduardo.financialcontrol.saldo;

import com.eduardo.financialcontrol.saldo.dto.ResumoDashboard;
import com.eduardo.financialcontrol.saldo.dto.ResumoDiario;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final SaldoService saldoService;

    @GetMapping
    public ResumoDashboard dashboard() {
        return saldoService.dashboard();
    }

    @GetMapping("/resumo-diario")
    public ResumoDiario resumoDiario(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data
    ) {
        if (data == null) {
            data = LocalDate.now();
        }
        return saldoService.resumoDiario(data);
    }
}
