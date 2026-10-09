package cl.biblioteca.util;

import java.time.LocalDate;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Centraliza las validaciones y la normalización de los datos ingresados.
 */
public final class Validador {

    private static final Pattern PATRON_RUT =
            Pattern.compile("[1-9][0-9]{0,7}-[0-9K]");

    private static final Pattern PATRON_CORREO =
            Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private static final Pattern PATRON_ISBN =
            Pattern.compile("(?:[0-9]{9}[0-9X]|[0-9]{13})");

    private Validador() {
    }

    public static String textoObligatorio(String valor, String campo, int longitudMaxima) {
        String texto = valor == null ? "" : valor.strip();
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("El campo \"" + campo + "\" es obligatorio.");
        }

        comprobarLongitud(texto, campo, longitudMaxima);
        return texto;
    }

    /**
     * Comprueba el formato y devuelve el RUT sin puntos ni espacios.
     */
    public static String validarRut(String valor) {
        String rut = valor == null ? "" : valor
                .replace(".", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        rut = textoObligatorio(rut, "RUT", 12);
        if (!PATRON_RUT.matcher(rut).matches()) {
            throw new IllegalArgumentException("El RUT debe tener el formato 12345678-9.");
        }

        return rut;
    }

    public static String validarCorreo(String valor) {
        String correo = textoObligatorio(valor, "Correo", 100);
        if (!PATRON_CORREO.matcher(correo).matches()) {
            throw new IllegalArgumentException("El correo debe tener el formato nombre@dominio.cl.");
        }

        return correo;
    }

    public static String validarIsbn(String valor) {
        String isbn = valor == null ? "" : valor
                .replace("-", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        isbn = textoObligatorio(isbn, "ISBN", 20);
        if (!PATRON_ISBN.matcher(isbn).matches()) {
            throw new IllegalArgumentException(
                    "El ISBN debe tener 10 dígitos (el último puede ser X) o 13 dígitos."
            );
        }

        return isbn;
    }

    /**
     * Conserva la clave exacta, incluidos los espacios que acompañan otros caracteres.
     */
    public static String validarContrasena(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("El campo \"Contraseña\" es obligatorio.");
        }

        comprobarLongitud(valor, "Contraseña", 100);
        return valor;
    }

    public static int validarStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo.");
        }

        return stock;
    }

    public static int validarStock(String valor) {
        String texto = textoObligatorio(valor, "Stock", 100);
        try {
            return validarStock(Integer.parseInt(texto));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "El stock debe ser un número entero entre 0 y " + Integer.MAX_VALUE + "."
            );
        }
    }

    public static void validarSeleccion(Object valor, String campo) {
        if (valor == null) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una opción en \"" + campo + "\"."
            );
        }
    }

    public static int validarId(int id, String campo) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una opción válida en \"" + campo + "\"."
            );
        }

        return id;
    }

    public static LocalDate validarFecha(LocalDate fecha, String campo) {
        if (fecha == null) {
            throw new IllegalArgumentException("El campo \"" + campo + "\" es obligatorio.");
        }

        return fecha;
    }

    public static void validarFechasPrestamo(LocalDate fechaPrestamo, LocalDate fechaLimite) {
        validarFecha(fechaPrestamo, "Fecha del préstamo");
        validarFecha(fechaLimite, "Fecha límite");

        if (fechaLimite.isBefore(fechaPrestamo)) {
            throw new IllegalArgumentException(
                    "La fecha límite no puede ser anterior a la fecha del préstamo."
            );
        }
    }

    public static void validarFechaDevolucion(LocalDate fechaPrestamo, LocalDate fechaReal) {
        validarFecha(fechaPrestamo, "Fecha del préstamo");
        validarFecha(fechaReal, "Fecha real de devolución");

        if (fechaReal.isBefore(fechaPrestamo)) {
            throw new IllegalArgumentException(
                    "La fecha real de devolución no puede ser anterior a la fecha del préstamo."
            );
        }
    }

    private static void comprobarLongitud(String valor, String campo, int longitudMaxima) {
        if (longitudMaxima <= 0) {
            throw new IllegalArgumentException("La longitud máxima debe ser mayor que cero.");
        }

        if (valor.codePointCount(0, valor.length()) > longitudMaxima) {
            throw new IllegalArgumentException(
                    "El campo \"" + campo + "\" permite hasta " + longitudMaxima + " caracteres."
            );
        }
    }
}