package com.jacs.movimientoservice.controller;

import com.jacs.movimientoservice.dto.MovimientoConTitularDTO;
import com.jacs.movimientoservice.model.Movimiento;
import com.jacs.movimientoservice.observer.SseService;
import com.jacs.movimientoservice.service.MovimientoService;
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
 * Controlador REST para el Microservicio DETALLE — Movimientos (puerto 8081).
 *
 * Endpoints:
 *   GET  /movimientos/eventos        — SSE (patron Observer)
 *   GET  /movimientos                — listar todos
 *   GET  /movimientos/{id}           — buscar por ID
 *   GET  /movimientos/cuenta/{num}   — listar por numero de cuenta
 *   GET  /movimientos/filtrar        — consulta personalizada #1 con JOIN
 *   POST /movimientos                — crear (valida cuenta en MS-8080)
 *   PUT  /movimientos/{id}           — actualizar
 *   DELETE /movimientos/{id}         — eliminar
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
    // SSE — patron Observer en servidor
    // ---------------------------------------------------------------

    /**
     * Los clientes se suscriben aqui y reciben eventos SSE cada vez que
     * un movimiento es creado, actualizado o eliminado.
     * GET /movimientos/eventos
     */
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir() {
        return sseService.registrar();
    }

    // ---------------------------------------------------------------
    // Health check
    // ---------------------------------------------------------------

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Servicio Movimiento Ok! Puerto 8081");
    }

    // ---------------------------------------------------------------
    // Leer
    // ---------------------------------------------------------------

    /** GET /movimientos — Lista todos los movimientos. */
    @GetMapping
    public ResponseEntity<List<Movimiento>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    /** GET /movimientos/{id} — Buscar movimiento por ID. */
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarPorId(id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /** GET /movimientos/cuenta/{numeroCuenta} — Listar por cuenta. */
    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<List<Movimiento>> listarPorCuenta(@PathVariable int numeroCuenta) {
        return ResponseEntity.ok(service.listarPorCuenta(numeroCuenta));
    }

    /**
     * GET /movimientos/filtrar — Consulta personalizada #1 (TERCER PROTOTIPO).
     * JOIN entre MOVIMIENTO (B) y CUENTA_AHORROS (A) via @ManyToOne.
     * Devuelve todos los atributos de B + FK + titular de A.
     *
     * @param numeroCuenta 0 = todas las cuentas
     * @param tipo         CREDITO / DEBITO (opcional)
     * @param desde        fecha inicial ISO-8601 (opcional)
     * @param hasta        fecha final   ISO-8601 (opcional)
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

    /**
     * POST /movimientos — Registrar nuevo movimiento.
     *
     * INTERCOMUNICACION: el servicio llama a MS-CuentaAhorros (8080)
     * para verificar que la cuenta exista y este activa.
     *
     * Body JSON ejemplo:
     * {
     *   "numeroCuenta": 1001,
     *   "tipo": "CREDITO",
     *   "monto": 150000.0
     * }
     */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Movimiento datos) {
        try {
            Movimiento creado = service.agregar(datos);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Actualizar
    // ---------------------------------------------------------------

    /** PUT /movimientos/{id} — Actualizar movimiento. */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable int id,
                                        @RequestBody Movimiento datos) {
        try {
            return ResponseEntity.ok(service.actualizar(id, datos));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Eliminar
    // ---------------------------------------------------------------

    /** DELETE /movimientos/{id} — Eliminar movimiento. */
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
