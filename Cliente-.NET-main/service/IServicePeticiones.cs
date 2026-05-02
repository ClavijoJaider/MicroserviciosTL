using System.Collections.Generic;
using WayBankClient.model;

namespace WayBankClient.service
{
    /// <summary>
    /// Interfaz unificada del cliente REST para los dos microservicios:
    ///   - MS-CuentaAhorros  (puerto 8080) — operaciones de cuentas
    ///   - MS-Movimiento     (puerto 8081) — operaciones de movimientos
    /// </summary>
    internal interface IServicePeticiones
    {
        // ============ CRUD Cuentas — MS-CuentaAhorros (8080) ============
        bool CrearCuenta(CuentaAhorrosDto cuenta);
        List<CuentaAhorrosDto> ListarCuentas();
        List<CuentaAhorrosDto> ListarCuentasPorEstado(string estado);
        List<CuentaAhorrosDto> FiltrarCuentas(string titular, string estado);
        List<CuentaAhorrosDto> BuscarPorTitular(string nombreTitular);
        CuentaAhorrosDto BuscarPorNumeroCuenta(int numeroCuenta);
        bool EliminarLogico(int numeroCuenta);
        bool ActualizarCuenta(int numeroCuenta, CuentaAhorrosDto cuentaEditada);
        void Healthcheck();

        // ============ CRUD Movimientos — MS-Movimiento (8081) ============
        bool AgregarMovimiento(int numeroCuenta, double monto, string tipo);
        List<MovimientoDto> ListarMovimientos(int numeroCuenta);
        List<MovimientoDto> ListarTodosMovimientos();
        MovimientoDto BuscarMovimientoPorId(int numeroCuenta, int id);
        bool ActualizarMovimiento(int numeroCuenta, int id, double monto, string tipo);
        bool EliminarMovimiento(int numeroCuenta, int id);

        /// <summary>
        /// Consulta personalizada #1 — JOIN Tabla A-B via @ManyToOne.
        /// Retorna movimientos con el titular de la cuenta.
        /// GET /movimientos/filtrar en MS-Movimiento (8081).
        /// </summary>
        List<MovimientoConTitularDto> FiltrarMovimientosConTitular(
            int numeroCuenta, string tipo);

        // ============ Observer ============
        void NotificarCambios();
    }
}
