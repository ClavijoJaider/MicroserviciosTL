using System;
using System.Drawing;
using System.Windows.Forms;
using WayBankClient.model;
using WayBankClient.service;

namespace WayBankClient.Vista
{
    /// <summary>
    /// Formulario para eliminar un movimiento.
    /// Flujo: buscar por cuenta + ID → mostrar datos → confirmar eliminación.
    /// NOTA: La eliminación revierte el efecto del movimiento en el saldo de la cuenta.
    /// </summary>
    public class GuiEliminarMovimiento : Form
    {
        private ServicePeticiones servicio;
        private MovimientoDto movimientoActual;
        private int numeroCuentaActual;

        private Label lblTitulo;
        private Label lblNumero;
        private Label lblId;
        private TextBox txtNumero;
        private TextBox txtId;
        private Button btnBuscar;
        private Panel panelInfo;
        private Label lblInfoId;
        private Label lblInfoFecha;
        private Label lblInfoMonto;
        private Label lblInfoTipo;
        private Label lblInfoCuenta;
        private Label lblAdvertencia;
        private Button btnEliminar;
        private Button btnCancelar;
        private Button btnCerrar;
        private Label lblStatus;

        public GuiEliminarMovimiento()
        {
            InitializeComponent();
            servicio = ServicePeticiones.GetInstance();
        }

        private void InitializeComponent()
        {
            this.Text = "Eliminar Movimiento";
            this.ClientSize = new Size(390, 400);
            this.StartPosition = FormStartPosition.CenterScreen;
            this.FormBorderStyle = FormBorderStyle.FixedSingle;
            this.MaximizeBox = false;
            this.Font = new Font("Segoe UI", 9f);
            this.BackColor = Color.White;

            lblTitulo = new Label
            {
                Text = "Eliminar Movimiento",
                Font = new Font("Segoe UI", 11f, FontStyle.Bold),
                ForeColor = Color.FromArgb(0, 0, 0),
                Location = new Point(16, 14),
                AutoSize = true
            };

            lblNumero = new Label { Text = "Número de cuenta:", Location = new Point(16, 50), AutoSize = true };
            txtNumero = new TextBox { Location = new Point(160, 47), Width = 180 };

            lblId = new Label { Text = "ID del movimiento:", Location = new Point(16, 80), AutoSize = true };
            txtId = new TextBox { Location = new Point(160, 77), Width = 180 };

            btnBuscar = new Button
            {
                Text = "Buscar",
                Location = new Point(160, 110),
                Width = 180,
                Height = 28,
                
            };
            btnBuscar.FlatAppearance.BorderSize = 0;
            btnBuscar.Click += BtnBuscar_Click;

            // Panel de información del movimiento encontrado
            panelInfo = new Panel
            {
                Location = new Point(16, 152),
                Size = new Size(358, 190),
                BorderStyle = BorderStyle.FixedSingle,
                BackColor = Color.FromArgb(255, 243, 205),
                Visible = false
            };

            var lblInfoTitulo = new Label
            {
                Text = "Información del movimiento a eliminar:",
                Font = new Font("Segoe UI", 9f, FontStyle.Bold),
                Location = new Point(8, 6),
                AutoSize = true
            };

            lblInfoId     = new Label { Location = new Point(8, 28),  AutoSize = true };
            lblInfoFecha  = new Label { Location = new Point(8, 48),  AutoSize = true };
            lblInfoMonto  = new Label { Location = new Point(8, 68),  AutoSize = true };
            lblInfoTipo   = new Label { Location = new Point(8, 88),  AutoSize = true };
            lblInfoCuenta = new Label { Location = new Point(8, 108), AutoSize = true };

            lblAdvertencia = new Label
            {
                Text = "Esta acción revertirá el efecto del movimiento\n   en el saldo de la cuenta y no se puede deshacer.",
               
                Location = new Point(8, 130),
                AutoSize = true,
                Font = new Font("Segoe UI", 8.5f)
            };

            btnEliminar = new Button
            {
                Text = "Confirmar eliminación",
                Location = new Point(8, 162),
                Width = 170,
                Height = 24,
                
            };
            btnEliminar.FlatAppearance.BorderSize = 0;
            btnEliminar.Click += BtnEliminar_Click;

            btnCancelar = new Button
            {
                Text = "Cancelar",
                Location = new Point(184, 162),
                Width = 100,
                Height = 24,
                
            };
            btnCancelar.FlatAppearance.BorderSize = 0;
            btnCancelar.Click += (s, e) =>
            {
                panelInfo.Visible = false;
                movimientoActual = null;
                txtNumero.Clear();
                txtId.Clear();
                lblStatus.Text = "";
            };

            panelInfo.Controls.AddRange(new Control[]
            {
                lblInfoTitulo, lblInfoId, lblInfoFecha, lblInfoMonto,
                lblInfoTipo, lblInfoCuenta, lblAdvertencia, btnEliminar, btnCancelar
            });

            lblStatus = new Label
            {
                Location = new Point(16, 356),
                AutoSize = true,
                Font = new Font("Segoe UI", 8.5f)
            };

            btnCerrar = new Button
            {
                Text = "Cerrar",
                Location = new Point(294, 368),
                Width = 80,
                Height = 26,
                
            };
            btnCerrar.FlatAppearance.BorderSize = 0;
            btnCerrar.Click += (s, e) => Close();

            this.Controls.AddRange(new Control[]
            {
                lblTitulo, lblNumero, txtNumero, lblId, txtId,
                btnBuscar, panelInfo, lblStatus, btnCerrar
            });
        }

        private void BtnBuscar_Click(object sender, EventArgs e)
        {
            lblStatus.Text = "";
            panelInfo.Visible = false;
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

            numeroCuentaActual = numero;
            lblInfoId.Text     = $"ID:      {movimientoActual.Id}";
            lblInfoFecha.Text  = $"Fecha:   {movimientoActual.FechaMovimiento:yyyy-MM-dd HH:mm:ss}";
            lblInfoMonto.Text  = $"Monto:   ${movimientoActual.Monto:N2}";
            lblInfoTipo.Text   = $"Tipo:    {movimientoActual.Tipo}";
            lblInfoCuenta.Text = $"Cuenta:  {movimientoActual.NumeroCuenta}";
            lblInfoTipo.ForeColor = movimientoActual.Tipo == "CREDITO"
                ? Color.FromArgb(27, 138, 90)
                : Color.FromArgb(201, 16, 47);

            panelInfo.Visible = true;
        }

        private void BtnEliminar_Click(object sender, EventArgs e)
        {
            if (movimientoActual == null) return;

            var confirm = MessageBox.Show(
                $"¿Confirma que desea eliminar el movimiento {movimientoActual.Id}?\n" +
                $"Monto: ${movimientoActual.Monto:N2} | Tipo: {movimientoActual.Tipo}\n\n" +
                "Esta acción revertirá el efecto en el saldo de la cuenta.",
                "Confirmar eliminación",
                MessageBoxButtons.YesNo,
                MessageBoxIcon.Warning);

            if (confirm != DialogResult.Yes) return;

            bool ok = servicio.EliminarMovimiento(numeroCuentaActual, movimientoActual.Id);
            if (ok)
            {
                MostrarExito($"✓ Movimiento {movimientoActual.Id} eliminado correctamente");
                panelInfo.Visible = false;
                movimientoActual = null;
                txtNumero.Clear();
                txtId.Clear();
            }
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
