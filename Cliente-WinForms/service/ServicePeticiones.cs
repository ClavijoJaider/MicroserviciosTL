using RestSharp;
using System;
using System.IO;
using System.Net;
using System.Threading;
using System.Collections.Generic;
using System.Windows.Forms;
using WayBankClient.model;

namespace WayBankClient.service
{
    /// <summary>
    /// Singleton que centraliza todas las peticiones REST a los dos microservicios:
    ///
    ///   clientCuentas     → http://localhost:8080/cuentas   (MS-CuentaAhorros)
    ///   clientMovimientos → http://localhost:8080/movimientos (MS-Movimiento)
    ///
    /// OBSERVER en cliente (patrón Observer — lado cliente):
    ///   Este servicio abre DOS hilos SSE, uno por microservicio.
    ///   Cuando cualquiera de los servidores emite un evento, se disparan
    ///   los eventos OnCuentasActualizadas / OnMovimientosActualizados
    ///   para que todas las vistas reaccionen en tiempo real sin polling.
    ///
    ///   SSE Hilo 1 → GET http://localhost:8080/cuentas/eventos
    ///   SSE Hilo 2 → GET http://localhost:8080/movimientos/eventos
    /// </summary>
    public class ServicePeticiones : IServicePeticiones
    {
        private static ServicePeticiones instance;

        // Un cliente REST por microservicio
        private readonly RestClient clientCuentas;
        private readonly RestClient clientMovimientos;

        // Eventos Observer que las vistas suscriben
        public event Action OnCuentasActualizadas;
        public event Action OnMovimientosActualizados;

        // Dos hilos SSE, uno por microservicio
        private Thread sseCuentasThread;
        private Thread sseMovimientosThread;
        private volatile bool sseActivo = false;

        private ServicePeticiones()
        {
            clientCuentas     = new RestClient(new RestClientOptions("http://localhost:8080/cuentas"));
            clientMovimientos = new RestClient(new RestClientOptions("http://localhost:8080/movimientos"));
            IniciarSSE();
        }

        public static ServicePeticiones GetInstance()
        {
            if (instance == null)
                instance = new ServicePeticiones();
            return instance;
        }

        // ================================================================
        // OBSERVER — conexión SSE a los dos microservicios
        // ================================================================

        private void IniciarSSE()
        {
            sseActivo = true;

            // Hilo SSE → MS-CuentaAhorros (8080)
            sseCuentasThread = new Thread(() => ConectarSSE(
                "http://localhost:8080/cuentas/eventos",
                () => OnCuentasActualizadas?.Invoke()
            ))
            {
                IsBackground = true,
                Name = "SSE-CuentaAhorros-Thread"
            };
            sseCuentasThread.Start();

            // Hilo SSE → MS-Movimiento (8081)
            sseMovimientosThread = new Thread(() => ConectarSSE(
                "http://localhost:8081/movimientos/eventos",
                () => OnMovimientosActualizados?.Invoke()
            ))
            {
                IsBackground = true,
                Name = "SSE-Movimiento-Thread"
            };
            sseMovimientosThread.Start();
        }

        /// <summary>
        /// Mantiene la conexión SSE al endpoint indicado.
        /// Al recibir cualquier evento "data:", invoca el callback para notificar vistas.
        /// Se reconecta automáticamente si el servidor no está disponible.
        /// </summary>
        private void ConectarSSE(string url, Action onEvento)
        {
            while (sseActivo)
            {
                try
                {
                    var req = (HttpWebRequest)WebRequest.Create(url);
                    req.Accept           = "text/event-stream";
                    req.Timeout          = System.Threading.Timeout.Infinite;
                    req.ReadWriteTimeout = System.Threading.Timeout.Infinite;

                    using (var resp   = (HttpWebResponse)req.GetResponse())
                    using (var stream = resp.GetResponseStream())
                    using (var reader = new StreamReader(stream))
                    {
                        while (sseActivo && !reader.EndOfStream)
                        {
                            var linea = reader.ReadLine();
                            if (linea != null && linea.StartsWith("data:"))
                                onEvento();
                        }
                    }
                }
                catch (ThreadInterruptedException)
                {
                    break;
                }
                catch (Exception)
                {
                    // Servidor no disponible — reintentar en 3 s
                    if (sseActivo) Thread.Sleep(3000);
                }
            }
        }

        public void DetenerSSE()
        {
            sseActivo = false;
            sseCuentasThread?.Interrupt();
            sseMovimientosThread?.Interrupt();
        }

        public void NotificarCambios()
        {
            OnCuentasActualizadas?.Invoke();
            OnMovimientosActualizados?.Invoke();
        }

        // ================================================================
        // CRUD Cuentas → MS-CuentaAhorros (8080)
        // ================================================================

        public bool CrearCuenta(CuentaAhorrosDto cuenta)
        {
            var request = new RestRequest("", Method.Post);
            request.AddJsonBody(cuenta);
            var response = clientCuentas.Execute(request);
            if (!response.IsSuccessful)
            {
                MessageBox.Show("Error al crear cuenta: " + response.Content,
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
            return true;
        }

        public List<CuentaAhorrosDto> ListarCuentas()
        {
            return clientCuentas.Get<List<CuentaAhorrosDto>>(new RestRequest("", Method.Get))
                   ?? new List<CuentaAhorrosDto>();
        }

        public List<CuentaAhorrosDto> ListarCuentasPorEstado(string estado)
        {
            var request = new RestRequest("filtrar", Method.Get);
            request.AddQueryParameter("estado", estado);
            return clientCuentas.Get<List<CuentaAhorrosDto>>(request)
                   ?? new List<CuentaAhorrosDto>();
        }

        public List<CuentaAhorrosDto> FiltrarCuentas(string titular, string estado)
        {
            var request = new RestRequest("filtrar", Method.Get);
            if (!string.IsNullOrWhiteSpace(titular))
                request.AddQueryParameter("titular", titular);
            if (!string.IsNullOrWhiteSpace(estado) && estado != "Todos")
                request.AddQueryParameter("estado", estado);
            return clientCuentas.Get<List<CuentaAhorrosDto>>(request)
                   ?? new List<CuentaAhorrosDto>();
        }

        public List<CuentaAhorrosDto> BuscarPorTitular(string nombreTitular)
        {
            var request = new RestRequest("buscar", Method.Get);
            request.AddQueryParameter("titular", nombreTitular);
            return clientCuentas.Get<List<CuentaAhorrosDto>>(request)
                   ?? new List<CuentaAhorrosDto>();
        }

        public CuentaAhorrosDto BuscarPorNumeroCuenta(int numeroCuenta)
        {
            var response = clientCuentas.Execute<CuentaAhorrosDto>(
                new RestRequest($"{numeroCuenta}", Method.Get));
            return response.IsSuccessful ? response.Data : null;
        }

        public bool EliminarLogico(int numeroCuenta)
        {
            var response = clientCuentas.Execute(
                new RestRequest($"{numeroCuenta}", Method.Delete));
            if (!response.IsSuccessful)
            {
                MessageBox.Show("Error al eliminar cuenta: " + response.Content,
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
            return true;
        }

        public bool ActualizarCuenta(int numeroCuenta, CuentaAhorrosDto cuentaEditada)
        {
            var request = new RestRequest($"{numeroCuenta}", Method.Put);
            request.AddJsonBody(cuentaEditada);
            var response = clientCuentas.Execute(request);
            if (!response.IsSuccessful)
            {
                MessageBox.Show("Error al actualizar cuenta: " + response.Content,
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
            return true;
        }

        public void Healthcheck()
        {
            var response = clientCuentas.Execute(
                new RestRequest("healthcheck", Method.Get));
            if (response.IsSuccessful)
                MessageBox.Show(response.Content, "Estado del servidor",
                    MessageBoxButtons.OK, MessageBoxIcon.Information);
            else
                MessageBox.Show("No se pudo conectar con MS-CuentaAhorros (8080).",
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }

        // ================================================================
        // CRUD Movimientos → MS-Movimiento (8081)
        //
        // Cambios respecto a la versión anterior (todo en 8080):
        //   POST   /cuentas/{n}/movimientos → POST   /movimientos  (numeroCuenta en body)
        //   GET    /cuentas/{n}/movimientos → GET    /movimientos/cuenta/{n}
        //   GET    /cuentas/movimientos     → GET    /movimientos
        //   GET    /cuentas/{n}/mov/{id}   → GET    /movimientos/{id}
        //   PUT    /cuentas/{n}/mov/{id}   → PUT    /movimientos/{id}
        //   DELETE /cuentas/{n}/mov/{id}   → DELETE /movimientos/{id}
        // ================================================================

        /// <summary>
        /// Registra un nuevo movimiento.
        /// Llama a MS-Movimiento (8081), que a su vez valida la cuenta en MS-CuentaAhorros (8080).
        /// Intercomunicación entre microservicios — transparente para el cliente.
        /// </summary>
        public bool AgregarMovimiento(int numeroCuenta, double monto, string tipo)
        {
            var request = new RestRequest("", Method.Post);
            request.AddJsonBody(new { numeroCuenta, monto, tipo = tipo.ToUpper() });
            var response = clientMovimientos.Execute(request);
            if (!response.IsSuccessful)
            {
                MessageBox.Show("Error al agregar movimiento: " + response.Content,
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
            return true;
        }

        public List<MovimientoDto> ListarMovimientos(int numeroCuenta)
        {
            return clientMovimientos.Get<List<MovimientoDto>>(
                new RestRequest($"cuenta/{numeroCuenta}", Method.Get))
                ?? new List<MovimientoDto>();
        }

        public List<MovimientoDto> ListarTodosMovimientos()
        {
            try
            {
                return clientMovimientos.Get<List<MovimientoDto>>(
                    new RestRequest("", Method.Get))
                    ?? new List<MovimientoDto>();
            }
            catch (Exception ex)
            {
                MessageBox.Show("No se pudo obtener los movimientos: " + ex.Message,
                    "Error de conexión", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return new List<MovimientoDto>();
            }
        }

        public MovimientoDto BuscarMovimientoPorId(int numeroCuenta, int id)
        {
            // numeroCuenta ya no es parte de la URL en MS-Movimiento
            var response = clientMovimientos.Execute<MovimientoDto>(
                new RestRequest($"{id}", Method.Get));
            return response.IsSuccessful ? response.Data : null;
        }

        public bool ActualizarMovimiento(int numeroCuenta, int id, double monto, string tipo)
        {
            var request = new RestRequest($"{id}", Method.Put);
            // Incluir numeroCuenta en el body para que MS-Movimiento pueda re-validar si cambia
            request.AddJsonBody(new { numeroCuenta, monto, tipo = tipo.ToUpper() });
            var response = clientMovimientos.Execute(request);
            if (!response.IsSuccessful)
            {
                MessageBox.Show("Error al actualizar movimiento: " + response.Content,
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
            return true;
        }

        public bool EliminarMovimiento(int numeroCuenta, int id)
        {
            var response = clientMovimientos.Execute(
                new RestRequest($"{id}", Method.Delete));
            if (!response.IsSuccessful)
            {
                MessageBox.Show("Error al eliminar movimiento: " + response.Content,
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
            return true;
        }

        /// <summary>
        /// Consulta personalizada #1 — JOIN @ManyToOne entre MOVIMIENTO y CUENTA_AHORROS.
        /// Devuelve movimientos con el titular de la cuenta (campo de Tabla A).
        /// GET /movimientos/filtrar?numeroCuenta=X&tipo=Y en MS-Movimiento (8081).
        /// </summary>
        public List<MovimientoConTitularDto> FiltrarMovimientosConTitular(
            int numeroCuenta, string tipo)
        {
            var request = new RestRequest("filtrar", Method.Get);
            if (numeroCuenta > 0)
                request.AddQueryParameter("numeroCuenta", numeroCuenta.ToString());
            if (!string.IsNullOrWhiteSpace(tipo) && tipo != "Todos")
                request.AddQueryParameter("tipo", tipo.ToUpper());
            return clientMovimientos.Get<List<MovimientoConTitularDto>>(request)
                   ?? new List<MovimientoConTitularDto>();
        }

        /// <summary>
        /// Consulta personalizada #2 — datos del MAESTRO + dos del DETALLE.
        /// "Una consulta debe mostrar datos de la tabla maestro y dos de detalle."
        /// GET /cuentas/{numeroCuenta}/resumen en MS-CuentaAhorros (8080).
        /// Devuelve: numeroCuenta, titular, saldo, estado, tasaInteres,
        ///           totalMovimientos (COUNT), totalCreditos (SUM CREDITO).
        /// </summary>
        public ResumenCuentaDto ObtenerResumen(int numeroCuenta)
        {
            var request = new RestRequest($"{numeroCuenta}/resumen", Method.Get);
            return clientCuentas.Get<ResumenCuentaDto>(request);
        }
    }
}
