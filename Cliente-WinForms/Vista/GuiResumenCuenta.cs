using System;
using System.Drawing;
using System.Windows.Forms;
using WayBankClient.model;
using WayBankClient.service;

namespace WayBankClient.Vista
{
    /// <summary>
    /// Resumen de Cuenta — Consulta personalizada #2 (TERCER PROTOTIPO).
    ///
    /// "Una consulta debe mostrar datos de la tabla maestro y dos de detalle."
    ///
    /// Muestra:
    ///   • Datos del MAESTRO (CUENTA_AHORROS): numeroCuenta, titular, saldo, tasaInteres, estado
    ///   • Dato detalle 1: totalMovimientos — COUNT de movimientos asociados
    ///   • Dato detalle 2: totalCreditos    — SUM de montos CREDITO
    ///
    /// Endpoint: GET /cuentas/{numero}/resumen en MS-CuentaAhorros (8080).
    /// </summary>
    public class GuiResumenCuenta : Form
    {
        private readonly ServicePeticiones servicio;

        // ---- Controles ----
        private TextBox  txtNumero;
        private Button   btnConsultar;
        private Button   btnCerrar;
        private Label    lblError;

        // Panel maestro
        private Label    lblSecMaestro;
        private Label    lblNumeroCuenta, lblTitular, lblSaldo, lblTasa, lblEstado;
        private TextBox  txtNumeroCuenta, txtTitular, txtSaldo, txtTasa, txtEstado;

        // Panel detalle
        private Label    lblSecDetalle;
        private Label    lblTotalMov, lblTotalCred;
        private TextBox  txtTotalMov, txtTotalCred;

        public GuiResumenCuenta()
        {
            servicio = ServicePeticiones.GetInstance();
            InicializarComponentes();
            // Observer: refresca si cambia la cuenta o sus movimientos
            servicio.OnCuentasActualizadas    += RefrescarSiAbierto;
            servicio.OnMovimientosActualizados += RefrescarSiAbierto;
        }

        private void InicializarComponentes()
        {
            this.Text = "Resumen de Cuenta — Consulta Personalizada #2";
            this.ClientSize = new Size(430, 460);
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.StartPosition = FormStartPosition.CenterParent;

            // ---- Búsqueda ----
            Label lblNum = new Label { Text = "N° de cuenta:", Location = new Point(14, 16), AutoSize = true };
            txtNumero = new TextBox { Location = new Point(130, 13), Size = new Size(120, 26) };
            btnConsultar = new Button
            {
                Text = "Consultar",
                Location = new Point(268, 12),
                Size = new Size(100, 30),
                BackColor = Color.FromArgb(31, 78, 121),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnConsultar.Click += BtnConsultar_Click;

            lblError = new Label
            {
                Location = new Point(14, 50),
                Size = new Size(400, 18),
                ForeColor = Color.FromArgb(201, 16, 47),
                Font = new Font("Arial", 8F)
            };

            // ---- Separador + Título Maestro ----
            lblSecMaestro = new Label
            {
                Text = "▌ Datos del Maestro — CUENTA_AHORROS",
                Location = new Point(14, 78),
                Size = new Size(400, 20),
                Font = new Font("Arial", 9F, FontStyle.Bold),
                ForeColor = Color.FromArgb(31, 78, 121)
            };

            int y = 104;
            (lblNumeroCuenta, txtNumeroCuenta) = CrearFila("N° de cuenta:",  y); y += 36;
            (lblTitular,      txtTitular)      = CrearFila("Titular:",        y); y += 36;
            (lblSaldo,        txtSaldo)        = CrearFila("Saldo:",          y); y += 36;
            (lblTasa,         txtTasa)         = CrearFila("Tasa de interés:", y); y += 36;
            (lblEstado,       txtEstado)       = CrearFila("Estado:",         y); y += 50;

            // ---- Separador + Título Detalle ----
            lblSecDetalle = new Label
            {
                Text = "▌ Datos del Detalle — MOVIMIENTO (agregados)",
                Location = new Point(14, y),
                Size = new Size(400, 20),
                Font = new Font("Arial", 9F, FontStyle.Bold),
                ForeColor = Color.FromArgb(27, 138, 90)
            };
            y += 30;

            (lblTotalMov,  txtTotalMov)  = CrearFila("Total movimientos (COUNT):", y); y += 36;
            (lblTotalCred, txtTotalCred) = CrearFila("Total créditos (SUM):",      y); y += 50;

            // ---- Botón Cerrar ----
            btnCerrar = new Button
            {
                Text = "Cerrar",
                Location = new Point(320, y),
                Size = new Size(90, 30)
            };
            btnCerrar.Click += (s, e) => Close();

            // ---- Agregar controles ----
            this.Controls.AddRange(new Control[]
            {
                lblNum, txtNumero, btnConsultar, lblError,
                lblSecMaestro,
                lblNumeroCuenta, txtNumeroCuenta,
                lblTitular,      txtTitular,
                lblSaldo,        txtSaldo,
                lblTasa,         txtTasa,
                lblEstado,       txtEstado,
                lblSecDetalle,
                lblTotalMov,  txtTotalMov,
                lblTotalCred, txtTotalCred,
                btnCerrar
            });

            LimpiarCampos();
        }

        // ---------------------------------------------------------------
        // Lógica
        // ---------------------------------------------------------------

        private void BtnConsultar_Click(object sender, EventArgs e)
        {
            lblError.Text = "";
            if (!int.TryParse(txtNumero.Text.Trim(), out int numero) || numero <= 0)
            {
                lblError.Text = "✗ Ingrese un número de cuenta válido";
                return;
            }

            ResumenCuentaDto r = servicio.ObtenerResumen(numero);
            if (r == null)
            {
                lblError.Text = "✗ Cuenta no encontrada: " + numero;
                LimpiarCampos();
                return;
            }

            txtNumeroCuenta.Text = r.NumeroCuenta.ToString();
            txtTitular.Text      = r.Titular;
            txtSaldo.Text        = r.Saldo.ToString("C0", new System.Globalization.CultureInfo("es-CO"));
            txtTasa.Text         = (r.TasaInteres * 100).ToString("F2") + " %";
            txtEstado.Text       = r.Estado;
            txtTotalMov.Text     = r.TotalMovimientos + " movimiento(s)";
            txtTotalCred.Text    = r.TotalCreditos.ToString("C0", new System.Globalization.CultureInfo("es-CO"));
        }

        private void LimpiarCampos()
        {
            txtNumeroCuenta.Text = txtTitular.Text = txtSaldo.Text =
            txtTasa.Text = txtEstado.Text = txtTotalMov.Text = txtTotalCred.Text = "";
        }

        private void RefrescarSiAbierto()
        {
            if (InvokeRequired) { Invoke(new Action(RefrescarSiAbierto)); return; }
            if (int.TryParse(txtNumeroCuenta.Text.Trim(), out int n) && n > 0)
                BtnConsultar_Click(null, null); // refresca si ya hay datos visibles
        }

        protected override void OnFormClosed(FormClosedEventArgs e)
        {
            servicio.OnCuentasActualizadas    -= RefrescarSiAbierto;
            servicio.OnMovimientosActualizados -= RefrescarSiAbierto;
            base.OnFormClosed(e);
        }

        // ---------------------------------------------------------------
        // Helper
        // ---------------------------------------------------------------

        private static (Label lbl, TextBox txt) CrearFila(string etiqueta, int y)
        {
            var lbl = new Label
            {
                Text = etiqueta,
                Location = new Point(14, y + 3),
                Size = new Size(180, 20),
                TextAlign = ContentAlignment.MiddleRight,
                Font = new Font("Arial", 8.5F, FontStyle.Bold)
            };
            var txt = new TextBox
            {
                Location = new Point(202, y),
                Size = new Size(210, 26),
                ReadOnly = true,
                BackColor = Color.FromArgb(235, 242, 255)
            };
            return (lbl, txt);
        }
    }
}
