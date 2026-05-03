package com.jacs.movimientoservice.service;

import com.jacs.movimientoservice.client.CuentaAhorrosClient;
import com.jacs.movimientoservice.dto.MovimientoConTitularDTO;
import com.jacs.movimientoservice.model.Movimiento;
import com.jacs.movimientoservice.observer.SseService;
import com.jacs.movimientoservice.repository.MovimientoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Servicio de negocio para MOVIMIENTO (Microservicio Detalle, puerto 8081).
 *
 * Intercomunicacion con MS-CuentaAhorros (8080):
 *   Antes de crear o actualizar un movimiento, se valida que la cuenta
 *   exista y este activa llamando a {@link CuentaAhorrosClient}.
 *
 * Observer SSE:
 *   Cada mutacion notifica a los clientes suscritos via SSE.
 */
@Service
public class MovimientoService {

    private final MovimientoRepository  repo;
    private final CuentaAhorrosClient   cuentasClient;
    private final SseService            sseService;

    public MovimientoService(MovimientoRepository repo,
                             CuentaAhorrosClient cuentasClient,
                             SseService sseService) {
        this.repo          = repo;
        this.cuentasClient = cuentasClient;
        this.sseService    = sseService;
    }

    // ---------------------------------------------------------------
    // Crear
    // ---------------------------------------------------------------

    /**
     * Registra un nuevo movimiento.
     *
     * Intercomunicacion: valida la cuenta en MS-CuentaAhorros antes de persistir.
     *
     * @param datos datos del movimiento (numeroCuenta, tipo, monto)
     * @return movimiento guardado con ID asignado
     */
    public Movimiento agregar(Movimiento datos) {
        if (datos.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        String tipo = datos.getTipo();
        if (tipo == null || (!tipo.equalsIgnoreCase("CREDITO") && !tipo.equalsIgnoreCase("DEBITO"))) {
            throw new IllegalArgumentException("Tipo debe ser CREDITO o DEBITO");
        }

        // INTERCOMUNICACION: verifica que la cuenta exista y este activa en MS-CuentaAhorros
        cuentasClient.validarCuentaActiva(datos.getNumeroCuenta());

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

    /** Lista todos los movimientos. */
    public List<Movimiento> listar() {
        return repo.findAll();
    }

    /** Lista movimientos de una cuenta especifica. */
    public List<Movimiento> listarPorCuenta(int numeroCuenta) {
        return repo.findByNumeroCuenta(numeroCuenta);
    }

    /** Busca un movimiento por ID. */
    public Movimiento buscarPorId(int id) {
        return repo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Movimiento " + id + " no encontrado"));
    }

    /**
     * Consulta personalizada #1 — JOIN Tabla A-B via @ManyToOne.
     * Devuelve movimientos con el titular de la cuenta.
     */
    public List<MovimientoConTitularDTO> filtrarConTitular(
            int numeroCuenta, String tipo,
            LocalDateTime desde, LocalDateTime hasta) {
        return repo.filtrarConTitular(numeroCuenta, tipo, desde, hasta);
    }

    // ---------------------------------------------------------------
    // Actualizar
    // ---------------------------------------------------------------

    /**
     * Actualiza un movimiento existente.
     *
     * Intercomunicacion: si cambia la cuenta destino, re-valida en MS-CuentaAhorros.
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

        // INTERCOMUNICACION: si la cuenta cambia, validar en MS-CuentaAhorros
        if (datos.getNumeroCuenta() != existente.getNumeroCuenta()) {
            cuentasClient.validarCuentaActiva(datos.getNumeroCuenta());
        }

        existente.setMonto(datos.getMonto());
        existente.setTipo(tipo.toUpperCase());
        existente.setNumeroCuenta(datos.getNumeroCuenta());
        // La fecha no se modifica en actualizaciones

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

    /** Elimina un movimiento por ID. */
    public void eliminar(int id) {
        Movimiento existente = buscarPorId(id);
        repo.delete(existente);

        sseService.notificar("MOVIMIENTO_ELIMINADO", Map.of(
                "id",           existente.getId(),
                "numeroCuenta", existente.getNumeroCuenta()
        ));
    }
}
