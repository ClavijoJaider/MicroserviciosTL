package com.jacs.proyectomicroservicios.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad JPA para Oracle — Movimiento.
 *
 * USO: Activa Oracle en application.properties para habilitar persistencia.
 *
 * Tabla Oracle: MOVIMIENTO
 * Secuencia Oracle: SEQ_MOVIMIENTO (para auto-incremento del ID).
 *
 * Script Oracle para crear la secuencia:
 *   CREATE SEQUENCE SEQ_MOVIMIENTO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
 */
@Entity
@Table(name = "MOVIMIENTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoEntity {

    /**
     * ID del movimiento — generado por secuencia Oracle.
     * Cambia la estrategia a IDENTITY si usas una columna de identidad en Oracle 12c+.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_movimiento")
    @SequenceGenerator(name = "seq_movimiento", sequenceName = "SEQ_MOVIMIENTO", allocationSize = 1)
    @Column(name = "ID", nullable = false)
    private int id;

    @Column(name = "FECHA_MOVIMIENTO")
    private LocalDateTime fechaMovimiento;

    @Column(name = "MONTO", nullable = false)
    private double monto;

    /** CREDITO o DEBITO */
    @Column(name = "TIPO", nullable = false, length = 10)
    private String tipo;

    /**
     * Clave foránea hacia CUENTA_AHORROS.
     * @JsonIgnore para evitar serialización circular al devolver el movimiento como JSON.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NUMERO_CUENTA", nullable = false)
    @JsonIgnore
    private CuentaAhorrosEntity cuenta;
}
