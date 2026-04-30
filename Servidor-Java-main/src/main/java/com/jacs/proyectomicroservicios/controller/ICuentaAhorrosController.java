package com.jacs.proyectomicroservicios.controller;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ICuentaAhorrosController {

    // Observer SSE: suscribir cliente como observador
    SseEmitter suscribir();

    // Healthcheck
    ResponseEntity<String> healthCheck();

    // Crear cuenta
    ResponseEntity<?> crear(CuentaAhorros cuenta);

    // Agregar movimiento a cuenta
    ResponseEntity<?> agregarMovimiento(int numero, Movimiento datos);

    // Listar movimientos de una cuenta
    ResponseEntity<List<Movimiento>> listarMovimientos(int numero);

    // Listar todos los movimientos
    ResponseEntity<List<Movimiento>> listarTodosMovimientos();

    // Listar cuentas (con filtros opcionales)
    ResponseEntity<List<CuentaAhorros>> listar(String titular, String estado);

    // Filtrar cuentas por titular y/o estado
    ResponseEntity<List<CuentaAhorros>> filtrar(String titular, String estado);

    // Buscar por titular
    ResponseEntity<List<CuentaAhorros>> buscarPorTitular(String titular);

    // Buscar por número de cuenta
    ResponseEntity<?> buscarPorNumero(int numero);

    // Eliminar cuenta (baja lógica)
    ResponseEntity<?> eliminar(int numero);

    // Actualizar cuenta
    ResponseEntity<?> actualizar(int numero, CuentaAhorros datos);

    // CRUD completo de Movimientos
    ResponseEntity<?> buscarMovimientoPorId(int numero, int id);

    ResponseEntity<?> actualizarMovimiento(int numero, int id, Movimiento datos);

    ResponseEntity<?> eliminarMovimiento(int numero, int id);
}
