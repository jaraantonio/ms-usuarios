package com.perfulandia.usuarios.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.perfulandia.usuarios.model.enums.Rol;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
// @Getter y @Setter en lugar de @Data para evitar problemas de recursividad
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(unique = true, nullable = false, length = 100)
    private String correo;

    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Rol rol;

    @Column(nullable = false, length = 20)
    private String estado = "ACTIVO";

    @JsonIgnore
    @Column(name = "intentos_fallidos")
    private int intentosFallidos = 0;

    @JsonIgnore
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cliente perfilCliente;

    // metodo para relación bidireccional entre Usuario y Cliente
    public void setPerfilCliente(Cliente cliente) {
        if (cliente != null) {
            cliente.setUsuario(this);
        }
        this.perfilCliente = cliente;
    }
}