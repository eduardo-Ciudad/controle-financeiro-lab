package com.eduardo.financialcontrol.fornecedor;

import com.eduardo.financialcontrol.fornecedor.dto.FornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.FornecedorResponse;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FornecedorService {

    private final FornecedorRepository fornecedorRepository;
    private final LancamentoFornecedorRepository lancamentoFornecedorRepository;

    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        validarDocumentoUnico(request.documento(), null);
        Fornecedor fornecedor = new Fornecedor();
        mapear(request, fornecedor);
        return FornecedorResponse.de(fornecedorRepository.save(fornecedor));
    }
    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(String nome, Pageable pageable) {

        Page<Fornecedor> page;

        if (nome != null && !nome.isBlank()) {
            page = fornecedorRepository.buscarAtivosPorNome(nome, pageable);
        } else {
            page = fornecedorRepository.findByAtivoTrue(pageable);
        }

        return page.map(FornecedorResponse::de);
    }

    @Transactional(readOnly = true)
    public FornecedorResponse buscarPorId(Long id) {
        return FornecedorResponse.de(encontrarOuLancar(id));
    }

    @Transactional
    public FornecedorResponse atualizar(Long id, FornecedorRequest request) {
        Fornecedor fornecedor = encontrarOuLancar(id);
        validarDocumentoUnico(request.documento(), id);
        mapear(request, fornecedor);
        return FornecedorResponse.de(fornecedorRepository.save(fornecedor));
    }

    @Transactional
    public void inativar(Long id) {
        Fornecedor fornecedor = encontrarOuLancar(id);
        fornecedor.setAtivo(false);
        fornecedorRepository.save(fornecedor);
    }

    public Fornecedor encontrarOuLancar(Long id) {
        return fornecedorRepository.findById(id)
                .filter(f -> Boolean.TRUE.equals(f.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    }

    private void validarDocumentoUnico(String documento, Long idAtual) {
        if (documento == null || documento.isBlank()) return;
        fornecedorRepository.findByDocumento(documento)
                .filter(f -> !f.getId().equals(idAtual))
                .ifPresent(f -> {
                    throw new RegraDeNegocioException("Documento já cadastrado para outro fornecedor.");
                });
    }

    private void mapear(FornecedorRequest request, Fornecedor fornecedor) {
        fornecedor.setNome(request.nome());
        fornecedor.setDocumento(request.documento());
        fornecedor.setTelefone(request.telefone());
        fornecedor.setEmail(request.email());
        fornecedor.setObservacao(request.observacao());
    }
}
