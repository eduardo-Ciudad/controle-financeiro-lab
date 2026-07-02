package com.eduardo.financialcontrol.saldo;

import com.eduardo.financialcontrol.auth.Usuario;
import com.eduardo.financialcontrol.cliente.Cliente;
import com.eduardo.financialcontrol.cliente.ClienteRepository;
import com.eduardo.financialcontrol.cliente.ClienteService;
import com.eduardo.financialcontrol.lancamento.Categoria;
import com.eduardo.financialcontrol.lancamento.Lancamento;
import com.eduardo.financialcontrol.lancamento.LancamentoRepository;
import com.eduardo.financialcontrol.lancamento.Natureza;
import com.eduardo.financialcontrol.saldo.dto.ResumoDashboard;
import com.eduardo.financialcontrol.saldo.dto.SaldoClienteResponse;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaldoServiceTest {

    @Mock LancamentoRepository lancamentoRepository;
    @Mock ClienteRepository clienteRepository;
    @Mock ClienteService clienteService;
    @Mock UsuarioAutenticadoService usuarioAutenticadoService;
    @InjectMocks SaldoService saldoService;

    private Usuario usuario;

    @BeforeEach
    void setUpUsuario() {
        usuario = new Usuario("Teste", "teste@lab.com", "hash");
        ReflectionTestUtils.setField(usuario, "id", 1L);
        lenient().when(usuarioAutenticadoService.getUsuario()).thenReturn(usuario);
        lenient().when(usuarioAutenticadoService.getUsuarioId()).thenReturn(1L);
    }

    @Test
    void saldoCliente_saldoPositivo_situacaoDevedor() {
        // Arrange
        Cliente cliente = clienteComId(1L, "João");
        when(clienteService.encontrarOuLancar(1L)).thenReturn(cliente);
        when(lancamentoRepository.calcularSaldo(1L, 1L)).thenReturn(new BigDecimal("8000.00"));

        // Act
        SaldoClienteResponse response = saldoService.saldoCliente(1L);

        // Assert
        assertThat(response.situacao()).isEqualTo(SituacaoSaldo.DEVEDOR);
        assertThat(response.saldo()).isEqualByComparingTo("8000.00");
        assertThat(response.valorAbsoluto()).isEqualByComparingTo("8000.00");
    }

    @Test
    void saldoCliente_saldoNegativo_situacaoCredor() {
        // Arrange
        Cliente cliente = clienteComId(1L, "Maria");
        when(clienteService.encontrarOuLancar(1L)).thenReturn(cliente);
        when(lancamentoRepository.calcularSaldo(1L, 1L)).thenReturn(new BigDecimal("-500.00"));

        // Act
        SaldoClienteResponse response = saldoService.saldoCliente(1L);

        // Assert
        assertThat(response.situacao()).isEqualTo(SituacaoSaldo.CREDOR);
        assertThat(response.valorAbsoluto()).isEqualByComparingTo("500.00");
    }

    @Test
    void saldoCliente_saldoZero_situacaoQuitado() {
        // Arrange
        Cliente cliente = clienteComId(1L, "Pedro");
        when(clienteService.encontrarOuLancar(1L)).thenReturn(cliente);
        when(lancamentoRepository.calcularSaldo(1L, 1L)).thenReturn(BigDecimal.ZERO);

        // Act
        SaldoClienteResponse response = saldoService.saldoCliente(1L);

        // Assert
        assertThat(response.situacao()).isEqualTo(SituacaoSaldo.QUITADO);
    }

    @Test
    void dashboard_retornaTotaisCorretos() {
        // Arrange
        Cliente c1 = clienteComId(1L, "Devedor A");
        Cliente c2 = clienteComId(2L, "Devedor B");
        when(clienteRepository.findByAtivoTrueAndUsuarioId(1L, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(c1, c2)));
        when(lancamentoRepository.calcularSaldo(1L, 1L)).thenReturn(new BigDecimal("3000.00"));
        when(lancamentoRepository.calcularSaldo(2L, 1L)).thenReturn(new BigDecimal("1000.00"));

        // Act
        ResumoDashboard dashboard = saldoService.dashboard();

        // Assert
        assertThat(dashboard.totalAReceber()).isEqualByComparingTo("4000.00");
        assertThat(dashboard.qtdDevedores()).isEqualTo(2);
        assertThat(dashboard.topDevedores().get(0).nome()).isEqualTo("Devedor A");
    }

    @Test
    void extrato_calculaSaldoAcumulado() {
        // Arrange
        Cliente cliente = clienteComId(1L, "João");
        when(clienteService.encontrarOuLancar(1L)).thenReturn(cliente);

        Lancamento compra = lancamento(1L, cliente, Natureza.DEBITO, Categoria.COMPRA, "20000.00");
        Lancamento pagamento = lancamento(2L, cliente, Natureza.CREDITO, Categoria.PAGAMENTO, "10000.00");

        PageRequest pageable = PageRequest.of(0, 10);
        when(lancamentoRepository.findByClienteIdAndUsuarioIdOrderByDataCompetenciaAscIdAsc(1L, 1L, pageable))
                .thenReturn(new PageImpl<>(List.of(compra, pagamento)));

        // Act
        var page = saldoService.extrato(1L, pageable);

        // Assert
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).saldoAcumulado()).isEqualByComparingTo("20000.00");
        assertThat(page.getContent().get(1).saldoAcumulado()).isEqualByComparingTo("10000.00");
    }

    private Cliente clienteComId(Long id, String nome) {
        Cliente c = new Cliente(usuario);
        c.setId(id);
        c.setNome(nome);
        c.setAtivo(true);
        return c;
    }

    private Lancamento lancamento(Long id, Cliente cliente, Natureza natureza, Categoria categoria, String valor) {
        Lancamento l = new Lancamento(usuario);
        l.setId(id);
        l.setCliente(cliente);
        l.setNatureza(natureza);
        l.setCategoria(categoria);
        l.setValor(new BigDecimal(valor));
        l.setDataCompetencia(LocalDate.now());
        l.setCriadoEm(OffsetDateTime.now());
        return l;
    }
}
