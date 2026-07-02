package com.eduardo.financialcontrol.saldo;

import com.eduardo.financialcontrol.cliente.Cliente;
import com.eduardo.financialcontrol.cliente.ClienteRepository;
import com.eduardo.financialcontrol.cliente.ClienteService;
import com.eduardo.financialcontrol.fornecedor.LancamentoFornecedor;
import com.eduardo.financialcontrol.fornecedor.LancamentoFornecedorRepository;
import com.eduardo.financialcontrol.lancamento.Lancamento;
import com.eduardo.financialcontrol.lancamento.LancamentoRepository;
import com.eduardo.financialcontrol.lancamento.Natureza;
import com.eduardo.financialcontrol.saldo.dto.*;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
@Service
@RequiredArgsConstructor
public class SaldoService {

    private final LancamentoFornecedorRepository lancamentoFornecedorRepository;
    private final LancamentoRepository lancamentoRepository;
    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional(readOnly = true)
    public SaldoClienteResponse saldoCliente(Long clienteId) {
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();
        Cliente cliente = clienteService.encontrarOuLancar(clienteId);
        BigDecimal saldo = lancamentoRepository.calcularSaldo(clienteId, usuarioId);
        return toResponse(cliente.getId(), cliente.getNome(), saldo);
    }

    @Transactional(readOnly = true)
    public Page<LinhaExtrato> extrato(Long clienteId, Pageable pageable) {
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();
        clienteService.encontrarOuLancar(clienteId);
        Page<Lancamento> page = lancamentoRepository
                .findByClienteIdAndUsuarioIdOrderByDataCompetenciaAscIdAsc(clienteId, usuarioId, pageable);

        BigDecimal acumulado = BigDecimal.ZERO;
        List<LinhaExtrato> linhas = new ArrayList<>();
        for (Lancamento l : page.getContent()) {
            if (l.getNatureza() == Natureza.DEBITO) {
                acumulado = acumulado.add(l.getValor());
            } else {
                acumulado = acumulado.subtract(l.getValor());
            }
            linhas.add(new LinhaExtrato(
                    l.getId(), l.getNatureza(), l.getCategoria(),
                    l.getValor(), acumulado, l.getDataCompetencia(),
                    l.getDescricao(), l.getFormaPagamento(), l.getCriadoEm()
            ));
        }
        return new PageImpl<>(linhas, pageable, page.getTotalElements());
    }

    @Transactional(readOnly = true)
    public ResumoDiario resumoDiario(LocalDate data) {
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();

        List<Lancamento> lancamentos = lancamentoRepository
                .findByDataCompetenciaAndUsuarioIdOrderByIdAsc(data, usuarioId);

        BigDecimal totalVendido = BigDecimal.ZERO;
        BigDecimal totalRecebido = BigDecimal.ZERO;
        int quantidadeVendas = 0;
        int quantidadePagamentos = 0;

        List<ResumoDiario.LancamentoDiarioItem> itensClientes = new ArrayList<>();

        for (Lancamento l : lancamentos) {
            if (l.getNatureza() == Natureza.DEBITO) {
                totalVendido = totalVendido.add(l.getValor());
                quantidadeVendas++;
            } else {
                totalRecebido = totalRecebido.add(l.getValor());
                quantidadePagamentos++;
            }

            itensClientes.add(new ResumoDiario.LancamentoDiarioItem(
                    l.getId(),
                    l.getCliente().getNome(),
                    l.getNatureza(),
                    l.getCategoria(),
                    l.getValor(),
                    l.getDescricao(),
                    l.getFormaPagamento()
            ));
        }

        List<LancamentoFornecedor> lancamentosFornecedor = lancamentoFornecedorRepository
                .findByDataCompetenciaAndUsuarioIdOrderByIdAsc(data, usuarioId);

        List<ResumoDiario.LancamentoDiarioItem> itensFornecedores = lancamentosFornecedor.stream()
                .map(lf -> new ResumoDiario.LancamentoDiarioItem(
                        lf.getId(),
                        lf.getFornecedor().getNome(),
                        lf.getNatureza(),
                        lf.getCategoria(),
                        lf.getValor(),
                        lf.getDescricao(),
                        lf.getFormaPagamento()
                ))
                .toList();

        return new ResumoDiario(
                data,
                totalVendido,
                totalRecebido,
                quantidadeVendas,
                quantidadePagamentos,
                itensClientes,
                itensFornecedores
        );
    }

    @Transactional(readOnly = true)
    public ResumoDashboard dashboard() {
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();

        List<Cliente> clientes = clienteRepository.findByAtivoTrueAndUsuarioId(usuarioId, Pageable.unpaged())
                .getContent();

        List<ResumoDashboard.DevedorItem> devedores = new ArrayList<>();
        BigDecimal totalAReceber = BigDecimal.ZERO;

        for (Cliente c : clientes) {
            BigDecimal saldo = lancamentoRepository.calcularSaldo(c.getId(), usuarioId);
            if (saldo.compareTo(BigDecimal.ZERO) > 0) {
                totalAReceber = totalAReceber.add(saldo);
                devedores.add(new ResumoDashboard.DevedorItem(c.getId(), c.getNome(), saldo));
            }
        }

        devedores.sort(Comparator.comparing(ResumoDashboard.DevedorItem::saldo).reversed());

        return new ResumoDashboard(totalAReceber, devedores.size(), devedores);
    }

    @Transactional(readOnly = true)
    public List<ResumoMensalResponse> resumoMensal() {
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();
        return lancamentoRepository.resumoMensal(usuarioId).stream()
                .map(row -> new ResumoMensalResponse(
                        (String) row[0],
                        row[1] instanceof BigDecimal ? (BigDecimal) row[1] : new BigDecimal(row[1].toString()),
                        row[2] instanceof BigDecimal ? (BigDecimal) row[2] : new BigDecimal(row[2].toString())
                ))
                .toList();
    }

    private SaldoClienteResponse toResponse(Long id, String nome, BigDecimal saldo) {
        SituacaoSaldo situacao;
        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            situacao = SituacaoSaldo.DEVEDOR;
        } else if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            situacao = SituacaoSaldo.CREDOR;
        } else {
            situacao = SituacaoSaldo.QUITADO;
        }
        return new SaldoClienteResponse(id, nome, saldo, situacao, saldo.abs());
    }
}