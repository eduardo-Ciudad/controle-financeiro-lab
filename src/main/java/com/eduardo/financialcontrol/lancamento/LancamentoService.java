package com.eduardo.financialcontrol.lancamento;

import com.eduardo.financialcontrol.cliente.Cliente;
import com.eduardo.financialcontrol.cliente.ClienteService;
import com.eduardo.financialcontrol.estoque.EstoqueService;
import com.eduardo.financialcontrol.estoque.MovimentacaoEstoque;
import com.eduardo.financialcontrol.estoque.MovimentacaoEstoqueRepository;
import com.eduardo.financialcontrol.estoque.OrigemMovimentacao;
import com.eduardo.financialcontrol.lancamento.dto.*;
import com.eduardo.financialcontrol.produto.Produto;
import com.eduardo.financialcontrol.produto.ProdutoService;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LancamentoService {

    private final LancamentoRepository lancamentoRepository;
    private final ClienteService clienteService;
    private final ProdutoService produtoService;
    private final EstoqueService estoqueService;
    private final MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Transactional
    public LancamentoResponse registrarCompra(Long clienteId, CompraRequest request) {
        Cliente cliente = clienteService.encontrarOuLancar(clienteId);

        List<Produto> produtos = new ArrayList<>();
        BigDecimal valorTotal = BigDecimal.ZERO;

        List<BigDecimal> precos = new ArrayList<>();

        for (ItemCompraRequest item : request.itens()) {
            Produto produto = produtoService.encontrarOuLancar(item.produtoId());
            BigDecimal disponivel = estoqueService.calcularEstoque(produto.getId());
            if (disponivel.compareTo(item.quantidade()) < 0) {
                throw new RegraDeNegocioException("Estoque insuficiente para o produto " + produto.getNome()
                        + ": disponível " + disponivel + ", solicitado " + item.quantidade());
            }
            BigDecimal preco = item.precoUnitario() != null ? item.precoUnitario() : produto.getPrecoVenda();
            valorTotal = valorTotal.add(item.quantidade().multiply(preco));
            produtos.add(produto);
            precos.add(preco);
        }

        valorTotal = valorTotal.setScale(2, RoundingMode.HALF_UP);

        Lancamento lancamento = new Lancamento();
        lancamento.setCliente(cliente);
        lancamento.setNatureza(Natureza.DEBITO);
        lancamento.setCategoria(Categoria.COMPRA);
        lancamento.setValor(valorTotal);
        lancamento.setDataCompetencia(request.dataCompetencia());
        lancamento.setDescricao(request.descricao());
        lancamento = lancamentoRepository.save(lancamento);

        for (int i = 0; i < produtos.size(); i++) {
            Produto produto = produtos.get(i);
            BigDecimal quantidade = request.itens().get(i).quantidade();
            estoqueService.registrarSaida(produto, quantidade, precos.get(i),
                    OrigemMovimentacao.VENDA, lancamento.getId(), request.dataCompetencia());
        }

        return montarResponse(lancamento);
    }

    @Transactional
    public LancamentoResponse registrarPagamento(Long clienteId, PagamentoRequest request) {
        Cliente cliente = clienteService.encontrarOuLancar(clienteId);
        Lancamento lancamento = new Lancamento();
        lancamento.setCliente(cliente);
        lancamento.setNatureza(Natureza.CREDITO);
        lancamento.setCategoria(Categoria.PAGAMENTO);
        lancamento.setValor(request.valor());
        lancamento.setDataCompetencia(request.dataCompetencia());
        lancamento.setDescricao(request.descricao());
        lancamento.setFormaPagamento(request.formaPagamento());
        return montarResponse(lancamentoRepository.save(lancamento));
    }

    @Transactional
    public LancamentoResponse estornar(Long lancamentoId, EstornoRequest request) {
        Lancamento original = lancamentoRepository.findById(lancamentoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento não encontrado: " + lancamentoId));

        if (original.getCategoria() == Categoria.ESTORNO) {
            throw new RegraDeNegocioException("Não é possível estornar um lançamento de estorno.");
        }
        if (lancamentoRepository.findByEstornoDe(original).isPresent()) {
            throw new RegraDeNegocioException("Lançamento já foi estornado.");
        }

        Natureza naturezaOposta = original.getNatureza() == Natureza.DEBITO ? Natureza.CREDITO : Natureza.DEBITO;
        LocalDate dataCompetencia = request.dataCompetencia() != null ? request.dataCompetencia() : LocalDate.now();

        Lancamento estorno = new Lancamento();
        estorno.setCliente(original.getCliente());
        estorno.setNatureza(naturezaOposta);
        estorno.setCategoria(Categoria.ESTORNO);
        estorno.setValor(original.getValor());
        estorno.setDataCompetencia(dataCompetencia);
        estorno.setDescricao(request.descricao() != null ? request.descricao() : "Estorno de lançamento #" + original.getId());
        estorno.setEstornoDe(original);
        estorno = lancamentoRepository.save(estorno);

        if (original.getCategoria() == Categoria.COMPRA) {
            List<MovimentacaoEstoque> itensOriginais = movimentacaoEstoqueRepository.buscarItensVenda(original.getId());
            for (MovimentacaoEstoque item : itensOriginais) {
                estoqueService.registrarEntrada(item.getProduto(), item.getQuantidade(), item.getPrecoUnitario(),
                        OrigemMovimentacao.ESTORNO, estorno.getId(), dataCompetencia);
            }
        }

        return montarResponse(estorno);
    }

    @Transactional
    public LancamentoResponse registrarVenda(VendaRequest request) {
        Cliente cliente = clienteService.encontrarOuLancar(request.clienteId());
        Produto produto = produtoService.encontrarOuLancar(request.produtoId());

        BigDecimal disponivel = estoqueService.calcularEstoque(produto.getId());
        if (disponivel.compareTo(request.quantidade()) < 0) {
            throw new RegraDeNegocioException("Estoque insuficiente para " + produto.getNome()
                    + ": disponível " + disponivel + ", solicitado " + request.quantidade());
        }

        BigDecimal preco = request.precoUnitario() != null
                ? request.precoUnitario()
                : produto.getPrecoVenda();

        BigDecimal valorTotal = request.quantidade()
                .multiply(preco)
                .setScale(2, RoundingMode.HALF_UP);

        // 1. Lançamento de débito (a venda em si)
        Lancamento lancamento = new Lancamento();
        lancamento.setCliente(cliente);
        lancamento.setNatureza(Natureza.DEBITO);
        lancamento.setCategoria(Categoria.COMPRA);
        lancamento.setValor(valorTotal);
        lancamento.setDataCompetencia(request.dataCompetencia());
        lancamento.setDescricao("Venda: " + request.quantidade() + "x " + produto.getNome());
        lancamento.setFormaPagamento(request.formaPagamento());
        lancamento = lancamentoRepository.save(lancamento);

        // 2. Saída de estoque
        estoqueService.registrarSaida(produto, request.quantidade(), preco,
                OrigemMovimentacao.VENDA, lancamento.getId(), request.dataCompetencia());

        // 3. Se NÃO é fiado, registrar pagamento automático (crédito)
        boolean fiado = request.formaPagamento() == null
                || request.formaPagamento() == FormaPagamento.FIADO;

        if (!fiado) {
            Lancamento pagamento = new Lancamento();
            pagamento.setCliente(cliente);
            pagamento.setNatureza(Natureza.CREDITO);
            pagamento.setCategoria(Categoria.PAGAMENTO);
            pagamento.setValor(valorTotal);
            pagamento.setDataCompetencia(request.dataCompetencia());
            pagamento.setDescricao("Pagamento ref. venda: " + request.quantidade() + "x " + produto.getNome());
            pagamento.setFormaPagamento(request.formaPagamento());
            lancamentoRepository.save(pagamento);
        }

        return montarResponse(lancamento);
    }

    @Transactional(readOnly = true)
    public LancamentoResponse buscarPorId(Long id) {
        return montarResponse(
                lancamentoRepository.findById(id)
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Lançamento não encontrado: " + id))
        );
    }

    private LancamentoResponse montarResponse(Lancamento lancamento) {
        List<ItemVendaResponse> itens = lancamento.getCategoria() == Categoria.COMPRA
                ? movimentacaoEstoqueRepository.buscarItensVenda(lancamento.getId()).stream()
                        .map(ItemVendaResponse::de)
                        .toList()
                : null;
        return LancamentoResponse.de(lancamento, itens);
    }

    @Transactional(readOnly = true)
    public List<LancamentoResponse> listarVendasPorMes(String mes) {
        YearMonth ym = YearMonth.parse(mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<Lancamento> vendas = lancamentoRepository
                .findByCategoriaAndDataCompetenciaBetween(Categoria.COMPRA, inicio, fim);

        return vendas.stream().map(l -> {
            List<ItemVendaResponse> itens = movimentacaoEstoqueRepository
                    .findByLancamentoClienteId(l.getId())
                    .stream()
                    .map(ItemVendaResponse::de)
                    .toList();
            return LancamentoResponse.de(l, itens);
        }).toList();

    }
}
