using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace WayBankClient.Vista
{
    public partial class GuiInicio : Form
    {
        public GuiInicio()
        {
            InitializeComponent();
        }

        private void crearToolStripMenuItem_Click(object sender, EventArgs e)
        {
            GuiCrearAhorros crearAhorros = new GuiCrearAhorros();
            crearAhorros.Show();
        }

        private void buscarToolStripMenuItem_Click(object sender, EventArgs e)
        {
            GuiBuscarAhorros buscarAhorros = new GuiBuscarAhorros();
            buscarAhorros.Show();
        }

        private void actualizarToolStripMenuItem_Click(object sender, EventArgs e)
        {
            GuiActualizarAhorros actualizarAhorros = new GuiActualizarAhorros(); 
            actualizarAhorros.Show();
        }

        private void eliminarToolStripMenuItem_Click(object sender, EventArgs e)
        {
            GuiEliminarAhorros eliminarAhorros = new GuiEliminarAhorros();
            eliminarAhorros.Show();
        }

        private void listarToolStripMenuItem_Click(object sender, EventArgs e)
        {
            GuiListarAhorros listarAhorros = new GuiListarAhorros(); 
            listarAhorros.Show();
        }

        private void agregarToolStripMenuItem_Click(object sender, EventArgs e)
        {
            GuiMovimientos1 guiMovimientos1 = new GuiMovimientos1();
            guiMovimientos1.Show();
        }

        private void listarToolStripMenuItem1_Click(object sender, EventArgs e)
        {
            GuiListarMovimientos1 gui = new GuiListarMovimientos1();
            gui.Show();
        }

        // ============ CRUD completo de Movimientos ============

        private void buscarMovToolStripMenuItem_Click(object sender, EventArgs e)
        {
            new GuiBuscarMovimiento().Show();
        }

        private void actualizarMovToolStripMenuItem_Click(object sender, EventArgs e)
        {
            new GuiActualizarMovimiento().Show();
        }

        private void eliminarMovToolStripMenuItem_Click(object sender, EventArgs e)
        {
            new GuiEliminarMovimiento().Show();
        }

        /// <summary>
        /// Consulta personalizada #2 — datos del maestro + totalMovimientos + totalCreditos.
        /// </summary>
        private void resumenToolStripMenuItem_Click(object sender, EventArgs e)
        {
            new GuiResumenCuenta().Show();
        }

        private void acercaDeToolStripMenuItem_Click(object sender, EventArgs e)
        {
            MessageBox.Show("Desarrollado por Carlos Gil, Jaider Clavijo y Santiago Lozano\nVersión: 1.0.0\nArquitectura: REST + SSE + Microservicios\nMS-CuentaAhorros: 8080 | MS-Movimiento: 8081",
                "Acerca de WayBank", MessageBoxButtons.OK, MessageBoxIcon.Information);
        }
    }
}
