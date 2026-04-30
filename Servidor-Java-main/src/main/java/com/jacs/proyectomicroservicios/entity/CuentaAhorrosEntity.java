package com.jacs.proyectomicroservicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA para Oracle — CuentaAhorros.
 *
 * USO: Activa Oracle en application.properties para habilitar persistencia.
 * Mientras Oracle está desactivado, el servicio usa almacenamiento en memoria
 * (CuentaAhorrosService) y estas clases de entidad solo están compiladas pero inactivas.
 *
 * Tabla Oracle: CUENTA_AHORROS
 */
@Entity
@Table(name = "CUENTA_AHORROS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaAhorrosEntity {

    /** Número de cuenta — clave primaria (asignada manualmente, no auto-generada). */
    @Id
    @Column(name = "NUMERO_CUENTA", nullable = false)
    private int numeroCuenta;

    @Column(name = "TITULAR", nullable = false, length = 150)
    private String titular;

    @Column(name = "SALDO", nullable = false)
    private double saldo;

    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    @Column(name = "FECHA_APERTURA")
    private LocalDateTime fechaApertura;

    @Column(name = "TASA_INTERES")
    private double tasaInteres;

    /**
     * Relación 1..1 a 0..* con MovimientoEntity.
     * LAZY para no cargar movimientos a menos que se soliciten.
     * CascadeType.ALL: si se elimina la cuenta, se eliminan sus movimientos.
     */
    @OneToMany(mappedBy = "cuenta", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore
    private List<MovimientoEntity> movimientos = new ArrayList<>();
}
