package com.jacs.movimientoservice.client;

import com.jacs.movimientoservice.dto.CuentaAhorrosDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Cliente REST para comunicarse con MS-CuentaAhorros (puerto 8080).
 *
 * INTERCOMUNICACION entre microservicios (Tercer Prototipo):
 *   MS-Movimiento (este servicio, 8081) llama a MS-CuentaAhorros (8080)
 *   para validar que una cuenta exista y este activa antes de registrar
 *   un movimiento.
 *
 * Usa {@link RestClient} de Spring 6+/Spring Boot 4.x (reemplazo moderno
 * de RestTemplate con API fluida y soporte de tipos genericos).
 */
@Component
public class CuentaAhorrosClient {

    private final RestClient restClient;

    public CuentaAhorrosClient(@Value("${microservicio.cuentas.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * Consulta el microservicio maestro para obtener los datos de una cuenta.
     *
     * @param numeroCuenta numero de cuenta a buscar
     * @return Optional con los datos de la cuenta, o empty si no existe (404)
     */
    public Optional<CuentaAhorrosDTO> buscarCuenta(int numeroCuenta) {
        try {
            CuentaAhorrosDTO cuenta = restClient.get()
                    .uri("/cuentas/{numero}", numeroCuenta)
                    .retrieve()
                    .body(CuentaAhorrosDTO.class);
            return Optional.ofNullable(cuenta);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            // Si MS-CuentaAhorros no responde, lanzar excepcion descriptiva
            throw new IllegalStateException(
                "No se pudo conectar con MS-CuentaAhorros (8080): " + e.getMessage(), e);
        }
    }

    /**
     * Verifica que una cuenta exista y este activa.
     *
     * @param numeroCuenta numero de cuenta
     * @return datos de la cuenta
     * @throws java.util.NoSuchElementException si no existe
     * @throws IllegalArgumentException si la cuenta esta inactiva
     */
    public CuentaAhorrosDTO validarCuentaActiva(int numeroCuenta) {
        CuentaAhorrosDTO cuenta = buscarCuenta(numeroCuenta)
                .orElseThrow(() -> new java.util.NoSuchElementException(
                        "Cuenta " + numeroCuenta + " no encontrada en MS-CuentaAhorros"));

        if ("Inactivo".equalsIgnoreCase(cuenta.getEstado())) {
            throw new IllegalArgumentException(
                    "La cuenta " + numeroCuenta + " esta inactiva y no admite movimientos");
        }
        return cuenta;
    }
}
