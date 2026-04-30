package com.jacs.proyectomicroservicios.repository;

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
 * Incluye filtros avanzados por tipo, rango de fecha y cuenta.
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

    /**
     * Movimientos de una cuenta dentro de un rango de fechas, ordenados por fecha desc.
     */
    List<Movimiento> findByNumeroCuentaAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            int numeroCuenta,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    /**
     * Todos los movimientos dentro de un rango de fechas (sin filtro de cuenta).
     */
    List<Movimiento> findByFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            LocalDateTime desde,
            LocalDateTime hasta
    );

    // ====== Filtro combinado avanzado (JPQL) ======

    /**
     * Filtro combinado con todos los parámetros opcionales:
     *   - numeroCuenta: si es null o 0, no filtra por cuenta
     *   - tipo: si es null o vacío, no filtra por tipo
     *   - desde / hasta: si son null, no filtra por fecha
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
            @Param("hasta")        LocalDateTime hasta
    );
}
