package com.eduardo.financialcontrol.produto;

import com.eduardo.financialcontrol.auth.Usuario;
import com.eduardo.financialcontrol.estoque.EstoqueService;
import com.eduardo.financialcontrol.produto.dto.ProdutoRequest;
import com.eduardo.financialcontrol.produto.dto.ProdutoResponse;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    ProdutoRepository produtoRepository;

    @Mock
    EstoqueService estoqueService;

    @Mock
    UsuarioAutenticadoService usuarioAutenticadoService;

    @InjectMocks
    ProdutoService produtoService;

    private Usuario usuario;

    @BeforeEach
    void setUpUsuario() {
        usuario = new Usuario("Teste", "teste@lab.com", "hash");
        ReflectionTestUtils.setField(usuario, "id", 1L);
        lenient().when(usuarioAutenticadoService.getUsuario()).thenReturn(usuario);
        lenient().when(usuarioAutenticadoService.getUsuarioId()).thenReturn(1L);
    }

    @Test
    void criar_comPrecoVenda_naoAplicaMarkup() {
        ProdutoRequest request = new ProdutoRequest(
                "Tecido Liso", "Tecido Algodao",
                new BigDecimal("100.00"),
                new BigDecimal("70.00")

        );

        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(estoqueService.calcularEstoque(1L))
                .thenReturn( BigDecimal.ZERO);

        ProdutoResponse response = produtoService.criar(request);

        assertThat(response.nome()).isEqualTo("Tecido Liso");
        assertThat(response.precoVenda())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void criar_semPrecoVenda_aplicaMarkup30Porcento() {
        ProdutoRequest request = new ProdutoRequest(
                "tecido", null,
                null,
                new BigDecimal("100.00")

        );

        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        when(estoqueService.calcularEstoque(1L))
                .thenReturn( BigDecimal.ZERO);

        ProdutoResponse response = produtoService.criar(request);

        assertThat(response.precoVenda())
        .isEqualByComparingTo("130.00");
    }

    @Test
    void crir_precoVendaZero_aplicaMarkup() {
        ProdutoRequest request = new ProdutoRequest(
                "Rolo seda", null, BigDecimal.ZERO,new  BigDecimal("80.00")
        );

        when(produtoRepository.save(any())).thenAnswer(inv -> {
            Produto p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        when(estoqueService.calcularEstoque(1L))
                .thenReturn(BigDecimal.ZERO);

        // Act
        ProdutoResponse response = produtoService.criar(request);

        // Assert
        assertThat(response.precoVenda())
                .isEqualByComparingTo("104.00");
    }

    // ========== BUSCAR ==========

    @Test
    void buscarPorId_produtoAtivo_retornaResponse() {
        // Arrange
        Produto produto = produtoComId(1L, "Tecido", "100.00");
        when(produtoRepository.findByIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(produto));
        when(estoqueService.calcularEstoque(1L))
                .thenReturn(new BigDecimal("25.000"));

        // Act
        ProdutoResponse response = produtoService.buscarPorId(1L);

        // Assert
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("Tecido");
    }

    @Test
    void buscarPorId_produtoInexistente_lancaExcecao() {
        when(produtoRepository.findByIdAndUsuarioId(99L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> produtoService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void buscarPorId_produtoInativo_lancaExcecao() {
        // Arrange
        Produto produto = produtoComId(1L, "Tecido", "100.00");
        produto.setAtivo(false); // inativo!

        when(produtoRepository.findByIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(produto));

        // Assert
        assertThatThrownBy(() -> produtoService.buscarPorId(1L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ========== ATUALIZAR ==========

    @Test
    void atualizar_produtoExistente_atualizaCampos() {
        // Arrange
        Produto produto = produtoComId(1L, "Nome Antigo", "80.00");
        when(produtoRepository.findByIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(produto));
        when(produtoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(estoqueService.calcularEstoque(1L))
                .thenReturn(BigDecimal.ZERO);

        ProdutoRequest request = new ProdutoRequest(
                "Nome Novo", "Descrição nova",
                new BigDecimal("150.00"), new BigDecimal("100.00")
        );

        // Act
        ProdutoResponse response = produtoService.atualizar(1L, request);

        // Assert
        assertThat(response.nome()).isEqualTo("Nome Novo");
        assertThat(response.precoVenda())
                .isEqualByComparingTo("150.00");
    }

    // ========== INATIVAR ==========

    @Test
    void inativar_produtoAtivo_setaAtivoFalse() {
        // Arrange
        Produto produto = produtoComId(1L, "Tecido", "100.00");
        when(produtoRepository.findByIdAndUsuarioId(1L, 1L))
                .thenReturn(Optional.of(produto));

        // Act
        produtoService.inativar(1L);

        // Assert
        assertThat(produto.getAtivo()).isFalse();
        verify(produtoRepository).save(produto);
    }

    // ========== LISTAR ==========

    @Test
    void listar_semFiltro_buscaTodosAtivos() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Produto produto = produtoComId(1L, "Tecido", "100.00");
        when(produtoRepository.findByAtivoTrueAndUsuarioId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(produto)));
        when(estoqueService.calcularEstoque(1L))
                .thenReturn(BigDecimal.ZERO);

        // Act
        Page<ProdutoResponse> page = produtoService.listar(null, pageable);

        // Assert
        assertThat(page.getContent()).hasSize(1);
        verify(produtoRepository, never()).buscarAtivosPorNome(any(), any(), any());
    }

    @Test
    void listar_comNome_filtraPorNome() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(produtoRepository.buscarAtivosPorNome("seda", 1L, pageable))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        produtoService.listar("seda", pageable);

        // Assert
        verify(produtoRepository).buscarAtivosPorNome("seda", 1L, pageable);
        verify(produtoRepository, never()).findByAtivoTrueAndUsuarioId(any(), any());
    }

    // ========== HELPER ==========

    private Produto produtoComId(Long id, String nome, String precoVenda) {
        Produto p = new Produto(usuario);
        p.setId(id);
        p.setNome(nome);
        p.setPrecoVenda(new BigDecimal(precoVenda));
        p.setAtivo(true);
        return p;
    }

}
