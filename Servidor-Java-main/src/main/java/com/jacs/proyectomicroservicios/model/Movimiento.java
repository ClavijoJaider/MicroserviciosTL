package com.jacs.proyectomicroservicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA — tabla MOVIMIENTO (detalle).
 *
 * TERCER PROTOTIPO — relación @ManyToOne:
 *   La relación con CUENTA_AHORROS se declara con @ManyToOne en este lado (detalle).
 *   El campo {@code numeroCuenta} (int) sigue siendo la columna FK "propietaria" y
 *   es el campo que se usa en la serialización JSON y en el código de servicio.
 *   El campo {@code cuenta} (@ManyToOne, insertable=false, updatable=false) es de
 *   solo lectura y permite navegar la relación en consultas JPQL:
 *     FROM Movimiento m JOIN m.cuenta c WHERE ...
 *   Sin él no podríamos crear las consultas JOIN personalizadas exigidas por la rúbrica.
 *
 * SECUENCIA:
 *   SEQ_MOVIMIENTO — compatible con H2 (crea automáticamente) y Oracle XE.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "MOVIMIENTO")
public class Movimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_movimiento_gen")
    @SequenceGenerator(
            name           = "seq_movimiento_gen",
            sequenceName   = "SEQ_MOVIMIENTO",
            allocationSize = 1
    )
    @Column(name = "ID")
    private Integer id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "FECHA_MOVIMIENTO")
    private LocalDateTime fechaMovimiento;

    @Column(name = "MONTO", nullable = false)
    private double monto;

    /** "CREDITO" o "DEBITO". */
    @Column(name = "TIPO", nullable = false, length = 10)
    private String tipo;

    /**
     * FK hacia CUENTA_AHORROS — columna propietaria.
     * Este campo es el que se incluye en el JSON y maneja el INSERT/UPDATE.
     */
    @Column(name = "NUMERO_CUENTA", nullable = false)
    private int numeroCuenta;

    /**
     * Relación @ManyToOne (TERCER PROTOTIPO).
     * insertable/updatable=false: la columna la gestiona {@code numeroCuenta}.
     * @JsonIgnore: evita serialización circular; el titular se expone vía DTO.
     * Permite consultas JPQL tipo: FROM Movimiento m JOIN m.cuenta c
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "NUMERO_CUENTA", insertable = false, updatable = false)
    @JsonIgnore
    private CuentaAhorros cuenta;
}
