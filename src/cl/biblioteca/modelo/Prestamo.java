package cl.biblioteca.modelo;

import java.time.LocalDate;

/**
 * Representa el préstamo de un libro a un estudiante.
 * Distingue la fecha límite y la fecha real de devolución.
 */
public class Prestamo {

    private int id;
    private Estudiante estudiante;
    private Libro libro;
    private LocalDate fechaPrestamo;
    private LocalDate fechaDevolucion;
    private LocalDate fechaDevolucionReal;
    private boolean devuelto;

    public Prestamo(int id, Estudiante estudiante, Libro libro,
                    LocalDate fechaPrestamo, LocalDate fechaDevolucion,
                    LocalDate fechaDevolucionReal, boolean devuelto) {

        this.id = id;
        this.estudiante = estudiante;
        this.libro = libro;
        this.fechaPrestamo = fechaPrestamo;
        this.fechaDevolucion = fechaDevolucion;
        this.fechaDevolucionReal = fechaDevolucionReal;
        this.devuelto = devuelto;
    }

    /**
     * Crea un préstamo pendiente cuyo ID será generado por MySQL.
     */
    public Prestamo(Estudiante estudiante, Libro libro,
                    LocalDate fechaPrestamo, LocalDate fechaDevolucion) {

        this(0, estudiante, libro, fechaPrestamo,
                fechaDevolucion, null, false);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public void setEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
    }

    public Libro getLibro() {
        return libro;
    }

    public void setLibro(Libro libro) {
        this.libro = libro;
    }

    public LocalDate getFechaPrestamo() {
        return fechaPrestamo;
    }

    public void setFechaPrestamo(LocalDate fechaPrestamo) {
        this.fechaPrestamo = fechaPrestamo;
    }

    public LocalDate getFechaDevolucion() {
        return fechaDevolucion;
    }

    public void setFechaDevolucion(LocalDate fechaDevolucion) {
        this.fechaDevolucion = fechaDevolucion;
    }

    public LocalDate getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(LocalDate fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public boolean isDevuelto() {
        return devuelto;
    }

    public void setDevuelto(boolean devuelto) {
        this.devuelto = devuelto;
    }

    /**
     * Comprueba si un préstamo pendiente superó su fecha límite.
     */
    public boolean estaAtrasado() {
        return !devuelto
                && fechaDevolucion != null
                && LocalDate.now().isAfter(fechaDevolucion);
    }

    /**
     * Comprueba si la devolución registrada ocurrió fuera de plazo.
     */
    public boolean fueDevueltoConAtraso() {
        return devuelto
                && fechaDevolucion != null
                && fechaDevolucionReal != null
                && fechaDevolucionReal.isAfter(fechaDevolucion);
    }
}