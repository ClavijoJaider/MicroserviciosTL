package com.jacs.movimientoservice.repository;

import com.jacs.movimientoservice.dto.MovimientoConTitularDTO;
import com.jacs.movimientoservice.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio JPA para MOVIMIENTO.
 *
 * Consultas personalizadas (TERCER PROTOTIPO):
 *
 * 1. {@link #filtrarConTitular} — JPQL con JOIN @ManyToOne.
 *    Devuelve todos los atributos de MOVIMIENTO (Tabla B) + FK + un atributo
 *    de CUENTA_AHORROS (Tabla A): el titular.
 *    Usa constructor expression para proyectar a DTO sin cargar las entidades.
 *
 * 2. {@link #findByNumeroCuenta} — finders derivados de Spring Data.
 *    Lista todos los movimientos de una cuenta especifica.
 */
@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    /** Listar movimientos de una cuenta especifica (finder derivado). */
    List<Movimiento> findByNumeroCuenta(int numeroCuenta);

    /**
     * Consulta personalizada #1 — JOIN entre tablas A y B via @ManyToOne.
     *
     * Retorna MovimientoConTitularDTO con:
     *   - Todos los atributos de MOVIMIENTO (id, fechaMovimiento, monto, tipo)
     *   - La FK (numeroCuenta)
     *   - Un atributo de CUENTA_AHORROS: titular
     *
     * Filtros opcionales:
     *   - numeroCuenta = 0  -> todas las cuentas
     *   - tipo = null       -> ambos tipos (CREDITO y DEBITO)
     *   - desde/hasta = null -> sin rango de fecha
     */
    @Query("SELECT new com.jacs.movimientoservice.dto.MovimientoConTitularDTO(" +
           "    m.id, m.fechaMovimiento, m.monto, m.tipo, m.numeroCuenta, c.titular) " +
           "FROM Movimiento m JOIN m.cuenta c " +
           "WHERE (:numeroCuenta = 0 OR m.numeroCuenta = :numeroCuenta) " +
           "  AND (:tipo IS NULL OR m.tipo = :tipo) " +
           "  AND (:desde IS NULL OR m.fechaMovimiento >= :desde) " +
           "  AND (:hasta IS NULL OR m.fechaMovimiento <= :hasta) " +
           "ORDER BY m.fechaMovimiento DESC")
    List<MovimientoConTitularDTO> filtrarConTitular(
            @Param("numeroCuenta") int numeroCuenta,
            @Param("tipo")         String tipo,
            @Param("desde")        LocalDateTime desde,
            @Param("hasta")        LocalDateTime hasta);
}
