package com.jacs.movimientoservice.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Entidad JPA de referencia — tabla CUENTA_AHORROS (maestro).
 *
 * Este microservicio (MS-Movimiento) es el lado "detalle" de la relacion
 * @ManyToOne. Necesita tener esta entidad mapeada para:
 *   - Navegar la relacion en consultas JPQL (FROM Movimiento m JOIN m.cuenta c).
 *   - Obtener el titular de la cuenta en la consulta personalizada #1.
 *
 * La tabla es gestionada (DDL create/update) por MS-CuentaAhorros (8080).
 * Este servicio opera en modo ddl-auto=none (solo lectura del esquema).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CUENTA_AHORROS")
public class CuentaAhorros {

    @Id
    @Column(name = "NUMERO_CUENTA", nullable = false)
    private int numeroCuenta;

    @Column(name = "TITULAR", nullable = false, length = 150)
    private String titular;

    @Column(name = "SALDO", nullable = false)
    private double saldo;

    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "FECHA_APERTURA")
    private LocalDateTime fechaApertura;

    @Column(name = "TASA_INTERES")
    private double tasaInteres;
}
