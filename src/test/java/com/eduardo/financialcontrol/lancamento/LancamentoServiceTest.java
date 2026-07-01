package com.eduardo.financialcontrol.lancamento;

import com.eduardo.financialcontrol.cliente.Cliente;
import com.eduardo.financialcontrol.cliente.ClienteService;
import com.eduardo.financialcontrol.estoque.EstoqueService;
import com.eduardo.financialcontrol.estoque.MovimentacaoEstoque;
import com.eduardo.financialcontrol.estoque.MovimentacaoEstoqueRepository;
import com.eduardo.financialcontrol.estoque.OrigemMovimentacao;
import com.eduardo.financialcontrol.lancamento.dto.CompraRequest;
import com.eduardo.financialcontrol.lancamento.dto.EstornoRequest;
import com.eduardo.financialcontrol.lancamento.dto.ItemCompraRequest;
import com.eduardo.financialcontrol.lancamento.dto.LancamentoResponse;
import com.eduardo.financialcontrol.lancamento.dto.PagamentoRequest;
import com.eduardo.financialcontrol.produto.Produto;
import com.eduardo.financialcontrol.produto.ProdutoService;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LancamentoServiceTest {

    @Mock LancamentoRepository lancamentoRepository;
    @Mock ClienteService clienteService;
    @Mock ProdutoService produtoService;
    @Mock EstoqueService estoqueService;
    @Mock MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @InjectMocks LancamentoService lancamentoService;

    @Test
    void registrarCompra_criaDebitoCompra() {
        // Arrange
        Cliente cliente = clienteComId(1L);
        when(clienteService.encontrarOuLancar(1L)).thenReturn(cliente);

        Produto produto = produtoComId(1L, "Rolo de tecido", new BigDecimal("100.00"));
        when(produtoService.encontrarOuLancar(1L)).thenReturn(produto);
        when(estoqueService.calcularEstoque(1L)).thenReturn(new BigDecimal("50.000"));

        CompraRequest request = new CompraRequest(
                List.of(new ItemCompraRequest(1L, new BigDecimal("10.000"), null)),
                LocalDate.now(), "Venda de tecido");

        when(lancamentoRepository.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            l.setId(10L);
            l.setCriadoEm(OffsetDateTime.now());
            return l;
        });
        when(movimentacaoEstoqueRepository.buscarItensVenda(10L)).thenReturn(List.of());

        // Act
        LancamentoResponse response = lancamentoService.registrarCompra(1L, request);

        // Assert
        assertThat(response.natureza()).isEqualTo(Natureza.DEBITO);
        assertThat(response.categoria()).isEqualTo(Categoria.COMPRA);
        assertThat(response.valor()).isEqualByComparingTo("1000.00");
        verify(estoqueService).registrarSaida(produto, new BigDecimal("10.000"), produto.getPrecoVenda(),
                OrigemMovimentacao.VENDA, 10L, request.dataCompetencia());
    }

    @Test
    void registrarCompra_estoqueInsuficiente_lancaExcecao() {
        // Arrange
        Cliente cliente = clienteComId(1L);
        when(clienteService.encontrarOuLancar(1L)).thenReturn(cliente);

        Produto produto = produtoComId(1L, "Rolo de tecido", new BigDecimal("100.00"));
        when(produtoService.encontrarOuLancar(1L)).thenReturn(produto);
        when(estoqueService.calcularEstoque(1L)).thenReturn(new BigDecimal("5.000"));

        CompraRequest request = new CompraRequest(
                List.of(new ItemCompraRequest(1L, new BigDecimal("10.000"), null)),
                LocalDate.now(), null);

        // Act & Assert
        assertThatThrownBy(() -> lancamentoService.registrarCompra(1L, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");
        verify(lancamentoRepository, never()).save(any());
    }

    @Test
    void registrarPagamento_criaCreditoPagamento() {
        // Arrange
        Cliente cliente = clienteComId(1L);
        when(clienteService.encontrarOuLancar(1L)).thenReturn(cliente);
        PagamentoRequest request = new PagamentoRequest(new BigDecimal("500.00"), LocalDate.now(), FormaPagamento.PIX, null);
        when(lancamentoRepository.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            l.setId(11L);
            l.setCriadoEm(OffsetDateTime.now());
            return l;
        });

        // Act
        LancamentoResponse response = lancamentoService.registrarPagamento(1L, request);

        // Assert
        assertThat(response.natureza()).isEqualTo(Natureza.CREDITO);
        assertThat(response.categoria()).isEqualTo(Categoria.PAGAMENTO);
        assertThat(response.formaPagamento()).isEqualTo(FormaPagamento.PIX);
    }

    @Test
    void estornar_lancamentoJaEstornado_lancaExcecao() {
        // Arrange
        Lancamento original = lancamentoComId(1L, Natureza.DEBITO, Categoria.COMPRA);
        when(lancamentoRepository.findById(1L)).thenReturn(Optional.of(original));
        when(lancamentoRepository.findByEstornoDe(original)).thenReturn(Optional.of(new Lancamento()));

        // Act & Assert
        assertThatThrownBy(() -> lancamentoService.estornar(1L, new EstornoRequest(LocalDate.now(), null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("estornado");
    }

    @Test
    void estornar_lancamentoDeEstorno_lancaExcecao() {
        // Arrange
        Lancamento estorno = lancamentoComId(1L, Natureza.CREDITO, Categoria.ESTORNO);
        when(lancamentoRepository.findById(1L)).thenReturn(Optional.of(estorno));

        // Act & Assert
        assertThatThrownBy(() -> lancamentoService.estornar(1L, new EstornoRequest(LocalDate.now(), null)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("estorno");
    }

    @Test
    void estornar_lancamentoValido_criaEstorno() {
        // Arrange
        Cliente cliente = clienteComId(1L);
        Lancamento original = lancamentoComId(5L, Natureza.DEBITO, Categoria.COMPRA);
        original.setCliente(cliente);
        original.setValor(new BigDecimal("200.00"));
        when(lancamentoRepository.findById(5L)).thenReturn(Optional.of(original));
        when(lancamentoRepository.findByEstornoDe(original)).thenReturn(Optional.empty());
        when(movimentacaoEstoqueRepository.buscarItensVenda(5L)).thenReturn(List.of());
        when(lancamentoRepository.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            l.setId(20L);
            l.setCriadoEm(OffsetDateTime.now());
            return l;
        });

        // Act
        LancamentoResponse response = lancamentoService.estornar(5L, new EstornoRequest(LocalDate.now(), null));

        // Assert
        assertThat(response.natureza()).isEqualTo(Natureza.CREDITO);
        assertThat(response.categoria()).isEqualTo(Categoria.ESTORNO);
        assertThat(response.valor()).isEqualByComparingTo("200.00");
        assertThat(response.estornoDeId()).isEqualTo(5L);
    }

    @Test
    void estornar_compraComItens_devolveEstoque() {
        // Arrange
        Cliente cliente = clienteComId(1L);
        Lancamento original = lancamentoComId(5L, Natureza.DEBITO, Categoria.COMPRA);
        original.setCliente(cliente);
        original.setValor(new BigDecimal("1000.00"));
        when(lancamentoRepository.findById(5L)).thenReturn(Optional.of(original));
        when(lancamentoRepository.findByEstornoDe(original)).thenReturn(Optional.empty());

        Produto produto = produtoComId(1L, "Rolo de tecido", new BigDecimal("100.00"));
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(produto);
        movimentacao.setQuantidade(new BigDecimal("10.000"));
        movimentacao.setPrecoUnitario(new BigDecimal("100.00"));
        when(movimentacaoEstoqueRepository.buscarItensVenda(5L)).thenReturn(List.of(movimentacao));

        LocalDate dataCompetencia = LocalDate.now();
        when(lancamentoRepository.save(any())).thenAnswer(inv -> {
            Lancamento l = inv.getArgument(0);
            l.setId(20L);
            l.setCriadoEm(OffsetDateTime.now());
            return l;
        });

        // Act
        lancamentoService.estornar(5L, new EstornoRequest(dataCompetencia, null));

        // Assert
        verify(estoqueService).registrarEntrada(produto, new BigDecimal("10.000"), new BigDecimal("100.00"),
                OrigemMovimentacao.ESTORNO, 20L, dataCompetencia);
    }

    @Test
    void buscarPorId_naoEncontrado_lancaExcecao() {
        when(lancamentoRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> lancamentoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    private Cliente clienteComId(Long id) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome("Cliente " + id);
        c.setAtivo(true);
        return c;
    }

    private Lancamento lancamentoComId(Long id, Natureza natureza, Categoria categoria) {
        Lancamento l = new Lancamento();
        l.setId(id);
        l.setNatureza(natureza);
        l.setCategoria(categoria);
        l.setValor(new BigDecimal("100.00"));
        l.setDataCompetencia(LocalDate.now());
        return l;
    }

    private Produto produtoComId(Long id, String nome, BigDecimal precoVenda) {
        Produto p = new Produto();
        p.setId(id);
        p.setNome(nome);
        p.setPrecoVenda(precoVenda);
        p.setAtivo(true);
        return p;
    }
}
