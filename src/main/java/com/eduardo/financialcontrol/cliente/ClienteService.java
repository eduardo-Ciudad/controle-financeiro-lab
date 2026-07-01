package com.eduardo.financialcontrol.cliente;

import com.eduardo.financialcontrol.cliente.dto.ClienteRequest;
import com.eduardo.financialcontrol.cliente.dto.ClienteResponse;
import com.eduardo.financialcontrol.lancamento.LancamentoRepository;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final LancamentoRepository lancamentoRepository;

    @Transactional
    public ClienteResponse criar(ClienteRequest request) {
        validarDocumentoUnico(request.documento(), null);
        Cliente cliente = new Cliente();
        mapear(request, cliente);
        return ClienteResponse.de(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public Page<ClienteResponse> listar(
            String nome,
            Boolean comDivida,
            Pageable pageable) {

        boolean filtrarNome =
                nome != null && !nome.isBlank();

        Page<Cliente> page;

        if (Boolean.TRUE.equals(comDivida)) {

            if (filtrarNome) {
                page = clienteRepository
                        .buscarAtivosComDividaPorNome(nome, pageable);
            } else {
                page = clienteRepository
                        .buscarAtivosComDivida(pageable);
            }

        } else {

            if (filtrarNome) {
                page = clienteRepository
                        .buscarAtivosPorNome(nome, pageable);
            } else {
                page = clienteRepository
                        .findByAtivoTrue(pageable);
            }
        }

        return page.map(ClienteResponse::de);
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscarPorId(Long id) {
        return ClienteResponse.de(encontrarOuLancar(id));
    }

    @Transactional
    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = encontrarOuLancar(id);
        validarDocumentoUnico(request.documento(), id);
        mapear(request, cliente);
        return ClienteResponse.de(clienteRepository.save(cliente));
    }

    @Transactional
    public void inativar(Long id) {
        Cliente cliente = encontrarOuLancar(id);

        BigDecimal saldo = lancamentoRepository.calcularSaldo(id);
        if (saldo != null && saldo.compareTo(BigDecimal.ZERO) != 0) {
            throw new RegraDeNegocioException("Cliente possui saldo pendente de R$ " + saldo);
        }

        cliente.setAtivo(false);
        clienteRepository.save(cliente);
    }

    public Cliente encontrarOuLancar(Long id) {
        return clienteRepository.findById(id)
                .filter(c -> Boolean.TRUE.equals(c.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    private void validarDocumentoUnico(String documento, Long idAtual) {
        if (documento == null || documento.isBlank()) return;
        clienteRepository.findByDocumento(documento)
                .filter(c -> !c.getId().equals(idAtual))
                .ifPresent(c -> {
                    throw new RegraDeNegocioException("Documento já cadastrado para outro cliente.");
                });
    }

    private void mapear(ClienteRequest request, Cliente cliente) {
        cliente.setNome(request.nome());
        cliente.setDocumento(request.documento());
        cliente.setTelefone(request.telefone());
        cliente.setEmail(request.email());
        cliente.setObservacao(request.observacao());
    }
}
