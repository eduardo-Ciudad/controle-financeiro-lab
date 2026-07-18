package com.eduardo.financialcontrol.auth;

import com.eduardo.financialcontrol.auth.dto.AlterarSenhaRequest;
import com.eduardo.financialcontrol.auth.dto.LoginRequest;
import com.eduardo.financialcontrol.auth.dto.TokenResponse;
import com.eduardo.financialcontrol.email.EmailService;
import com.eduardo.financialcontrol.security.JwtService;
import com.eduardo.financialcontrol.shared.exception.RegraDeNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UsuarioRepository usuarioRepository;
    @Mock BCryptPasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock
    EmailService emailService;
    @Mock PasswordTokenRepository passwordTokenRepository;
    @InjectMocks AuthService authService;

    @Test
    void login_credenciaisValidas_retornaToken() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hash");
        usuario.setAtivo(true);
        usuario.setEmailVerificado(true);
        LoginRequest request = new LoginRequest("admin@test.com", "senha123");

        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senha123", "hash")).thenReturn(true);
        when(jwtService.gerarToken("admin@test.com")).thenReturn("token.jwt");
        when(jwtService.calcularExpiracao()).thenReturn(OffsetDateTime.now().plusHours(24));

        // Act
        TokenResponse response = authService.login(request);

        // Assert
        assertThat(response.token()).isEqualTo("token.jwt");
        assertThat(response.expiraEm()).isNotNull();
    }

    @Test
    void login_senhaErrada_lancaExcecao() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hash");
        usuario.setAtivo(true);
        LoginRequest request = new LoginRequest("admin@test.com", "senhaErrada");

        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaErrada", "hash")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_usuarioInativo_lancaExcecao() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hash");
        usuario.setAtivo(false);
        LoginRequest request = new LoginRequest("admin@test.com", "senha123");

        when(usuarioRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(usuario));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_emailNaoCadastrado_lancaExcecao() {
        // Arrange
        LoginRequest request = new LoginRequest("nao@existe.com", "senha123");
        when(usuarioRepository.findByEmail("nao@existe.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void solicitarAlteracaoSenha_senhaAtualCorreta_enviaEmail() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hashAtual");
        AlterarSenhaRequest request = new AlterarSenhaRequest("senhaAtual", "novaSenha123");

        when(passwordEncoder.matches("senhaAtual", "hashAtual")).thenReturn(true);
        when(passwordEncoder.matches("novaSenha123", "hashAtual")).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hashNovo");

        // Act
        authService.solicitarAlteracaoSenha(request, usuario);

        // Assert
        verify(passwordTokenRepository).save(any(PasswordToken.class));
        verify(emailService).enviarEmailAlteracaoSenha(eq("admin@test.com"), anyString());
    }

    @Test
    void solicitarAlteracaoSenha_senhaAtualIncorreta_lancaExcecao() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hashAtual");
        AlterarSenhaRequest request = new AlterarSenhaRequest("senhaErrada", "novaSenha123");

        when(passwordEncoder.matches("senhaErrada", "hashAtual")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.solicitarAlteracaoSenha(request, usuario))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Senha atual está incorreta.");

        verify(passwordTokenRepository, never()).save(any());
        verify(emailService, never()).enviarEmailAlteracaoSenha(anyString(), anyString());
    }

    @Test
    void solicitarAlteracaoSenha_novaSenhaIgualAtual_lancaExcecao() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hashAtual");
        AlterarSenhaRequest request = new AlterarSenhaRequest("senhaAtual", "senhaAtual");

        when(passwordEncoder.matches("senhaAtual", "hashAtual")).thenReturn(true);
        when(passwordEncoder.matches("senhaAtual", "hashAtual")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.solicitarAlteracaoSenha(request, usuario))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("erro em atualizar a senha");
    }

    @Test
    void confirmarAlteracaoSenha_tokenValido_alteraSenha() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hashAntigo");
        PasswordToken token = new PasswordToken(usuario, "token123", TipoTokenSenha.ALTERACAO,
                OffsetDateTime.now().plusHours(1));
        token.setNovaSenhaHash("hashNovo");

        when(passwordTokenRepository.findByTokenAndUtilizadoFalse("token123"))
                .thenReturn(Optional.of(token));

        // Act
        authService.confirmarAlteracaoSenha("token123");

        // Assert
        assertThat(usuario.getSenhaHash()).isEqualTo("hashNovo");
        assertThat(token.getUtilizado()).isTrue();
        verify(usuarioRepository).save(usuario);
        verify(passwordTokenRepository).save(token);
    }

    @Test
    void confirmarAlteracaoSenha_tokenInvalido_lancaExcecao() {
        // Arrange
        when(passwordTokenRepository.findByTokenAndUtilizadoFalse("tokenInvalido"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.confirmarAlteracaoSenha("tokenInvalido"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Token inválido ou já utilizado.");
    }

    @Test
    void confirmarAlteracaoSenha_tokenExpirado_lancaExcecao() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hash");
        PasswordToken token = new PasswordToken(usuario, "tokenExpirado", TipoTokenSenha.ALTERACAO,
                OffsetDateTime.now().minusHours(1));

        when(passwordTokenRepository.findByTokenAndUtilizadoFalse("tokenExpirado"))
                .thenReturn(Optional.of(token));

        // Act & Assert
        assertThatThrownBy(() -> authService.confirmarAlteracaoSenha("tokenExpirado"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Token expirado. Solicite novamente.");
    }
}