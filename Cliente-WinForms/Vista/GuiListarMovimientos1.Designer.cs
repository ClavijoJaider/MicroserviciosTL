using System;
using System.Drawing;
using System.Windows.Forms;

namespace WayBankClient.Vista
{
    partial class GuiListarMovimientos1
    {
        private System.ComponentModel.IContainer components = null;

        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
                components.Dispose();
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        private void InitializeComponent()
        {
            this.lblNumero   = new System.Windows.Forms.Label();
            this.txtNumero   = new System.Windows.Forms.TextBox();
            this.lblTipo     = new System.Windows.Forms.Label();
            this.cboTipo     = new System.Windows.Forms.ComboBox();
            this.btnFiltrar  = new System.Windows.Forms.Button();
            this.btnTodos    = new System.Windows.Forms.Button();
            this.lblHistorial= new System.Windows.Forms.Label();
            this.dgvMovimientos = new System.Windows.Forms.DataGridView();
            this.colId       = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.colCuenta   = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.colTitular  = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.colFecha    = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.colTipo     = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.colMonto    = new System.Windows.Forms.DataGridViewTextBoxColumn();
            this.lblStatus   = new System.Windows.Forms.Label();
            this.btnCerrar   = new System.Windows.Forms.Button();
            ((System.ComponentModel.ISupportInitialize)(this.dgvMovimientos)).BeginInit();
            this.SuspendLayout();

            // lblNumero
            this.lblNumero.AutoSize = true;
            this.lblNumero.Location = new System.Drawing.Point(14, 20);
            this.lblNumero.Text = "N° cuenta (0 = todos):";

            // txtNumero
            this.txtNumero.Location = new System.Drawing.Point(180, 17);
            this.txtNumero.Size = new System.Drawing.Size(100, 26);

            // lblTipo
            this.lblTipo.AutoSize = true;
            this.lblTipo.Location = new System.Drawing.Point(14, 58);
            this.lblTipo.Text = "Tipo:";

            // cboTipo
            this.cboTipo.Location = new System.Drawing.Point(180, 55);
            this.cboTipo.Size = new System.Drawing.Size(130, 26);
            this.cboTipo.DropDownStyle = System.Windows.Forms.ComboBoxStyle.DropDownList;
            this.cboTipo.Items.AddRange(new object[] { "Todos", "CREDITO", "DEBITO" });
            this.cboTipo.SelectedIndex = 0;

            // btnFiltrar
            this.btnFiltrar.Location = new System.Drawing.Point(330, 15);
            this.btnFiltrar.Size = new System.Drawing.Size(100, 35);
            this.btnFiltrar.Text = "Filtrar";
            this.btnFiltrar.BackColor = System.Drawing.Color.FromArgb(31, 78, 121);
            this.btnFiltrar.ForeColor = System.Drawing.Color.White;
            this.btnFiltrar.FlatStyle = System.Windows.Forms.FlatStyle.Flat;
            this.btnFiltrar.Click += new System.EventHandler(this.btnFiltrar_Click);

            // btnTodos
            this.btnTodos.Location = new System.Drawing.Point(330, 55);
            this.btnTodos.Size = new System.Drawing.Size(100, 30);
            this.btnTodos.Text = "Todos";
            this.btnTodos.Click += new System.EventHandler(this.btnTodos_Click);

            // lblHistorial
            this.lblHistorial.AutoSize = true;
            this.lblHistorial.Font = new System.Drawing.Font("Arial", 9F, System.Drawing.FontStyle.Bold);
            this.lblHistorial.Location = new System.Drawing.Point(14, 100);
            this.lblHistorial.Text = "Movimientos — Consulta #1 (JOIN con titular de cuenta):";

            // dgvMovimientos
            this.dgvMovimientos.AllowUserToAddRows = false;
            this.dgvMovimientos.AllowUserToDeleteRows = false;
            this.dgvMovimientos.ReadOnly = true;
            this.dgvMovimientos.Location = new System.Drawing.Point(14, 125);
            this.dgvMovimientos.Size = new System.Drawing.Size(680, 310);
            this.dgvMovimientos.RowTemplate.Height = 21;
            this.dgvMovimientos.AutoSizeColumnsMode = System.Windows.Forms.DataGridViewAutoSizeColumnsMode.Fill;
            this.dgvMovimientos.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.AutoSize;
            this.dgvMovimientos.Columns.AddRange(new System.Windows.Forms.DataGridViewColumn[] {
                this.colId, this.colCuenta, this.colTitular,
                this.colFecha, this.colTipo, this.colMonto });

            // colId
            this.colId.HeaderText = "ID";
            this.colId.Name = "colId";
            this.colId.FillWeight = 40;

            // colCuenta
            this.colCuenta.HeaderText = "N° Cuenta (FK)";
            this.colCuenta.Name = "colCuenta";
            this.colCuenta.FillWeight = 80;

            // colTitular
            this.colTitular.HeaderText = "Titular (Tabla A)";
            this.colTitular.Name = "colTitular";
            this.colTitular.FillWeight = 130;

            // colFecha
            this.colFecha.HeaderText = "Fecha";
            this.colFecha.Name = "colFecha";
            this.colFecha.FillWeight = 110;

            // colTipo
            this.colTipo.HeaderText = "Tipo";
            this.colTipo.Name = "colTipo";
            this.colTipo.FillWeight = 60;

            // colMonto
            this.colMonto.HeaderText = "Monto";
            this.colMonto.Name = "colMonto";
            this.colMonto.FillWeight = 80;

            // lblStatus
            this.lblStatus.AutoSize = true;
            this.lblStatus.Location = new System.Drawing.Point(14, 447);

            // btnCerrar
            this.btnCerrar.Location = new System.Drawing.Point(594, 455);
            this.btnCerrar.Size = new System.Drawing.Size(100, 35);
            this.btnCerrar.Text = "Cerrar";
            this.btnCerrar.Click += new System.EventHandler(this.btnCerrar_Click);

            // GuiListarMovimientos1
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(710, 505);
            this.FormBorderStyle = System.Windows.Forms.FormBorderStyle.FixedDialog;
            this.MaximizeBox = false;
            this.MinimizeBox = false;
            this.Name = "GuiListarMovimientos1";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterParent;
            this.Text = "Listar Movimientos con Titular — Consulta Personalizada #1";
            this.Controls.AddRange(new System.Windows.Forms.Control[] {
                this.lblNumero, this.txtNumero,
                this.lblTipo, this.cboTipo,
                this.btnFiltrar, this.btnTodos,
                this.lblHistorial, this.dgvMovimientos,
                this.lblStatus, this.btnCerrar });
            ((System.ComponentModel.ISupportInitialize)(this.dgvMovimientos)).EndInit();
            this.ResumeLayout(false);
            this.PerformLayout();
        }

        #endregion

        private System.Windows.Forms.Label  lblNumero;
        private System.Windows.Forms.TextBox txtNumero;
        private System.Windows.Forms.Label  lblTipo;
        private System.Windows.Forms.ComboBox cboTipo;
        private System.Windows.Forms.Button btnFiltrar;
        private System.Windows.Forms.Button btnTodos;
        private System.Windows.Forms.Label  lblHistorial;
        private System.Windows.Forms.DataGridView dgvMovimientos;
        private System.Windows.Forms.DataGridViewTextBoxColumn colId;
        private System.Windows.Forms.DataGridViewTextBoxColumn colCuenta;
        private System.Windows.Forms.DataGridViewTextBoxColumn colTitular;
        private System.Windows.Forms.DataGridViewTextBoxColumn colFecha;
        private System.Windows.Forms.DataGridViewTextBoxColumn colTipo;
        private System.Windows.Forms.DataGridViewTextBoxColumn colMonto;
        private System.Windows.Forms.Label  lblStatus;
        private System.Windows.Forms.Button btnCerrar;
    }
}
