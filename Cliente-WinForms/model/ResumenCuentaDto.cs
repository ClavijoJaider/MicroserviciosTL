using Newtonsoft.Json;

namespace WayBankClient.model
{
    /// <summary>
    /// DTO — Consulta personalizada #2 (TERCER PROTOTIPO).
    ///
    /// "Una consulta debe mostrar datos de la tabla maestro y dos de detalle."
    ///
    /// Devuelve datos de CUENTA_AHORROS (maestro) más dos campos agregados
    /// calculados sobre MOVIMIENTO (detalle):
    ///   • totalMovimientos — COUNT de movimientos asociados
    ///   • totalCreditos    — SUM de montos CREDITO
    ///
    /// Endpoint: GET /cuentas/{numero}/resumen en MS-CuentaAhorros (8080).
    /// </summary>
    public class ResumenCuentaDto
    {
        // ---- Datos del MAESTRO (CUENTA_AHORROS) ----
        [JsonProperty("numeroCuenta")]
        public int NumeroCuenta { get; set; }

        [JsonProperty("titular")]
        public string Titular { get; set; }

        [JsonProperty("saldo")]
        public double Saldo { get; set; }

        [JsonProperty("estado")]
        public string Estado { get; set; }

        [JsonProperty("tasaInteres")]
        public double TasaInteres { get; set; }

        // ---- Datos del DETALLE (MOVIMIENTO) — agregados ----

        /// <summary>COUNT de movimientos asociados a esta cuenta.</summary>
        [JsonProperty("totalMovimientos")]
        public long TotalMovimientos { get; set; }

        /// <summary>SUM de montos con tipo CREDITO.</summary>
        [JsonProperty("totalCreditos")]
        public double TotalCreditos { get; set; }
    }
}
