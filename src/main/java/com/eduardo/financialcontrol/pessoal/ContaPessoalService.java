package com.eduardo.financialcontrol.pessoal;

import com.eduardo.financialcontrol.pessoal.dto.ContaPessoalRequest;
import com.eduardo.financialcontrol.pessoal.dto.ContaPessoalResponse;
import com.eduardo.financialcontrol.pessoal.dto.ParcelamentoRequest;
import com.eduardo.financialcontrol.pessoal.dto.ResumoMensalPessoalResponse;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
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
public class ContaPessoalService {

    private final ContaPessoalRepository contaPessoalRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public ContaPessoalResponse criar(ContaPessoalRequest request) {
        ContaPessoal conta = new ContaPessoal();
        conta.setDescricao(request.descricao());
        conta.setValor(request.valor());
        conta.setDataVencimento(request.dataVencimento());
        conta.setUsuario(usuarioAutenticadoService.getUsuario());
        return toResponse(contaPessoalRepository.save(conta));
    }

    @Transactional(readOnly = true)
    public List<ContaPessoalResponse> listar(String mes, StatusConta status) {
        LocalDate inicio;
        LocalDate fim;

        if (mes != null) {
            YearMonth ym = YearMonth.parse(mes); // "2026-06"
            inicio = ym.atDay(1);
            fim = ym.atEndOfMonth();
        } else {
            YearMonth ym = YearMonth.now();
            inicio = ym.atDay(1);
            fim = ym.atEndOfMonth();
        }

        Long usuarioId = usuarioAutenticadoService.getUsuarioId();

        List<ContaPessoal> contas;
        if (status != null) {
            contas = contaPessoalRepository
                    .findByDataVencimentoBetweenAndStatusAndUsuarioIdOrderByDataVencimentoAsc(inicio, fim, status, usuarioId);
        } else {
            contas = contaPessoalRepository
                    .findByDataVencimentoBetweenAndUsuarioIdOrderByDataVencimentoAsc(inicio, fim, usuarioId);
        }

        return contas.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ResumoMensalPessoalResponse resumo(String mes) {
        YearMonth ym = mes != null ? YearMonth.parse(mes) : YearMonth.now();
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        List<ContaPessoal> contas = contaPessoalRepository
                .findByDataVencimentoBetweenAndUsuarioIdOrderByDataVencimentoAsc(
                        inicio, fim, usuarioAutenticadoService.getUsuarioId());

        BigDecimal totalMes = contas.stream()
                .map(ContaPessoal::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPago = contas.stream()
                .filter(c -> c.getStatus() == StatusConta.PAGA)
                .map(ContaPessoal::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPendente = totalMes.subtract(totalPago);

        return new ResumoMensalPessoalResponse(
                ym.toString(),
                totalMes, totalPago, totalPendente,
                contas.size(),
                (int) contas.stream().filter(c -> c.getStatus() == StatusConta.PAGA).count(),
                (int) contas.stream().filter(c -> c.getStatus() == StatusConta.PENDENTE).count()
        );
    }

    @Transactional
    public ContaPessoalResponse pagar(Long id) {
        ContaPessoal conta = encontrarOuLancar(id);
        if (conta.getStatus() == StatusConta.PAGA) {
            throw new RegraDeNegocioException("Esta conta já está paga.");
        }
        conta.setStatus(StatusConta.PAGA);
        conta.setDataPagamento(LocalDate.now());
        return toResponse(conta);
    }

    @Transactional
    public List<ContaPessoalResponse> criarParcelamento(ParcelamentoRequest request) {
        BigDecimal valorParcela = request.valorTotal()
                .divide(BigDecimal.valueOf(request.quantidadeParcelas()), 2, RoundingMode.HALF_UP);

        // Resto dos centavos vai na última parcela
        BigDecimal somaParcelasNormais = valorParcela
                .multiply(BigDecimal.valueOf(request.quantidadeParcelas() - 1));
        BigDecimal valorUltimaParcela = request.valorTotal().subtract(somaParcelasNormais);

        List<ContaPessoal> parcelas = new ArrayList<>();

        for (int i = 1; i <= request.quantidadeParcelas(); i++) {
            ContaPessoal conta = new ContaPessoal();
            conta.setDescricao(request.descricao() + " (" + i + "/" + request.quantidadeParcelas() + ")");
            conta.setValor(i == request.quantidadeParcelas() ? valorUltimaParcela : valorParcela);
            conta.setDataVencimento(request.dataVencimentoPrimeira().plusMonths(i - 1));
            parcelas.add(conta);
        }

        parcelas.forEach(conta -> conta.setUsuario(usuarioAutenticadoService.getUsuario()));

        return contaPessoalRepository.saveAll(parcelas)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deletar(Long id) {
        ContaPessoal conta = encontrarOuLancar(id);
        contaPessoalRepository.delete(conta);
    }

    private ContaPessoal encontrarOuLancar(Long id) {
        return contaPessoalRepository.findByIdAndUsuarioId(id, usuarioAutenticadoService.getUsuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conta não encontrada."));
    }

    private ContaPessoalResponse toResponse(ContaPessoal conta) {
        return new ContaPessoalResponse(
                conta.getId(),
                conta.getDescricao(),
                conta.getValor(),
                conta.getStatus(),
                conta.getDataVencimento(),
                conta.getDataPagamento()
        );
    }
}
