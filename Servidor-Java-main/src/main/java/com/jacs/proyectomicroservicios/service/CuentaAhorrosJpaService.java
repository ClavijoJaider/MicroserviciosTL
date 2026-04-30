package com.jacs.proyectomicroservicios.service;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;
import com.jacs.proyectomicroservicios.observer.CuentaListSubject;
import com.jacs.proyectomicroservicios.observer.SseService;
import com.jacs.proyectomicroservicios.repository.CuentaAhorrosRepository;
import com.jacs.proyectomicroservicios.repository.MovimientoRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio JPA — implementación PRINCIPAL de ICuentaAhorrosService.
 *
 * Usa Spring Data JPA para persistir todas las operaciones en la base de datos:
 *   - H2 embebida (perfil por defecto — desarrollo sin instalar nada)
 *   - Oracle XE     (perfil "oracle" — configurado en application-oracle.properties)
 *
 * PATRONES IMPLEMENTADOS:
 *   Singleton  → Spring gestiona esta clase como singleton (@Service); la referencia
 *                estática permite acceso legacy vía getInstance().
 *   Builder    → CuentaAhorros.builder() y Movimiento.builder() (Lombok @Builder).
 *   Observer   → Notifica a SseService en cada cambio; SseService envía eventos SSE
 *                a todos los clientes suscritos (Observer en servidor).
 *
 * @Primary garantiza que Spring inyecte este servicio en lugar del servicio en memoria
 * cuando ambos están en el classpath.
 */
@Service
@Primary
@Transactional
public class CuentaAhorrosJpaService implements ICuentaAhorrosService {

    // Singleton estático para acceso legacy (ej. DataSeeder, código sin inyección)
    private static CuentaAhorrosJpaService instance;

    private final CuentaAhorrosRepository cuentaRepo;
    private final MovimientoRepository    movimientoRepo;
    private final SseService              sseService;

    public CuentaAhorrosJpaService(CuentaAhorrosRepository cuentaRepo,
                                   MovimientoRepository    movimientoRepo,
                                   SseService              sseService) {
        this.cuentaRepo    = cuentaRepo;
        this.movimientoRepo = movimientoRepo;
        this.sseService    = sseService;
    }

    @PostConstruct
    private void init() {
        instance = this;
        CuentaListSubject.attach(sseService);
    }

    public static CuentaAhorrosJpaService getInstance() {
        return instance;
    }

    // ===================================================================
    // CRUD Cuentas de Ahorros
    // ===================================================================

    @Override
    public void agregar(CuentaAhorros cuenta) {
        validarCuenta(cuenta);

        if (cuentaRepo.existsById(cuenta.getNumeroCuenta())) {
            throw new IllegalArgumentException(
                    "Ya existe una cuenta con el número: " + cuenta.getNumeroCuenta());
        }

        cuenta.setEstado("Activo");
        if (cuenta.getFechaApertura() == null) {
            cuenta.setFechaApertura(LocalDateTime.now());
        }

        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", cuenta.getNumeroCuenta());
        payload.put("titular",      cuenta.getTitular());
        sseService.notificar("CUENTA_CREADA", payload);
        CuentaListSubject.notifyObservers(listar());
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaAhorros buscarPorNumero(int numeroCuenta) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));
        // Carga movimientos en el campo @Transient
        cuenta.setMovimientos(movimientoRepo
                .findByNumeroCuentaOrderByFechaMovimientoDesc(numeroCuenta));
        return cuenta;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaAhorros> listar() {
        List<CuentaAhorros> cuentas = cuentaRepo.findAllByOrderByNumeroCuentaAsc();
        cuentas.forEach(c -> c.setMovimientos(
                movimientoRepo.findByNumeroCuentaOrderByFechaMovimientoDesc(c.getNumeroCuenta())));
        return cuentas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaAhorros> listarConFiltro(String titular, String estado) {
        String t = (titular != null && titular.isBlank()) ? null : titular;
        String e = (estado  != null && estado.isBlank())  ? null : estado;
        List<CuentaAhorros> cuentas = cuentaRepo.filtrar(t, e);
        cuentas.forEach(c -> c.setMovimientos(
                movimientoRepo.findByNumeroCuentaOrderByFechaMovimientoDesc(c.getNumeroCuenta())));
        return cuentas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaAhorros> buscarPorTitular(String titular) {
        List<CuentaAhorros> cuentas = cuentaRepo.findByTitularContainingIgnoreCase(titular);
        cuentas.forEach(c -> c.setMovimientos(
                movimientoRepo.findByNumeroCuentaOrderByFechaMovimientoDesc(c.getNumeroCuenta())));
        return cuentas;
    }

    @Override
    public CuentaAhorros actualizar(int numeroCuenta, CuentaAhorros datos) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));

        if (datos.getTitular() != null && !datos.getTitular().isBlank()) {
            cuenta.setTitular(datos.getTitular());
        }
        if (datos.getSaldo() >= 200_000.0) {
            cuenta.setSaldo(datos.getSaldo());
        }
        if (datos.getTasaInteres() > 0) {
            cuenta.setTasaInteres(datos.getTasaInteres());
        }
        if (datos.getEstado() != null && !datos.getEstado().isBlank()) {
            cuenta.setEstado(datos.getEstado());
        }

        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("titular",      cuenta.getTitular());
        sseService.notificar("CUENTA_ACTUALIZADA", payload);
        CuentaListSubject.notifyObservers(listar());

        cuenta.setMovimientos(movimientoRepo
                .findByNumeroCuentaOrderByFechaMovimientoDesc(numeroCuenta));
        return cuenta;
    }

    @Override
    public boolean eliminar(int numeroCuenta) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));

        if (!"Activo".equalsIgnoreCase(cuenta.getEstado())) {
            return false; // ya inactiva
        }

        cuenta.setEstado("Inactivo");
        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        sseService.notificar("CUENTA_ELIMINADA", payload);
        CuentaListSubject.notifyObservers(listar());
        return true;
    }

    // ===================================================================
    // CRUD Movimientos
    // ===================================================================

    @Override
    public Movimiento agregarMovimiento(int numeroCuenta, Movimiento datos) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));

        if (!"Activo".equalsIgnoreCase(cuenta.getEstado())) {
            throw new IllegalArgumentException("La cuenta está inactiva y no puede operar");
        }
        if (datos.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }

        String tipo = validarTipo(datos.getTipo());

        if ("DEBITO".equals(tipo) && cuenta.getSaldo() < datos.getMonto()) {
            throw new IllegalArgumentException(
                    "Saldo insuficiente. Saldo actual: $" + cuenta.getSaldo());
        }

        Movimiento movimiento = Movimiento.builder()
                .fechaMovimiento(LocalDateTime.now())
                .monto(datos.getMonto())
                .tipo(tipo)
                .numeroCuenta(numeroCuenta)
                .build();

        movimientoRepo.save(movimiento);

        // Actualizar saldo
        if ("CREDITO".equals(tipo)) {
            cuenta.setSaldo(cuenta.getSaldo() + datos.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() - datos.getMonto());
        }
        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("tipo",         tipo);
        payload.put("monto",        movimiento.getMonto());
        sseService.notificar("MOVIMIENTO_AGREGADO", payload);

        return movimiento;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movimiento> listarMovimientos(int numeroCuenta) {
        if (!cuentaRepo.existsById(numeroCuenta)) {
            throw new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta);
        }
        return movimientoRepo.findByNumeroCuentaOrderByFechaMovimientoDesc(numeroCuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movimiento> listarTodosMovimientos() {
        return movimientoRepo.findAllByOrderByFechaMovimientoDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Movimiento buscarMovimientoPorId(int numeroCuenta, int id) {
        Movimiento m = movimientoRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Movimiento no encontrado: id=" + id));
        if (m.getNumeroCuenta() != numeroCuenta) {
            throw new NoSuchElementException(
                    "El movimiento " + id + " no pertenece a la cuenta " + numeroCuenta);
        }
        return m;
    }

    @Override
    public Movimiento actualizarMovimiento(int numeroCuenta, int id, Movimiento datos) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));
        Movimiento movimiento = buscarMovimientoPorId(numeroCuenta, id);

        if (datos.getMonto() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        String tipoNuevo = (datos.getTipo() != null && !datos.getTipo().isBlank())
                ? validarTipo(datos.getTipo())
                : movimiento.getTipo();

        // Revertir efecto anterior sobre el saldo
        if ("CREDITO".equals(movimiento.getTipo())) {
            cuenta.setSaldo(cuenta.getSaldo() - movimiento.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() + movimiento.getMonto());
        }

        // Validar saldo para DEBITO nuevo
        if ("DEBITO".equals(tipoNuevo) && cuenta.getSaldo() < datos.getMonto()) {
            // Restaurar saldo
            if ("CREDITO".equals(movimiento.getTipo())) {
                cuenta.setSaldo(cuenta.getSaldo() + movimiento.getMonto());
            } else {
                cuenta.setSaldo(cuenta.getSaldo() - movimiento.getMonto());
            }
            throw new IllegalArgumentException(
                    "Saldo insuficiente para el monto actualizado. Saldo: $" + cuenta.getSaldo());
        }

        // Aplicar nuevo efecto
        if ("CREDITO".equals(tipoNuevo)) {
            cuenta.setSaldo(cuenta.getSaldo() + datos.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() - datos.getMonto());
        }

        movimiento.setMonto(datos.getMonto());
        movimiento.setTipo(tipoNuevo);

        movimientoRepo.save(movimiento);
        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("movimientoId", id);
        payload.put("tipo",         tipoNuevo);
        payload.put("monto",        datos.getMonto());
        sseService.notificar("MOVIMIENTO_ACTUALIZADO", payload);

        return movimiento;
    }

    @Override
    public boolean eliminarMovimiento(int numeroCuenta, int id) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));
        Movimiento movimiento = buscarMovimientoPorId(numeroCuenta, id);

        // Revertir efecto en el saldo
        if ("CREDITO".equals(movimiento.getTipo())) {
            cuenta.setSaldo(cuenta.getSaldo() - movimiento.getMonto());
        } else {
            cuenta.setSaldo(cuenta.getSaldo() + movimiento.getMonto());
        }

        movimientoRepo.delete(movimiento);
        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("movimientoId", id);
        sseService.notificar("MOVIMIENTO_ELIMINADO", payload);

        return true;
    }

    // ===================================================================
    // Filtros avanzados de movimientos
    // ===================================================================

    /**
     * Filtra movimientos de una cuenta por tipo y/o rango de fechas.
     *
     * @param numeroCuenta número de cuenta (0 = todas las cuentas)
     * @param tipo         "CREDITO", "DEBITO" o null para todos
     * @param desde        fecha inicial (null = sin límite inferior)
     * @param hasta        fecha final   (null = sin límite superior)
     */
    public List<Movimiento> filtrarMovimientos(int numeroCuenta, String tipo,
                                               LocalDateTime desde, LocalDateTime hasta) {
        String tipoFiltro = (tipo != null && !tipo.isBlank()) ? tipo.toUpperCase() : null;
        return movimientoRepo.filtrar(numeroCuenta, tipoFiltro, desde, hasta);
    }

    // ===================================================================
    // Validaciones internas
    // ===================================================================

    private void validarCuenta(CuentaAhorros cuenta) {
        if (cuenta == null) {
            throw new IllegalArgumentException("La cuenta no puede ser nula");
        }
        if (cuenta.getTitular() == null || cuenta.getTitular().isBlank()) {
            throw new IllegalArgumentException("El titular no puede estar vacío");
        }
        if (cuenta.getNumeroCuenta() <= 0) {
            throw new IllegalArgumentException("El número de cuenta debe ser mayor a cero");
        }
        if (cuenta.getSaldo() < 200_000.0) {
            throw new IllegalArgumentException("El saldo inicial mínimo es $200.000");
        }
    }

    private String validarTipo(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("El tipo de movimiento es requerido");
        }
        String t = tipo.toUpperCase();
        if (!"CREDITO".equals(t) && !"DEBITO".equals(t)) {
            throw new IllegalArgumentException(
                    "Tipo inválido: '" + tipo + "'. Use CREDITO o DEBITO");
        }
        return t;
    }
}
