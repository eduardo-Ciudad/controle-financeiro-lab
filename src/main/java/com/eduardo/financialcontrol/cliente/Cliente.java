package com.eduardo.financialcontrol.cliente;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "clientes")
@Getter  @NoArgsConstructor
public class Cliente {

    @Setter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 18)
    private String documento;

    @Setter
    @Column(length = 20)
    private String telefone;

    @Setter
    @Column(length = 160)
    private String email;

    @Setter
    @Column(columnDefinition = "TEXT")
    private String observacao;

    @Setter
    @Column(nullable = false)
    private Boolean ativo = true;

    @Setter
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm = OffsetDateTime.now();

    @Setter
    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm = OffsetDateTime.now();

    @PreUpdate
    private void preUpdate() {
        this.atualizadoEm = OffsetDateTime.now();
    }

    public void setDocumento(String documento) {
        this.documento = (documento != null && documento.isBlank()) ? null : documento;
    }
}
