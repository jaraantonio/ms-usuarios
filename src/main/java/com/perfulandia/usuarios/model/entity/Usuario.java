package com.perfulandia.usuarios.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.perfulandia.usuarios.model.enums.EstadoUsuario;
import com.perfulandia.usuarios.model.enums.Rol;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoUsuario estado = EstadoUsuario.ACTIVO;

    @JsonIgnore
    @Column(name = "intentos_fallidos")
    private int intentosFallidos = 0;

    @JsonIgnore
    @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Cliente perfilCliente;

    // Para ofuscar el método de pago (simplificado)
    @Column(name = "metodo_pago_ofuscado", length = 50)
    private String metodoPagoOfuscado;

    public void setPerfilCliente(Cliente cliente) {
        if (cliente != null)
            cliente.setUsuario(this);
        this.perfilCliente = cliente;
    }
}