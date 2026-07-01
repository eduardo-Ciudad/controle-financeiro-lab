package com.eduardo.financialcontrol.cliente;

import com.eduardo.financialcontrol.cliente.dto.ClienteRequest;
import com.eduardo.financialcontrol.cliente.dto.ClienteResponse;
import com.eduardo.financialcontrol.lancamento.LancamentoRepository;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock ClienteRepository clienteRepository;
    @Mock LancamentoRepository lancamentoRepository;
    @InjectMocks ClienteService clienteService;

    @Test
    void criar_clienteValido_retornaResponse() {
        // Arrange
        ClienteRequest request = new ClienteRequest("João Silva", null, null, null, null);
        Cliente salvo = clienteComId(1L, "João Silva");
        when(clienteRepository.save(any())).thenReturn(salvo);

        // Act
        ClienteResponse response = clienteService.criar(request);

        // Assert
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nome()).isEqualTo("João Silva");
    }

    @Test
    void criar_documentoDuplicado_lancaExcecao() {
        // Arrange
        ClienteRequest request = new ClienteRequest("João", "123.456.789-00", null, null, null);
        Cliente existente = clienteComId(99L, "Outro");
        existente.setDocumento("123.456.789-00");
        when(clienteRepository.findByDocumento("123.456.789-00")).thenReturn(Optional.of(existente));

        // Act & Assert
        assertThatThrownBy(() -> clienteService.criar(request))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Documento");
    }

    @Test
    void buscarPorId_naoEncontrado_lancaExcecao() {
        // Arrange
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void inativar_comSaldoZero_inativa() {
        // Arrange
        Cliente cliente = clienteComId(1L, "João");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(lancamentoRepository.calcularSaldo(1L)).thenReturn(BigDecimal.ZERO);

        // Act
        clienteService.inativar(1L);

        // Assert
        assertThat(cliente.getAtivo()).isFalse();
        verify(clienteRepository).save(cliente);
    }

    @Test
    void inativar_comSaldoPositivo_lancaExcecao() {
        // Arrange
        Cliente cliente = clienteComId(1L, "João");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(lancamentoRepository.calcularSaldo(1L)).thenReturn(new BigDecimal("500.00"));

        // Act & Assert
        assertThatThrownBy(() -> clienteService.inativar(1L))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    private Cliente clienteComId(Long id, String nome) {
        Cliente c = new Cliente();
        c.setId(id);
        c.setNome(nome);
        c.setAtivo(true);
        return c;
    }
}
