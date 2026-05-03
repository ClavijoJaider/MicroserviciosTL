package com.jacs.proyectomicroservicios.controller;

import com.jacs.proyectomicroservicios.dto.ResumenCuentaDTO;
import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.observer.SseService;
import com.jacs.proyectomicroservicios.service.CuentaAhorrosJpaService;
import com.jacs.proyectomicroservicios.service.ICuentaAhorrosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Controlador REST del Microservicio MAESTRO — CuentaAhorros (puerto 8080).
 *
 * SEPARACION ESTRICTA (Tercer Prototipo):
 *   Este controlador expone EXCLUSIVAMENTE operaciones sobre CUENTA_AHORROS.
 *   No existe ningún endpoint de MOVIMIENTO aquí.
 *   Todo lo relacionado con movimientos está en MS-Movimiento (puerto 8081).
 *
 * Endpoints:
 *   GET  /cuentas/eventos          — SSE (patrón Observer en servidor)
 *   GET  /cuentas/healthcheck      — verificación de estado
 *   POST /cuentas                  — crear cuenta
 *   GET  /cuentas                  — listar (con filtros opcionales)
 *   GET  /cuentas/filtrar          — filtrar por titular y/o estado
 *   GET  /cuentas/buscar           — buscar por titular
 *   GET  /cuentas/{numero}         — buscar por número
 *   PUT  /cuentas/{numero}         — actualizar
 *   DELETE /cuentas/{numero}       — baja lógica (estado → Inactivo)
 *   GET  /cuentas/{numero}/resumen — consulta personalizada #2 (maestro + 2 agregados)
 */
@RestController
@RequestMapping("/cuentas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CuentaAhorrosController implements ICuentaAhorrosController {

    private final ICuentaAhorrosService   service;
    private final CuentaAhorrosJpaService jpaService;   // solo para obtenerResumen
    private final SseService              sseService;

    public CuentaAhorrosController(ICuentaAhorrosService service,
                                   CuentaAhorrosJpaService jpaService,
                                   SseService sseService) {
        this.service    = service;
        this.jpaService = jpaService;
        this.sseService = sseService;
    }

    // ---------------------------------------------------------------
    // SSE — patrón Observer en servidor
    // ---------------------------------------------------------------

    /**
     * Los clientes se suscriben aquí y reciben eventos SSE cada vez que
     * una cuenta es creada, actualizada o eliminada en este microservicio.
     */
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir() {
        return sseService.registrar();
    }

    // ---------------------------------------------------------------
    // Healthcheck
    // ---------------------------------------------------------------

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Servicio CuentaAhorros Ok! Puerto 8080");
    }

    // ---------------------------------------------------------------
    // CRUD CuentaAhorros
    // ---------------------------------------------------------------

    /** POST /cuentas — crear nueva cuenta. */
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CuentaAhorros cuenta) {
        try {
            service.agregar(cuenta);
            return ResponseEntity.status(HttpStatus.CREATED).body(cuenta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** GET /cuentas — listar todas (con filtros opcionales ?titular=&estado=). */
    @GetMapping
    public ResponseEntity<List<CuentaAhorros>> listar(
            @RequestParam(required = false) String titular,
            @RequestParam(required = false) String estado) {
        if ((titular == null || titular.isBlank()) && (estado == null || estado.isBlank()))
            return ResponseEntity.ok(service.listar());
        return ResponseEntity.ok(service.listarConFiltro(titular, estado));
    }

    /** GET /cuentas/filtrar?titular=X&estado=Y — filtro explícito. */
    @GetMapping("/filtrar")
    public ResponseEntity<List<CuentaAhorros>> filtrar(
            @RequestParam(required = false) String titular,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarConFiltro(titular, estado));
    }

    /** GET /cuentas/buscar?titular=X — buscar por nombre del titular. */
    @GetMapping("/buscar")
    public ResponseEntity<List<CuentaAhorros>> buscarPorTitular(
            @RequestParam String titular) {
        return ResponseEntity.ok(service.buscarPorTitular(titular));
    }

    /** GET /cuentas/{numero} — buscar por número de cuenta. */
    @GetMapping("/{numero}")
    public ResponseEntity<?> buscarPorNumero(@PathVariable int numero) {
        try {
            return ResponseEntity.ok(service.buscarPorNumero(numero));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /** PUT /cuentas/{numero} — actualizar datos de una cuenta. */
    @PutMapping("/{numero}")
    public ResponseEntity<?> actualizar(@PathVariable int numero,
                                        @RequestBody CuentaAhorros datos) {
        try {
            return ResponseEntity.ok(service.actualizar(numero, datos));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /** DELETE /cuentas/{numero} — baja lógica (estado → Inactivo). */
    @DeleteMapping("/{numero}")
    public ResponseEntity<?> eliminar(@PathVariable int numero) {
        try {
            boolean resultado = service.eliminar(numero);
            if (resultado) return ResponseEntity.ok("Cuenta " + numero + " marcada como Inactiva");
            return ResponseEntity.badRequest().body("La cuenta ya estaba inactiva");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Consulta personalizada #2 — TERCER PROTOTIPO
    // ---------------------------------------------------------------

    /**
     * GET /cuentas/{numero}/resumen
     *
     * Devuelve datos de CUENTA_AHORROS (maestro) más dos campos agregados
     * del detalle calculados con subconsultas JPQL:
     *   • totalMovimientos — COUNT de movimientos asociados
     *   • totalCreditos    — SUM de montos CREDITO
     *
     * Separación: este endpoint SOLO lee agregados, no expone CRUD de movimientos.
     */
    @GetMapping("/{numero}/resumen")
    public ResponseEntity<?> obtenerResumen(@PathVariable int numero) {
        try {
            ResumenCuentaDTO resumen = jpaService.obtenerResumen(numero);
            return ResponseEntity.ok(resumen);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
