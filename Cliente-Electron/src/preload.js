const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  // Cuentas
  healthcheck: () => ipcRenderer.invoke('healthcheck'),
  crearCuenta: (cuenta) => ipcRenderer.invoke('crear-cuenta', cuenta),
  listarCuentas: () => ipcRenderer.invoke('listar-cuentas'),
  filtrarCuentas: (titular, estado) => ipcRenderer.invoke('filtrar-cuentas', titular, estado),
  buscarCuenta: (numero) => ipcRenderer.invoke('buscar-cuenta', numero),
  buscarPorTitular: (titular) => ipcRenderer.invoke('buscar-por-titular', titular),
  actualizarCuenta: (numero, cuenta) => ipcRenderer.invoke('actualizar-cuenta', numero, cuenta),
  eliminarCuenta: (numero) => ipcRenderer.invoke('eliminar-cuenta', numero),

  // Movimientos — CRUD completo
  agregarMovimiento: (numero, monto, tipo) => ipcRenderer.invoke('agregar-movimiento', numero, monto, tipo),
  listarMovimientos: (numero) => ipcRenderer.invoke('listar-movimientos', numero),
  listarTodosMovimientos: () => ipcRenderer.invoke('listar-todos-movimientos'),
  buscarMovimiento: (numero, id) => ipcRenderer.invoke('buscar-movimiento', numero, id),
  actualizarMovimiento: (numero, id, datos) => ipcRenderer.invoke('actualizar-movimiento', numero, id, datos),
  eliminarMovimiento: (numero, id) => ipcRenderer.invoke('eliminar-movimiento', numero, id),

  // Observer: escucha eventos enviados desde main.js cuando el servidor SSE notifica
  onActualizacion: (callback) => ipcRenderer.on('actualizacion', (e, data) => callback(data)),
  // Alias para compatibilidad con ventanas que usan onNotificacion
  onNotificacion: (callback) => ipcRenderer.on('actualizacion', (e, data) => callback(data))
});

console.log('Preload cargado - Observer SSE activo');
