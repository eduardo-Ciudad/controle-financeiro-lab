package com.eduardo.financialcontrol.lancamento;

import com.eduardo.financialcontrol.lancamento.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class LancamentoController {

    private final LancamentoService lancamentoService;

    @PostMapping("/clientes/{id}/compras")
    public ResponseEntity<LancamentoResponse> compra(
            @PathVariable Long id,
            @Valid @RequestBody CompraRequest request) {
        LancamentoResponse response = lancamentoService.registrarCompra(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/lancamentos/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/clientes/{id}/pagamentos")
    public ResponseEntity<LancamentoResponse> pagamento(
            @PathVariable Long id,
            @Valid @RequestBody PagamentoRequest request) {
        LancamentoResponse response = lancamentoService.registrarPagamento(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/lancamentos/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/lancamentos/{id}/estornos")
    public ResponseEntity<LancamentoResponse> estorno(
            @PathVariable Long id,
            @Valid @RequestBody EstornoRequest request) {
        LancamentoResponse response = lancamentoService.estornar(id, request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/lancamentos/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/lancamentos/{id}")
    public LancamentoResponse buscar(@PathVariable Long id) {
        return lancamentoService.buscarPorId(id);
    }

    @PostMapping("/vendas")
    public ResponseEntity<LancamentoResponse> registrarVenda(@RequestBody @Valid VendaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lancamentoService.registrarVenda(request));
    }

    @GetMapping("/vendas")
    public List<LancamentoResponse> listarVendasPorMes(@RequestParam String mes) {
        return lancamentoService.listarVendasPorMes(mes);
    }
}
