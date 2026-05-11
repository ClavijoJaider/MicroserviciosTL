package com.jacs.proyectomicroservicios.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO — Consulta personalizada 1 (BACKEND UNIFICADO).
 *
 * Retorna todos los campos de MOVIMIENTO (Tabla B) mas {@code titularCuenta}
 * obtenido de CUENTA_AHORROS (Tabla A) mediante JOIN JPQL.
 *
 * Endpoint: GET /movimientos/filtrar
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoConTitularDTO {

    /** ID del movimiento (Tabla B - PK). */
    private Integer id;

    /** Fecha y hora del movimiento. */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaMovimiento;

    /** Monto de la transaccion. */
    private double monto;

    /** Tipo: "CREDITO" o "DEBITO". */
    private String tipo;

    /** FK hacia CUENTA_AHORROS (llave foranea explicita). */
    private int numeroCuenta;

    /** Titular de la cuenta - atributo de Tabla A (CUENTA_AHORROS). */
    private String titularCuenta;
}
