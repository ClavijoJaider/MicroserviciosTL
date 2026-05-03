package com.jacs.proyectomicroservicios;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.service.ICuentaAhorrosService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Carga datos de prueba al iniciar el Microservicio MAESTRO (CuentaAhorros, puerto 8080).
 *
 * SEPARACION ESTRICTA (Tercer Prototipo):
 *   Solo siembra registros de CUENTA_AHORROS.
 *   Los movimientos son responsabilidad exclusiva de MS-Movimiento (puerto 8081),
 *   que tiene su propio DataSeeder.
 *
 * IDEMPOTENTE: solo inserta datos si la BD está vacía.
 *   Con ddl-auto=update los datos persisten entre reinicios;
 *   este guard evita duplicados y errores de PK al reiniciar.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final ICuentaAhorrosService service;

    public DataSeeder(ICuentaAhorrosService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        // Guard: si ya hay cuentas en BD no duplicar datos
        if (!service.listar().isEmpty()) {
            System.out.println("=== DataSeeder [CuentaAhorros-8080]: BD ya tiene datos, omitiendo carga inicial ===");
            return;
        }

        // Cuentas maestro con al menos 5 atributos: int, double, String, LocalDateTime, double
        CuentaAhorros c1 = new CuentaAhorros();
        c1.setNumeroCuenta(1001);
        c1.setTitular("Carlos Gil");
        c1.setSaldo(500000.0);
        c1.setTasaInteres(0.045);
        c1.setFechaApertura(LocalDateTime.of(2024, 1, 15, 10, 0, 0));
        service.agregar(c1);

        CuentaAhorros c2 = new CuentaAhorros();
        c2.setNumeroCuenta(1002);
        c2.setTitular("Jaider Clavijo");
        c2.setSaldo(350000.0);
        c2.setTasaInteres(0.038);
        c2.setFechaApertura(LocalDateTime.of(2024, 3, 20, 14, 30, 0));
        service.agregar(c2);

        CuentaAhorros c3 = new CuentaAhorros();
        c3.setNumeroCuenta(1003);
        c3.setTitular("Santiago Lozano");
        c3.setSaldo(750000.0);
        c3.setTasaInteres(0.05);
        c3.setFechaApertura(LocalDateTime.of(2023, 11, 5, 8, 0, 0));
        service.agregar(c3);

        CuentaAhorros c4 = new CuentaAhorros();
        c4.setNumeroCuenta(1004);
        c4.setTitular("Ana María Torres");
        c4.setSaldo(1200000.0);
        c4.setTasaInteres(0.06);
        c4.setFechaApertura(LocalDateTime.of(2022, 6, 10, 9, 0, 0));
        service.agregar(c4);

        System.out.println("=== DataSeeder [CuentaAhorros-8080]: 4 cuentas cargadas ===");
    }
}
