package com.jacs.proyectomicroservicios.service;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.observer.CuentaListSubject;
import com.jacs.proyectomicroservicios.observer.SseService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementación en-memoria del servicio MAESTRO — activa SOLO con el perfil "inmemory".
 *
 * SEPARACION ESTRICTA (Tercer Prototipo):
 *   Solo gestiona CUENTA_AHORROS. Los movimientos son responsabilidad
 *   exclusiva de MS-Movimiento (puerto 8081).
 */
@Service
@Profile("inmemory")
public class CuentaAhorrosService implements ICuentaAhorrosService {

    private static CuentaAhorrosService instance;

    private final SseService              sseService;
    private final List<CuentaAhorros>     cuentas = new ArrayList<>();

    public CuentaAhorrosService(SseService sseService) {
        this.sseService = sseService;
    }

    @PostConstruct
    private void initSingleton() {
        instance = this;
        CuentaListSubject.attach(sseService);
    }

    public static CuentaAhorrosService getInstance() { return instance; }

    // ===================================================================
    // CRUD CuentaAhorros — unica responsabilidad de este microservicio
    // ===================================================================

    @Override
    public void agregar(CuentaAhorros cuenta) {
        if (cuenta == null)
            throw new IllegalArgumentException("La cuenta no puede ser nula");
        if (cuenta.getTitular() == null || cuenta.getTitular().isBlank())
            throw new IllegalArgumentException("El titular no puede estar vacío");
        if (cuenta.getNumeroCuenta() <= 0)
            throw new IllegalArgumentException("El número de cuenta debe ser mayor a cero");
        if (cuenta.getSaldo() < 200_000.0)
            throw new IllegalArgumentException("El saldo inicial mínimo es $200.000");
        if (cuentas.stream().anyMatch(c -> c.getNumeroCuenta() == cuenta.getNumeroCuenta()))
            throw new IllegalArgumentException("Ya existe una cuenta con el número: " + cuenta.getNumeroCuenta());

        cuenta.setEstado("Activo");
        cuentas.add(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", cuenta.getNumeroCuenta());
        payload.put("titular",      cuenta.getTitular());
        sseService.notificar("CUENTA_CREADA", payload);
        CuentaListSubject.notifyObservers(listar());
    }

    @Override
    public CuentaAhorros buscarPorNumero(int numeroCuenta) {
        return cuentas.stream()
                .filter(c -> c.getNumeroCuenta() == numeroCuenta)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));
    }

    @Override
    public List<CuentaAhorros> listar() {
        return cuentas.stream()
                .sorted(Comparator.comparingInt(CuentaAhorros::getNumeroCuenta))
                .collect(Collectors.toList());
    }

    @Override
    public List<CuentaAhorros> listarConFiltro(String titular, String estado) {
        return cuentas.stream()
                .filter(c -> titular == null || titular.isBlank()
                        || c.getTitular().toLowerCase().contains(titular.toLowerCase()))
                .filter(c -> estado == null || estado.isBlank()
                        || c.getEstado().equalsIgnoreCase(estado))
                .sorted(Comparator.comparingInt(CuentaAhorros::getNumeroCuenta))
                .collect(Collectors.toList());
    }

    @Override
    public List<CuentaAhorros> buscarPorTitular(String titular) {
        return cuentas.stream()
                .filter(c -> c.getTitular().toLowerCase().contains(titular.toLowerCase()))
                .sorted(Comparator.comparingInt(CuentaAhorros::getNumeroCuenta))
                .collect(Collectors.toList());
    }

    @Override
    public boolean eliminar(int numeroCuenta) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        if (!"Activo".equalsIgnoreCase(cuenta.getEstado())) return false;
        cuenta.setEstado("Inactivo");

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        sseService.notificar("CUENTA_ELIMINADA", payload);
        CuentaListSubject.notifyObservers(listar());
        return true;
    }

    @Override
    public CuentaAhorros actualizar(int numeroCuenta, CuentaAhorros datos) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        if (datos.getTitular() != null && !datos.getTitular().isBlank())
            cuenta.setTitular(datos.getTitular());
        if (datos.getSaldo() >= 200_000.0)
            cuenta.setSaldo(datos.getSaldo());
        if (datos.getTasaInteres() > 0)
            cuenta.setTasaInteres(datos.getTasaInteres());
        if (datos.getEstado() != null && !datos.getEstado().isBlank())
            cuenta.setEstado(datos.getEstado());

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("titular",      cuenta.getTitular());
        sseService.notificar("CUENTA_ACTUALIZADA", payload);
        CuentaListSubject.notifyObservers(listar());
        return cuenta;
    }
}
