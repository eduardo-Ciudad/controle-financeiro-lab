package com.eduardo.financialcontrol.estoque;

import com.eduardo.financialcontrol.estoque.dto.AjusteEstoqueRequest;
import com.eduardo.financialcontrol.estoque.dto.EstoqueResponse;
import com.eduardo.financialcontrol.produto.Produto;
import com.eduardo.financialcontrol.produto.ProdutoRepository;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional(readOnly = true)
    public BigDecimal calcularEstoque(Long produtoId) {
        return movimentacaoEstoqueRepository.calcularEstoque(produtoId);
    }

    @Transactional
    public MovimentacaoEstoque registrarEntrada(Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                                                 OrigemMovimentacao origem, Long lancamentoClienteId, LocalDate dataCompetencia) {
        return salvar(produto, TipoMovimentacao.ENTRADA, quantidade, precoUnitario, origem, lancamentoClienteId, null, dataCompetencia);
    }

    @Transactional
    public MovimentacaoEstoque registrarSaida(Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                                               OrigemMovimentacao origem, Long lancamentoClienteId, LocalDate dataCompetencia) {
        return salvar(produto, TipoMovimentacao.SAIDA, quantidade, precoUnitario, origem, lancamentoClienteId, null, dataCompetencia);
    }

    @Transactional
    public MovimentacaoEstoque registrarEntradaFornecedor(Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                                                           OrigemMovimentacao origem, Long lancamentoFornecedorId, LocalDate dataCompetencia) {
        return salvar(produto, TipoMovimentacao.ENTRADA, quantidade, precoUnitario, origem, null, lancamentoFornecedorId, dataCompetencia);
    }

    @Transactional
    public MovimentacaoEstoque registrarSaidaFornecedor(Produto produto, BigDecimal quantidade, BigDecimal precoUnitario,
                                                         OrigemMovimentacao origem, Long lancamentoFornecedorId, LocalDate dataCompetencia) {
        return salvar(produto, TipoMovimentacao.SAIDA, quantidade, precoUnitario, origem, null, lancamentoFornecedorId, dataCompetencia);
    }

    @Transactional(readOnly = true)
    public BigDecimal valorEmEstoque(Produto produto) {
        return calcularEstoque(produto.getId()).multiply(produto.getPrecoVenda()).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Map<String, BigDecimal> calcularValorTotalEstoque() {
        List<Produto> produtos = produtoRepository.findAllByAtivoTrue();
        BigDecimal totalCusto = BigDecimal.ZERO;
        BigDecimal totalVenda = BigDecimal.ZERO;

        for (Produto produto : produtos) {
            BigDecimal qtd = calcularEstoque(produto.getId());
            if (qtd.compareTo(BigDecimal.ZERO) > 0) {
                if (produto.getPrecoCusto() != null) {
                    totalCusto = totalCusto.add(qtd.multiply(produto.getPrecoCusto()));
                }
                totalVenda = totalVenda.add(qtd.multiply(produto.getPrecoVenda()));
            }
        }

        return Map.of(
                "totalCusto", totalCusto.setScale(2, RoundingMode.HALF_UP),
                "totalVenda", totalVenda.setScale(2, RoundingMode.HALF_UP)
        );
    }

    @Transactional(readOnly = true)
    public EstoqueResponse consultarEstoque(Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + produtoId));
        BigDecimal quantidadeAtual = calcularEstoque(produtoId);
        BigDecimal valor = quantidadeAtual.multiply(produto.getPrecoVenda()).setScale(2, RoundingMode.HALF_UP);
        return new EstoqueResponse(produto.getId(), produto.getNome(), quantidadeAtual, valor);
    }

    @Transactional
    public EstoqueResponse registrarAjuste(Long produtoId, AjusteEstoqueRequest request) {
        Produto produto = produtoRepository.findById(produtoId)
                .filter(p -> Boolean.TRUE.equals(p.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto não encontrado: " + produtoId));

        LocalDate hoje = LocalDate.now();
        if (request.tipo() == TipoMovimentacao.SAIDA) {
            BigDecimal disponivel = calcularEstoque(produtoId);
            if (disponivel.compareTo(request.quantidade()) < 0) {
                throw new RegraDeNegocioException("Estoque insuficiente para o produto " + produto.getNome()
                        + ": disponível " + disponivel + ", solicitado " + request.quantidade());
            }
            registrarSaida(produto, request.quantidade(), produto.getPrecoVenda(), OrigemMovimentacao.AJUSTE, null, hoje);
        } else {
            registrarEntrada(produto, request.quantidade(), produto.getPrecoVenda(), OrigemMovimentacao.AJUSTE, null, hoje);
        }
        log.info("Ajuste de estoque: produto={} tipo={} quantidade={} motivo={}",
                produtoId, request.tipo(), request.quantidade(), request.motivo());
        return consultarEstoque(produtoId);
    }

    private MovimentacaoEstoque salvar(Produto produto, TipoMovimentacao tipo, BigDecimal quantidade, BigDecimal precoUnitario,
                                        OrigemMovimentacao origem, Long lancamentoClienteId, Long lancamentoFornecedorId,
                                        LocalDate dataCompetencia) {
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(produto);
        movimentacao.setTipo(tipo);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setPrecoUnitario(precoUnitario);
        movimentacao.setOrigem(origem);
        movimentacao.setLancamentoClienteId(lancamentoClienteId);
        movimentacao.setLancamentoFornecedorId(lancamentoFornecedorId);
        movimentacao.setDataCompetencia(dataCompetencia);
        return movimentacaoEstoqueRepository.save(movimentacao);
    }
}
