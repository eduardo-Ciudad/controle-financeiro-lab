package com.eduardo.financialcontrol.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapUsuario implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.senha}")
    private String adminSenha;

    @Override
    public void run(String... args) {
        if (usuarioRepository.count() == 0) {
            Usuario usuario = new Usuario("Administrador", adminEmail, passwordEncoder.encode(adminSenha), Role.ADMIN);
            usuarioRepository.save(usuario);
            log.info("Usuário administrador criado com e-mail: {}", adminEmail);
        }
    }
}