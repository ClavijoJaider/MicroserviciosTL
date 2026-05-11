package com.jacs.proyectomicroservicios;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;
import com.jacs.proyectomicroservicios.repository.MovimientoRepository;
import com.jacs.proyectomicroservicios.service.ICuentaAhorrosService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Carga datos de prueba al iniciar el BACKEND UNIFICADO (puerto 8080).
 *
 * Tras la fusion siembra tanto CUENTA_AHORROS (via servicio) como
 * MOVIMIENTO (via repositorio, en una sola unidad de persistencia).
 *
 * IDEMPOTENTE: cada bloque verifica si su tabla esta vacia antes de insertar.
 *   Asi cuentas y movimientos pueden recargar de manera independiente
 *   (por ejemplo, si solo MOVIMIENTO se trunca a mano).
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ICuentaAhorrosService cuentaService;
    private final MovimientoRepository  movRepo;

    public DataSeeder(ICuentaAhorrosService cuentaService,
                      MovimientoRepository movRepo) {
        this.cuentaService = cuentaService;
        this.movRepo       = movRepo;
    }

    @Override
    public void run(String... args) {
        seedCuentas();
        seedMovimientos();
    }

    // ------------------------------------------------------------------
    // Cuentas
    // ------------------------------------------------------------------

    private void seedCuentas() {
        if (!cuentaService.listar().isEmpty()) {
            System.out.println("=== DataSeeder: cuentas ya cargadas, omitiendo. ===");
            return;
        }

        CuentaAhorros c1 = new CuentaAhorros();
        c1.setNumeroCuenta(1001);
        c1.setTitular("Carlos Gil");
        c1.setSaldo(500000.0);
        c1.setTasaInteres(0.045);
        c1.setFechaApertura(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        cuentaService.agregar(c1);

        CuentaAhorros c2 = new CuentaAhorros();
        c2.setNumeroCuenta(1002);
        c2.setTitular("Jaider Clavijo");
        c2.setSaldo(350000.0);
        c2.setTasaInteres(0.038);
        c2.setFechaApertura(LocalDateTime.of(2024, 3, 20, 14, 30, 0));
        cuentaService.agregar(c2);

        CuentaAhorros c3 = new CuentaAhorros();
        c3.setNumeroCuenta(1003);
        c3.setTitular("Santiago Lozano");
        c3.setSaldo(750000.0);
        c3.setTasaInteres(0.05);
        c3.setFechaApertura(LocalDateTime.of(2023, 11, 5, 8, 0, 0));
        cuentaService.agregar(c3);

        CuentaAhorros c4 = new CuentaAhorros();
        c4.setNumeroCuenta(1004);
        c4.setTitular("Ana Maria Torres");
        c4.setSaldo(1200000.0);
        c4.setTasaInteres(0.06);
        c4.setFechaApertura(LocalDateTime.of(2022, 6, 10, 9, 0, 0));
        cuentaService.agregar(c4);

        System.out.println("=== DataSeeder: 4 cuentas cargadas ===");
    }

    // ------------------------------------------------------------------
    // Movimientos - se usa el repositorio directamente para no disparar
    // la validacion del servicio (no haria falta ahora, pero mantiene la
    // siembra idempotente y trivialmente reproducible).
    // ------------------------------------------------------------------

    private void seedMovimientos() {
        if (movRepo.count() > 0) {
            System.out.println("=== DataSeeder: movimientos ya cargados, omitiendo. ===");
            return;
        }

        Movimiento m1 = new Movimiento();
        m1.setNumeroCuenta(1001);
        m1.setTipo("CREDITO");
        m1.setMonto(100000.0);
        m1.setFechaMovimiento(LocalDateTime.of(2024, 2, 1, 9, 0, 0));
        movRepo.save(m1);

        Movimiento m2 = new Movimiento();
        m2.setNumeroCuenta(1001);
        m2.setTipo("DEBITO");
        m2.setMonto(50000.0);
        m2.setFechaMovimiento(LocalDateTime.of(2024, 2, 15, 11, 30, 0));
        movRepo.save(m2);

        Movimiento m3 = new Movimiento();
        m3.setNumeroCuenta(1002);
        m3.setTipo("CREDITO");
        m3.setMonto(200000.0);
        m3.setFechaMovimiento(LocalDateTime.of(2024, 4, 5, 10, 0, 0));
        movRepo.save(m3);

        Movimiento m4 = new Movimiento();
        m4.setNumeroCuenta(1003);
        m4.setTipo("DEBITO");
        m4.setMonto(80000.0);
        m4.setFechaMovimiento(LocalDateTime.of(2024, 5, 20, 16, 0, 0));
        movRepo.save(m4);

        System.out.println("=== DataSeeder: 4 movimientos cargados ===");
    }
}
