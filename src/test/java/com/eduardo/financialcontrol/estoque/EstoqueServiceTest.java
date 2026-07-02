package com.eduardo.financialcontrol.estoque;


import com.eduardo.financialcontrol.auth.Usuario;
import com.eduardo.financialcontrol.estoque.dto.AjusteEstoqueRequest;
import com.eduardo.financialcontrol.estoque.dto.EstoqueResponse;
import com.eduardo.financialcontrol.produto.Produto;
import com.eduardo.financialcontrol.produto.ProdutoRepository;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    @Mock
    private MovimentacaoEstoque movimentacaoEstoque;

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;

    @InjectMocks
    private EstoqueService estoqueService;

    private Produto produto;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario("Teste", "teste@lab.com", "hash");
        ReflectionTestUtils.setField(usuario, "id", 1L);
        lenient().when(usuarioAutenticadoService.getUsuario()).thenReturn(usuario);
        lenient().when(usuarioAutenticadoService.getUsuarioId()).thenReturn(1L);

        produto = new Produto(usuario);
        produto.setId(1L);
        produto.setNome("Tecido Algodão");
        produto.setPrecoVenda(new BigDecimal("25.00"));
        produto.setPrecoCusto(new BigDecimal("15.00"));
        produto.setAtivo(true);
    }


    @Test
    void cacularEstoque_deveRetornarQuantidadeDoRepository() {

        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L))
                .thenReturn(new BigDecimal("50.000"));

        BigDecimal resultado = estoqueService.calcularEstoque(1L);

        assertThat(resultado).isEqualByComparingTo("50.000");
        verify(movimentacaoEstoqueRepository).calcularEstoque(1L, 1L);
    }


    @Test
    void registrarEntrada_deveSalvarComTipoEntradaLancamentoCliente() {
        Long lancamentoClienteId = 1L;
        LocalDate data = LocalDate.of(2026, 6, 27);

        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {
                    MovimentacaoEstoque mov = invocation.getArgument(0);
                    mov.setId(1L);
                    return mov;
                });

        MovimentacaoEstoque resultado = estoqueService.registrarEntrada(
                produto, new BigDecimal("10"), new BigDecimal("25.00"), OrigemMovimentacao.COMPRA, lancamentoClienteId, data
        );

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque salvo = captor.getValue();
        assertThat(salvo.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(salvo.getProduto()).isEqualTo(produto);
        assertThat(salvo.getQuantidade()).isEqualByComparingTo("10");
        assertThat(salvo.getPrecoUnitario()).isEqualByComparingTo("25.00");
        assertThat(salvo.getOrigem()).isEqualTo(OrigemMovimentacao.COMPRA);
        assertThat(salvo.getLancamentoClienteId()).isEqualTo(lancamentoClienteId);
        assertThat(salvo.getLancamentoFornecedorId()).isNull(); // <-- campo do fornecedor deve ser null
        assertThat(salvo.getDataCompetencia()).isEqualTo(data);
    }

    @Test
    void registrarSaida_deveSalvarComTipoSaidaELancamentoCliente() {
        Long lancamentoClienteId = 20L;
        LocalDate data = LocalDate.of(2026, 6, 27);

        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {
                    MovimentacaoEstoque mov = invocation.getArgument(0);
                    mov.setId(2L);
                    return mov;
                });

        estoqueService.registrarSaida(
                produto, new BigDecimal("5"), new BigDecimal("30.00"),
                OrigemMovimentacao.VENDA, lancamentoClienteId, data);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque salvo = captor.getValue();
        assertThat(salvo.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(salvo.getLancamentoClienteId()).isEqualTo(lancamentoClienteId);
        assertThat(salvo.getLancamentoFornecedorId()).isNull();
    }

    // ========== registrarEntradaFornecedor / registrarSaidaFornecedor ==========
    // Mesma lógica, mas aqui o ID deve ir pro campo lancamentoFornecedorId
    // e lancamentoClienteId deve ser null. Se alguém inverter os parâmetros
    // no salvar(), esses testes pegam.

    @Test
    void registrarEntradaFornecedor_deveSalvarComLancamentoFornecedor() {
        Long lancamentoFornecedorId = 30L;
        LocalDate data = LocalDate.of(2026, 6, 27);

        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {
                    MovimentacaoEstoque mov = invocation.getArgument(0);
                    mov.setId(3L);
                    return mov;
                });

        estoqueService.registrarEntradaFornecedor(
                produto, new BigDecimal("100"), new BigDecimal("15.00"),
                OrigemMovimentacao.COMPRA, lancamentoFornecedorId, data);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque salvo = captor.getValue();
        assertThat(salvo.getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(salvo.getLancamentoFornecedorId()).isEqualTo(lancamentoFornecedorId);
        assertThat(salvo.getLancamentoClienteId()).isNull(); // <-- campo do cliente deve ser null
    }

    @Test
    void registrarSaidaFornecedor_deveSalvarComTipoSaidaELancamentoFornecedor() {
        Long lancamentoFornecedorId = 40L;
        LocalDate data = LocalDate.of(2026, 6, 27);

        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {
                    MovimentacaoEstoque mov = invocation.getArgument(0);
                    mov.setId(4L);
                    return mov;
                });

        estoqueService.registrarSaidaFornecedor(
                produto, new BigDecimal("20"), new BigDecimal("15.00"),
                OrigemMovimentacao.ESTORNO, lancamentoFornecedorId, data);

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());

        MovimentacaoEstoque salvo = captor.getValue();
        assertThat(salvo.getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(salvo.getLancamentoFornecedorId()).isEqualTo(lancamentoFornecedorId);
        assertThat(salvo.getLancamentoClienteId()).isNull();
    }

    // ========== valorEmEstoque ==========
    // Cálculo financeiro: qtd * precoVenda, arredondado scale 2 HALF_UP.
    // O teste com casas decimais "feias" é proposital — BigDecimal sem
    // arredondamento explícito causa ArithmeticException ou valores errados.

    @Test
    void valorEmEstoque_deveCalcularQuantidadeVezesPrecoVenda() {
        // 10 * 25.00 = 250.00
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L))
                .thenReturn(new BigDecimal("10"));

        BigDecimal valor = estoqueService.valorEmEstoque(produto);

        assertThat(valor).isEqualByComparingTo("250.00");
        assertThat(valor.scale()).isEqualTo(2); // garante que o scale tá certo
    }

    @Test
    void valorEmEstoque_deveArredondarCorretamente() {

        produto.setPrecoVenda(new BigDecimal("25.00"));
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L))
                .thenReturn(new BigDecimal("3.333"));

        BigDecimal valor = estoqueService.valorEmEstoque(produto);

        assertThat(valor).isEqualByComparingTo("83.33");
    }


    @Test
    void calcularValorTotalEstoque_deveCalcularCustoEVenda() {
        Produto produto2 = new Produto(usuario);
        produto2.setId(2L);
        produto2.setNome("Tecido Seda");
        produto2.setPrecoVenda(new BigDecimal("50.00"));
        produto2.setPrecoCusto(new BigDecimal("30.00"));
        produto2.setAtivo(true);

        when(produtoRepository.findAllByAtivoTrueAndUsuarioId(1L)).thenReturn(List.of(produto, produto2));
        // produto1: 10 unidades → custo 150.00, venda 250.00
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L)).thenReturn(new BigDecimal("10"));
        // produto2: 5 unidades → custo 150.00, venda 250.00
        when(movimentacaoEstoqueRepository.calcularEstoque(2L, 1L)).thenReturn(new BigDecimal("5"));

        var resultado = estoqueService.calcularValorTotalEstoque();

        // totalCusto = (10*15) + (5*30) = 150 + 150 = 300.00
        // totalVenda = (10*25) + (5*50) = 250 + 250 = 500.00
        assertThat(resultado.get("totalCusto")).isEqualByComparingTo("300.00");
        assertThat(resultado.get("totalVenda")).isEqualByComparingTo("500.00");
    }

    @Test
    void calcularValorTotalEstoque_deveIgnorarProdutoComEstoqueZero() {
        // Produto com estoque zero não deve entrar na soma.
        // Esse cenário protege contra o caso de alguém remover o if > 0.
        when(produtoRepository.findAllByAtivoTrueAndUsuarioId(1L)).thenReturn(List.of(produto));
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L)).thenReturn(BigDecimal.ZERO);

        var resultado = estoqueService.calcularValorTotalEstoque();

        assertThat(resultado.get("totalCusto")).isEqualByComparingTo("0.00");
        assertThat(resultado.get("totalVenda")).isEqualByComparingTo("0.00");
    }

    @Test
    void calcularValorTotalEstoque_deveCalcularVendaMesmoPrecoCustoNull() {

        produto.setPrecoCusto(null);

        when(produtoRepository.findAllByAtivoTrueAndUsuarioId(1L)).thenReturn(List.of(produto));
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L)).thenReturn(new BigDecimal("10"));
        var resultado = estoqueService.calcularValorTotalEstoque();

        assertThat(resultado.get("totalCusto")).isEqualByComparingTo("0.00");
        assertThat(resultado.get("totalVenda")).isEqualByComparingTo("250.00");
    }

    // ========== consultarEstoque ==========

    @Test
    void consultarEstoque_deveRetornarEstoqueResponse() {
        when(produtoRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(produto));
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L)).thenReturn(new BigDecimal("10"));

        EstoqueResponse response = estoqueService.consultarEstoque(1L);

        assertThat(response.produtoId()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Tecido Algodão");
        assertThat(response.quantidadeAtual()).isEqualByComparingTo("10");
        // 10 * 25.00 = 250.00
        assertThat(response.valorEmEstoque()).isEqualByComparingTo("250.00");
    }

    @Test
    void consultarEstoque_deveLancarExcecaoQuandoProdutoNaoExiste() {
        when(produtoRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoqueService.consultarEstoque(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("99");
    }



    @Test
    void registrarAjuste_saida_comEstoqueSuficiente_deveRegistrar() {
        var request = new AjusteEstoqueRequest(TipoMovimentacao.SAIDA, new BigDecimal("5"), "Perda");

        when(produtoRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(produto));

        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L))
                .thenReturn(new BigDecimal("10"))  // pra validação
                .thenReturn(new BigDecimal("5"));   // pós-ajuste, pro retorno

        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {
                    MovimentacaoEstoque mov = invocation.getArgument(0);
                    mov.setId(1L);
                    return mov;
                });

        EstoqueResponse response = estoqueService.registrarAjuste(1L, request);

        assertThat(response.quantidadeAtual()).isEqualByComparingTo("5");
        // 5 * 25.00 = 125.00
        assertThat(response.valorEmEstoque()).isEqualByComparingTo("125.00");

        // Verifica que salvou com tipo SAIDA e origem AJUSTE
        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(captor.getValue().getOrigem()).isEqualTo(OrigemMovimentacao.AJUSTE);
    }

    @Test
    void registrarAjuste_saida_comEstoqueInsuficiente_deveLancarExcecao() {
        var request = new AjusteEstoqueRequest(TipoMovimentacao.SAIDA, new BigDecimal("20"), "Perda");

        when(produtoRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(produto));
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L))
                .thenReturn(new BigDecimal("5")); // só tem 5, quer tirar 20

        assertThatThrownBy(() -> estoqueService.registrarAjuste(1L, request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Estoque insuficiente");

        // Garante que NÃO salvou nada — a transação nem deve acontecer
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    @Test
    void registrarAjuste_entrada_deveRegistrarSemValidarEstoque() {
        var request = new AjusteEstoqueRequest(TipoMovimentacao.ENTRADA, new BigDecimal("50"), "Reposição");

        when(produtoRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(produto));
        // Só precisa de uma chamada: consultarEstoque no retorno
        when(movimentacaoEstoqueRepository.calcularEstoque(1L, 1L))
                .thenReturn(new BigDecimal("50"));

        when(movimentacaoEstoqueRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(invocation -> {
                    MovimentacaoEstoque mov = invocation.getArgument(0);
                    mov.setId(1L);
                    return mov;
                });

        EstoqueResponse response = estoqueService.registrarAjuste(1L, request);

        assertThat(response.quantidadeAtual()).isEqualByComparingTo("50");

        ArgumentCaptor<MovimentacaoEstoque> captor = ArgumentCaptor.forClass(MovimentacaoEstoque.class);
        verify(movimentacaoEstoqueRepository).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoMovimentacao.ENTRADA);
    }

    @Test
    void registrarAjuste_produtoInativo_deveLancarExcecao() {
        produto.setAtivo(false);
        var request = new AjusteEstoqueRequest(TipoMovimentacao.ENTRADA, new BigDecimal("10"), "Teste");

        when(produtoRepository.findByIdAndUsuarioId(1L, 1L)).thenReturn(Optional.of(produto));

        assertThatThrownBy(() -> estoqueService.registrarAjuste(1L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    @Test
    void registrarAjuste_produtoInexistente_deveLancarExcecao() {
        var request = new AjusteEstoqueRequest(TipoMovimentacao.ENTRADA, new BigDecimal("10"), "Teste");

        when(produtoRepository.findByIdAndUsuarioId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> estoqueService.registrarAjuste(99L, request))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(movimentacaoEstoqueRepository, never()).save(any());
    }
}

