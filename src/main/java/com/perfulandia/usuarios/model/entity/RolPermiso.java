package com.perfulandia.usuarios.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rol_permiso")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RolPermiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rol", nullable = false, length = 30)
    private String rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_permiso", nullable = false)
    private Permiso permiso;
}
