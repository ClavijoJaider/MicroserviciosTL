package com.jacs.proyectomicroservicios.repository;

import com.jacs.proyectomicroservicios.dto.MovimientoConTitularDTO;
import com.jacs.proyectomicroservicios.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositorio Spring Data JPA para Movimiento.
 *
 * TERCER PROTOTIPO — dos consultas personalizadas obligatorias:
 *   1. {@link #filtrarConTitular} — JOIN @ManyToOne: devuelve Movimiento + titular
 *      de CuentaAhorros. Cumple: "Tabla B muestra atributos + FK + atributo de Tabla A".
 *   2. {@link #filtrar} — filtro combinado avanzado (cuenta, tipo, fechas).
 */
@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Integer> {

    // ====== Por cuenta ======

    /** Todos los movimientos de una cuenta, ordenados por fecha descendente. */
    List<Movimiento> findByNumeroCuentaOrderByFechaMovimientoDesc(int numeroCuenta);

    /** Movimientos de una cuenta filtrados por tipo (CREDITO / DEBITO). */
    List<Movimiento> findByNumeroCuentaAndTipo(int numeroCuenta, String tipo);

    // ====== Filtros globales ======

    /** Todos los movimientos de un tipo específico. */
    List<Movimiento> findByTipoOrderByFechaMovimientoDesc(String tipo);

    /** Todos los movimientos ordenados por fecha descendente. */
    List<Movimiento> findAllByOrderByFechaMovimientoDesc();

    // ====== Filtros por rango de fecha ======

    /** Movimientos de una cuenta dentro de un rango de fechas. */
    List<Movimiento> findByNumeroCuentaAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            int numeroCuenta, LocalDateTime desde, LocalDateTime hasta);

    /** Todos los movimientos dentro de un rango de fechas. */
    List<Movimiento> findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            LocalDateTime desde, LocalDateTime hasta);

    // ====== Filtro combinado avanzado (JPQL) ======

    /**
     * Filtro combinado con todos los parámetros opcionales.
     *   numeroCuenta=0 → sin filtro de cuenta.
     *   tipo=null      → sin filtro de tipo.
     *   desde/hasta=null → sin límite de fecha.
     */
    @Query("SELECT m FROM Movimiento m WHERE " +
           "(:numeroCuenta = 0 OR m.numeroCuenta = :numeroCuenta) AND " +
           "(:tipo IS NULL OR m.tipo = :tipo) AND " +
           "(:desde IS NULL OR m.fechaMovimiento >= :desde) AND " +
           "(:hasta IS NULL OR m.fechaMovimiento <= :hasta) " +
           "ORDER BY m.fechaMovimiento DESC")
    List<Movimiento> filtrar(
            @Param("numeroCuenta") int numeroCuenta,
            @Param("tipo")         String tipo,
            @Param("desde")        LocalDateTime desde,
            @Param("hasta")        LocalDateTime hasta);

    // ====== CONSULTA PERSONALIZADA #1 — TERCER PROTOTIPO ======

    /**
     * JOIN con CuentaAhorros vía relación @ManyToOne (m.cuenta).
     *
     * Devuelve {@link MovimientoConTitularDTO}:
     *   • Todos los atributos de MOVIMIENTO (id, fechaMovimiento, monto, tipo)
     *   • FK: numeroCuenta
     *   • Atributo de Tabla A: titular (CUENTA_AHORROS.TITULAR)
     *
     * Parámetros opcionales: numeroCuenta=0 y tipo=null desactivan su filtro.
     * Usado por: GET /cuentas/movimientos/filtrar
     */
    @Query("SELECT new com.jacs.proyectomicroservicios.dto.MovimientoConTitularDTO(" +
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
