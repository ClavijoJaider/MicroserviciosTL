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
    /**
     * Singleton que centraliza todas las peticiones REST al servidor.
     *
     * OBSERVER en cliente: en lugar de un timer de polling, este servicio
     * se suscribe al endpoint SSE del servidor (GET /cuentas/eventos).
     * Cuando el servidor emite un evento (cuenta creada, actualizada, eliminada,
     * movimiento agregado), se disparan los eventos OnCuentasActualizadas y
     * OnMovimientosActualizados para que las vistas reaccionen en tiempo real.
     */
    public class ServicePeticiones : IServicePeticiones
    {
        private static ServicePeticiones instance;
        private readonly RestClient client;

        public event Action OnCuentasActualizadas;
        public event Action OnMovimientosActualizados;

        private Thread sseThread;
        private volatile bool sseActivo = false;

        private ServicePeticiones()
        {
            var options = new RestClientOptions("http://localhost:8080/cuentas");
            client = new RestClient(options);
            IniciarSSE();
        }

        public static ServicePeticiones GetInstance()
        {
            if (instance == null)
                instance = new ServicePeticiones();
            return instance;
        }

        // ============ OBSERVER: conexión SSE al servidor ============

        private void IniciarSSE()
        {
            sseActivo = true;
            sseThread = new Thread(ConectarSSE)
            {
                IsBackground = true,
                Name = "SSE-Observer-Thread"
            };
            sseThread.Start();
        }

        private void ConectarSSE()
        {
            while (sseActivo)
            {
                try
                {
                    var req = (HttpWebRequest)WebRequest.Create("http://localhost:8080/cuentas/eventos");
                    req.Accept = "text/event-stream";
                    req.Timeout = System.Threading.Timeout.Infinite;
                    req.ReadWriteTimeout = System.Threading.Timeout.Infinite;

                    using (var resp = (HttpWebResponse)req.GetResponse())
                    using (var stream = resp.GetResponseStream())
                    using (var reader = new StreamReader(stream))
                    {
                        while (sseActivo && !reader.EndOfStream)
                        {
                            var linea = reader.ReadLine();
                            if (linea == null) continue;

                            if (linea.StartsWith("data:"))
                            {
                                // El servidor notificó un cambio: avisar a todas las vistas
                                OnCuentasActualizadas?.Invoke();
                                OnMovimientosActualizados?.Invoke();
                            }
                        }
                    }
                }
                catch (ThreadInterruptedException)
                {
                    break;
                }
                catch (Exception)
                {
                    // Reconectar tras 3 segundos si el servidor no está disponible
                    if (sseActivo)
                        Thread.Sleep(3000);
                }
            }
        }

        public void DetenerSSE()
        {
            sseActivo = false;
            sseThread?.Interrupt();
        }

        public void NotificarCambios()
        {
            OnCuentasActualizadas?.Invoke();
            OnMovimientosActualizados?.Invoke();
        }

        // ============ CRUD Cuentas ============

        public bool CrearCuenta(CuentaAhorrosDto cuenta)
        {
            var request = new RestRequest("", Method.Post);
            request.AddJsonBody(cuenta);
            var response = client.Execute(request);

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
            var request = new RestRequest("", Method.Get);
            var response = client.Get<List<CuentaAhorrosDto>>(request);
            return response ?? new List<CuentaAhorrosDto>();
        }

        public List<CuentaAhorrosDto> ListarCuentasPorEstado(string estado)
        {
            var request = new RestRequest("filtrar", Method.Get);
            request.AddQueryParameter("estado", estado);
            var response = client.Get<List<CuentaAhorrosDto>>(request);
            return response ?? new List<CuentaAhorrosDto>();
        }

        public List<CuentaAhorrosDto> FiltrarCuentas(string titular, string estado)
        {
            var request = new RestRequest("filtrar", Method.Get);

            if (!string.IsNullOrWhiteSpace(titular))
                request.AddQueryParameter("titular", titular);

            if (!string.IsNullOrWhiteSpace(estado) && estado != "Todos")
                request.AddQueryParameter("estado", estado);

            var response = client.Get<List<CuentaAhorrosDto>>(request);
            return response ?? new List<CuentaAhorrosDto>();
        }

        public List<CuentaAhorrosDto> BuscarPorTitular(string nombreTitular)
        {
            var request = new RestRequest("buscar", Method.Get);
            request.AddQueryParameter("titular", nombreTitular);
            var response = client.Get<List<CuentaAhorrosDto>>(request);
            return response ?? new List<CuentaAhorrosDto>();
        }

        public CuentaAhorrosDto BuscarPorNumeroCuenta(int numeroCuenta)
        {
            var request = new RestRequest($"{numeroCuenta}", Method.Get);
            var response = client.Execute<CuentaAhorrosDto>(request);

            if (!response.IsSuccessful || response.Data == null)
                return null;

            return response.Data;
        }

        public bool EliminarLogico(int numeroCuenta)
        {
            var request = new RestRequest($"{numeroCuenta}", Method.Delete);
            var response = client.Execute(request);

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
            var response = client.Execute(request);

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
            var request = new RestRequest("healthcheck", Method.Get);
            var response = client.Execute(request);

            if (response.IsSuccessful)
            {
                MessageBox.Show(response.Content, "Estado del servidor",
                    MessageBoxButtons.OK, MessageBoxIcon.Information);
            }
            else
            {
                MessageBox.Show("No se pudo conectar con el servidor.",
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
            }
        }

        // ============ CRUD Movimientos ============

        public bool AgregarMovimiento(int numeroCuenta, double monto, string tipo)
        {
            var request = new RestRequest($"{numeroCuenta}/movimientos", Method.Post);
            var datos = new { monto = monto, tipo = tipo.ToUpper() };
            request.AddJsonBody(datos);
            var response = client.Execute(request);

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
            var request = new RestRequest($"{numeroCuenta}/movimientos", Method.Get);
            var response = client.Get<List<MovimientoDto>>(request);
            return response ?? new List<MovimientoDto>();
        }

        public List<MovimientoDto> ListarTodosMovimientos()
        {
            var request = new RestRequest("movimientos", Method.Get);
            try
            {
                var response = client.Get<List<MovimientoDto>>(request);
                return response ?? new List<MovimientoDto>();
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
            var request = new RestRequest($"{numeroCuenta}/movimientos/{id}", Method.Get);
            var response = client.Execute<MovimientoDto>(request);
            if (!response.IsSuccessful || response.Data == null)
                return null;
            return response.Data;
        }

        public bool ActualizarMovimiento(int numeroCuenta, int id, double monto, string tipo)
        {
            var request = new RestRequest($"{numeroCuenta}/movimientos/{id}", Method.Put);
            var datos = new { monto = monto, tipo = tipo.ToUpper() };
            request.AddJsonBody(datos);
            var response = client.Execute(request);
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
            var request = new RestRequest($"{numeroCuenta}/movimientos/{id}", Method.Delete);
            var response = client.Execute(request);
            if (!response.IsSuccessful)
            {
                MessageBox.Show("Error al eliminar movimiento: " + response.Content,
                    "Error", MessageBoxButtons.OK, MessageBoxIcon.Error);
                return false;
            }
            return true;
        }
    }
}
