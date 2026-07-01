package com.eduardo.financialcontrol.fornecedor;

import com.eduardo.financialcontrol.fornecedor.dto.CompraFornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.EstornoFornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.LancamentoFornecedorResponse;
import com.eduardo.financialcontrol.fornecedor.dto.PagamentoFornecedorRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequiredArgsConstructor
public class LancamentoFornecedorController {

    private final LancamentoFornecedorService lancamentoFornecedorService;

    @PostMapping("/fornecedores/{id}/compras")
    public ResponseEntity<LancamentoFornecedorResponse> compra(
            @PathVariable Long id,
            @Valid @RequestBody CompraFornecedorRequest request) {
        LancamentoFornecedorResponse response = lancamentoFornecedorService.registrarCompra(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/lancamentos-fornecedor/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/fornecedores/{id}/pagamentos")
    public ResponseEntity<LancamentoFornecedorResponse> pagamento(
            @PathVariable Long id,
            @Valid @RequestBody PagamentoFornecedorRequest request) {
        LancamentoFornecedorResponse response = lancamentoFornecedorService.registrarPagamento(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/lancamentos-fornecedor/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/lancamentos-fornecedor/{id}/estornos")
    public ResponseEntity<LancamentoFornecedorResponse> estorno(
            @PathVariable Long id,
            @Valid @RequestBody EstornoFornecedorRequest request) {
        LancamentoFornecedorResponse response = lancamentoFornecedorService.estornar(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/lancamentos-fornecedor/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/lancamentos-fornecedor/{id}")
    public LancamentoFornecedorResponse buscar(@PathVariable Long id) {
        return lancamentoFornecedorService.buscarPorId(id);
    }
}
