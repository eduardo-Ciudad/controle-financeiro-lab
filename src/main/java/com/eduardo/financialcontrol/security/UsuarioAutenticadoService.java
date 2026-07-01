package com.eduardo.financialcontrol.security;

import com.eduardo.financialcontrol.auth.Usuario;
import com.eduardo.financialcontrol.auth.UsuarioRepository;
import com.eduardo.financialcontrol.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsuarioAutenticadoService {

    private final UsuarioRepository usuarioRepository;

    public Usuario getUsuario() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuario nao encontrado"));
    }

    public Long getUsuarioId() {
        return getUsuario().getId();
    }
}
