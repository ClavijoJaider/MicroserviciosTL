package com.jacs.proyectomicroservicios.repository;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio Spring Data JPA para CuentaAhorros.
 *
 * Operaciones heredadas de JpaRepository:
 *   save(entity)       → INSERT si es nueva / UPDATE si ya existe
 *   findById(id)       → SELECT por PK
 *   findAll()          → SELECT todos
 *   deleteById(id)     → DELETE por PK
 *   existsById(id)     → EXISTS
 *   count()            → COUNT
 *
 * Métodos de consulta derivados (Spring Data genera el SQL automáticamente):
 */
@Repository
public interface CuentaAhorrosRepository extends JpaRepository<CuentaAhorros, Integer> {

    /** Buscar cuentas cuyo titular contenga el texto (sin distinción de mayúsculas). */
    List<CuentaAhorros> findByTitularContainingIgnoreCase(String titular);

    /** Buscar cuentas por estado exacto (ej. "Activo" o "Inactivo"). */
    List<CuentaAhorros> findByEstado(String estado);

    /** Buscar cuentas por titular Y estado. */
    List<CuentaAhorros> findByTitularContainingIgnoreCaseAndEstado(String titular, String estado);

    /** Ordenar todas las cuentas por número de cuenta ascendente. */
    List<CuentaAhorros> findAllByOrderByNumeroCuentaAsc();

    /** Buscar cuentas con saldo mayor o igual al valor indicado. */
    List<CuentaAhorros> findBySaldoGreaterThanEqual(double saldoMinimo);

    /**
     * Filtro combinado: titular (opcional) + estado (opcional).
     * Usa JPQL para manejar ambos parámetros opcionales.
     */
    @Query("SELECT c FROM CuentaAhorros c WHERE " +
           "(:titular IS NULL OR LOWER(c.titular) LIKE LOWER(CONCAT('%', :titular, '%'))) AND " +
           "(:estado IS NULL OR c.estado = :estado) " +
           "ORDER BY c.numeroCuenta ASC")
    List<CuentaAhorros> filtrar(
            @Param("titular") String titular,
            @Param("estado")  String estado
    );
}
