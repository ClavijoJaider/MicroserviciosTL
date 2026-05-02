package com.jacs.proyectomicroservicios.observer;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer en servidor (patrón Observer — lado servidor).
 *
 * Cada cliente REST que llame a GET /cuentas/eventos queda registrado.
 * En cada cambio de cuentas o movimientos, notifica a todos los suscritos vía SSE.
 *
 * NOTA: No usa ObjectMapper — Spring Boot 4.x migró Jackson a tools.jackson
 * (package distinto a com.fasterxml.jackson). El JSON se construye con un
 * helper interno para no depender de ninguna versión específica de Jackson.
 */
@Service
public class SseService implements ListObserver {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // ---------------------------------------------------------------
    // Registro de suscriptores
    // ---------------------------------------------------------------

    /** Registra un cliente como observador SSE. */
    public SseEmitter registrar() {
        SseEmitter emitter = new SseEmitter(0L);

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> { emitters.remove(emitter); emitter.complete(); });
        emitter.onError(e -> emitters.remove(emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("conexion")
                    .data("{\"estado\":\"conectado\",\"suscriptores\":" + emitters.size() + "}"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }
        return emitter;
    }

    // ---------------------------------------------------------------
    // Notificación (notify del patrón Observer)
    // ---------------------------------------------------------------

    /**
     * Notifica a todos los clientes suscritos.
     *
     * @param evento nombre del evento SSE (CUENTA_CREADA, MOVIMIENTO_AGREGADO, …)
     * @param datos  mapa de campos a serializar como JSON
     */
    public void notificar(String evento, Map<String, Object> datos) {
        if (emitters.isEmpty()) return;

        String json = toJson(evento, datos);
        List<SseEmitter> muertos = new ArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(json));
            } catch (IOException | IllegalStateException e) {
                muertos.add(emitter);
            }
        }
        emitters.removeAll(muertos);
    }

    @Override
    public void onListUpdated(List<?> elementos) {
        notificar("LISTA_ACTUALIZADA", Map.of("total", elementos.size()));
    }

    public int getCantidadSuscriptores() { return emitters.size(); }

    // ---------------------------------------------------------------
    // Helper — serialización JSON mínima sin dependencia de Jackson
    // ---------------------------------------------------------------

    /**
     * Serializa un mapa plano (String → valor primitivo o String) a JSON.
     * Suficiente para los payloads de eventos SSE del servidor.
     */
    private static String toJson(String evento, Map<String, Object> datos) {
        StringBuilder sb = new StringBuilder("{\"evento\":\"")
                .append(evento).append("\",\"datos\":{");

        boolean primero = true;
        for (Map.Entry<String, Object> e : datos.entrySet()) {
            if (!primero) sb.append(',');
            primero = false;
            sb.append('"').append(e.getKey()).append("\":");
            Object v = e.getValue();
            if (v instanceof String) {
                sb.append('"').append(v).append('"');
            } else {
                sb.append(v);           // números, boolean, null
            }
        }
        return sb.append("}}").toString();
    }
}
