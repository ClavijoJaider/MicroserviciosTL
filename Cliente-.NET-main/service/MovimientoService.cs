using System.Collections.Generic;
using RestSharp;
using WayBankClient.model;

namespace WayBankClient.service
{
    /// <summary>
    /// Singleton secundario para operaciones de movimiento.
    /// Apunta a MS-Movimiento (puerto 8081).
    ///
    /// Para la mayoría de las vistas se recomienda usar ServicePeticiones.GetInstance()
    /// que unifica los dos microservicios y gestiona el patrón Observer SSE.
    /// </summary>
    public class MovimientoService : IMovimientoService
    {
        private static MovimientoService instance;
        private readonly RestClient client;

        private MovimientoService()
        {
            // MS-Movimiento corre en puerto 8081
            client = new RestClient(new RestClientOptions("http://localhost:8081/movimientos"));
        }

        public static MovimientoService GetInstance()
        {
            if (instance == null)
                instance = new MovimientoService();
            return instance;
        }

        /// <summary>
        /// Registra un movimiento. POST /movimientos con numeroCuenta en el body.
        /// MS-Movimiento valida la cuenta en MS-CuentaAhorros (8080) internamente.
        /// </summary>
        public bool AgregarMovimiento(int numeroCuenta, double monto, string tipo)
        {
            var request = new RestRequest("", Method.Post);
            request.AddJsonBody(new { numeroCuenta, monto, tipo = tipo.ToUpper() });
            var response = client.Execute(request);
            return response.IsSuccessful;
        }

        /// <summary>Lista movimientos de una cuenta. GET /movimientos/cuenta/{n}</summary>
        public List<MovimientoDto> ListarMovimientos(int numeroCuenta)
        {
            return client.Get<List<MovimientoDto>>(
                new RestRequest($"cuenta/{numeroCuenta}", Method.Get))
                ?? new List<MovimientoDto>();
        }

        /// <summary>
        /// Filtra movimientos por tipo usando el endpoint personalizado.
        /// GET /movimientos/filtrar?numeroCuenta=n&tipo=tipo
        /// </summary>
        public List<MovimientoDto> ListarMovimientosPorTipo(int numeroCuenta, string tipo)
        {
            var request = new RestRequest("filtrar", Method.Get);
            if (numeroCuenta > 0)
                request.AddQueryParameter("numeroCuenta", numeroCuenta.ToString());
            if (!string.IsNullOrWhiteSpace(tipo))
                request.AddQueryParameter("tipo", tipo.ToUpper());
            return client.Get<List<MovimientoDto>>(request)
                   ?? new List<MovimientoDto>();
        }
    }
}
