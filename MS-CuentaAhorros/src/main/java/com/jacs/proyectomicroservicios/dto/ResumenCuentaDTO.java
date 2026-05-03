package com.jacs.proyectomicroservicios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO — Consulta personalizada 2 (TERCER PROTOTIPO).
 *
 * "Una de las consultas debe permitir mostrar los datos de la tabla maestro
 *  y dos de detalle."
 *
 * Retorna los campos principales de CUENTA_AHORROS (maestro) más dos
 * campos agregados de MOVIMIENTO (detalle):
 *   - totalMovimientos : cantidad de movimientos registrados
 *   - totalCreditos    : suma de montos con tipo CREDITO
 *
 * Endpoint: GET /cuentas/{numero}/resumen
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumenCuentaDTO {

    // ---- Datos de la tabla maestro (CUENTA_AHORROS) ----

    private int    numeroCuenta;
    private String titular;
    private double saldo;
    private String estado;
    private double tasaInteres;

    // ---- Dos datos de la tabla detalle (MOVIMIENTO) ----

    /** Cantidad total de movimientos asociados a la cuenta. */
    private long totalMovimientos;

    /** Suma de montos de todos los movimientos tipo CREDITO. */
    private double totalCreditos;
}
