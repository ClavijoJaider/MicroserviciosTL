package com.jacs.proyectomicroservicios.service;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;

import java.util.List;

/**
 * Contrato del Microservicio MAESTRO — CuentaAhorros (puerto 8080).
 *
 * SEPARACION ESTRICTA DE RESPONSABILIDADES (Tercer Prototipo):
 *   Este servicio gestiona EXCLUSIVAMENTE la tabla CUENTA_AHORROS.
 *   Todo lo relacionado con MOVIMIENTO se delega al MS-Movimiento (puerto 8081).
 */
public interface ICuentaAhorrosService {

    // ---- Crear ----
    void agregar(CuentaAhorros cuenta);

    // ---- Leer ----
    CuentaAhorros buscarPorNumero(int numeroCuenta);
    List<CuentaAhorros> listar();
    List<CuentaAhorros> listarConFiltro(String titular, String estado);
    List<CuentaAhorros> buscarPorTitular(String titular);

    // ---- Actualizar ----
    CuentaAhorros actualizar(int numeroCuenta, CuentaAhorros datos);

    // ---- Eliminar (baja lógica) ----
    boolean eliminar(int numeroCuenta);
}
