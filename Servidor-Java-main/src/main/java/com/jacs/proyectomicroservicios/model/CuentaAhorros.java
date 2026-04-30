package com.jacs.proyectomicroservicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de dominio y entidad JPA para la tabla CUENTA_AHORROS.
 *
 * LOMBOK:
 *   - @AllArgsConstructor ELIMINADO intencionalmente: conflicto con @Builder.Default.
 *     Cuando coexisten, Lombok incluye el campo sintético $default$movimientos
 *     en el constructor generado por @AllArgsConstructor, causando un error de
 *     compilación/arranque. Con solo @NoArgsConstructor + @Builder el problema
 *     desaparece porque Lombok usa su propio constructor interno de tipo package-private.
 *
 * JPA:
 *   - numeroCuenta es la clave primaria (asignada manualmente, no auto-generada).
 *   - movimientos está marcado @Transient: no se persiste en la columna; el servicio
 *     JPA la rellena manualmente llamando a MovimientoRepository.findByNumeroCuenta().
 */
@Data
@Builder
@NoArgsConstructor
@Entity
@Table(name = "CUENTA_AHORROS")
public class CuentaAhorros {

    /** Número de cuenta — PK asignada por el cliente (no auto-incremento). */
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

    /**
     * Lista de movimientos — NO se persiste en DB (@Transient).
     * Los movimientos se guardan en la tabla MOVIMIENTO con FK NUMERO_CUENTA.
     * El servicio JPA rellena esta lista a pedido usando MovimientoRepository.
     */
    @Transient
    @Builder.Default
    private List<Movimiento> movimientos = new ArrayList<>();

    // ---- Métodos de negocio ----

    /** Costo mensual de mantenimiento (0.5% del saldo). */
    public double calcularCostoMensual() {
        return saldo * 0.005;
    }

    /** Rendimiento mensual estimado según tasa de interés. */
    public double calcularRendimiento() {
        return saldo * tasaInteres;
    }
}
