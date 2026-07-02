package com.eduardo.financialcontrol.fornecedor;

import com.eduardo.financialcontrol.fornecedor.dto.FornecedorRequest;
import com.eduardo.financialcontrol.fornecedor.dto.FornecedorResponse;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
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
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public FornecedorResponse criar(FornecedorRequest request) {
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();
        validarDocumentoUnico(request.documento(), null, usuarioId);
        Fornecedor fornecedor = new Fornecedor(usuarioAutenticadoService.getUsuario());
        mapear(request, fornecedor);
        return FornecedorResponse.de(fornecedorRepository.save(fornecedor));
    }
    @Transactional(readOnly = true)
    public Page<FornecedorResponse> listar(String nome, Pageable pageable) {

        Long usuarioId = usuarioAutenticadoService.getUsuarioId();

        Page<Fornecedor> page;

        if (nome != null && !nome.isBlank()) {
            page = fornecedorRepository.buscarAtivosPorNome(nome, usuarioId, pageable);
        } else {
            page = fornecedorRepository.findByAtivoTrueAndUsuarioId(usuarioId, pageable);
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
        validarDocumentoUnico(request.documento(), id, usuarioAutenticadoService.getUsuarioId());
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
        Long usuarioId = usuarioAutenticadoService.getUsuarioId();
        return fornecedorRepository.findByIdAndUsuarioId(id, usuarioId)
                .filter(f -> Boolean.TRUE.equals(f.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Fornecedor não encontrado: " + id));
    }

    private void validarDocumentoUnico(String documento, Long idAtual, Long usuarioId) {
        if (documento == null || documento.isBlank()) return;
        fornecedorRepository.findByDocumentoAndUsuarioId(documento, usuarioId)
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
