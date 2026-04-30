package com.jacs.proyectomicroservicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Modelo de dominio y entidad JPA para la tabla MOVIMIENTO.
 *
 * El ID se genera mediante secuencia Oracle (SEQ_MOVIMIENTO).
 * Con H2 (desarrollo), Hibernate crea la secuencia automáticamente.
 * Con Oracle (producción), Hibernate la crea si no existe (ddl-auto=update).
 *
 * RELACIÓN: numeroCuenta es la FK hacia CUENTA_AHORROS, manejada como
 * columna simple (sin mapeo JPA bidireccional) para simplificar el código.
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
            name         = "seq_movimiento_gen",
            sequenceName = "SEQ_MOVIMIENTO",
            allocationSize = 1
    )
    @Column(name = "ID")
    private int id;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "FECHA_MOVIMIENTO")
    private LocalDateTime fechaMovimiento;

    @Column(name = "MONTO", nullable = false)
    private double monto;

    /** CREDITO o DEBITO */
    @Column(name = "TIPO", nullable = false, length = 10)
    private String tipo;

    /** FK hacia CUENTA_AHORROS (clave primaria de la cuenta). */
    @Column(name = "NUMERO_CUENTA", nullable = false)
    private int numeroCuenta;
}
