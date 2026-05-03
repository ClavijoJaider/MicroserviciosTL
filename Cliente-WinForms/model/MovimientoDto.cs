using Newtonsoft.Json;
using System;

namespace WayBankClient.model
{
    /// <summary>
    /// DTO para los endpoints CRUD básicos de movimiento:
    ///   GET /movimientos/{numeroCuenta}
    ///   GET /movimientos/{numeroCuenta}/{id}
    ///   POST /movimientos
    ///   PUT /movimientos/{numeroCuenta}/{id}
    ///   DELETE /movimientos/{numeroCuenta}/{id}
    /// (MS-Movimiento — puerto 8081)
    /// </summary>
    public class MovimientoDto
    {
        [JsonProperty("id")]
        public int Id { get; set; }

        [JsonProperty("numeroCuenta")]
        public int NumeroCuenta { get; set; }

        [JsonProperty("monto")]
        public double Monto { get; set; }

        [JsonProperty("tipo")]
        public string Tipo { get; set; }

        [JsonProperty("fechaMovimiento")]
        public DateTime? FechaMovimiento { get; set; }
    }
}
