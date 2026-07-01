package com.eduardo.financialcontrol.fornecedor;

import com.eduardo.financialcontrol.estoque.EstoqueService;
import com.eduardo.financialcontrol.estoque.MovimentacaoEstoque;
import com.eduardo.financialcontrol.estoque.MovimentacaoEstoqueRepository;
import com.eduardo.financialcontrol.estoque.OrigemMovimentacao;
import com.eduardo.financialcontrol.fornecedor.dto.CompraFornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.EstornoFornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.ItemCompraFornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.ItemCompraFornecedorResponse;
import com.eduardo.financialcontrol.fornecedor.dto.LancamentoFornecedorResponse;
import com.eduardo.financialcontrol.fornecedor.dto.LinhaExtratoFornecedor;
import com.eduardo.financialcontrol.fornecedor.dto.PagamentoFornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.SaldoFornecedorResponse;
import com.eduardo.financialcontrol.lancamento.Categoria;
import com.eduardo.financialcontrol.lancamento.Natureza;
import com.eduardo.financialcontrol.produto.Produto;
import com.eduardo.financialcontrol.produto.ProdutoService;
import com.eduardo.financialcontrol.saldo.SituacaoSaldo;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LancamentoFornecedorService {

    private final LancamentoFornecedorRepository lancamentoFornecedorRepository;
    private final FornecedorService fornecedorService;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public LancamentoFornecedorResponse registrarCompra(Long fornecedorId, CompraFornecedorRequest request) {
        Fornecedor fornecedor = fornecedorService.encontrarOuLancar(fornecedorId);

        List<Produto> produtos = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;
        for (ItemCompraFornecedorRequest item : request.itens()) {
            Produto produto = produtoService.encontrarOuLancar(item.produtoId());
            valorTotal = valorTotal.add(item.quantidade().multiply(item.custoUnitario()));
            produtos.add(produto);
        }
        valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);

        LancamentoFornecedor lancamento = new LancamentoFornecedor();
        lancamento.setFornecedor(fornecedor);
        lancamento.setNatureza(Natureza.DEBITO);
        lancamento.setCategoria(Categoria.COMPRA);
        lancamento.setValor(valorTotal);
        lancamento.setDataCompetencia(request.dataCompetencia());
        lancamento.setDescricao(request.descricao());
        lancamento.setUsuario(usuarioAutenticadoService.getUsuario());
        lancamento = lancamentoFornecedorRepository.save(lancamento);

        for (int i = 0; i < produtos.size(); i++) {
            Produto produto = produtos.get(i);
            ItemCompraFornecedorRequest item = request.itens().get(i);
            estoqueService.registrarEntradaFornecedor(produto, item.quantidade(), item.custoUnitario(),
                    OrigemMovimentacao.COMPRA, lancamento.getId(), request.dataCompetencia());
        }

        return montarResponse(lancamento);
    }

    @Transactional
    public LancamentoFornecedorResponse registrarPagamento(Long fornecedorId, PagamentoFornecedorRequest request) {
        Fornecedor fornecedor = fornecedorService.encontrarOuLancar(fornecedorId);
        LancamentoFornecedor lancamento = new LancamentoFornecedor();
        lancamento.setFornecedor(fornecedor);
        lancamento.setNatureza(Natureza.CREDITO);
        lancamento.setCategoria(Categoria.PAGAMENTO);
        lancamento.setValor(request.valor());
        lancamento.setDataCompetencia(request.dataCompetencia());
        lancamento.setDescricao(request.descricao());
        lancamento.setFormaPagamento(request.formaPagamento());
        lancamento.setUsuario(usuarioAutenticadoService.getUsuario());
        return montarResponse(lancamentoFornecedorRepository.save(lancamento));
    }

    @Transactional
    public LancamentoFornecedorResponse estornar(Long lancamentoId, EstornoFornecedorRequest request) {
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();
        LancamentoFornecedor original = lancamentoFornecedorRepository.findByIdAndUsuarioId(lancamentoId, usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento de fornecedor não encontrado: " + lancamentoId));

        if (original.getCategoria() == Categoria.ESTORNO) {
            throw new RegraDeNegocioException("Não é possível estornar um lançamento de estorno.");
        }
        if (lancamentoFornecedorRepository.findByEstornoDeAndUsuarioId(original, usuarioId).isPresent()) {
            throw new RegraDeNegocioException("Lançamento já foi estornado.");
        }

        Natureza naturezaOposta = original.getNatureza() == Natureza.DEBITO ? Natureza.CREDITO : Natureza.DEBITO;
        LocalDate dataCompetencia = request.dataCompetencia() != null ? request.dataCompetencia() : LocalDate.now();

        List<MovimentacaoEstoque> itensOriginais = original.getCategoria() == Categoria.COMPRA
                ? movimentacaoEstoqueRepository.buscarItensCompra(original.getId())
                : List.of();

        for (MovimentacaoEstoque item : itensOriginais) {
            BigDecimal disponivel = estoqueService.calcularEstoque(item.getProduto().getId());
            if (disponivel.compareTo(item.getQuantidade()) < 0) {
                throw new RegraDeNegocioException("Estoque insuficiente para estornar a compra do produto "
                        + item.getProduto().getNome() + ": disponível " + disponivel + ", necessário " + item.getQuantidade());
            }
        }

        LancamentoFornecedor estorno = new LancamentoFornecedor();
        estorno.setFornecedor(original.getFornecedor());
        estorno.setNatureza(naturezaOposta);
        estorno.setCategoria(Categoria.ESTORNO);
        estorno.setValor(original.getValor());
        estorno.setDataCompetencia(dataCompetencia);
        estorno.setDescricao(request.descricao() != null ? request.descricao() : "Estorno de lançamento #" + original.getId());
        estorno.setEstornoDe(original);
        estorno.setUsuario(usuarioAutenticadoService.getUsuario());
        estorno = lancamentoFornecedorRepository.save(estorno);

        for (MovimentacaoEstoque item : itensOriginais) {
            estoqueService.registrarSaidaFornecedor(item.getProduto(), item.getQuantidade(), item.getPrecoUnitario(),
                    OrigemMovimentacao.ESTORNO, estorno.getId(), dataCompetencia);
        }

        return montarResponse(estorno);
    }

    @Transactional(readOnly = true)
    public SaldoFornecedorResponse saldoAPagar(Long fornecedorId) {
        Fornecedor fornecedor = fornecedorService.encontrarOuLancar(fornecedorId);
        BigDecimal saldo = lancamentoFornecedorRepository
                .calcularSaldoAPagar(fornecedorId, usuarioAutenticadoService.getUsuarioId());
        SituacaoSaldo situacao;
        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            situacao = SituacaoSaldo.DEVEDOR;
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            situacao = SituacaoSaldo.CREDOR;
        } else {
            situacao = SituacaoSaldo.QUITADO;
        }
        return new SaldoFornecedorResponse(fornecedor.getId(), fornecedor.getNome(), saldo, situacao, saldo.abs());
    }

    @Transactional(readOnly = true)
    public Page<LinhaExtratoFornecedor> extrato(Long fornecedorId, Pageable pageable) {
        fornecedorService.encontrarOuLancar(fornecedorId);
        Page<LancamentoFornecedor> page = lancamentoFornecedorRepository
                .findByFornecedorIdAndUsuarioIdOrderByDataCompetenciaAscIdAsc(
                        fornecedorId, usuarioAutenticadoService.getUsuarioId(), pageable);

        BigDecimal acumulado = BigDecimal.ZERO;
        List<LinhaExtratoFornecedor> linhas = new ArrayList<>();
        for (LancamentoFornecedor l : page.getContent()) {
            if (l.getNatureza() == Natureza.DEBITO) {
                acumulado = acumulado.add(l.getValor());
            } else {
                acumulado = acumulado.subtract(l.getValor());
            }
            linhas.add(new LinhaExtratoFornecedor(
                    l.getId(), l.getNatureza(), l.getCategoria(),
                    l.getValor(), acumulado, l.getDataCompetencia(),
                    l.getDescricao(), l.getFormaPagamento(), l.getCriadoEm()
            ));
        }
        return new PageImpl<>(linhas, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public LancamentoFornecedorResponse buscarPorId(Long id) {
        return montarResponse(
                lancamentoFornecedorRepository.findByIdAndUsuarioId(id, usuarioAutenticadoService.getUsuarioId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento de fornecedor não encontrado: " + id))
        );
    }

    private LancamentoFornecedorResponse montarResponse(LancamentoFornecedor lancamento) {
        List<ItemCompraFornecedorResponse> itens = lancamento.getCategoria() == Categoria.COMPRA
                ? movimentacaoEstoqueRepository.buscarItensCompra(lancamento.getId()).stream()
                        .map(ItemCompraFornecedorResponse::de)
                        .toList()
                : null;
        return LancamentoFornecedorResponse.de(lancamento, itens);
    }
}
