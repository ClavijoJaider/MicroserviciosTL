package com.jacs.proyectomicroservicios.service;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;

import java.time.LocalDateTime;
import java.util.List;

public interface ICuentaAhorrosService {

    void agregar(CuentaAhorros cuenta);

    CuentaAhorros buscarPorNumero(int numeroCuenta);

    Movimiento agregarMovimiento(int numeroCuenta, Movimiento datos);

    List<Movimiento> listarMovimientos(int numeroCuenta);

    List<CuentaAhorros> buscarPorTitular(String titular);

    List<CuentaAhorros> listar();

    List<CuentaAhorros> listarConFiltro(String titular, String estado);

    boolean eliminar(int numeroCuenta);

    CuentaAhorros actualizar(int numeroCuenta, CuentaAhorros datos);

    List<Movimiento> listarTodosMovimientos();

    // CRUD completo de Movimientos
    Movimiento buscarMovimientoPorId(int numeroCuenta, int id);

    Movimiento actualizarMovimiento(int numeroCuenta, int id, Movimiento datos);

    boolean eliminarMovimiento(int numeroCuenta, int id);

    /**
     * Filtro avanzado de movimientos.
     *
     * @param numeroCuenta 0 = todas las cuentas; >0 = cuenta específica
     * @param tipo         "CREDITO", "DEBITO" o null/blank para todos
     * @param desde        fecha inicial inclusive (null = sin límite)
     * @param hasta        fecha final   inclusive (null = sin límite)
     */
    List<Movimiento> filtrarMovimientos(int numeroCuenta, String tipo,
                                        LocalDateTime desde, LocalDateTime hasta);
}