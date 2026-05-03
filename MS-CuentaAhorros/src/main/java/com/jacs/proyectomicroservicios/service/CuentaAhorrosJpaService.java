package com.jacs.proyectomicroservicios.service;

import com.jacs.proyectomicroservicios.dto.ResumenCuentaDTO;
import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.observer.CuentaListSubject;
import com.jacs.proyectomicroservicios.observer.SseService;
import com.jacs.proyectomicroservicios.repository.CuentaAhorrosRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementación JPA del servicio MAESTRO — activa por defecto (@Primary).
 *
 * SEPARACION ESTRICTA (Tercer Prototipo):
 *   Este servicio gestiona EXCLUSIVAMENTE la tabla CUENTA_AHORROS.
 *   No expone ningún endpoint ni lógica de MOVIMIENTO.
 *   Los movimientos son responsabilidad exclusiva de MS-Movimiento (8081).
 *
 * EXCEPCION PERMITIDA — consulta personalizada #2 (resumen):
 *   {@link #obtenerResumen} usa JPQL con subconsultas correlacionadas sobre
 *   la tabla MOVIMIENTO para calcular totalMovimientos y totalCreditos.
 *   Esto es solo una LECTURA agregada; NO expone CRUD de movimientos.
 *
 * PATRONES:
 *   Singleton  → Spring gestiona esta clase; referencia estática para acceso legacy.
 *   Observer   → Notifica a SseService en cada mutación de CuentaAhorros.
 */
@Service
@Primary
@Transactional
public class CuentaAhorrosJpaService implements ICuentaAhorrosService {

    private static CuentaAhorrosJpaService instance;

    private final CuentaAhorrosRepository cuentaRepo;
    private final SseService              sseService;

    public CuentaAhorrosJpaService(CuentaAhorrosRepository cuentaRepo,
                                   SseService sseService) {
        this.cuentaRepo = cuentaRepo;
        this.sseService = sseService;
    }

    @PostConstruct
    private void init() {
        instance = this;
        CuentaListSubject.attach(sseService);
    }

    public static CuentaAhorrosJpaService getInstance() { return instance; }

    // ===================================================================
    // CRUD CuentaAhorros — única responsabilidad de este microservicio
    // ===================================================================

    @Override
    public void agregar(CuentaAhorros cuenta) {
        validarCuenta(cuenta);
        if (cuentaRepo.existsById(cuenta.getNumeroCuenta()))
            throw new IllegalArgumentException(
                    "Ya existe una cuenta con el número: " + cuenta.getNumeroCuenta());

        cuenta.setEstado("Activo");
        if (cuenta.getFechaApertura() == null)
            cuenta.setFechaApertura(LocalDateTime.now());

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
        return cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaAhorros> listar() {
        return cuentaRepo.findAllByOrderByNumeroCuentaAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaAhorros> listarConFiltro(String titular, String estado) {
        String t = (titular != null && titular.isBlank()) ? null : titular;
        String e = (estado  != null && estado.isBlank())  ? null : estado;
        return cuentaRepo.filtrar(t, e);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaAhorros> buscarPorTitular(String titular) {
        return cuentaRepo.findByTitularContainingIgnoreCase(titular);
    }

    @Override
    public CuentaAhorros actualizar(int numeroCuenta, CuentaAhorros datos) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));

        if (datos.getTitular() != null && !datos.getTitular().isBlank())
            cuenta.setTitular(datos.getTitular());
        if (datos.getSaldo() >= 200_000.0)
            cuenta.setSaldo(datos.getSaldo());
        if (datos.getTasaInteres() > 0)
            cuenta.setTasaInteres(datos.getTasaInteres());
        if (datos.getEstado() != null && !datos.getEstado().isBlank())
            cuenta.setEstado(datos.getEstado());

        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        payload.put("titular",      cuenta.getTitular());
        sseService.notificar("CUENTA_ACTUALIZADA", payload);
        CuentaListSubject.notifyObservers(listar());
        return cuenta;
    }

    @Override
    public boolean eliminar(int numeroCuenta) {
        CuentaAhorros cuenta = cuentaRepo.findById(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));

        if (!"Activo".equalsIgnoreCase(cuenta.getEstado())) return false;

        cuenta.setEstado("Inactivo");
        cuentaRepo.save(cuenta);

        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroCuenta", numeroCuenta);
        sseService.notificar("CUENTA_ELIMINADA", payload);
        CuentaListSubject.notifyObservers(listar());
        return true;
    }

    // ===================================================================
    // Consulta personalizada #2 — TERCER PROTOTIPO
    // Datos del maestro + dos campos agregados del detalle (solo lectura).
    // Endpoint: GET /cuentas/{numero}/resumen
    // ===================================================================

    /**
     * Devuelve {@link ResumenCuentaDTO} con datos de CUENTA_AHORROS
     * más totalMovimientos (COUNT) y totalCreditos (SUM) calculados
     * mediante subconsultas JPQL sobre MOVIMIENTO.
     *
     * Solo lectura — NO expone CRUD de movimientos.
     */
    public ResumenCuentaDTO obtenerResumen(int numeroCuenta) {
        return cuentaRepo.findResumenPorNumero(numeroCuenta)
                .orElseThrow(() -> new NoSuchElementException("Cuenta no encontrada: " + numeroCuenta));
    }

    // ===================================================================
    // Validaciones internas
    // ===================================================================

    private void validarCuenta(CuentaAhorros cuenta) {
        if (cuenta == null)
            throw new IllegalArgumentException("La cuenta no puede ser nula");
        if (cuenta.getTitular() == null || cuenta.getTitular().isBlank())
            throw new IllegalArgumentException("El titular no puede estar vacío");
        if (cuenta.getNumeroCuenta() <= 0)
            throw new IllegalArgumentException("El número de cuenta debe ser mayor a cero");
        if (cuenta.getSaldo() < 200_000.0)
            throw new IllegalArgumentException("El saldo inicial mínimo es $200.000");
    }
}
