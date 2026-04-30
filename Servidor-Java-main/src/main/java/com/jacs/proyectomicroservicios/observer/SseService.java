package com.jacs.proyectomicroservicios.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Observer en servidor: cada cliente REST que llame a GET /cuentas/eventos
 * queda registrado como observador. Cuando hay cambios en cuentas o movimientos,
 * este servicio notifica a todos los clientes suscritos vía Server-Sent Events.
 */
@Service
public class SseService implements ListObserver {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Autowired
    private ObjectMapper mapper;

    /**
     * Registra un nuevo cliente observador y devuelve el SseEmitter.
     * El cliente debe mantener la conexión abierta para recibir eventos.
     */
    public SseEmitter registrar() {
        SseEmitter emitter = new SseEmitter(0L); // 0L = sin timeout

        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
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

    /**
     * Notifica a todos los clientes suscritos. Corresponde al notify() del patrón Observer.
     */
    public void notificar(String evento, Object datos) {
        List<SseEmitter> muertos = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("evento", evento);
                payload.put("datos", datos);
                String json = mapper.writeValueAsString(payload);
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

    public int getCantidadSuscriptores() {
        return emitters.size();
    }
}
