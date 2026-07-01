package com.eduardo.financialcontrol.pessoal;

import com.eduardo.financialcontrol.pessoal.dto.ContaPessoalRequest;
import com.eduardo.financialcontrol.pessoal.dto.ContaPessoalResponse;
import com.eduardo.financialcontrol.pessoal.dto.ParcelamentoRequest;
import com.eduardo.financialcontrol.pessoal.dto.ResumoMensalPessoalResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pessoal")
@RequiredArgsConstructor
public class ContaPessoalController {

    private final ContaPessoalService contaPessoalService;

    @PostMapping
    public ResponseEntity<ContaPessoalResponse> criar(@Valid @RequestBody ContaPessoalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contaPessoalService.criar(request));
    }

    @PostMapping("/parcelamentos")
    public ResponseEntity<List<ContaPessoalResponse>> criarParcelamento(
            @RequestBody @Valid ParcelamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contaPessoalService.criarParcelamento(request));
    }

    @GetMapping
    public List<ContaPessoalResponse> listar(
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) StatusConta status) {
        return contaPessoalService.listar(mes, status);
    }

    @GetMapping("/resumo")
    public ResumoMensalPessoalResponse resumo(
            @RequestParam(required = false) String mes) {
        return contaPessoalService.resumo(mes);
    }

    @PatchMapping("/{id}/pagar")
    public ContaPessoalResponse pagar(@PathVariable Long id) {
        return contaPessoalService.pagar(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        contaPessoalService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
