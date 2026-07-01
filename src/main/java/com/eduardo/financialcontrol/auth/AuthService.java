package com.eduardo.financialcontrol.auth;

import com.eduardo.financialcontrol.auth.dto.LoginRequest;
import com.eduardo.financialcontrol.auth.dto.TokenResponse;
import com.eduardo.financialcontrol.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(() -> {
                    log.warn("Tentativa de login falhou para e-mail: {}", request.email());
                    return new org.springframework.security.authentication.BadCredentialsException("Credenciais inválidas");
                });

        String token = jwtService.gerarToken(usuario.getEmail(), usuario.getRole().name());
        return new TokenResponse(token, jwtService.calcularExpiracao());
    }
}
