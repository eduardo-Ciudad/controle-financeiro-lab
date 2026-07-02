package com.eduardo.financialcontrol.auth;

import com.eduardo.financialcontrol.auth.dto.LoginRequest;
import com.eduardo.financialcontrol.auth.dto.TokenResponse;
import com.eduardo.financialcontrol.security.JwtService;
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
    @InjectMocks AuthService authService;

    @Test
    void login_credenciaisValidas_retornaToken() {
        // Arrange
        Usuario usuario = new Usuario("Admin", "admin@test.com", "hash");
        usuario.setAtivo(true);
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
}