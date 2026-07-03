package com.perfulandia.usuarios.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permiso")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(length = 50)
    private String modulo;
}
