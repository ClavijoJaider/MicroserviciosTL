package com.jacs.movimientoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio DETALLE — Movimientos (puerto 8081).
 *
 * Tercer Prototipo — Arquitectura de microservicios:
 *   - MS-CuentaAhorros (8080): gestiona la tabla maestra CUENTA_AHORROS.
 *   - MS-Movimiento    (8081): gestiona la tabla detalle MOVIMIENTO.
 *
 * Intercomunicacion:
 *   Al agregar/actualizar un movimiento, este servicio llama a
 *   MS-CuentaAhorros via RestClient para validar que la cuenta exista.
 *
 * Base de datos:
 *   Comparte el archivo H2 "./waybank" con MS-CuentaAhorros usando AUTO_SERVER=TRUE.
 *   MS-CuentaAhorros debe iniciarse primero (crea el esquema).
 */
@SpringBootApplication
public class MovimientoServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MovimientoServiceApplication.class, args);
    }
}
