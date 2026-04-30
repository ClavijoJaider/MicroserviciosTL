using System.Collections.Generic;
using WayBankClient.model;

namespace WayBankClient.service
{
    internal interface IServicePeticiones
    {
        // CRUD Cuentas
        bool CrearCuenta(CuentaAhorrosDto cuenta);
        List<CuentaAhorrosDto> ListarCuentas();
        List<CuentaAhorrosDto> ListarCuentasPorEstado(string estado);
        List<CuentaAhorrosDto> FiltrarCuentas(string titular, string estado);
        List<CuentaAhorrosDto> BuscarPorTitular(string nombreTitular);
        CuentaAhorrosDto BuscarPorNumeroCuenta(int numeroCuenta);
        bool EliminarLogico(int numeroCuenta);
        bool ActualizarCuenta(int numeroCuenta, CuentaAhorrosDto cuentaEditada);
        void Healthcheck();

        // CRUD Movimientos (completo)
        bool AgregarMovimiento(int numeroCuenta, double monto, string tipo);
        List<MovimientoDto> ListarMovimientos(int numeroCuenta);
        List<MovimientoDto> ListarTodosMovimientos();
        MovimientoDto BuscarMovimientoPorId(int numeroCuenta, int id);
        bool ActualizarMovimiento(int numeroCuenta, int id, double monto, string tipo);
        bool EliminarMovimiento(int numeroCuenta, int id);

        // Observer
        void NotificarCambios();
    }
}
