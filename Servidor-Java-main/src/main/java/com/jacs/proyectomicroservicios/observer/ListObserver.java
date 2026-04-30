package com.jacs.proyectomicroservicios.observer;

import java.util.List;

/**
 * Interfaz Observer del patrón Observer implementado en el SERVIDOR.
 * Los observadores concretos (ej: SseService) reciben notificaciones
 * cuando la lista de datos cambia.
 */
public interface ListObserver {
    void onListUpdated(List<?> elementos);
}
