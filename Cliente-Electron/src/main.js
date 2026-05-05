const { app, BrowserWindow, ipcMain, Menu, dialog } = require('electron');
const axios = require('axios');
const http = require('http');
const path = require('path');

// URLs de los dos microservicios
const CUENTAS_URL    = 'http://localhost:8080/cuentas';
const MOVIMIENTOS_URL = 'http://localhost:8081/movimientos';

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
 * CuentaObserver: recibe notificaciones de AMBOS servidores (SSE) y las reenvía
 * a todas las ventanas Electron abiertas mediante IPC.
 *
 * El Subject son los dos microservicios:
 *   - MS-CuentaAhorros  (8080): eventos CUENTA_CREADA, CUENTA_ACTUALIZADA, CUENTA_ELIMINADA
 *   - MS-Movimiento     (8081): eventos MOVIMIENTO_CREADO, MOVIMIENTO_ACTUALIZADO, MOVIMIENTO_ELIMINADO
 *
 * Este observer actúa de puente entre los streams SSE y las ventanas Electron.
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
    // Usamos "datos" para coincidir con la clave que envía el SseService de Java
    this.vm.notificarTodos('actualizacion', { evento, datos: data });
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
/**
 * ApiService genérico — instanciado dos veces:
 *   apiCuentas      → http://localhost:8080/cuentas
 *   apiMovimientos  → http://localhost:8081/movimientos
 */
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

  // --- Endpoints de Cuentas (usado por apiCuentas) ---
  listar()                      { return this.request('GET',    ''); }
  crear(cuenta)                 { return this.request('POST',   '', cuenta); }
  buscar(numero)                { return this.request('GET',    '/' + numero); }
  buscarPorTitular(titular)     { return this.request('GET',    '/buscar?titular=' + encodeURIComponent(titular)); }
  filtrar(titular, estado) {
    const params = [];
    if (titular) params.push('titular=' + encodeURIComponent(titular));
    if (estado && estado !== 'Todos') params.push('estado=' + encodeURIComponent(estado));
    return this.request('GET', '/filtrar' + (params.length ? '?' + params.join('&') : ''));
  }
  actualizar(numero, cuenta)    { return this.request('PUT',    '/' + numero, cuenta); }
  eliminar(numero)              { return this.request('DELETE', '/' + numero); }
  // Consulta personalizada #2 — GET /cuentas/{numero}/resumen (maestro + 2 agregados)
  resumen(numero)               { return this.request('GET',    '/' + numero + '/resumen'); }

  // --- Endpoints de Movimientos (usado por apiMovimientos) ---
  // POST /movimientos  — numeroCuenta va en el body, MS-Movimiento valida en MS-CuentaAhorros (intercomunicación)
  crearMovimiento(datos)        { return this.request('POST',   '', datos); }
  listarMovimientos()           { return this.request('GET',    ''); }
  listarMovimientosCuenta(num)  { return this.request('GET',    '/cuenta/' + num); }
  buscarMovimiento(id)          { return this.request('GET',    '/' + id); }
  filtrarMovimientos(num, tipo) {
    const params = [];
    if (num > 0) params.push('numeroCuenta=' + num);
    if (tipo && tipo !== 'Todos') params.push('tipo=' + encodeURIComponent(tipo));
    return this.request('GET', '/filtrar' + (params.length ? '?' + params.join('&') : ''));
  }
  actualizarMovimiento(id, datos) { return this.request('PUT',    '/' + id, datos); }
  eliminarMovimiento(id)          { return this.request('DELETE', '/' + id); }
}

// ============== SINGLE RESPONSIBILITY ==============
class CuentaRepository extends ICuentaRepository {
  constructor(api) { super(); this.api = api; }
  crear(cuenta)          { return this.api.crear(cuenta); }
  listar()               { return this.api.listar(); }
  filtrar(titular, est)  { return this.api.filtrar(titular, est); }
  buscar(numero)         { return this.api.buscar(numero); }
  buscarPorTitular(t)    { return this.api.buscarPorTitular(t); }
  actualizar(n, c)       { return this.api.actualizar(n, c); }
  eliminar(n)            { return this.api.eliminar(n); }
}

class MovimientoRepository extends IMovimientoRepository {
  constructor(api) { super(); this.api = api; }

  // POST /movimientos — body: {numeroCuenta, monto, tipo}
  agregar(numeroCuenta, monto, tipo) {
    return this.api.request('POST', '', { numeroCuenta, monto, tipo: tipo.toUpperCase() });
  }
  listar(numeroCuenta)  { return this.api.listarMovimientosCuenta(numeroCuenta); }
  listarTodos()         { return this.api.listarMovimientos(); }
  buscar(id)            { return this.api.buscarMovimiento(id); }
  filtrar(num, tipo)    { return this.api.filtrarMovimientos(num, tipo); }
  actualizar(id, datos) { return this.api.actualizarMovimiento(id, datos); }
  eliminar(id)          { return this.api.eliminarMovimiento(id); }
}

// ============== OPEN/CLOSED ==============
class CuentaService {
  constructor(repo, obs) { this.repo = repo; this.obs = obs; }
  async crear(cuenta) {
    const r = await this.repo.crear(cuenta);
    if (r.success) this.obs.notificar('CUENTA_CREADA', cuenta);
    return r;
  }
  buscar(n)            { return this.repo.buscar(n); }
  listar()             { return this.repo.listar(); }
  filtrar(t, es)       { return this.repo.filtrar(t, es); }
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
  // Consulta personalizada #2 — maestro (CuentaAhorros) + 2 agregados de Movimiento
  resumen(n) { return this.repo.api.resumen(n); }
}

class MovimientoService {
  constructor(repo, obs) { this.repo = repo; this.obs = obs; }
  async agregar(numeroCuenta, monto, tipo) {
    const r = await this.repo.agregar(numeroCuenta, monto, tipo);
    if (r.success) this.obs.notificar('MOVIMIENTO_CREADO', { numeroCuenta, monto, tipo });
    return r;
  }
  listar(n)         { return this.repo.listar(n); }
  listarTodos()     { return this.repo.listarTodos(); }
  buscar(id)        { return this.repo.buscar(id); }
  filtrar(num, tipo){ return this.repo.filtrar(num, tipo); }
  async actualizar(id, datos) {
    const r = await this.repo.actualizar(id, datos);
    if (r.success) this.obs.notificar('MOVIMIENTO_ACTUALIZADO', { id, ...datos });
    return r;
  }
  async eliminar(id) {
    const r = await this.repo.eliminar(id);
    if (r.success) this.obs.notificar('MOVIMIENTO_ELIMINADO', { id });
    return r;
  }
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

// ============== INSTANCIACIÓN ==============
const apiCuentas     = new ApiService(CUENTAS_URL);
const apiMovimientos = new ApiService(MOVIMIENTOS_URL);

const vm       = new VentanaManager();
const observer = new CuentaObserver(vm);
observer.agregar(new ConsoleLogger());

const repoCuentas = new CacheCuentaRepository(apiCuentas);
const repoMov     = new MovimientoRepository(apiMovimientos);
const cuentaSvc   = new CuentaService(repoCuentas, observer);
const movSvc      = new MovimientoService(repoMov, observer);

// ============== OBSERVER SSE — dos conexiones, una por microservicio ==============
/**
 * Los dos microservicios son los Subjects del patrón Observer.
 * Este cliente se suscribe a ambos endpoints SSE para recibir notificaciones
 * en tiempo real de cualquier cambio — sin polling.
 *
 *   SSE-1: GET http://localhost:8080/cuentas/eventos     → eventos de cuentas
 *   SSE-2: GET http://localhost:8081/movimientos/eventos → eventos de movimientos
 */
let sseRequests = [];

function conectarSSE(url, etiqueta) {
  const req = http.get(url, {
    headers: { 'Accept': 'text/event-stream', 'Cache-Control': 'no-cache' }
  }, (res) => {
    console.log(`[SSE-${etiqueta}] Conectado. Status: ${res.statusCode}`);
    let buffer = '';

    res.on('data', (chunk) => {
      buffer += chunk.toString();
      const lineas = buffer.split('\n');
      buffer = lineas.pop(); // conservar línea incompleta

      for (const linea of lineas) {
        if (linea.startsWith('data:')) {
          try {
            const jsonStr  = linea.substring(5).trim();
            const payload  = JSON.parse(jsonStr);
            observer.notificar(payload.evento || 'ACTUALIZACION', payload.datos || {});
          } catch (e) { /* ignorar líneas no JSON */ }
        }
      }
    });

    res.on('error', () => {
      console.log(`[SSE-${etiqueta}] Error en stream. Reconectando en 3s...`);
      setTimeout(() => conectarSSE(url, etiqueta), 3000);
    });

    res.on('close', () => {
      console.log(`[SSE-${etiqueta}] Conexión cerrada. Reconectando en 3s...`);
      setTimeout(() => conectarSSE(url, etiqueta), 3000);
    });
  });

  req.on('error', () => {
    console.log(`[SSE-${etiqueta}] Servidor no disponible. Reintentando en 3s...`);
    setTimeout(() => conectarSSE(url, etiqueta), 3000);
  });

  sseRequests.push(req);
}

// ============== IPC HANDLERS ==============
// Cuentas → MS-CuentaAhorros (8080)
ipcMain.handle('healthcheck',       ()        => cuentaSvc.listar());
ipcMain.handle('crear-cuenta',      (e, c)    => cuentaSvc.crear(c));
ipcMain.handle('listar-cuentas',    ()        => cuentaSvc.listar());
ipcMain.handle('filtrar-cuentas',   (e, t, s) => cuentaSvc.filtrar(t, s));
ipcMain.handle('buscar-cuenta',     (e, n)    => cuentaSvc.buscar(n));
ipcMain.handle('buscar-por-titular',(e, t)    => repoCuentas.buscarPorTitular(t));
ipcMain.handle('actualizar-cuenta', (e, n, c) => cuentaSvc.actualizar(n, c));
ipcMain.handle('eliminar-cuenta',   (e, n)    => cuentaSvc.eliminar(n));
// Consulta personalizada #2 — GET /cuentas/{numero}/resumen
ipcMain.handle('obtener-resumen',   (e, n)    => cuentaSvc.resumen(n));

// Movimientos → MS-Movimiento (8081)
// agregar-movimiento: body {numeroCuenta, monto, tipo} — MS-Movimiento valida cuenta en 8080 (intercomunicación)
ipcMain.handle('agregar-movimiento',    (e, n, m, t)    => movSvc.agregar(n, m, t));
ipcMain.handle('listar-movimientos',    (e, n)          => movSvc.listar(n));
ipcMain.handle('listar-todos-movimientos', ()           => movSvc.listarTodos());
ipcMain.handle('buscar-movimiento',     (e, _n, id)     => movSvc.buscar(id));          // numeroCuenta ignorado, ya no en URL
ipcMain.handle('actualizar-movimiento', (e, n, id, datos) => movSvc.actualizar(id, { numeroCuenta: n, ...datos }));
ipcMain.handle('eliminar-movimiento',   (e, _n, id)     => movSvc.eliminar(id));
ipcMain.handle('filtrar-movimientos',   (e, num, tipo)  => movSvc.filtrar(num, tipo));  // nuevo

// ============== VENTANA PRINCIPAL ==============
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
        { label: 'Crear',      click: () => vm.crear('crear',      { width: 360, height: 430, archivo: 'ventanas/crear.html',      parent: principal, preload: preloadPath }) },
        { label: 'Buscar',     click: () => vm.crear('buscar',     { width: 360, height: 280, archivo: 'ventanas/buscar.html',     parent: principal, preload: preloadPath }) },
        { label: 'Actualizar', click: () => vm.crear('actualizar', { width: 360, height: 340, archivo: 'ventanas/actualizar.html', parent: principal, preload: preloadPath }) },
        { label: 'Eliminar',   click: () => vm.crear('eliminar',   { width: 360, height: 350, archivo: 'ventanas/eliminar.html',   parent: principal, preload: preloadPath }) },
        { label: 'Listar',     click: () => vm.crear('listar',     { width: 560, height: 420, archivo: 'ventanas/listar.html',     parent: principal, preload: preloadPath }) }
      ]
    },
    {
      label: 'Movimientos', submenu: [
        { label: 'Agregar',              click: () => vm.crear('movimientos',  { width: 360, height: 360, archivo: 'ventanas/movimientos.html',  parent: principal, preload: preloadPath }) },
        { label: 'Buscar Transacción',   click: () => vm.crear('buscarmovi',   { width: 400, height: 350, archivo: 'ventanas/buscarmovi.html',   parent: principal, preload: preloadPath }) },
        { label: 'Ver Historial',        click: () => vm.crear('vermovi',      { width: 480, height: 400, archivo: 'ventanas/vermovi.html',      parent: principal, preload: preloadPath }) },
        { label: 'Editar',            click: () => vm.crear('editarmovi',   { width: 380, height: 370, archivo: 'ventanas/editarmovi.html',   parent: principal, preload: preloadPath }) },
        { label: 'Eliminar',          click: () => vm.crear('eliminarmovi', { width: 380, height: 390, archivo: 'ventanas/eliminarmovi.html', parent: principal, preload: preloadPath }) },
        { label: 'Filtrar con Titular', click: () => vm.crear('filtrarmovi', { width: 560, height: 460, archivo: 'ventanas/filtrarmovi.html', parent: principal, preload: preloadPath }) }
      ]
    },
    { type: 'separator' },
    {
      label: 'Ayuda', submenu: [
        {
          label: 'Acerca de...', click: () => {
            dialog.showMessageBox(principal, {
              title: 'Acerca de WayBank',
              message: 'WayBank - Sistema de Gestión Bancaria\n\nDesarrollado por:\n  Carlos Gil\n  Jaider Clavijo\n  Santiago Lozano\n\nVersión: 1.0.0\nArquitectura: REST + SSE + MVC\nMicroservicios: CuentaAhorros:8080 | Movimiento:8081',
              buttons: ['Cerrar']
            });
          }
        }
      ]
    },
    { label: 'Salir', click: () => app.quit() }
  ]);
  Menu.setApplicationMenu(menu);

  // Iniciar SSE a los DOS microservicios (Observer en servidor)
  setTimeout(() => {
    conectarSSE(`${CUENTAS_URL}/eventos`,     'CuentaAhorros-8080');
    conectarSSE(`${MOVIMIENTOS_URL}/eventos`, 'Movimiento-8081');
  }, 1000);
}

app.whenReady().then(iniciar);
app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
app.on('activate', () => { if (!principal || principal.isDestroyed()) iniciar(); });
app.on('before-quit', () => {
  sseRequests.forEach(r => { try { r.destroy(); } catch(e){} });
  sseRequests = [];
});
