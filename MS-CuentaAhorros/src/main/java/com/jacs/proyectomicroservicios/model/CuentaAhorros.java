package com.jacs.proyectomicroservicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA — tabla CUENTA_AHORROS (maestro).
 *
 * TERCER PROTOTIPO:
 *   - La lista de movimientos fue eliminada: "el uso de una lista está prohibido".
 *     Los movimientos se acceden exclusivamente a través de MovimientoRepository.
 *   - La relación con Movimiento es unidireccional @ManyToOne desde el detalle;
 *     aquí solo existe la tabla maestra sin colección embebida.
 *
 * LOMBOK:
 *   - @AllArgsConstructor restaurado (el conflicto con @Builder.Default desaparece
 *     al eliminar la lista @Transient que lo causaba).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CUENTA_AHORROS")
public class CuentaAhorros {

    /** PK asignada por el cliente (no auto-incremento). */
    @Id
    @Column(name = "NUMERO_CUENTA", nullable = false)
    private int numeroCuenta;

    @Column(name = "TITULAR", nullable = false, length = 150)
    private String titular;

    @Column(name = "SALDO", nullable = false)
    private double saldo;

    /** "Activo" o "Inactivo" (baja lógica). */
    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "FECHA_APERTURA")
    private LocalDateTime fechaApertura;

    @Column(name = "TASA_INTERES")
    private double tasaInteres;

    // ---- Métodos de negocio ----

    /** Costo mensual de mantenimiento (0.5 % del saldo). */
    public double calcularCostoMensual() {
        return saldo * 0.005;
    }

    /** Rendimiento mensual estimado según tasa de interés. */
    public double calcularRendimiento() {
        return saldo * tasaInteres;
    }
}
