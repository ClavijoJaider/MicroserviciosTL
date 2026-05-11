const { contextBridge, ipcRenderer } = require('electron');

/**
 * Puente seguro entre el proceso renderer (HTML) y el proceso main (Node.js).
 *
 * Cuentas    → llaman a MS-CuentaAhorros  (8080) vía main.js
 * Movimientos → llaman a MS-Movimiento    (8081) vía main.js
 *
 * El proceso main gestiona las dos conexiones SSE y unifica los eventos
 * en el canal 'actualizacion' que las ventanas suscriben via onActualizacion().
 */
contextBridge.exposeInMainWorld('api', {

  // ============ Cuentas — MS-CuentaAhorros (8080) ============
  healthcheck:      ()                    => ipcRenderer.invoke('healthcheck'),
  crearCuenta:      (cuenta)              => ipcRenderer.invoke('crear-cuenta', cuenta),
  listarCuentas:    ()                    => ipcRenderer.invoke('listar-cuentas'),
  filtrarCuentas:   (titular, estado)     => ipcRenderer.invoke('filtrar-cuentas', titular, estado),
  buscarCuenta:     (numero)              => ipcRenderer.invoke('buscar-cuenta', numero),
  buscarPorTitular: (titular)             => ipcRenderer.invoke('buscar-por-titular', titular),
  actualizarCuenta: (numero, cuenta)      => ipcRenderer.invoke('actualizar-cuenta', numero, cuenta),
  eliminarCuenta:   (numero)              => ipcRenderer.invoke('eliminar-cuenta', numero),
  // Consulta personalizada #2: datos del maestro + totalMovimientos + totalCreditos
  obtenerResumen:   (numero)              => ipcRenderer.invoke('obtener-resumen', numero),

  // ============ Movimientos — MS-Movimiento (8081) ============
  // POST /movimientos — body: {numeroCuenta, monto, tipo}
  // MS-Movimiento valida la cuenta en MS-CuentaAhorros (intercomunicación automática)
  agregarMovimiento:     (numero, monto, tipo) => ipcRenderer.invoke('agregar-movimiento', numero, monto, tipo),
  listarMovimientos:     (numero)              => ipcRenderer.invoke('listar-movimientos', numero),
  listarTodosMovimientos:()                    => ipcRenderer.invoke('listar-todos-movimientos'),
  // id ya es suficiente — numeroCuenta ya no forma parte de la URL
  buscarMovimiento:      (numero, id)          => ipcRenderer.invoke('buscar-movimiento', numero, id),
  actualizarMovimiento:  (numero, id, datos)   => ipcRenderer.invoke('actualizar-movimiento', numero, id, datos),
  eliminarMovimiento:    (numero, id)          => ipcRenderer.invoke('eliminar-movimiento', numero, id),
  // Consulta personalizada #1: JOIN MOVIMIENTO-CUENTA_AHORROS via @ManyToOne
  filtrarMovimientos:    (numero, tipo)        => ipcRenderer.invoke('filtrar-movimientos', numero, tipo),

  // ============ Observer SSE (patron Observer en cliente) ============
  // Recibe eventos de AMBOS microservicios unificados en un solo canal.
  // Eventos posibles: CUENTA_CREADA, CUENTA_ACTUALIZADA, CUENTA_ELIMINADA,
  //                   MOVIMIENTO_CREADO, MOVIMIENTO_ACTUALIZADO, MOVIMIENTO_ELIMINADO
  onActualizacion: (callback) => ipcRenderer.on('actualizacion', (e, data) => callback(data)),
  // Alias para compatibilidad con ventanas que usan onNotificacion
  onNotificacion:  (callback) => ipcRenderer.on('actualizacion', (e, data) => callback(data))
});

console.log('Preload cargado — Observer SSE activo (8080 + 8081)');
