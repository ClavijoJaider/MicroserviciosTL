package com.jacs.proyectomicroservicios.controller;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Contrato REST del backend unificado - lado CUENTA_AHORROS.
 * Los endpoints de MOVIMIENTO viven en MovimientoController.
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

    /** Consulta personalizada #2: datos del maestro + dos agregados del detalle. */
    ResponseEntity<?> obtenerResumen(int numero);
}
