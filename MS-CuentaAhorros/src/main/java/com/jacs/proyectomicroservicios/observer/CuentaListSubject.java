package com.jacs.proyectomicroservicios.observer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Sujeto (Subject) del patrón Observer en el servidor.
 * Mantiene la lista de observadores y los notifica cuando la colección cambia.
 * El observador concreto registrado es SseService, que empuja los eventos
 * a todos los clientes HTTP conectados via Server-Sent Events.
 */
public class CuentaListSubject {

    private static final List<ListObserver> observadores = new CopyOnWriteArrayList<>();

    public static void attach(ListObserver observer) {
        if (!observadores.contains(observer)) {
            observadores.add(observer);
        }
    }

    public static void detach(ListObserver observer) {
        observadores.remove(observer);
    }

    public static void notifyObservers(List<?> elementos) {
        for (ListObserver observer : observadores) {
            observer.onListUpdated(elementos);
        }
    }
}
