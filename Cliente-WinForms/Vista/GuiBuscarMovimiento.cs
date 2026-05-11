using System;
using System.Drawing;
using System.Windows.Forms;
using WayBankClient.model;
using WayBankClient.service;

namespace WayBankClient.Vista
{
    /// <summary>
    /// Formulario para buscar un movimiento específico por número de cuenta e ID.
    /// </summary>
    public class GuiBuscarMovimiento : Form
    {
        private ServicePeticiones servicio;

        private Label lblTitulo;
        private Label lblNumero;
        private Label lblId;
        private TextBox txtNumero;
        private TextBox txtId;
        private Button btnBuscar;
        private Button btnLimpiar;
        private Button btnCerrar;
        private Panel panelResultado;
        private Label lblResId;
        private Label lblResFecha;
        private Label lblResMonto;
        private Label lblResTipo;
        private Label lblResCuenta;
        private Label lblStatus;

        public GuiBuscarMovimiento()
        {
            InitializeComponent();
            servicio = ServicePeticiones.GetInstance();
        }

        private void InitializeComponent()
        {
            this.Text = "Buscar Movimiento";
            this.ClientSize = new Size(380, 340);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedSingle;
            this.MaximizeBox = false;
            this.Font = new Font("Segoe UI", 9f);
            this.BackColor = Color.White;

            // Título
            lblTitulo = new Label
            {
                Text = "Buscar Movimiento por ID",
                Font = new Font("Segoe UI", 11f, FontStyle.Bold),
                ForeColor = Color.FromArgb(0, 0, 0),
                Location = new Point(16, 14),
                AutoSize = true
            };

            // Número de cuenta
            lblNumero = new Label { Text = "Número de cuenta:", Location = new Point(16, 50), AutoSize = true };
            txtNumero = new TextBox { Location = new Point(160, 47), Width = 180 };

            // ID del movimiento
            lblId = new Label { Text = "ID del movimiento:", Location = new Point(16, 80), AutoSize = true };
            txtId = new TextBox { Location = new Point(160, 77), Width = 180 };

            // Botones
            btnBuscar = new Button
            {
                Text = "Buscar",
                Location = new Point(160, 110),
                Width = 88,
                Height = 28,
                
            };
            btnBuscar.FlatAppearance.BorderSize = 0;
            btnBuscar.Click += BtnBuscar_Click;

            btnLimpiar = new Button
            {
                Text = "Limpiar",
                Location = new Point(254, 110),
                Width = 86,
                Height = 28,
                
            };
            btnLimpiar.FlatAppearance.BorderSize = 0;
            btnLimpiar.Click += (s, e) => Limpiar();

            // Panel resultado
            panelResultado = new Panel
            {
                Location = new Point(16, 150),
                Size = new Size(348, 140),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.FromArgb(232, 244, 255),
                Visible = false
            };

            lblResId     = new Label { Location = new Point(8, 8),   AutoSize = true, Font = new Font("Segoe UI", 9f) };
            lblResFecha  = new Label { Location = new Point(8, 30),  AutoSize = true, Font = new Font("Segoe UI", 9f) };
            lblResMonto  = new Label { Location = new Point(8, 52),  AutoSize = true, Font = new Font("Segoe UI", 9f) };
            lblResTipo   = new Label { Location = new Point(8, 74),  AutoSize = true, Font = new Font("Segoe UI", 9f) };
            lblResCuenta = new Label { Location = new Point(8, 96),  AutoSize = true, Font = new Font("Segoe UI", 9f) };

            panelResultado.Controls.AddRange(new Control[]
                { lblResId, lblResFecha, lblResMonto, lblResTipo, lblResCuenta });

            // Status
            lblStatus = new Label
            {
                Location = new Point(16, 300),
                AutoSize = true,
                ForeColor = Color.Red,
                Font = new Font("Segoe UI", 8.5f)
            };

            // Botón cerrar
            btnCerrar = new Button
            {
                Text = "Cerrar",
                Location = new Point(280, 300),
                Width = 80,
                Height = 26,
                
            };
            btnCerrar.FlatAppearance.BorderSize = 0;
            btnCerrar.Click += (s, e) => Close();

            this.Controls.AddRange(new Control[]
            {
                lblTitulo, lblNumero, txtNumero, lblId, txtId,
                btnBuscar, btnLimpiar, panelResultado, lblStatus, btnCerrar
            });
        }

        private void BtnBuscar_Click(object sender, EventArgs e)
        {
            lblStatus.Text = "";
            panelResultado.Visible = false;

            if (!int.TryParse(txtNumero.Text.Trim(), out int numero) || numero <= 0)
            {
                lblStatus.Text = "✗ Ingrese un número de cuenta válido";
                return;
            }
            if (!int.TryParse(txtId.Text.Trim(), out int id) || id <= 0)
            {
                lblStatus.Text = "✗ Ingrese un ID de movimiento válido";
                return;
            }

            MovimientoDto m = servicio.BuscarMovimientoPorId(numero, id);
            if (m == null)
            {
                lblStatus.Text = "✗ Movimiento no encontrado";
                return;
            }

            lblResId.Text     = $"ID:     {m.Id}";
            lblResFecha.Text  = $"Fecha:  {m.FechaMovimiento:yyyy-MM-dd HH:mm:ss}";
            lblResMonto.Text  = $"Monto:  ${m.Monto:N2}";
            lblResTipo.Text   = $"Tipo:   {m.Tipo}";
            lblResCuenta.Text = $"Cuenta: {m.NumeroCuenta}";

            lblResTipo.ForeColor = m.Tipo == "CREDITO"
                ? Color.FromArgb(27, 138, 90)
                : Color.FromArgb(201, 16, 47);

            panelResultado.Visible = true;
        }

        private void Limpiar()
        {
            txtNumero.Clear();
            txtId.Clear();
            panelResultado.Visible = false;
            lblStatus.Text = "";
        }
    }
}
