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
 * Servicio en-memoria — activo SOLO con el perfil "inmemory".
 *
 * TERCER PROTOTIPO:
 *   La lista de movimientos ya no está en el modelo CuentaAhorros; se gestiona
 *   internamente en este servicio mediante un Map<numeroCuenta, List<Movimiento>>.
 *   Esto garantiza que el modelo compile sin la List eliminada.
 */
@Service
@Profile("inmemory")
public class CuentaAhorrosService implements ICuentaAhorrosService {

    private static CuentaAhorrosService instance;

    private final SseService sseService;
    private final List<CuentaAhorros>            cuentas  = new ArrayList<>();
    private final Map<Integer, List<Movimiento>> movimMap = new HashMap<>();
    private int nextMovimientoId = 1;

    public CuentaAhorrosService(SseService sseService) {
        this.sseService = sseService;
    }

    @PostConstruct
    private void initSingleton() {
        instance = this;
        CuentaListSubject.attach(sseService);
    }

    public static CuentaAhorrosService getInstance() { return instance; }

    // ---- helpers internos ----

    private List<Movimiento> movimientosDe(int numeroCuenta) {
        return movimMap.computeIfAbsent(numeroCuenta, k -> new ArrayList<>());
    }

    // ===================================================================
    // CRUD Cuentas
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
        payload.put("titular", cuenta.getTitular());
        sseService.notificar("CUENTA_ACTUALIZADA", payload);
        CuentaListSubject.notifyObservers(listar());
        return cuenta;
    }

    // ===================================================================
    // CRUD Movimientos
    // ===================================================================

    @Override
    public Movimiento agregarMovimiento(int numeroCuenta, Movimiento datos) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        if (datos.getMonto() <= 0)
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        String tipo = validarTipo(datos.getTipo());
        if ("DEBITO".equals(tipo) && cuenta.getSaldo() < datos.getMonto())
            throw new IllegalArgumentException("Saldo insuficiente. Saldo actual: $" + cuenta.getSaldo());

        Movimiento mov = Movimiento.builder()
                .id(nextMovimientoId++)
                .fechaMovimiento(LocalDateTime.now())
                .monto(datos.getMonto())
                .tipo(tipo)
                .numeroCuenta(numeroCuenta)
                .build();

        movimientosDe(numeroCuenta).add(mov);

        if ("CREDITO".equals(tipo)) cuenta.setSaldo(cuenta.getSaldo() + datos.getMonto());
        else                         cuenta.setSaldo(cuenta.getSaldo() - datos.getMonto());

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("tipo", tipo);
        payload.put("monto", mov.getMonto());
        sseService.notificar("MOVIMIENTO_AGREGADO", payload);
        return mov;
    }

    @Override
    public List<Movimiento> listarMovimientos(int numeroCuenta) {
        buscarPorNumero(numeroCuenta); // valida existencia
        return movimientosDe(numeroCuenta);
    }

    @Override
    public List<Movimiento> listarTodosMovimientos() {
        return movimMap.values().stream()
                .flatMap(List::stream)
                .sorted(Comparator.comparing(Movimiento::getFechaMovimiento).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Movimiento buscarMovimientoPorId(int numeroCuenta, int id) {
        return movimientosDe(numeroCuenta).stream()
                .filter(m -> m.getId() == id)
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                        "Movimiento no encontrado: id=" + id + " en cuenta=" + numeroCuenta));
    }

    @Override
    public Movimiento actualizarMovimiento(int numeroCuenta, int id, Movimiento datos) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        Movimiento mov = buscarMovimientoPorId(numeroCuenta, id);
        if (datos.getMonto() <= 0)
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        String tipoNuevo = (datos.getTipo() != null && !datos.getTipo().isBlank())
                ? validarTipo(datos.getTipo()) : mov.getTipo();

        // Revertir
        if ("CREDITO".equals(mov.getTipo())) cuenta.setSaldo(cuenta.getSaldo() - mov.getMonto());
        else                                  cuenta.setSaldo(cuenta.getSaldo() + mov.getMonto());

        // Validar
        if ("DEBITO".equals(tipoNuevo) && cuenta.getSaldo() < datos.getMonto()) {
            if ("CREDITO".equals(mov.getTipo())) cuenta.setSaldo(cuenta.getSaldo() + mov.getMonto());
            else                                  cuenta.setSaldo(cuenta.getSaldo() - mov.getMonto());
            throw new IllegalArgumentException("Saldo insuficiente para el monto actualizado.");
        }

        // Aplicar
        if ("CREDITO".equals(tipoNuevo)) cuenta.setSaldo(cuenta.getSaldo() + datos.getMonto());
        else                              cuenta.setSaldo(cuenta.getSaldo() - datos.getMonto());

        mov.setMonto(datos.getMonto());
        mov.setTipo(tipoNuevo);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("movimientoId", id);
        payload.put("tipo", tipoNuevo);
        payload.put("monto", datos.getMonto());
        sseService.notificar("MOVIMIENTO_ACTUALIZADO", payload);
        return mov;
    }

    @Override
    public boolean eliminarMovimiento(int numeroCuenta, int id) {
        CuentaAhorros cuenta = buscarPorNumero(numeroCuenta);
        Movimiento mov = buscarMovimientoPorId(numeroCuenta, id);
        if ("CREDITO".equals(mov.getTipo())) cuenta.setSaldo(cuenta.getSaldo() - mov.getMonto());
        else                                  cuenta.setSaldo(cuenta.getSaldo() + mov.getMonto());

        movimientosDe(numeroCuenta).remove(mov);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("movimientoId", id);
        sseService.notificar("MOVIMIENTO_ELIMINADO", payload);
        return true;
    }

    @Override
    public List<Movimiento> filtrarMovimientos(int numeroCuenta, String tipo,
                                               LocalDateTime desde, LocalDateTime hasta) {
        return listarTodosMovimientos().stream()
                .filter(m -> numeroCuenta == 0 || m.getNumeroCuenta() == numeroCuenta)
                .filter(m -> tipo == null || tipo.isBlank() || tipo.equalsIgnoreCase(m.getTipo()))
                .filter(m -> desde == null || !m.getFechaMovimiento().isBefore(desde))
                .filter(m -> hasta == null || !m.getFechaMovimiento().isAfter(hasta))
                .collect(Collectors.toList());
    }

    // ===================================================================
    private String validarTipo(String tipo) {
        if (tipo == null || tipo.isBlank())
            throw new IllegalArgumentException("El tipo de movimiento es requerido");
        String t = tipo.toUpperCase();
        if (!"CREDITO".equals(t) && !"DEBITO".equals(t))
            throw new IllegalArgumentException("Tipo inválido: '" + tipo + "'. Use CREDITO o DEBITO");
        return t;
    }
}
