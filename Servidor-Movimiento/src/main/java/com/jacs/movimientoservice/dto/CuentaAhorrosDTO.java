package com.jacs.movimientoservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO que representa la respuesta de MS-CuentaAhorros (puerto 8080).
 *
 * Usado por {@link com.jacs.movimientoservice.client.CuentaAhorrosClient}
 * para deserializar el JSON que retorna GET /cuentas/{numero}.
 *
 * Intercomunicacion: MS-Movimiento (8081) llama a MS-CuentaAhorros (8080)
 * para validar la existencia y el estado de una cuenta antes de registrar
 * un movimiento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CuentaAhorrosDTO {

    private int    numeroCuenta;
    private String titular;
    private double saldo;
    private String estado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaApertura;

    private double tasaInteres;
}
