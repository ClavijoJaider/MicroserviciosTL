package com.jacs.proyectomicroservicios.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA — tabla CUENTA_AHORROS (maestro).
 *
 * BACKEND UNIFICADO:
 *   Como ahora ambas entidades viven en el mismo contexto de persistencia,
 *   se declara la relacion @OneToMany bidireccional con MOVIMIENTO.
 *
 *   - fetch = LAZY  -> no se cargan movimientos a menos que el codigo
 *                      los pida explicitamente (evita N+1 y JSON gigante).
 *   - @JsonIgnore   -> evita la serializacion ciclica (CuentaAhorros tiene
 *                      lista de Movimiento, Movimiento referencia a CuentaAhorros).
 *   - mappedBy      -> la columna FK la sigue gestionando Movimiento.cuenta;
 *                      esta lista es de SOLO lectura desde el lado maestro.
 *
 *   El JPQL existente (subconsultas en ResumenCuentaDTO) sigue funcionando
 *   exactamente igual porque no depende de esta lista.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CUENTA_AHORROS")
public class CuentaAhorros {

    /** PK asignada por el cliente (no auto-incremento). */
    @Id
    @Column(name = "NUMERO_CUENTA", nullable = false)
    private int numeroCuenta;

    @Column(name = "TITULAR", nullable = false, length = 150)
    private String titular;

    @Column(name = "SALDO", nullable = false)
    private double saldo;

    /** "Activo" o "Inactivo" (baja logica). */
    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "FECHA_APERTURA")
    private LocalDateTime fechaApertura;

    @Column(name = "TASA_INTERES")
    private double tasaInteres;

    /**
     * Relacion @OneToMany bidireccional con MOVIMIENTO (BACKEND UNIFICADO).
     *
     * @JsonIgnore: para que el JSON de /cuentas no incluya la lista entera de
     *              movimientos (esos se piden via /movimientos/cuenta/{numero}).
     * LAZY: no se cargan hasta que se acceden explicitamente dentro de una
     *       transaccion.
     * cascade vacio + orphanRemoval=false: cada microcorriente gestiona su CRUD
     *       de manera independiente. Eliminar una cuenta NO borra sus movimientos
     *       (la baja es logica via estado=Inactivo).
     */
    @OneToMany(mappedBy = "cuenta", fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<Movimiento> movimientos = new ArrayList<>();

    // ---- Metodos de negocio ----

    /** Costo mensual de mantenimiento (0.5 % del saldo). */
    public double calcularCostoMensual() {
        return saldo * 0.005;
    }

    /** Rendimiento mensual estimado segun tasa de interes. */
    public double calcularRendimiento() {
        return saldo * tasaInteres;
    }
}
