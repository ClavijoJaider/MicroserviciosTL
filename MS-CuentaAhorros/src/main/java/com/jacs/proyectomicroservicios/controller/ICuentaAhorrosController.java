package com.jacs.proyectomicroservicios.controller;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Contrato REST del Microservicio MAESTRO — CuentaAhorros (puerto 8080).
 *
 * SEPARACION ESTRICTA (Tercer Prototipo):
 *   Solo expone operaciones sobre CUENTA_AHORROS.
 *   Los endpoints de MOVIMIENTO pertenecen a MS-Movimiento (puerto 8081).
 */
public interface ICuentaAhorrosController {

    SseEmitter suscribir();
    ResponseEntity<String> healthCheck();

    ResponseEntity<?> crear(CuentaAhorros cuenta);
    ResponseEntity<List<CuentaAhorros>> listar(String titular, String estado);
    ResponseEntity<List<CuentaAhorros>> filtrar(String titular, String estado);
    ResponseEntity<List<CuentaAhorros>> buscarPorTitular(String titular);
    ResponseEntity<?> buscarPorNumero(int numero);
    ResponseEntity<?> actualizar(int numero, CuentaAhorros datos);
    ResponseEntity<?> eliminar(int numero);

    /** Consulta personalizada #2: datos maestro + dos agregados del detalle. */
    ResponseEntity<?> obtenerResumen(int numero);
}
