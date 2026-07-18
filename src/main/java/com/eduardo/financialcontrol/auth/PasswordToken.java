package com.eduardo.financialcontrol.auth;


import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "password_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTokenSenha tipo;

    @Column(name = "nova_senha_hash")
    private String novaSenhaHash;

    @Column(nullable = false)
    private OffsetDateTime expiracao;

    @Column(nullable = false)
    private Boolean utilizado = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    public PasswordToken(Usuario usuario, String token, TipoTokenSenha tipo, OffsetDateTime expiracao) {
        this.usuario = usuario;
        this.token = token;
        this.tipo = tipo;
        this.expiracao = expiracao;
    }

    public boolean isExpirado() {
        return expiracao.isBefore(OffsetDateTime.now());
    }

}
