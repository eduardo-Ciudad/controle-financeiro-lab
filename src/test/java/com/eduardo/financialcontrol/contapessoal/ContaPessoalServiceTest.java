package com.eduardo.financialcontrol.contapessoal;

import com.eduardo.financialcontrol.pessoal.ContaPessoal;
import com.eduardo.financialcontrol.pessoal.ContaPessoalRepository;
import com.eduardo.financialcontrol.pessoal.ContaPessoalService;
import com.eduardo.financialcontrol.pessoal.StatusConta;
import com.eduardo.financialcontrol.pessoal.dto.ContaPessoalRequest;
import com.eduardo.financialcontrol.pessoal.dto.ContaPessoalResponse;
import com.eduardo.financialcontrol.pessoal.dto.ParcelamentoRequest;
import com.eduardo.financialcontrol.pessoal.dto.ResumoMensalPessoalResponse;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class ContaPessoalServiceTest {

    @Mock
    ContaPessoalRepository contaPessoalRepository;

    @InjectMocks
    ContaPessoalService contaPessoalService;

    @Test
    void criar_contaValida_retornaResponse() {
        ContaPessoalRequest request = new ContaPessoalRequest(
                "Aluguel", new BigDecimal("1500.00"),
                LocalDate.of(2026, 7, 10)
        );
        when(contaPessoalRepository.save(any())).thenAnswer(inv -> {
            ContaPessoal c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        // Act
        ContaPessoalResponse response = contaPessoalService.criar(request);

        // Assert
        assertThat(response.descricao()).isEqualTo("Aluguel");
        assertThat(response.valor()).isEqualByComparingTo("1500.00");
        assertThat(response.status()).isEqualTo(StatusConta.PENDENTE);
    }

   // ========== PAGAR ==========

    @Test

    void pagar_contaPendente_marcaComoPaga() {
        // Arrange
        ContaPessoal conta = contaComId(1L, "Internet", "120.00",
                StatusConta.PENDENTE);
        when(contaPessoalRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        // Act
        ContaPessoalResponse response = contaPessoalService.pagar(1L);

        // Assert
        assertThat(response.status()).isEqualTo(StatusConta.PAGA);
        assertThat(response.dataPagamento()).isEqualTo(LocalDate.now());
    }

    @Test

    void pagar_contaJaPaga_lancaExcecao() {
        // Arrange
        ContaPessoal conta = contaComId(1L, "Internet", "120.00",
                StatusConta.PAGA);
        when(contaPessoalRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        // Act & Assert
        assertThatThrownBy(() -> contaPessoalService.pagar(1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já está paga");
    }

    @Test
    void pagar_contaInexistente_lancaExcecao() {
        when(contaPessoalRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> contaPessoalService.pagar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

     // ========== PARCELAMENTO ==========

    @Test

    void criarParcelamento_divisaoExata_todasParcelasIguais() {
        // Arrange — R$ 300,00 / 3 = R$ 100,00 exatos
        ParcelamentoRequest request = new ParcelamentoRequest(
                "Notebook", new BigDecimal("300.00"),
                3, LocalDate.of(2026, 7, 10)
        );

        when(contaPessoalRepository.saveAll(any()))
                .thenAnswer(inv -> {
                    List<ContaPessoal> lista = inv.getArgument(0);
                    long id = 1;
                    for (ContaPessoal c : lista) c.setId(id++);
                    return lista;
                });

        // Act
        List<ContaPessoalResponse> parcelas =
                contaPessoalService.criarParcelamento(request);

        // Assert
        assertThat(parcelas).hasSize(3);
        assertThat(parcelas.get(0).valor()).isEqualByComparingTo("100.00");
        assertThat(parcelas.get(1).valor()).isEqualByComparingTo("100.00");
        assertThat(parcelas.get(2).valor()).isEqualByComparingTo("100.00");
    }

   @Test

    void criarParcelamento_divisaoComResto_centavosNaUltimaParcela() {
        // Arrange — R$ 100,00 / 3 = R$ 33,33...
        // Parcelas 1 e 2: R$ 33,33
        // Parcela 3:       R$ 33,34 (absorve o centavo)
        ParcelamentoRequest request = new ParcelamentoRequest(
                "Curso Java", new BigDecimal("100.00"),
                3, LocalDate.of(2026, 8, 1)
        );

        when(contaPessoalRepository.saveAll(any()))
                .thenAnswer(inv -> {
                    List<ContaPessoal> lista = inv.getArgument(0);
                    long id = 1;
                    for (ContaPessoal c : lista) c.setId(id++);
                    return lista;
                });

        // Act
        List<ContaPessoalResponse> parcelas =
                contaPessoalService.criarParcelamento(request);

        // Assert
        assertThat(parcelas).hasSize(3);
        assertThat(parcelas.get(0).valor()).isEqualByComparingTo("33.33");
        assertThat(parcelas.get(1).valor()).isEqualByComparingTo("33.33");
        assertThat(parcelas.get(2).valor()).isEqualByComparingTo("33.34");

        // Garantia extra: soma das parcelas == valor total
        BigDecimal soma = parcelas.stream()
                .map(ContaPessoalResponse::valor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("100.00");
    }

    @Test

    void criarParcelamento_datasIncrementaisMensais() {
        // Arrange
        ParcelamentoRequest request = new ParcelamentoRequest(
                "Celular", new BigDecimal("600.00"),
                3, LocalDate.of(2026, 7, 15)
        );

        when(contaPessoalRepository.saveAll(any()))
                .thenAnswer(inv -> {
                    List<ContaPessoal> lista = inv.getArgument(0);
                    long id = 1;
                    for (ContaPessoal c : lista) c.setId(id++);
                    return lista;
                });

        // Act
        List<ContaPessoalResponse> parcelas =
                contaPessoalService.criarParcelamento(request);

        // Assert
        assertThat(parcelas.get(0).dataVencimento())
                .isEqualTo(LocalDate.of(2026, 7, 15));
        assertThat(parcelas.get(1).dataVencimento())
                .isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(parcelas.get(2).dataVencimento())
                .isEqualTo(LocalDate.of(2026, 9, 15));
    }

   @Test

    void criarParcelamento_descricaoComNumeracao() {
        // Arrange
        ParcelamentoRequest request = new ParcelamentoRequest(
                "Monitor", new BigDecimal("900.00"),
                3, LocalDate.of(2026, 7, 1)
        );

        when(contaPessoalRepository.saveAll(any()))
                .thenAnswer(inv -> {
                    List<ContaPessoal> lista = inv.getArgument(0);
                    long id = 1;
                    for (ContaPessoal c : lista) c.setId(id++);
                    return lista;
                });

        // Act
        List<ContaPessoalResponse> parcelas =
                contaPessoalService.criarParcelamento(request);

        // Assert
        assertThat(parcelas.get(0).descricao()).isEqualTo("Monitor (1/3)");
        assertThat(parcelas.get(1).descricao()).isEqualTo("Monitor (2/3)");
        assertThat(parcelas.get(2).descricao()).isEqualTo("Monitor (3/3)");
    }

       // ========== RESUMO ==========

    @Test

    void resumo_contasMistas_calculaTotaisCorretos() {
        // Arrange
        ContaPessoal paga = contaComId(1L, "Internet", "120.00",
                StatusConta.PAGA);
        ContaPessoal pendente = contaComId(2L, "Aluguel", "1500.00",
                StatusConta.PENDENTE);

        YearMonth mesAtual = YearMonth.now();
        paga.setDataVencimento(mesAtual.atDay(5));
        pendente.setDataVencimento(mesAtual.atDay(10));

        when(contaPessoalRepository
                .findByDataVencimentoBetweenOrderByDataVencimentoAsc(
                        mesAtual.atDay(1), mesAtual.atEndOfMonth()))
                .thenReturn(List.of(paga, pendente));

        // Act
        ResumoMensalPessoalResponse resumo =
                contaPessoalService.resumo(null); // null = mês atual

        // Assert
        assertThat(resumo.totalMes()).isEqualByComparingTo("1620.00");
        assertThat(resumo.totalPago()).isEqualByComparingTo("120.00");
        assertThat(resumo.totalPendente()).isEqualByComparingTo("1500.00");

    }
 // ========== DELETAR ==========

    @Test

    void deletar_contaExistente_chamaDelete() {
        // Arrange
        ContaPessoal conta = contaComId(1L, "Internet", "120.00",
                StatusConta.PENDENTE);
        when(contaPessoalRepository.findById(1L))
                .thenReturn(Optional.of(conta));

        // Act
        contaPessoalService.deletar(1L);

        // Assert
        verify(contaPessoalRepository).delete(conta);
    }

    @Test
    void deletar_contaInexistente_lancaExcecao() {
        when(contaPessoalRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> contaPessoalService.deletar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

      // ========== LISTAR ==========

    @Test

    void listar_comMesEStatus_filtraCorretamente() {
        // Arrange
        when(contaPessoalRepository
                .findByDataVencimentoBetweenAndStatusOrderByDataVencimentoAsc(
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 30),
                        StatusConta.PENDENTE))
                .thenReturn(List.of());

        // Act
        List<ContaPessoalResponse> result =
                contaPessoalService.listar("2026-06", StatusConta.PENDENTE);

        // Assert
        assertThat(result).isEmpty();
        verify(contaPessoalRepository, never())
                .findByDataVencimentoBetweenOrderByDataVencimentoAsc(any(), any());
    }

    @Test
    void listar_semMes_usaMesAtual() {
        // Arrange
        YearMonth atual = YearMonth.now();
        when(contaPessoalRepository
                .findByDataVencimentoBetweenOrderByDataVencimentoAsc(
                        atual.atDay(1), atual.atEndOfMonth()))
                .thenReturn(List.of());

        // Act
        contaPessoalService.listar(null, null);

        // Assert — confirma que usou o mês atual como fallback
        verify(contaPessoalRepository)
                .findByDataVencimentoBetweenOrderByDataVencimentoAsc(
                        atual.atDay(1), atual.atEndOfMonth());
    }


    private ContaPessoal contaComId(Long id, String descricao,
                                    String valor, StatusConta status) {
        ContaPessoal c = new ContaPessoal();
        c.setId(id);
        c.setDescricao(descricao);
        c.setValor(new BigDecimal(valor));
        c.setStatus(status);
        c.setDataVencimento(LocalDate.now());
        return c;


    }
}