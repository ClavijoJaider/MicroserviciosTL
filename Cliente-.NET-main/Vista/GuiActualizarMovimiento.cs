using System;
using System.Drawing;
using System.Windows.Forms;
using WayBankClient.model;
using WayBankClient.service;

namespace WayBankClient.Vista
{
    /// <summary>
    /// Formulario para actualizar el monto y/o tipo de un movimiento existente.
    /// Flujo: buscar movimiento → mostrar datos actuales → editar → confirmar.
    /// </summary>
    public class GuiActualizarMovimiento : Form
    {
        private ServicePeticiones servicio;
        private MovimientoDto movimientoActual;

        private Label lblTitulo;
        private Label lblNumero;
        private Label lblId;
        private TextBox txtNumero;
        private TextBox txtId;
        private Button btnBuscar;
        private Panel panelForm;
        private Label lblInfoActual;
        private Label lblNuevoMonto;
        private Label lblNuevoTipo;
        private TextBox txtNuevoMonto;
        private ComboBox cmbNuevoTipo;
        private Button btnActualizar;
        private Button btnLimpiar;
        private Button btnCerrar;
        private Label lblStatus;

        public GuiActualizarMovimiento()
        {
            InitializeComponent();
            servicio = ServicePeticiones.GetInstance();
        }

        private void InitializeComponent()
        {
            this.Text = "Actualizar Movimiento";
            this.ClientSize = new Size(390, 390);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedSingle;
            this.MaximizeBox = false;
            this.Font = new Font("Segoe UI", 9f);
            this.BackColor = Color.White;

            lblTitulo = new Label
            {
                Text = "✏️  Actualizar Movimiento",
                Font = new Font("Segoe UI", 11f, FontStyle.Bold),
                ForeColor = Color.FromArgb(31, 78, 121),
                Location = new Point(16, 14),
                AutoSize = true
            };

            lblNumero = new Label { Text = "Número de cuenta:", Location = new Point(16, 50), AutoSize = true };
            txtNumero = new TextBox { Location = new Point(160, 47), Width = 180, PlaceholderText = "Ej: 1001" };

            lblId = new Label { Text = "ID del movimiento:", Location = new Point(16, 80), AutoSize = true };
            txtId = new TextBox { Location = new Point(160, 77), Width = 180, PlaceholderText = "Ej: 1" };

            btnBuscar = new Button
            {
                Text = "Buscar",
                Location = new Point(160, 110),
                Width = 180,
                Height = 28,
                BackColor = Color.FromArgb(31, 78, 121),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnBuscar.FlatAppearance.BorderSize = 0;
            btnBuscar.Click += BtnBuscar_Click;

            // Panel con el formulario de edición (visible solo tras buscar)
            panelForm = new Panel
            {
                Location = new Point(16, 150),
                Size = new Size(358, 200),
                Visible = false
            };

            lblInfoActual = new Label
            {
                Location = new Point(0, 0),
                AutoSize = false,
                Size = new Size(358, 40),
                ForeColor = Color.FromArgb(80, 80, 80),
                Font = new Font("Segoe UI", 8.5f)
            };

            lblNuevoMonto = new Label { Text = "Nuevo monto:", Location = new Point(0, 52), AutoSize = true };
            txtNuevoMonto = new TextBox { Location = new Point(140, 49), Width = 160, PlaceholderText = "Ej: 150000" };

            lblNuevoTipo = new Label { Text = "Nuevo tipo:", Location = new Point(0, 85), AutoSize = true };
            cmbNuevoTipo = new ComboBox
            {
                Location = new Point(140, 82),
                Width = 160,
                DropDownStyle = ComboBoxStyle.DropDownList
            };
            cmbNuevoTipo.Items.AddRange(new object[] { "CREDITO", "DEBITO" });
            cmbNuevoTipo.SelectedIndex = 0;

            btnActualizar = new Button
            {
                Text = "Guardar cambios",
                Location = new Point(0, 120),
                Width = 155,
                Height = 30,
                BackColor = Color.FromArgb(27, 138, 90),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnActualizar.FlatAppearance.BorderSize = 0;
            btnActualizar.Click += BtnActualizar_Click;

            btnLimpiar = new Button
            {
                Text = "Limpiar",
                Location = new Point(162, 120),
                Width = 100,
                Height = 30,
                BackColor = Color.Gray,
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnLimpiar.FlatAppearance.BorderSize = 0;
            btnLimpiar.Click += (s, e) => Limpiar();

            panelForm.Controls.AddRange(new Control[]
                { lblInfoActual, lblNuevoMonto, txtNuevoMonto, lblNuevoTipo, cmbNuevoTipo, btnActualizar, btnLimpiar });

            lblStatus = new Label
            {
                Location = new Point(16, 360),
                AutoSize = true,
                Font = new Font("Segoe UI", 8.5f)
            };

            btnCerrar = new Button
            {
                Text = "Cerrar",
                Location = new Point(294, 357),
                Width = 80,
                Height = 26,
                BackColor = Color.FromArgb(102, 102, 102),
                ForeColor = Color.White,
                FlatStyle = FlatStyle.Flat
            };
            btnCerrar.FlatAppearance.BorderSize = 0;
            btnCerrar.Click += (s, e) => Close();

            this.Controls.AddRange(new Control[]
            {
                lblTitulo, lblNumero, txtNumero, lblId, txtId,
                btnBuscar, panelForm, lblStatus, btnCerrar
            });
        }

        private void BtnBuscar_Click(object sender, EventArgs e)
        {
            lblStatus.Text = "";
            panelForm.Visible = false;
            movimientoActual = null;

            if (!int.TryParse(txtNumero.Text.Trim(), out int numero) || numero <= 0)
            {
                MostrarError("✗ Ingrese un número de cuenta válido");
                return;
            }
            if (!int.TryParse(txtId.Text.Trim(), out int id) || id <= 0)
            {
                MostrarError("✗ Ingrese un ID de movimiento válido");
                return;
            }

            movimientoActual = servicio.BuscarMovimientoPorId(numero, id);
            if (movimientoActual == null)
            {
                MostrarError("✗ Movimiento no encontrado");
                return;
            }

            lblInfoActual.Text =
                $"Movimiento actual → ID: {movimientoActual.Id} | " +
                $"Monto: ${movimientoActual.Monto:N2} | " +
                $"Tipo: {movimientoActual.Tipo}";

            txtNuevoMonto.Text = movimientoActual.Monto.ToString("0.##");
            cmbNuevoTipo.SelectedItem = movimientoActual.Tipo;

            panelForm.Visible = true;
        }

        private void BtnActualizar_Click(object sender, EventArgs e)
        {
            if (movimientoActual == null) return;

            if (!double.TryParse(txtNuevoMonto.Text.Trim(), out double monto) || monto <= 0)
            {
                MostrarError("✗ El monto debe ser mayor a cero");
                return;
            }

            string tipo = cmbNuevoTipo.SelectedItem?.ToString() ?? "CREDITO";

            if (!int.TryParse(txtNumero.Text.Trim(), out int numero))
                return;

            bool ok = servicio.ActualizarMovimiento(numero, movimientoActual.Id, monto, tipo);
            if (ok)
            {
                MostrarExito("✓ Movimiento actualizado correctamente");
                panelForm.Visible = false;
                movimientoActual = null;
                txtNumero.Clear();
                txtId.Clear();
            }
        }

        private void Limpiar()
        {
            txtNumero.Clear();
            txtId.Clear();
            txtNuevoMonto.Clear();
            panelForm.Visible = false;
            movimientoActual = null;
            lblStatus.Text = "";
        }

        private void MostrarError(string msg)
        {
            lblStatus.ForeColor = Color.FromArgb(201, 16, 47);
            lblStatus.Text = msg;
        }

        private void MostrarExito(string msg)
        {
            lblStatus.ForeColor = Color.FromArgb(27, 138, 90);
            lblStatus.Text = msg;
        }
    }
}
