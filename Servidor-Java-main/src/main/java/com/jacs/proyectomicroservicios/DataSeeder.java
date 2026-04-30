package com.jacs.proyectomicroservicios;

import com.jacs.proyectomicroservicios.model.CuentaAhorros;
import com.jacs.proyectomicroservicios.model.Movimiento;
import com.jacs.proyectomicroservicios.service.CuentaAhorrosService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final CuentaAhorrosService service;

    public DataSeeder(CuentaAhorrosService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
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

        // Movimientos detalle asociados a las cuentas maestro
        Movimiento m1 = new Movimiento();
        m1.setMonto(100000.0);
        m1.setTipo("CREDITO");
        service.agregarMovimiento(1001, m1);

        Movimiento m2 = new Movimiento();
        m2.setMonto(50000.0);
        m2.setTipo("DEBITO");
        service.agregarMovimiento(1001, m2);

        Movimiento m3 = new Movimiento();
        m3.setMonto(200000.0);
        m3.setTipo("CREDITO");
        service.agregarMovimiento(1002, m3);

        Movimiento m4 = new Movimiento();
        m4.setMonto(80000.0);
        m4.setTipo("DEBITO");
        service.agregarMovimiento(1003, m4);

        System.out.println("=== DataSeeder: " + 4 + " cuentas y 4 movimientos cargados ===");
    }
}
