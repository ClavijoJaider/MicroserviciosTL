package com.jacs.proyectomicroservicios.service;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;
import com.jacs.proyectomicroservicios.observer.CuentaListSubject;
import com.jacs.proyectomicroservicios.observer.SseService;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Singleton gestionado por Spring (scope default = singleton).
 * Activo SOLO con el perfil "inmemory" — en producción usa CuentaAhorrosJpaService (@Primary).
 * La referencia estática permite acceso sin inyección cuando sea necesario,
 * y se asigna en @PostConstruct tras la creación del bean.
 */
@Service
@Profile("inmemory")
public class CuentaAhorrosService implements ICuentaAhorrosService {

    private static CuentaAhorrosService instance;

    private final SseService sseService;
    private final List<CuentaAhorros> cuentas = new ArrayList<>();
    private final List<Movimiento> movimientosGlobales = new ArrayList<>();
    private int nextMovimientoId = 1;

    public CuentaAhorrosService(SseService sseService) {
        this.sseService = sseService;
    }

    @PostConstruct
    private void initSingleton() {
        instance = this;
        // Registrar SseService como observador concreto del Subject
        CuentaListSubject.attach(sseService);
    }

    /** Acceso estático al singleton para código no-Spring (legacy). */
    public static CuentaAhorrosService getInstance() {
        return instance;
    }

    @Override
    public void agregar(CuentaAhorros cuenta) {
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta no puede ser nula");
        }
        if (cuenta.getTitular() == null || cuenta.getTitular().trim().isEmpty()) {
            throw new IllegalArgumentException("El titular no puede estar vacío");
        }
        if (cuenta.getNumeroCuenta() < 0) {
            throw new IllegalArgumentException("El número de cuenta no puede ser negativo");
        }
        if (cuenta.getSaldo() < 200000.0) {
            throw new IllegalArgumentException("El saldo inicial mínimo es $200.000");
        }
        boolean existe = cuentas.stream()
                .anyMatch(c -> c.getNumeroCuenta() == cuenta.getNumeroCuenta());
        if (existe) {
            throw new IllegalArgumentException("Ya existe una cuenta con el número: " + cuenta.getNumeroCuenta());
        }

        cuenta.setEstado("Activo");
        if (cuenta.getMovimientos() == null) {
            cuenta.setMovimientos(new ArrayList<>());
        }
        cuentas.add(cuenta);

        // Notificar observers (SseService envía el evento a todos los clientes conectados)
        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", cuenta.getNumeroCuenta());
        payload.put("titular", cuenta.getTitular());
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
    public Movimiento agregarMovimiento(int numeroCuenta, Movimiento datos) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);

        if (datos.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (datos.getTipo() == null || datos.getTipo().trim().isEmpty()) {
            throw new IllegalArgumentException("El tipo de movimiento es requerido");
        }

        String tipo = datos.getTipo().toUpperCase();
        if (!"CREDITO".equals(tipo) && !"DEBITO".equals(tipo)) {
            throw new IllegalArgumentException("Tipo de movimiento inválido: " + datos.getTipo() + ". Use CREDITO o DEBITO");
        }

        if ("DEBITO".equals(tipo) && cuenta.getSaldo() < datos.getMonto()) {
            throw new IllegalArgumentException("Saldo insuficiente. Saldo actual: $" + cuenta.getSaldo());
        }

        Movimiento movimiento = Movimiento.builder()
                .id(nextMovimientoId++)
                .fechaMovimiento(LocalDateTime.now())
                .monto(datos.getMonto())
                .tipo(tipo)
                .numeroCuenta(numeroCuenta)
                .build();

        cuenta.getMovimientos().add(movimiento);
        movimientosGlobales.add(movimiento);

        if ("CREDITO".equals(tipo)) {
            cuenta.setSaldo(cuenta.getSaldo() + datos.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() - datos.getMonto());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("tipo", tipo);
        payload.put("monto", movimiento.getMonto());
        sseService.notificar("MOVIMIENTO_AGREGADO", payload);

        return movimiento;
    }

    @Override
    public List<Movimiento> listarMovimientos(int numeroCuenta) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        return cuenta.getMovimientos();
    }

    @Override
    public List<Movimiento> listarTodosMovimientos() {
        return movimientosGlobales.stream()
                .sorted(Comparator.comparingInt(Movimiento::getId))
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
    public boolean eliminar(int numeroCuenta) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        if ("Activo".equalsIgnoreCase(cuenta.getEstado())) {
            cuenta.setEstado("Inactivo");
            Map<String, Object> payload = new HashMap<>();
            payload.put("numeroCuenta", numeroCuenta);
            sseService.notificar("CUENTA_ELIMINADA", payload);
            CuentaListSubject.notifyObservers(listar());
            return true;
        }
        return false;
    }

    @Override
    public CuentaAhorros actualizar(int numeroCuenta, CuentaAhorros datos) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);

        if (datos.getTitular() != null && !datos.getTitular().trim().isEmpty()) {
            cuenta.setTitular(datos.getTitular());
        }
        if (datos.getSaldo() >= 200000.0) {
            cuenta.setSaldo(datos.getSaldo());
        }
        if (datos.getTasaInteres() > 0) {
            cuenta.setTasaInteres(datos.getTasaInteres());
        }
        if (datos.getEstado() != null && !datos.getEstado().trim().isEmpty()) {
            cuenta.setEstado(datos.getEstado());
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("titular", cuenta.getTitular());
        sseService.notificar("CUENTA_ACTUALIZADA", payload);
        CuentaListSubject.notifyObservers(listar());

        return cuenta;
    }

    // ============================================================
    // CRUD Completo de Movimientos (buscar, actualizar, eliminar)
    // ============================================================

    @Override
    public Movimiento buscarMovimientoPorId(int numeroCuenta, int id) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        return cuenta.getMovimientos().stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Movimiento no encontrado: id=" + id + " en cuenta=" + numeroCuenta));
    }

    @Override
    public Movimiento actualizarMovimiento(int numeroCuenta, int id, Movimiento datos) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        Movimiento movimiento = buscarMovimientoPorId(numeroCuenta, id);

        if (datos.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        String tipoNuevo = (datos.getTipo() != null && !datos.getTipo().isBlank())
                ? datos.getTipo().toUpperCase()
                : movimiento.getTipo();
        if (!"CREDITO".equals(tipoNuevo) && !"DEBITO".equals(tipoNuevo)) {
            throw new IllegalArgumentException("Tipo inválido: " + tipoNuevo + ". Use CREDITO o DEBITO");
        }

        // Revertir efecto del movimiento anterior sobre el saldo
        if ("CREDITO".equals(movimiento.getTipo())) {
            cuenta.setSaldo(cuenta.getSaldo() - movimiento.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() + movimiento.getMonto());
        }

        // Validar saldo para el nuevo DEBITO
        if ("DEBITO".equals(tipoNuevo) && cuenta.getSaldo() < datos.getMonto()) {
            // Restaurar saldo antes de lanzar excepción
            if ("CREDITO".equals(movimiento.getTipo())) {
                cuenta.setSaldo(cuenta.getSaldo() + movimiento.getMonto());
            } else {
                cuenta.setSaldo(cuenta.getSaldo() - movimiento.getMonto());
            }
            throw new IllegalArgumentException("Saldo insuficiente para el monto actualizado. Saldo: $" + cuenta.getSaldo());
        }

        // Aplicar nuevo efecto en el saldo
        if ("CREDITO".equals(tipoNuevo)) {
            cuenta.setSaldo(cuenta.getSaldo() + datos.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() - datos.getMonto());
        }

        // Actualizar también en la lista global
        movimientosGlobales.stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .ifPresent(m -> { m.setMonto(datos.getMonto()); m.setTipo(tipoNuevo); });

        movimiento.setMonto(datos.getMonto());
        movimiento.setTipo(tipoNuevo);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("movimientoId", id);
        payload.put("tipo", tipoNuevo);
        payload.put("monto", datos.getMonto());
        sseService.notificar("MOVIMIENTO_ACTUALIZADO", payload);

        return movimiento;
    }

    @Override
    public boolean eliminarMovimiento(int numeroCuenta, int id) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        Movimiento movimiento = buscarMovimientoPorId(numeroCuenta, id);

        // Revertir efecto en el saldo
        if ("CREDITO".equals(movimiento.getTipo())) {
            cuenta.setSaldo(cuenta.getSaldo() - movimiento.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() + movimiento.getMonto());
        }

        cuenta.getMovimientos().remove(movimiento);
        movimientosGlobales.remove(movimiento);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("movimientoId", id);
        sseService.notificar("MOVIMIENTO_ELIMINADO", payload);

        return true;
    }

    @Override
    public List<Movimiento> filtrarMovimientos(int numeroCuenta, String tipo,
                                               LocalDateTime desde, LocalDateTime hasta) {
        return movimientosGlobales.stream()
                .filter(m -> numeroCuenta == 0 || m.getNumeroCuenta() == numeroCuenta)
                .filter(m -> tipo == null || tipo.isBlank()
                        || tipo.equalsIgnoreCase(m.getTipo()))
                .filter(m -> desde == null
                        || !m.getFechaMovimiento().isBefore(desde))
                .filter(m -> hasta == null
                        || !m.getFechaMovimiento().isAfter(hasta))
                .sorted(Comparator.comparing(Movimiento::getFechaMovimiento).reversed())
                .collect(Collectors.toList());
    }
}
