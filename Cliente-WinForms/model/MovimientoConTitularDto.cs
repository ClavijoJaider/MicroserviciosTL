using System;
using Newtonsoft.Json;

namespace WayBankClient.model
{
    /// <summary>
    /// DTO — Consulta personalizada #1 (Tercer Prototipo).
    /// Recibe los datos de GET /movimientos/filtrar en MS-Movimiento (8081).
    /// Incluye todos los atributos de MOVIMIENTO + titular de CUENTA_AHORROS.
    /// </summary>
    public class MovimientoConTitularDto
    {
        public int    Id           { get; set; }
        public double Monto        { get; set; }
        public string Tipo         { get; set; }
        public int    NumeroCuenta { get; set; }

        /// <summary>Titular de la cuenta — atributo de CUENTA_AHORROS (Tabla A).</summary>
        public string TitularCuenta { get; set; }

        [JsonProperty("fechaMovimiento")]
        public string FechaMovimientoStr { get; set; }

        public DateTime FechaMovimiento
        {
            get
            {
                if (string.IsNullOrEmpty(FechaMovimientoStr)) return DateTime.MinValue;
                return DateTime.Parse(FechaMovimientoStr);
            }
        }

        public override string ToString()
        {
            return $"[{Id}] {TitularCuenta} — {Tipo} ${Monto:N0} ({NumeroCuenta})";
        }
    }
}
