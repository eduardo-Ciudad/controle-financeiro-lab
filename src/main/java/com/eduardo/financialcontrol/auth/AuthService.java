package com.eduardo.financialcontrol.auth;

import com.eduardo.financialcontrol.auth.dto.LoginRequest;
import com.eduardo.financialcontrol.auth.dto.RegisterRequest;
import com.eduardo.financialcontrol.auth.dto.TokenResponse;
import com.eduardo.financialcontrol.email.EmailService;
import com.eduardo.financialcontrol.security.JwtService;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
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
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .filter(u -> Boolean.TRUE.equals(u.getAtivo()))
                .filter(u -> passwordEncoder.matches(request.senha(), u.getSenhaHash()))
                .orElseThrow(() -> {
                    log.warn("Tentativa de login falhou para e-mail: {}", request.email());
                    return new org.springframework.security.authentication.BadCredentialsException("Credenciais inválidas");
                });

        if (!Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new RegraDeNegocioException("Email não verificado. Verifique sua caixa de entrada.");
        }

        String token = jwtService.gerarToken(usuario.getEmail());
        return new TokenResponse(token, jwtService.calcularExpiracao());
    }

    @Transactional
    public void registrar(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new RegraDeNegocioException("E-mail já cadastrado.");
        }

        String tokenVerificacao = java.util.UUID.randomUUID().toString().replace("-", "");

        Usuario usuario = new Usuario(
                request.nome(),
                request.email(),
                passwordEncoder.encode(request.senha())
        );
        usuario.setEmailVerificado(false);
        usuario.setTokenVerificacao(tokenVerificacao);
        usuario.setTokenVerificacaoExpira(java.time.OffsetDateTime.now().plusHours(24));
        usuarioRepository.save(usuario);

        emailService.enviarEmailVerificacao(usuario.getEmail(), tokenVerificacao);
    }

    @Transactional
    public void verificarEmail(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacao(token)
                .orElseThrow(() -> new RegraDeNegocioException("Token de verificação inválido."));

        if (usuario.getTokenVerificacaoExpira().isBefore(java.time.OffsetDateTime.now())) {
            throw new RegraDeNegocioException("Token expirado. Solicite um novo email de verificação.");
        }

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacao(null);
        usuario.setTokenVerificacaoExpira(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void reenviarVerificacao(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("Email não encontrado."));

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new RegraDeNegocioException("Email já verificado.");
        }

        String novoToken = java.util.UUID.randomUUID().toString().replace("-", "");
        usuario.setTokenVerificacao(novoToken);
        usuario.setTokenVerificacaoExpira(java.time.OffsetDateTime.now().plusHours(24));
        usuarioRepository.save(usuario);

        emailService.enviarEmailVerificacao(usuario.getEmail(), novoToken);
    }
}