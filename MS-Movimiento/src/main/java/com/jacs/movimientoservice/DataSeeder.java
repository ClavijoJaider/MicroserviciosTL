package com.jacs.movimientoservice;

import com.jacs.movimientoservice.model.Movimiento;
import com.jacs.movimientoservice.repository.MovimientoRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Carga datos de prueba al iniciar el Microservicio DETALLE (Movimiento, puerto 8081).
 *
 * SEPARACION ESTRICTA (Tercer Prototipo):
 *   Solo siembra registros de MOVIMIENTO.
 *   Las cuentas de ahorro son responsabilidad exclusiva de MS-CuentaAhorros (8080).
 *
 * IMPORTANTE — por qué se inyecta el repositorio directamente en lugar del servicio:
 *   {@link com.jacs.movimientoservice.service.MovimientoService#agregar} llama a
 *   {@code CuentaAhorrosClient.validarCuentaActiva()} que requiere que MS-CuentaAhorros
 *   esté activo y haya sembrado sus cuentas primero.  Al usar el repositorio
 *   directamente evitamos esa dependencia de red en el arranque inicial y los datos
 *   de prueba se insertan sin importar el orden de inicio de los microservicios.
 *
 * IDEMPOTENTE: solo inserta datos si la tabla MOVIMIENTO está vacía.
 *   Con ddl-auto=none los datos persisten entre reinicios;
 *   este guard evita duplicados y errores de PK al reiniciar.
 *
 * PRECONDICION:
 *   Las cuentas 1001, 1002, 1003 deben existir en CUENTA_AHORROS (sembradas por
 *   MS-CuentaAhorros) para que la FK no viole la restricción referencial.
 *   Iniciar MS-CuentaAhorros (8080) ANTES que este microservicio.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final MovimientoRepository repo;

    public DataSeeder(MovimientoRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        // Guard: si ya hay movimientos en BD no duplicar datos
        if (repo.count() > 0) {
            System.out.println("=== DataSeeder [Movimiento-8081]: BD ya tiene datos, omitiendo carga inicial ===");
            return;
        }

        // m1 — Crédito en cuenta 1001
        Movimiento m1 = new Movimiento();
        m1.setNumeroCuenta(1001);
        m1.setTipo("CREDITO");
        m1.setMonto(100000.0);
        m1.setFechaMovimiento(LocalDateTime.of(2024, 2, 1, 9, 0, 0));
        repo.save(m1);

        // m2 — Débito en cuenta 1001
        Movimiento m2 = new Movimiento();
        m2.setNumeroCuenta(1001);
        m2.setTipo("DEBITO");
        m2.setMonto(50000.0);
        m2.setFechaMovimiento(LocalDateTime.of(2024, 2, 15, 11, 30, 0));
        repo.save(m2);

        // m3 — Crédito en cuenta 1002
        Movimiento m3 = new Movimiento();
        m3.setNumeroCuenta(1002);
        m3.setTipo("CREDITO");
        m3.setMonto(200000.0);
        m3.setFechaMovimiento(LocalDateTime.of(2024, 4, 5, 10, 0, 0));
        repo.save(m3);

        // m4 — Débito en cuenta 1003
        Movimiento m4 = new Movimiento();
        m4.setNumeroCuenta(1003);
        m4.setTipo("DEBITO");
        m4.setMonto(80000.0);
        m4.setFechaMovimiento(LocalDateTime.of(2024, 5, 20, 16, 0, 0));
        repo.save(m4);

        System.out.println("=== DataSeeder [Movimiento-8081]: 4 movimientos cargados ===");
    }
}
