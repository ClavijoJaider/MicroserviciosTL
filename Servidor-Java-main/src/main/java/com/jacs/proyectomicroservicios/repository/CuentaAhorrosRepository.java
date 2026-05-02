package com.jacs.proyectomicroservicios.repository;

import com.jacs.proyectomicroservicios.dto.ResumenCuentaDTO;
import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para CuentaAhorros.
 *
 * TERCER PROTOTIPO — consulta personalizada #2:
 *   {@link #findResumenPorNumero} — datos del maestro (CuentaAhorros) + dos campos
 *   calculados del detalle (Movimiento): totalMovimientos y totalCreditos.
 *   "Una consulta debe mostrar datos de la tabla maestro y dos de detalle."
 */
@Repository
public interface CuentaAhorrosRepository extends JpaRepository<CuentaAhorros, Integer> {

    /** Buscar cuentas cuyo titular contenga el texto (sin distinción de mayúsculas). */
    List<CuentaAhorros> findByTitularContainingIgnoreCase(String titular);

    /** Buscar cuentas por estado exacto ("Activo" / "Inactivo"). */
    List<CuentaAhorros> findByEstado(String estado);

    /** Buscar cuentas por titular Y estado. */
    List<CuentaAhorros> findByTitularContainingIgnoreCaseAndEstado(String titular, String estado);

    /** Todas las cuentas ordenadas por número ascendente. */
    List<CuentaAhorros> findAllByOrderByNumeroCuentaAsc();

    /** Cuentas con saldo mayor o igual al valor indicado. */
    List<CuentaAhorros> findBySaldoGreaterThanEqual(double saldoMinimo);

    /**
     * Filtro combinado: titular (opcional) + estado (opcional).
     */
    @Query("SELECT c FROM CuentaAhorros c WHERE " +
           "(:titular IS NULL OR LOWER(c.titular) LIKE LOWER(CONCAT('%', :titular, '%'))) AND " +
           "(:estado IS NULL OR c.estado = :estado) " +
           "ORDER BY c.numeroCuenta ASC")
    List<CuentaAhorros> filtrar(
            @Param("titular") String titular,
            @Param("estado")  String estado);

    // ====== CONSULTA PERSONALIZADA #2 — TERCER PROTOTIPO ======

    /**
     * Resumen de cuenta con dos datos del detalle.
     *
     * Devuelve {@link ResumenCuentaDTO}:
     *   • Datos de CUENTA_AHORROS (maestro): numeroCuenta, titular, saldo, estado, tasaInteres
     *   • Dato de detalle 1: totalMovimientos — COUNT de MOVIMIENTO asociados
     *   • Dato de detalle 2: totalCreditos    — SUM de montos CREDITO
     *
     * Usa subconsultas correlacionadas JPQL (sin @OneToMany en el maestro).
     * Usado por: GET /cuentas/{numero}/resumen
     */
    @Query("SELECT new com.jacs.proyectomicroservicios.dto.ResumenCuentaDTO(" +
           "    c.numeroCuenta, c.titular, c.saldo, c.estado, c.tasaInteres, " +
           "    (SELECT COUNT(m)  FROM Movimiento m WHERE m.numeroCuenta = c.numeroCuenta), " +
           "    (SELECT COALESCE(SUM(m2.monto), 0.0) FROM Movimiento m2 " +
           "     WHERE m2.numeroCuenta = c.numeroCuenta AND m2.tipo = 'CREDITO')) " +
           "FROM CuentaAhorros c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<ResumenCuentaDTO> findResumenPorNumero(@Param("numeroCuenta") int numeroCuenta);
}
