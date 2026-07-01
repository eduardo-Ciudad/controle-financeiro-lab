package com.eduardo.financialcontrol.estoque;

import com.eduardo.financialcontrol.estoque.dto.AjusteEstoqueRequest;
import com.eduardo.financialcontrol.estoque.dto.EstoqueResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class EstoqueController {

    private final EstoqueService estoqueService;

    @GetMapping("/produtos/{id}/estoque")
    public EstoqueResponse consultar(@PathVariable Long id) {
        return estoqueService.consultarEstoque(id);
    }

    @PostMapping("/produtos/{id}/estoque/ajustes")
    public EstoqueResponse ajustar(@PathVariable Long id, @Valid @RequestBody AjusteEstoqueRequest request) {
        return estoqueService.registrarAjuste(id, request);
    }

    @GetMapping("/produtos/estoque/valor-total")
    public ResponseEntity<Map<String, BigDecimal>> valorTotalEstoque() {
        return ResponseEntity.ok(estoqueService.calcularValorTotalEstoque());
    }
}
