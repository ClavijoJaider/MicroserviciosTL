package com.jacs.proyectomicroservicios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO — Consulta personalizada 1 (TERCER PROTOTIPO).
 *
 * "El listar de la Clase/Tabla B debe mostrar todos sus atributos más la
 *  llave foránea y un atributo de la Clase/Tabla A."
 *
 * Retorna todos los campos de MOVIMIENTO (Tabla B) más {@code titularCuenta}
 * obtenido de CUENTA_AHORROS (Tabla A) mediante JOIN JPQL.
 *
 * Endpoint: GET /cuentas/movimientos/filtrar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoConTitularDTO {

    /** ID del movimiento (Tabla B — PK). */
    private Integer id;

    /** Fecha y hora del movimiento. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaMovimiento;

    /** Monto de la transacción. */
    private double monto;

    /** Tipo: "CREDITO" o "DEBITO". */
    private String tipo;

    /** FK hacia CUENTA_AHORROS (llave foránea explícita). */
    private int numeroCuenta;

    /** Titular de la cuenta — atributo de Tabla A (CUENTA_AHORROS). */
    private String titularCuenta;
}
