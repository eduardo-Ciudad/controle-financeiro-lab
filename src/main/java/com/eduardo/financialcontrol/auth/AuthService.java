package com.eduardo.financialcontrol.auth;

import com.eduardo.financialcontrol.auth.dto.*;
import com.eduardo.financialcontrol.email.EmailService;
import com.eduardo.financialcontrol.security.JwtService;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordTokenRepository passwordTokenRepository;

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

    @Transactional
    public void solicitarAlteracaoSenha(AlterarSenhaRequest request, Usuario usuario) {
        if ( !passwordEncoder.matches(request.senhaAtual(),  usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("Senha atual está incorreta.");
        }

        if (passwordEncoder.matches(request.novaSenha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("erro em atualizar a senha");
        }

        String token = UUID.randomUUID().toString().replace("-", "");

        PasswordToken passwordToken = new PasswordToken(
                usuario,
                token,
                TipoTokenSenha.ALTERACAO,
                OffsetDateTime.now().plusHours(1)
        );
        passwordToken.setNovaSenhaHash(passwordEncoder.encode(request.novaSenha()));
        passwordTokenRepository.save(passwordToken);

        emailService.enviarEmailAlteracaoSenha(usuario.getEmail(), token);
    }

    @Transactional
    public void esqueciSenha(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            String token = UUID.randomUUID().toString().replace("-", "");

            PasswordToken passwordToken = new PasswordToken(
                    usuario,
                    token,
                    TipoTokenSenha.RESET,
                    OffsetDateTime.now().plusHours(1)
            );
            passwordTokenRepository.save(passwordToken);

            emailService.enviarEmailResetSenha(usuario.getEmail(), token);

        });
    }

    @Transactional
    public void resetarSenha(ResetarSenhaRequest request) {
        PasswordToken passwordToken = buscarTokenValido(request.token(),  TipoTokenSenha.RESET);

        Usuario usuario = passwordToken.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        usuarioRepository.save(usuario);

        passwordToken.setUtilizado(true);
        passwordTokenRepository.save(passwordToken);
    }

    @Transactional
    public void confirmarAlteracaoSenha(String token) {
        PasswordToken passwordToken = buscarTokenValido(token, TipoTokenSenha.ALTERACAO);

        Usuario usuario = passwordToken.getUsuario();
        usuario.setSenhaHash(passwordToken.getNovaSenhaHash());
        usuarioRepository.save(usuario);

        passwordToken.setUtilizado(true);
        passwordTokenRepository.save(passwordToken);
    }


    private PasswordToken buscarTokenValido(String token, TipoTokenSenha tipoEsperado) {
        PasswordToken passwordToken = passwordTokenRepository.findByTokenAndUtilizadoFalse(token)
                .orElseThrow(() -> new RegraDeNegocioException("Token inválido ou já utilizado."));

        if (passwordToken.isExpirado()) {
            throw new RegraDeNegocioException("Token expirado. Solicite novamente.");
        }

        if (passwordToken.getTipo() != tipoEsperado) {
            throw new RegraDeNegocioException("Token inválido.");
        }

        return passwordToken;
    }
}