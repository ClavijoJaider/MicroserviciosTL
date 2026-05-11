package com.jacs.proyectomicroservicios.service;

import com.jacs.proyectomicroservicios.dto.MovimientoConTitularDTO;
import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;
import com.jacs.proyectomicroservicios.observer.SseService;
import com.jacs.proyectomicroservicios.repository.MovimientoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Servicio de negocio para MOVIMIENTO (BACKEND UNIFICADO).
 *
 * Antes vivia en MS-Movimiento y usaba CuentaAhorrosClient (RestClient HTTP)
 * para validar la cuenta contra MS-CuentaAhorros. Tras la unificacion, la
 * validacion se hace IN-PROCESS llamando directamente a {@link ICuentaAhorrosService}.
 *
 * Beneficios:
 *   - Cero latencia de red entre microservicios.
 *   - Una sola transaccion abarca cuenta + movimiento si fuera necesario.
 *   - Se elimina el punto de fallo "MS-CuentaAhorros caido" -> 503.
 *
 * Observer SSE:
 *   Cada mutacion notifica a los clientes suscritos via SSE
 *   (mismo canal /cuentas/eventos o /movimientos/eventos).
 */
@Service
@Transactional
public class MovimientoService {

    private final MovimientoRepository  repo;
    private final ICuentaAhorrosService cuentaService;
    private final SseService            sseService;

    public MovimientoService(MovimientoRepository repo,
                             ICuentaAhorrosService cuentaService,
                             SseService sseService) {
        this.repo          = repo;
        this.cuentaService = cuentaService;
        this.sseService    = sseService;
    }

    // ---------------------------------------------------------------
    // Crear
    // ---------------------------------------------------------------

    /**
     * Registra un nuevo movimiento.
     * Valida la cuenta DIRECTAMENTE en la base (no HTTP).
     */
    public Movimiento agregar(Movimiento datos) {
        if (datos.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        String tipo = datos.getTipo();
        if (tipo == null || (!tipo.equalsIgnoreCase("CREDITO") && !tipo.equalsIgnoreCase("DEBITO"))) {
            throw new IllegalArgumentException("Tipo debe ser CREDITO o DEBITO");
        }

        // Validacion in-process: la cuenta debe existir y estar Activa.
        CuentaAhorros cuenta = cuentaService.buscarPorNumero(datos.getNumeroCuenta());
        if ("Inactivo".equalsIgnoreCase(cuenta.getEstado())) {
            throw new IllegalArgumentException(
                    "La cuenta " + datos.getNumeroCuenta() + " esta inactiva y no admite movimientos");
        }

        // ----------------------------
        // ACTUALIZAR SALDO
        // ----------------------------

        double saldoActual = cuenta.getSaldo();

        if (tipo.equalsIgnoreCase("CREDITO")) {

            cuenta.setSaldo(saldoActual + datos.getMonto());

        } else {

            // Validar fondos
            if (saldoActual < datos.getMonto()) {
                throw new IllegalArgumentException(
                        "Saldo insuficiente");
            }

            cuenta.setSaldo(saldoActual - datos.getMonto());
        }

        datos.setTipo(tipo.toUpperCase());
        datos.setFechaMovimiento(LocalDateTime.now());

        Movimiento guardado = repo.save(datos);

        sseService.notificar("MOVIMIENTO_CREADO", Map.of(
                "id",           guardado.getId(),
                "numeroCuenta", guardado.getNumeroCuenta(),
                "tipo",         guardado.getTipo(),
                "monto",        guardado.getMonto()
        ));
        return guardado;
    }

    // ---------------------------------------------------------------
    // Leer
    // ---------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Movimiento> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Movimiento> listarPorCuenta(int numeroCuenta) {
        return repo.findByNumeroCuenta(numeroCuenta);
    }

    @Transactional(readOnly = true)
    public Movimiento buscarPorId(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Movimiento " + id + " no encontrado"));
    }

    /**
     * Consulta personalizada #1 - JOIN Tabla A-B via @ManyToOne.
     * Devuelve movimientos con el titular de la cuenta.
     */
    @Transactional(readOnly = true)
    public List<MovimientoConTitularDTO> filtrarConTitular(
            int numeroCuenta, String tipo,
            LocalDateTime desde, LocalDateTime hasta) {
        return repo.filtrarConTitular(numeroCuenta, tipo, desde, hasta);
    }

    // ---------------------------------------------------------------
    // Actualizar
    // ---------------------------------------------------------------

    /**
     * Actualiza un movimiento existente. Si cambia la cuenta destino,
     * re-valida la nueva cuenta in-process.
     */
    public Movimiento actualizar(int id, Movimiento datos) {
        Movimiento existente = buscarPorId(id);

        if (datos.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        String tipo = datos.getTipo();
        if (tipo == null || (!tipo.equalsIgnoreCase("CREDITO") && !tipo.equalsIgnoreCase("DEBITO"))) {
            throw new IllegalArgumentException("Tipo debe ser CREDITO o DEBITO");
        }

        if (datos.getNumeroCuenta() != 0 && datos.getNumeroCuenta() != existente.getNumeroCuenta()) {
            CuentaAhorros cuenta = cuentaService.buscarPorNumero(datos.getNumeroCuenta());
            if ("Inactivo".equalsIgnoreCase(cuenta.getEstado())) {
                throw new IllegalArgumentException(
                        "La cuenta " + datos.getNumeroCuenta() + " esta inactiva");
            }
            existente.setNumeroCuenta(datos.getNumeroCuenta());
        }

        existente.setMonto(datos.getMonto());
        existente.setTipo(tipo.toUpperCase());
        // La fecha original no se modifica.

        Movimiento actualizado = repo.save(existente);

        sseService.notificar("MOVIMIENTO_ACTUALIZADO", Map.of(
                "id",           actualizado.getId(),
                "numeroCuenta", actualizado.getNumeroCuenta(),
                "tipo",         actualizado.getTipo(),
                "monto",        actualizado.getMonto()
        ));
        return actualizado;
    }

    // ---------------------------------------------------------------
    // Eliminar
    // ---------------------------------------------------------------

    public void eliminar(int id) {
        Movimiento existente = buscarPorId(id);
        repo.delete(existente);

        sseService.notificar("MOVIMIENTO_ELIMINADO", Map.of(
                "id",           existente.getId(),
                "numeroCuenta", existente.getNumeroCuenta()
        ));
    }
}
