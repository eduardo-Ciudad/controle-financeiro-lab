package com.eduardo.financialcontrol.auth;

import com.eduardo.financialcontrol.auth.dto.*;
import com.eduardo.financialcontrol.config.RateLimitConfig;
import com.eduardo.financialcontrol.security.UsuarioAutenticadoService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RateLimitConfig rateLimitConfig;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        Bucket bucket = rateLimitConfig.resolveBucket(httpRequest.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registrar(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        Bucket bucket = rateLimitConfig.resolveBucket(httpRequest.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MessageResponse("Conta criada. Verifique seu email para ativar."));
    }

    @GetMapping("/verificar")
    public ResponseEntity<MessageResponse> verificarEmail(@RequestParam String token) {
        authService.verificarEmail(token);
        return ResponseEntity.ok(new MessageResponse("Email verificado com sucesso! Você já pode fazer login."));
    }

    @PostMapping("/reenviar-verificacao")
    public ResponseEntity<MessageResponse> reenviarVerificacao(@RequestBody ReenviarRequest request,
                                                               HttpServletRequest httpRequest) {

        Bucket bucket = rateLimitConfig.resolveBucket(httpRequest.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        authService.reenviarVerificacao(request.email());
        return ResponseEntity.ok(new MessageResponse("Email de verificação reenviado."));
    }

    @PostMapping("/alterar-senha")
    public ResponseEntity<MessageResponse> alterarSenha(
            @Valid @RequestBody AlterarSenhaRequest request,
            HttpServletRequest httpRequest
    ){
        Bucket bucket = rateLimitConfig.resolveBucket(httpRequest.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        Usuario usuario = usuarioAutenticadoService.getUsuario();
        authService.solicitarAlteracaoSenha(request, usuario);
        return ResponseEntity.ok(new MessageResponse("Email de confirmação enviado. Verifique sua caixa de entrada."));

    }

    @GetMapping("/confirmar-alteracao-senha")
    public ResponseEntity<MessageResponse> confirmarAlteracaoSenha(@RequestParam String token) {
        authService.confirmarAlteracaoSenha(token);
        return ResponseEntity.ok(new MessageResponse("Senha alterada com sucesso!"));
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<MessageResponse> esqueciSenha(
            @Valid @RequestBody EsqueciSenhaRequest request,
            HttpServletRequest httpRequest) {

        Bucket bucket = rateLimitConfig.resolveBucket(httpRequest.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        authService.esqueciSenha(request.email());
        return ResponseEntity.ok(new MessageResponse("Se o email estiver cadastrado, você receberá um link de recuperação."));
    }

    @PostMapping("/resetar-senha")
    public ResponseEntity<MessageResponse> resetarSenha(
            @Valid @RequestBody ResetarSenhaRequest request,
            HttpServletRequest httpRequest) {

        Bucket bucket = rateLimitConfig.resolveBucket(httpRequest.getRemoteAddr());
        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        authService.resetarSenha(request);
        return ResponseEntity.ok(new MessageResponse("Senha redefinida com sucesso! Você já pode fazer login."));
    }
}