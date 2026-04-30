using System;
using System.Collections.Generic;
using System.Drawing;
using System.Windows.Forms;
using WayBankClient.model;
using WayBankClient.service;

namespace WayBankClient.Vista
{
    public class GuiListarMovimientos : Form
    {
        private ServicePeticiones servicio;
        private CuentaAhorrosDto cuentaActual;
        private TextBox txtNumero;
        private TextBox txtTitular;
        private TextBox txtSaldo;
        private DataGridView dgvMovimientos;
        private Label lblStatus;
        private Button btnBuscar;
        private Button btnCerrar;

        public GuiListarMovimientos()
        {
            InitializeComponent();
            servicio = ServicePeticiones.GetInstance();
        }

        private void InitializeComponent()
        {
            this.txtNumero = new TextBox();
            this.txtTitular = new TextBox();
            this.txtSaldo = new TextBox();
            this.dgvMovimientos = new DataGridView();
            this.lblStatus = new Label();
            this.btnBuscar = new Button();
            this.btnCerrar = new Button();
            ((System.ComponentModel.ISupportInitialize)(this.dgvMovimientos)).BeginInit();
            this.SuspendLayout();

            Label lblNumero = new Label();
            lblNumero.AutoSize = true;
            lblNumero.Location = new Point(12, 53);
            lblNumero.Text = "Número cuenta:";

            this.txtNumero.Location = new Point(120, 50);
            this.txtNumero.Size = new Size(120, 20);

            this.btnBuscar.Location = new Point(260, 48);
            this.btnBuscar.Size = new Size(90, 23);
            this.btnBuscar.Text = "Buscar";
            this.btnBuscar.BackColor = System.Drawing.Color.FromArgb(31, 78, 121);
            this.btnBuscar.ForeColor = System.Drawing.Color.White;
            this.btnBuscar.FlatStyle = FlatStyle.Flat;
            this.btnBuscar.Click += new System.EventHandler(this.btnBuscar_Click);

            Label lblTitular = new Label();
            lblTitular.AutoSize = true;
            lblTitular.Location = new Point(12, 90);
            lblTitular.Text = "Titular:";

            this.txtTitular.Location = new Point(120, 87);
            this.txtTitular.ReadOnly = true;
            this.txtTitular.Size = new Size(230, 20);

            Label lblSaldo = new Label();
            lblSaldo.AutoSize = true;
            lblSaldo.Location = new Point(12, 120);
            lblSaldo.Text = "Saldo actual:";

            this.txtSaldo.Location = new Point(120, 117);
            this.txtSaldo.ReadOnly = true;
            this.txtSaldo.Size = new Size(150, 20);

            Label lblHistorial = new Label();
            lblHistorial.AutoSize = true;
            lblHistorial.Font = new Font("Arial", 9F, FontStyle.Bold);
            lblHistorial.Location = new Point(12, 155);
            lblHistorial.Text = "Historial de movimientos:";

            this.dgvMovimientos.AllowUserToAddRows = false;
            this.dgvMovimientos.AllowUserToDeleteRows = false;
            this.dgvMovimientos.ColumnHeadersHeightSizeMode = DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            this.dgvMovimientos.Location = new Point(12, 175);
            this.dgvMovimientos.ReadOnly = true;
            this.dgvMovimientos.RowTemplate.Height = 21;
            this.dgvMovimientos.Size = new Size(340, 180);
            this.dgvMovimientos.Columns.Add("Id", "ID");
            this.dgvMovimientos.Columns.Add("Fecha", "Fecha");
            this.dgvMovimientos.Columns.Add("Monto", "Monto");
            this.dgvMovimientos.Columns.Add("Tipo", "Tipo");

            this.lblStatus.AutoSize = true;
            this.lblStatus.Location = new Point(12, 365);

            this.btnCerrar.Location = new Point(272, 390);
            this.btnCerrar.Size = new Size(80, 25);
            this.btnCerrar.Text = "Cerrar";
            this.btnCerrar.Click += new System.EventHandler(this.btnCerrar_Click);

            this.AutoScaleDimensions = new SizeF(6F, 13F);
            this.AutoScaleMode = AutoScaleMode.Font;
            this.ClientSize = new Size(364, 428);
            this.Text = "Ver Movimientos por Cuenta";
            this.StartPosition = FormStartPosition.CenterParent;
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;

            this.Controls.Add(this.btnCerrar);
            this.Controls.Add(this.lblStatus);
            this.Controls.Add(lblHistorial);
            this.Controls.Add(this.dgvMovimientos);
            this.Controls.Add(this.txtSaldo);
            this.Controls.Add(lblSaldo);
            this.Controls.Add(this.txtTitular);
            this.Controls.Add(lblTitular);
            this.Controls.Add(this.btnBuscar);
            this.Controls.Add(this.txtNumero);
            this.Controls.Add(lblNumero);

            ((System.ComponentModel.ISupportInitialize)(this.dgvMovimientos)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        private void btnBuscar_Click(object sender, EventArgs e)
        {
            if (string.IsNullOrWhiteSpace(txtNumero.Text))
            {
                MessageBox.Show("Ingrese el número de cuenta.", "Advertencia",
                    MessageBoxButtons.OK, MessageBoxIcon.Warning);
                return;
            }

            if (!int.TryParse(txtNumero.Text.Trim(), out int numero))
            {
                MessageBox.Show("Número inválido.", "Validación",
                    MessageBoxButtons.OK, MessageBoxIcon.Error);
                return;
            }

            cuentaActual = servicio.BuscarPorNumeroCuenta(numero);
            if (cuentaActual == null)
            {
                MessageBox.Show("Cuenta no encontrada.", "Sin resultados",
                    MessageBoxButtons.OK, MessageBoxIcon.Information);
                Limpiar();
                return;
            }

            txtTitular.Text = cuentaActual.Titular;
            txtSaldo.Text = cuentaActual.Saldo.ToString("N2");

            List<MovimientoDto> movimientos = servicio.ListarMovimientos(numero);
            dgvMovimientos.Rows.Clear();

            if (movimientos.Count == 0)
            {
                lblStatus.Text = "No hay movimientos registrados";
                return;
            }

            foreach (var m in movimientos)
            {
                dgvMovimientos.Rows.Add(
                    m.Id,
                    m.FechaMovimiento.ToString("yyyy-MM-dd HH:mm"),
                    m.Monto.ToString("N2"),
                    m.Tipo
                );
            }

            lblStatus.Text = $"{movimientos.Count} movimiento(s) encontrado(s)";
        }

        private void btnCerrar_Click(object sender, EventArgs e)
        {
            Close();
        }

        private void Limpiar()
        {
            txtTitular.Clear();
            txtSaldo.Clear();
            dgvMovimientos.Rows.Clear();
            lblStatus.Text = "";
            cuentaActual = null;
        }
    }
}
