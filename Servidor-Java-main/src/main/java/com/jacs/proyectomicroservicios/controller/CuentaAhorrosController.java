package com.jacs.proyectomicroservicios.controller;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;
import com.jacs.proyectomicroservicios.observer.SseService;
import com.jacs.proyectomicroservicios.service.ICuentaAhorrosService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/cuentas")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CuentaAhorrosController implements ICuentaAhorrosController {

    private final ICuentaAhorrosService service;
    private final SseService sseService;

    public CuentaAhorrosController(ICuentaAhorrosService service, SseService sseService) {
        this.service = service;
        this.sseService = sseService;
    }

    /**
     * Observer en servidor: los clientes se suscriben aquí y reciben eventos SSE
     * cada vez que cambia la información de cuentas o movimientos.
     * GET /cuentas/eventos
     */
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribir() {
        return sseService.registrar();
    }

    @GetMapping("/healthcheck")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Servicio CuentaAhorros Ok!");
    }

    @GetMapping("/movimientos")
    public ResponseEntity<List<Movimiento>> listarTodosMovimientos() {
        return ResponseEntity.ok(service.listarTodosMovimientos());
    }

    // POST /cuentas — Insertar
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody CuentaAhorros cuenta) {
        try {
            service.agregar(cuenta);
            return ResponseEntity.status(HttpStatus.CREATED).body(cuenta);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /cuentas/{numero}/movimientos — Agregar movimiento
    @PostMapping("/{numero}/movimientos")
    public ResponseEntity<?> agregarMovimiento(@PathVariable int numero,
                                               @RequestBody Movimiento datos) {
        try {
            Movimiento m = service.agregarMovimiento(numero, datos);
            return ResponseEntity.status(HttpStatus.CREATED).body(m);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /cuentas/{numero}/movimientos — Listar movimientos de una cuenta
    @GetMapping("/{numero}/movimientos")
    public ResponseEntity<List<Movimiento>> listarMovimientos(@PathVariable int numero) {
        try {
            return ResponseEntity.ok(service.listarMovimientos(numero));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    // GET /cuentas — Listar todos (con filtros opcionales ?titular=...&estado=...)
    @GetMapping
    public ResponseEntity<List<CuentaAhorros>> listar(
            @RequestParam(required = false) String titular,
            @RequestParam(required = false) String estado) {
        if ((titular == null || titular.isBlank()) && (estado == null || estado.isBlank())) {
            return ResponseEntity.ok(service.listar());
        }
        return ResponseEntity.ok(service.listarConFiltro(titular, estado));
    }

    // GET /cuentas/filtrar?titular=X&estado=Y — Filtro explícito
    @GetMapping("/filtrar")
    public ResponseEntity<List<CuentaAhorros>> filtrar(
            @RequestParam(required = false) String titular,
            @RequestParam(required = false) String estado) {
        return ResponseEntity.ok(service.listarConFiltro(titular, estado));
    }

    // GET /cuentas/buscar?titular=X — Buscar por titular
    @GetMapping("/buscar")
    public ResponseEntity<List<CuentaAhorros>> buscarPorTitular(
            @RequestParam String titular) {
        return ResponseEntity.ok(service.buscarPorTitular(titular));
    }

    // GET /cuentas/{numero} — Buscar por número
    @GetMapping("/{numero}")
    public ResponseEntity<?> buscarPorNumero(@PathVariable int numero) {
        try {
            return ResponseEntity.ok(service.buscarPorNumero(numero));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // DELETE /cuentas/{numero} — Eliminar (baja lógica: estado Inactivo)
    @DeleteMapping("/{numero}")
    public ResponseEntity<?> eliminar(@PathVariable int numero) {
        try {
            boolean resultado = service.eliminar(numero);
            if (resultado) {
                return ResponseEntity.ok("Cuenta " + numero + " marcada como Inactiva");
            }
            return ResponseEntity.badRequest().body("La cuenta ya estaba inactiva");
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // PUT /cuentas/{numero} — Actualizar
    @PutMapping("/{numero}")
    public ResponseEntity<?> actualizar(@PathVariable int numero,
                                        @RequestBody CuentaAhorros datos) {
        try {
            CuentaAhorros actualizada = service.actualizar(numero, datos);
            return ResponseEntity.ok(actualizada);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ============================================================
    // CRUD Completo de Movimientos: buscar, actualizar, eliminar
    // ============================================================

    // GET /cuentas/{numero}/movimientos/{id} — Buscar movimiento por ID
    @GetMapping("/{numero}/movimientos/{id}")
    public ResponseEntity<?> buscarMovimientoPorId(@PathVariable int numero,
                                                    @PathVariable int id) {
        try {
            return ResponseEntity.ok(service.buscarMovimientoPorId(numero, id));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    // PUT /cuentas/{numero}/movimientos/{id} — Actualizar movimiento
    @PutMapping("/{numero}/movimientos/{id}")
    public ResponseEntity<?> actualizarMovimiento(@PathVariable int numero,
                                                   @PathVariable int id,
                                                   @RequestBody Movimiento datos) {
        try {
            Movimiento actualizado = service.actualizarMovimiento(numero, id, datos);
            return ResponseEntity.ok(actualizado);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /cuentas/{numero}/movimientos/{id} — Eliminar movimiento
    @DeleteMapping("/{numero}/movimientos/{id}")
    public ResponseEntity<?> eliminarMovimiento(@PathVariable int numero,
                                                 @PathVariable int id) {
        try {
            service.eliminarMovimiento(numero, id);
            return ResponseEntity.ok("Movimiento " + id + " eliminado de cuenta " + numero);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}
