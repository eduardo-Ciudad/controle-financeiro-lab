package com.eduardo.financialcontrol.produto;

import com.eduardo.financialcontrol.estoque.EstoqueService;
import com.eduardo.financialcontrol.produto.dto.ProdutoRequest;
import com.eduardo.financialcontrol.produto.dto.ProdutoResponse;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final EstoqueService estoqueService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        Produto produto = new Produto();
        mapear(request, produto);
        aplicarMarkup(request, produto);
        produto.setUsuario(usuarioAutenticadoService.getUsuario());
        return toResponse(produtoRepository.save(produto));
    }

    @Transactional(readOnly = true)
    public Page<ProdutoResponse> listar(String nome, Pageable pageable) {

        Long usuarioId = usuarioAutenticadoService.getUsuarioId();

        Page<Produto> page;

        if (nome != null && !nome.isBlank()) {
            page = produtoRepository.buscarAtivosPorNome(nome, usuarioId, pageable);
        } else {
            page = produtoRepository.findByAtivoTrueAndUsuarioId(usuarioId, pageable);
        }

        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProdutoResponse buscarPorId(Long id) {
        return toResponse(encontrarOuLancar(id));
    }

    @Transactional
    public ProdutoResponse atualizar(Long id, ProdutoRequest request) {
        Produto produto = encontrarOuLancar(id);
        mapear(request, produto);
        aplicarMarkup(request, produto);
        return toResponse(produtoRepository.save(produto));
    }

    @Transactional
    public void inativar(Long id) {
        Produto produto = encontrarOuLancar(id);
        produto.setAtivo(false);
        produtoRepository.save(produto);
    }

    public Produto encontrarOuLancar(Long id) {
        return produtoRepository.findByIdAndUsuarioId(id, usuarioAutenticadoService.getUsuarioId())
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + id));
    }

    private ProdutoResponse toResponse(Produto produto) {
        BigDecimal quantidadeAtual = estoqueService.calcularEstoque(produto.getId());
        return ProdutoResponse.de(produto, quantidadeAtual);
    }

    private void mapear(ProdutoRequest request, Produto produto) {
        produto.setNome(request.nome());
        produto.setDescricao(request.descricao());
        produto.setPrecoVenda(request.precoVenda());
        produto.setPrecoCusto(request.precoCusto());
    }

    private void aplicarMarkup(ProdutoRequest request, Produto produto) {
        BigDecimal precoCusto = request.precoCusto();
        BigDecimal precoVenda = request.precoVenda();
        if (precoCusto != null && (precoVenda == null || precoVenda.compareTo(BigDecimal.ZERO) == 0)) {
            produto.setPrecoVenda(precoCusto.multiply(new BigDecimal("1.30")).setScale(2, RoundingMode.HALF_UP));
        }
    }
}
