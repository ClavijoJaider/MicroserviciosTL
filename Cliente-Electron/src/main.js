const { app, BrowserWindow, ipcMain, Menu, dialog } = require('electron');
const axios = require('axios');
const http = require('http');
const path = require('path');

const API_URL = 'http://localhost:8080/cuentas';

// ============== SINGLETON PATTERN ==============
class VentanaManager {
  static instancia = null;
  constructor() {
    if (VentanaManager.instancia) return VentanaManager.instancia;
    this.ventanas = {};
    VentanaManager.instancia = this;
  }
  crear(nombre, opciones) {
    if (this.ventanas[nombre]) { this.ventanas[nombre].focus(); return; }
    const win = new BrowserWindow({
      width: opciones.width,
      height: opciones.height,
      parent: opciones.parent,
      webPreferences: {
        preload: opciones.preload || path.join(__dirname, 'preload.js'),
        contextIsolation: true,
        nodeIntegration: false
      }
    });
    win.loadFile(opciones.archivo);
    win.on('closed', () => { delete this.ventanas[nombre]; });
    this.ventanas[nombre] = win;
    return win;
  }
  // Notificar a todas las ventanas abiertas (parte del Observer pattern)
  notificarTodos(canal, data) {
    Object.values(this.ventanas).forEach(win => {
      if (!win.isDestroyed()) win.webContents.send(canal, data);
    });
  }
}

// ============== OBSERVER PATTERN (en servidor via SSE) ==============
/**
 * CuentaObserver: recibe notificaciones del servidor (SSE) y las reenvía
 * a todas las ventanas Electron abiertas mediante IPC.
 * El servidor es el Subject; este observer actúa de puente entre el
 * stream SSE del servidor y las ventanas del cliente Electron.
 */
class CuentaObserver {
  constructor(ventanaMgr) {
    this.observadores = [];
    this.vm = ventanaMgr;
  }
  agregar(obs) { this.observadores.push(obs); }
  notificar(evento, data) {
    this.observadores.forEach(o => o.onCambio && o.onCambio(evento, data));
    // Propagar a todas las ventanas Electron (IPC renderer)
    this.vm.notificarTodos('actualizacion', { evento, data });
  }
}

class ConsoleLogger {
  onCambio(evento, data) {
    console.log(`[Observer] Evento: ${evento}`, data);
  }
}

// ============== BUILDER PATTERN ==============
class CuentaBuilder {
  constructor() { this.cuenta = {}; }
  setNumero(n) { this.cuenta.numeroCuenta = n; return this; }
  setTitular(t) { this.cuenta.titular = t; return this; }
  setSaldo(s) { this.cuenta.saldo = s; return this; }
  setTasa(t) { this.cuenta.tasaInteres = t; return this; }
  setEstado(e) { this.cuenta.estado = e; return this; }
  build() {
    const d = new Date();
    const fechaLocal = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}T${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}:${String(d.getSeconds()).padStart(2,'0')}`;
    return { ...this.cuenta, fechaApertura: fechaLocal };
  }
}

// ============== INTERFACE SEGREGATION ==============
class IApiService { async request() { throw new Error('Abstract'); } }
class ICuentaRepository {
  crear() { throw new Error('Abstract'); }
  buscar() { throw new Error('Abstract'); }
  listar() { throw new Error('Abstract'); }
  actualizar() { throw new Error('Abstract'); }
  eliminar() { throw new Error('Abstract'); }
}
class IMovimientoRepository {
  agregar() { throw new Error('Abstract'); }
  listar() { throw new Error('Abstract'); }
  listarTodos() { throw new Error('Abstract'); }
}

// ============== DEPENDENCY INVERSION ==============
class ApiService extends IApiService {
  constructor(baseUrl) {
    super();
    this.baseUrl = baseUrl;
  }
  async request(method, endpoint, data = null) {
    try {
      const config = { method: method.toLowerCase(), url: this.baseUrl + endpoint };
      if (data) config.data = data;
      const response = await axios(config);
      return { success: true, data: response.data };
    } catch (e) {
      const msg = e.response?.data || e.message;
      return { success: false, error: typeof msg === 'string' ? msg : JSON.stringify(msg) };
    }
  }
  listar() { return this.request('GET', ''); }
  crear(cuenta) { return this.request('POST', '', cuenta); }
  buscar(numero) { return this.request('GET', '/' + numero); }
  buscarPorTitular(titular) { return this.request('GET', '/buscar?titular=' + encodeURIComponent(titular)); }
  filtrar(titular, estado) {
    const params = [];
    if (titular) params.push('titular=' + encodeURIComponent(titular));
    if (estado && estado !== 'Todos') params.push('estado=' + encodeURIComponent(estado));
    return this.request('GET', '/filtrar' + (params.length ? '?' + params.join('&') : ''));
  }
  actualizar(numero, cuenta) { return this.request('PUT', '/' + numero, cuenta); }
  eliminar(numero) { return this.request('DELETE', '/' + numero); }
  agregarMovimiento(numero, datos) { return this.request('POST', '/' + numero + '/movimientos', datos); }
  listarMovimientos(numero) { return this.request('GET', '/' + numero + '/movimientos'); }
  listarTodosMovimientos() { return this.request('GET', '/movimientos'); }
  // CRUD completo movimientos
  buscarMovimiento(numero, id) { return this.request('GET', '/' + numero + '/movimientos/' + id); }
  actualizarMovimiento(numero, id, datos) { return this.request('PUT', '/' + numero + '/movimientos/' + id, datos); }
  eliminarMovimiento(numero, id) { return this.request('DELETE', '/' + numero + '/movimientos/' + id); }
}

// ============== SINGLE RESPONSIBILITY ==============
class CuentaRepository extends ICuentaRepository {
  constructor(api) { super(); this.api = api; }
  crear(cuenta) { return this.api.crear(cuenta); }
  listar() { return this.api.listar(); }
  filtrar(titular, estado) { return this.api.filtrar(titular, estado); }
  buscar(numero) { return this.api.buscar(numero); }
  buscarPorTitular(t) { return this.api.buscarPorTitular(t); }
  actualizar(n, c) { return this.api.actualizar(n, c); }
  eliminar(n) { return this.api.eliminar(n); }
}

class MovimientoRepository extends IMovimientoRepository {
  constructor(api) { super(); this.api = api; }
  agregar(n, monto, tipo) { return this.api.agregarMovimiento(n, { monto, tipo }); }
  listar(n) { return this.api.listarMovimientos(n); }
  listarTodos() { return this.api.listarTodosMovimientos(); }
  buscar(n, id) { return this.api.buscarMovimiento(n, id); }
  actualizar(n, id, datos) { return this.api.actualizarMovimiento(n, id, datos); }
  eliminar(n, id) { return this.api.eliminarMovimiento(n, id); }
}

// ============== OPEN/CLOSED ==============
class CuentaService {
  constructor(repo, obs) { this.repo = repo; this.obs = obs; }
  async crear(cuenta) {
    const r = await this.repo.crear(cuenta);
    if (r.success) this.obs.notificar('CUENTA_CREADA', cuenta);
    return r;
  }
  buscar(n) { return this.repo.buscar(n); }
  listar() { return this.repo.listar(); }
  filtrar(t, es) { return this.repo.filtrar(t, es); }
  async actualizar(n, c) {
    const r = await this.repo.actualizar(n, c);
    if (r.success) this.obs.notificar('CUENTA_ACTUALIZADA', c);
    return r;
  }
  async eliminar(n) {
    const r = await this.repo.eliminar(n);
    if (r.success) this.obs.notificar('CUENTA_ELIMINADA', n);
    return r;
  }
}

class MovimientoService {
  constructor(repo, obs) { this.repo = repo; this.obs = obs; }
  async agregar(n, monto, tipo) {
    const r = await this.repo.agregar(n, monto, tipo);
    if (r.success) this.obs.notificar('MOVIMIENTO_AGREGADO', { numero: n, monto, tipo });
    return r;
  }
  listar(n) { return this.repo.listar(n); }
  listarTodos() { return this.repo.listarTodos(); }
}

// ============== LISKOV SUBSTITUTION ==============
class CacheCuentaRepository extends CuentaRepository {
  constructor(api) { super(api); this.cache = new Map(); }
  async buscar(numero) {
    if (this.cache.has(numero)) return Promise.resolve({ success: true, data: this.cache.get(numero) });
    const result = await super.buscar(numero);
    if (result.success) this.cache.set(numero, result.data);
    return result;
  }
  invalidar(numero) { this.cache.delete(numero); }
}

// Instanciación
const api = new ApiService(API_URL);
const vm = new VentanaManager();
const observer = new CuentaObserver(vm);
observer.agregar(new ConsoleLogger());

const repoCuentas = new CacheCuentaRepository(api);
const repoMov = new MovimientoRepository(api);
const cuentaSvc = new CuentaService(repoCuentas, observer);
const movSvc = new MovimientoService(repoMov, observer);

// ============== CONEXIÓN SSE AL SERVIDOR (Observer en servidor) ==============
/**
 * El servidor es el Subject del patrón Observer. Este cliente se suscribe
 * al endpoint SSE para recibir notificaciones en tiempo real cuando
 * cambian los datos en el servidor.
 */
let sseRequest = null;

function conectarSSEServidor() {
  if (sseRequest) { try { sseRequest.destroy(); } catch(e){} sseRequest = null; }

  const req = http.get(`${API_URL}/eventos`, { headers: { 'Accept': 'text/event-stream', 'Cache-Control': 'no-cache' } }, (res) => {
    console.log('[SSE] Conectado al servidor. Status:', res.statusCode);
    let buffer = '';

    res.on('data', (chunk) => {
      buffer += chunk.toString();
      const lineas = buffer.split('\n');
      buffer = lineas.pop(); // Conservar línea incompleta

      for (const linea of lineas) {
        if (linea.startsWith('data:')) {
          try {
            const jsonStr = linea.substring(5).trim();
            const payload = JSON.parse(jsonStr);
            // Notificar a todas las ventanas Electron con el evento del servidor
            observer.notificar(payload.evento || 'ACTUALIZACION', payload.datos || {});
          } catch (e) { /* Ignorar líneas no JSON */ }
        }
      }
    });

    res.on('error', () => {
      console.log('[SSE] Error en stream. Reconectando en 3s...');
      setTimeout(conectarSSEServidor, 3000);
    });

    res.on('close', () => {
      console.log('[SSE] Conexión cerrada. Reconectando en 3s...');
      setTimeout(conectarSSEServidor, 3000);
    });
  });

  req.on('error', () => {
    console.log('[SSE] Servidor no disponible. Reintentando en 3s...');
    setTimeout(conectarSSEServidor, 3000);
  });

  sseRequest = req;
}

// IPC Handlers
ipcMain.handle('healthcheck', () => cuentaSvc.listar());
ipcMain.handle('crear-cuenta', (e, c) => cuentaSvc.crear(c));
ipcMain.handle('listar-cuentas', () => cuentaSvc.listar());
ipcMain.handle('filtrar-cuentas', (e, t, es) => cuentaSvc.filtrar(t, es));
ipcMain.handle('buscar-cuenta', (e, n) => cuentaSvc.buscar(n));
ipcMain.handle('buscar-por-titular', (e, t) => repoCuentas.buscarPorTitular(t));
ipcMain.handle('actualizar-cuenta', (e, n, c) => cuentaSvc.actualizar(n, c));
ipcMain.handle('eliminar-cuenta', (e, n) => cuentaSvc.eliminar(n));
ipcMain.handle('agregar-movimiento', (e, n, m, t) => movSvc.agregar(n, m, t));
ipcMain.handle('listar-movimientos', (e, n) => movSvc.listar(n));
ipcMain.handle('listar-todos-movimientos', () => movSvc.listarTodos());
// CRUD completo de movimientos
ipcMain.handle('buscar-movimiento', (e, n, id) => movSvc.buscar(n, id));
ipcMain.handle('actualizar-movimiento', (e, n, id, datos) => movSvc.actualizar(n, id, datos));
ipcMain.handle('eliminar-movimiento', (e, n, id) => movSvc.eliminar(n, id));

let principal = null;

function iniciar() {
  principal = new BrowserWindow({
    width: 400,
    height: 230,
    resizable: false,
    title: 'WayBank - Electron'
  });
  principal.loadFile('ventanas/inicio.html');

  const preloadPath = path.join(__dirname, 'preload.js');

  const menu = Menu.buildFromTemplate([
    {
      label: 'Cuentas', submenu: [
        { label: 'Crear', click: () => vm.crear('crear', { width: 360, height: 430, archivo: 'ventanas/crear.html', parent: principal, preload: preloadPath }) },
        { label: 'Buscar', click: () => vm.crear('buscar', { width: 360, height: 280, archivo: 'ventanas/buscar.html', parent: principal, preload: preloadPath }) },
        { label: 'Actualizar', click: () => vm.crear('actualizar', { width: 360, height: 340, archivo: 'ventanas/actualizar.html', parent: principal, preload: preloadPath }) },
        { label: 'Eliminar', click: () => vm.crear('eliminar', { width: 360, height: 350, archivo: 'ventanas/eliminar.html', parent: principal, preload: preloadPath }) },
        { label: 'Listar', click: () => vm.crear('listar', { width: 560, height: 420, archivo: 'ventanas/listar.html', parent: principal, preload: preloadPath }) }
      ]
    },
    {
      label: 'Movimientos', submenu: [
        { label: 'Agregar',       click: () => vm.crear('movimientos',  { width: 360, height: 360, archivo: 'ventanas/movimientos.html',  parent: principal, preload: preloadPath }) },
        { label: 'Ver Historial', click: () => vm.crear('vermovi',      { width: 480, height: 400, archivo: 'ventanas/vermovi.html',      parent: principal, preload: preloadPath }) },
        { label: 'Editar',        click: () => vm.crear('editarmovi',   { width: 380, height: 370, archivo: 'ventanas/editarmovi.html',   parent: principal, preload: preloadPath }) },
        { label: 'Eliminar',      click: () => vm.crear('eliminarmovi', { width: 380, height: 390, archivo: 'ventanas/eliminarmovi.html', parent: principal, preload: preloadPath }) }
      ]
    },
    { type: 'separator' },
    {
      label: 'Ayuda', submenu: [
        {
          label: 'Acerca de...', click: () => {
            dialog.showMessageBox(principal, {
              title: 'Acerca de WayBank',
              message: 'WayBank - Sistema de Gestión Bancaria\n\nDesarrollado por:\n  Carlos Gil\n  Jaider Clavijo\n  Santiago Lozano\n\nVersión: 1.0.0\nArquitectura: REST + SSE + MVC',
              buttons: ['Cerrar']
            });
          }
        }
      ]
    },
    { label: 'Salir', click: () => app.quit() }
  ]);
  Menu.setApplicationMenu(menu);

  // Iniciar conexión SSE al servidor (Observer en servidor)
  setTimeout(conectarSSEServidor, 1000);
}

app.whenReady().then(iniciar);
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
app.on('activate', () => { if (!principal || principal.isDestroyed()) iniciar(); });
app.on('before-quit', () => {
  if (sseRequest) { try { sseRequest.destroy(); } catch(e){} }
});
