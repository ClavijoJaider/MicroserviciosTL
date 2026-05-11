package com.jacs.proyectomicroservicios.controller;

import com.jacs.proyectomicroservicios.dto.MovimientoConTitularDTO;
import com.jacs.proyectomicroservicios.model.Movimiento;
import com.jacs.proyectomicroservicios.observer.SseService;
import com.jacs.proyectomicroservicios.service.MovimientoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Controlador REST de MOVIMIENTOS (BACKEND UNIFICADO).
 *
 * Mantiene los mismos endpoints que antes exponia MS-Movimiento (8081) para
 * que el cliente Electron no necesite cambios estructurales, solo apuntar
 * al puerto unificado 8080.
 *
 * Endpoints:
 *   GET    /movimientos/eventos       - SSE (patron Observer)
 *   GET    /movimientos/healthcheck   - estado
 *   GET    /movimientos               - listar todos
 *   GET    /movimientos/{id}          - buscar por ID
 *   GET    /movimientos/cuenta/{num}  - listar por cuenta
 *   GET    /movimientos/filtrar       - JOIN con titular (consulta #1)
 *   POST   /movimientos               - crear (valida cuenta en mismo proceso)
 *   PUT    /movimientos/{id}          - actualizar
 *   DELETE /movimientos/{id}          - eliminar
 */
@RestController
@RequestMapping("/movimientos")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class MovimientoController {

    private final MovimientoService service;
    private final SseService        sseService;

    public MovimientoController(MovimientoService service, SseService sseService) {
        this.service    = service;
        this.sseService = sseService;
    }

    // ---------------------------------------------------------------
    // SSE - patron Observer en servidor
    // ---------------------------------------------------------------

    /**
     * Comparte el MISMO SseService que CuentaAhorrosController, asi un
     * cliente que se suscribe a /movimientos/eventos recibe TODO (cuentas
     * y movimientos). En la practica el cliente Electron sigue suscribiendo
     * a los dos endpoints, pero ambos sirven exactamente la misma corriente.
     */
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir() {
        return sseService.registrar();
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Backend unificado - movimientos OK (puerto 8080)");
    }

    // ---------------------------------------------------------------
    // Leer
    // ---------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<Movimiento>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<List<Movimiento>> listarPorCuenta(@PathVariable int numeroCuenta) {
        return ResponseEntity.ok(service.listarPorCuenta(numeroCuenta));
    }

    /**
     * GET /movimientos/filtrar - Consulta personalizada #1.
     * JOIN entre MOVIMIENTO (B) y CUENTA_AHORROS (A) via @ManyToOne.
     */
    @GetMapping("/filtrar")
    public ResponseEntity<List<MovimientoConTitularDTO>> filtrar(
            @RequestParam(defaultValue = "0") int numeroCuenta,
            @RequestParam(required = false)   String tipo,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(service.filtrarConTitular(numeroCuenta, tipo, desde, hasta));
    }

    // ---------------------------------------------------------------
    // Crear
    // ---------------------------------------------------------------

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Movimiento datos) {
        try {
            Movimiento creado = service.agregar(datos);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Actualizar
    // ---------------------------------------------------------------

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id,
                                        @RequestBody Movimiento datos) {
        try {
            return ResponseEntity.ok(service.actualizar(id, datos));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Eliminar
    // ---------------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable int id) {
        try {
            service.eliminar(id);
            return ResponseEntity.ok("Movimiento " + id + " eliminado");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
