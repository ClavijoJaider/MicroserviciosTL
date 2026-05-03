using System;
using System.Collections.Generic;
using System.Windows.Forms;
using WayBankClient.model;
using WayBankClient.service;

namespace WayBankClient.Vista
{
    /// <summary>
    /// Listar Movimientos — Consulta personalizada #1 (TERCER PROTOTIPO).
    ///
    /// Muestra todos los atributos de MOVIMIENTO (Tabla B) más:
    ///   • numeroCuenta (llave foránea → Tabla A)
    ///   • titularCuenta (atributo de CUENTA_AHORROS / Tabla A)
    ///
    /// Usa GET /movimientos/filtrar?numeroCuenta=X&tipo=Y en MS-Movimiento (8081).
    /// JOIN @ManyToOne → JPQL: FROM Movimiento m JOIN m.cuenta c
    ///
    /// Filtros: número de cuenta (0 = todos) y tipo (CREDITO/DEBITO/Todos).
    /// Se actualiza automáticamente con el patrón Observer (SSE).
    /// </summary>
    public partial class GuiListarMovimientos1 : Form
    {
        private readonly ServicePeticiones servicio;

        public GuiListarMovimientos1()
        {
            InitializeComponent();
            servicio = ServicePeticiones.GetInstance();
            // Observer: refresca la grilla al recibir evento SSE de movimientos
            servicio.OnMovimientosActualizados += RecargarMovimientos;
            CargarTodos();
        }

        // ---------------------------------------------------------------
        // Carga inicial — todos los movimientos con titular
        // ---------------------------------------------------------------

        private void CargarTodos()
        {
            txtNumero.Text = "0";
            cboTipo.SelectedIndex = 0; // Todos
            EjecutarFiltro(0, null);
        }

        // ---------------------------------------------------------------
        // Filtrar
        // ---------------------------------------------------------------

        private void btnFiltrar_Click(object sender, EventArgs e)
        {
            if (!int.TryParse(txtNumero.Text.Trim(), out int numero))
                numero = 0;
            string tipo = cboTipo.SelectedItem?.ToString();
            if (tipo == "Todos") tipo = null;
            EjecutarFiltro(numero, tipo);
        }

        private void btnTodos_Click(object sender, EventArgs e)
        {
            CargarTodos();
        }

        private void EjecutarFiltro(int numeroCuenta, string tipo)
        {
            dgvMovimientos.Rows.Clear();

            List<MovimientoConTitularDto> lista = servicio.FiltrarMovimientosConTitular(numeroCuenta, tipo);

            foreach (var m in lista)
            {
                string fecha = m.FechaMovimiento.ToString("yyyy-MM-dd HH:mm");
                dgvMovimientos.Rows.Add(
                    m.Id,
                    m.NumeroCuenta,          // FK hacia CUENTA_AHORROS
                    m.TitularCuenta,         // atributo de Tabla A
                    fecha,
                    m.Tipo,
                    m.Monto.ToString("N2")
                );
            }

            lblStatus.Text = $"{lista.Count} resultado(s) — Cuenta: {(numeroCuenta > 0 ? numeroCuenta.ToString() : "todas")} | Tipo: {tipo ?? "todos"}";
        }

        // ---------------------------------------------------------------
        // Observer SSE
        // ---------------------------------------------------------------

        private void RecargarMovimientos()
        {
            if (InvokeRequired) { Invoke(new Action(RecargarMovimientos)); return; }
            if (!int.TryParse(txtNumero.Text.Trim(), out int numero)) numero = 0;
            string tipo = cboTipo.SelectedItem?.ToString();
            if (tipo == "Todos") tipo = null;
            EjecutarFiltro(numero, tipo);
        }

        protected override void OnFormClosed(FormClosedEventArgs e)
        {
            servicio.OnMovimientosActualizados -= RecargarMovimientos;
            base.OnFormClosed(e);
        }

        // ---------------------------------------------------------------
        // Cerrar
        // ---------------------------------------------------------------

        private void btnCerrar_Click(object sender, EventArgs e) => Close();
    }
}
