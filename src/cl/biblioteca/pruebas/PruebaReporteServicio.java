package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.PrestamoDAO;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.LibroMasPrestado;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.AutenticacionServicio;
import cl.biblioteca.servicio.ReporteServicio;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Comprueba los reportes y sus permisos, revirtiendo los préstamos de prueba.
 */
public final class PruebaReporteServicio {

    private static final String ERROR_PRUEBA = "PR001";

    private PruebaReporteServicio() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        int resultado = 0;

        System.out.println("PRUEBA DE REPORTES Y PERMISOS");

        try {
            AutenticacionServicio autenticacion = new AutenticacionServicio();
            SesionUsuario bibliotecaria = autenticacion.iniciarSesion("12345678-9", "clave123");
            SesionUsuario carlos = autenticacion.iniciarSesion("98765432-1", "clave123");
            SesionUsuario maria = autenticacion.iniciarSesion("11222333-4", "clave123");

            ReporteServicio reportesBibliotecaria = new ReporteServicio(bibliotecaria);
            ReporteServicio reportesCarlos = new ReporteServicio(carlos);
            ReporteServicio reportesMaria = new ReporteServicio(maria);

            List<Prestamo> todos = new PrestamoDAO().listar();
            comprobar(!todos.isEmpty()
                            && todos.stream().anyMatch(Prestamo::isDevuelto)
                            && todos.stream().anyMatch(p -> !p.isDevuelto()),
                    "Se necesitan préstamos pendientes y devueltos para comprobar los reportes.");

            List<LibroMasPrestado> ranking = reportesBibliotecaria.listarLibrosMasPrestados();
            comprobarRanking(ranking, todos);
            System.out.println("Ranking histórico, cantidades y préstamos devueltos incluidos: OK");

            comprobarCantidadesDistintas(reportesBibliotecaria, ranking, todos);
            System.out.println("Ranking con cantidades distintas y reversión de la prueba: OK");

            List<Prestamo> historialCarlos = filtrarPorEstudiante(todos, carlos.getIdEstudiante());
            List<Prestamo> historialMaria = filtrarPorEstudiante(todos, maria.getIdEstudiante());
            comprobar(!historialCarlos.isEmpty() && !historialMaria.isEmpty(),
                    "Carlos y María deben tener préstamos para comprobar sus historiales.");

            comprobarMismosPrestamos(
                    reportesBibliotecaria.listarHistorialPorEstudiante(carlos.getIdEstudiante()),
                    historialCarlos, "El historial administrativo de Carlos no es correcto.");
            comprobarMismosPrestamos(
                    reportesBibliotecaria.listarHistorialPorEstudiante(maria.getIdEstudiante()),
                    historialMaria, "El historial administrativo de María no es correcto.");
            comprobarMismosPrestamos(
                    reportesCarlos.listarHistorialPorEstudiante(carlos.getIdEstudiante()),
                    historialCarlos, "El historial propio de Carlos no es correcto.");
            comprobarMismosPrestamos(
                    reportesMaria.listarHistorialPorEstudiante(maria.getIdEstudiante()),
                    historialMaria, "El historial propio de María no es correcto.");
            System.out.println("Historial administrativo y propio de cada estudiante: OK");

            List<Prestamo> pendientes = todos.stream().filter(p -> !p.isDevuelto()).toList();
            List<Prestamo> pendientesCarlos = filtrarPorEstudiante(pendientes, carlos.getIdEstudiante());
            List<Prestamo> pendientesMaria = filtrarPorEstudiante(pendientes, maria.getIdEstudiante());

            comprobarMismosPrestamos(reportesBibliotecaria.listarLibrosEnPrestamo(), pendientes,
                    "El reporte general de libros en préstamo no es correcto.");
            comprobarMismosPrestamos(reportesCarlos.listarLibrosEnPrestamo(), pendientesCarlos,
                    "El reporte de Carlos incluye préstamos ajenos o devueltos.");
            comprobarMismosPrestamos(reportesMaria.listarLibrosEnPrestamo(), pendientesMaria,
                    "El reporte de María incluye préstamos ajenos o devueltos.");
            System.out.println("Libros en préstamo y exclusión de devoluciones: OK");

            String permisoAdministrativo = "Esta operación requiere una cuenta de bibliotecario.";
            String permisoPropio = "Solo puede acceder a sus propios préstamos.";

            comprobarRechazo(() -> reportesCarlos.listarLibrosMasPrestados(), permisoAdministrativo);
            comprobarRechazo(() -> reportesMaria.listarLibrosMasPrestados(), permisoAdministrativo);
            comprobarRechazo(
                    () -> reportesCarlos.listarHistorialPorEstudiante(maria.getIdEstudiante()),
                    permisoPropio);
            comprobarRechazo(
                    () -> reportesMaria.listarHistorialPorEstudiante(carlos.getIdEstudiante()),
                    permisoPropio);
            System.out.println("Rechazo del ranking administrativo y de historiales ajenos: OK");

            comprobarRechazo(() -> new ReporteServicio(null),
                    "Debe iniciar sesión para utilizar el sistema.");
            comprobarRechazo(() -> reportesBibliotecaria.listarHistorialPorEstudiante(0),
                    "Debe seleccionar una opción válida en \"Estudiante\".");
            System.out.println("Sesión obligatoria e identificación válida: OK");

            baseDatos.cerrar();
            List<LibroMasPrestado> rankingReabierto = reportesBibliotecaria.listarLibrosMasPrestados();

            comprobarRanking(rankingReabierto, todos);
            comprobar(ranking.stream().map(LibroMasPrestado::getIdLibro).toList().equals(
                            rankingReabierto.stream().map(LibroMasPrestado::getIdLibro).toList()),
                    "El orden del ranking cambió al reabrir la conexión.");

            comprobarMismosPrestamos(
                    reportesCarlos.listarHistorialPorEstudiante(carlos.getIdEstudiante()),
                    historialCarlos, "El historial cambió al reabrir la conexión.");
            comprobarMismosPrestamos(reportesBibliotecaria.listarLibrosEnPrestamo(), pendientes,
                    "Los préstamos pendientes cambiaron al reabrir la conexión.");
            System.out.println("Consultas después de cerrar y reabrir la conexión: OK");

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó.");
            System.err.println(e.getMessage());

        } finally {
            try {
                baseDatos.cerrar();
            } catch (SQLException e) {
                resultado = 1;
                System.err.println("No se pudo cerrar la conexión: " + e.getMessage());
            }
        }

        if (resultado != 0) {
            System.exit(resultado);
        }

        System.out.println("PRUEBA COMPLETADA CORRECTAMENTE");
    }

    private static void comprobarRanking(List<LibroMasPrestado> ranking, List<Prestamo> prestamos) {
        Map<Integer, Long> cantidades = new HashMap<>();
        Map<Integer, Libro> libros = new HashMap<>();

        for (Prestamo prestamo : prestamos) {
            comprobar(prestamo.getLibro() != null && prestamo.getEstudiante() != null,
                    "Existe un préstamo sin libro o estudiante asociado.");

            Libro libro = prestamo.getLibro();
            cantidades.merge(libro.getId(), 1L, Long::sum);
            libros.putIfAbsent(libro.getId(), libro);
        }

        comprobar(ranking.size() == cantidades.size(),
                "El ranking omitió libros prestados o incluyó libros sin préstamos.");

        Set<Integer> encontrados = new HashSet<>();
        long cantidadAnterior = Long.MAX_VALUE;

        for (LibroMasPrestado fila : ranking) {
            Libro libro = libros.get(fila.getIdLibro());

            comprobar(libro != null && encontrados.add(fila.getIdLibro())
                            && fila.getCantidadPrestamos() == cantidades.get(fila.getIdLibro())
                            && fila.getCantidadPrestamos() > 0,
                    "El ranking tiene un libro repetido o una cantidad incorrecta.");

            comprobar(Objects.equals(fila.getTitulo(), libro.getTitulo())
                            && Objects.equals(fila.getAutor(), libro.getAutor())
                            && Objects.equals(fila.getIsbn(), libro.getIsbn()),
                    "Los datos del libro en el ranking no son correctos.");

            comprobar(fila.getCantidadPrestamos() <= cantidadAnterior,
                    "El ranking no está ordenado de mayor a menor cantidad de préstamos.");

            cantidadAnterior = fila.getCantidadPrestamos();
        }
    }

    private static void comprobarCantidadesDistintas(ReporteServicio reportes,
                                                     List<LibroMasPrestado> rankingOriginal,
                                                     List<Prestamo> prestamosOriginales)
            throws SQLException {

        comprobar(rankingOriginal.size() >= 2,
                "Se necesitan al menos dos libros prestados para comprobar el orden.");

        LibroMasPrestado primero = rankingOriginal.get(0);
        Prestamo referencia = prestamosOriginales.stream()
                .filter(p -> p.getLibro().getId() == primero.getIdLibro())
                .findFirst().orElseThrow(
                        () -> new IllegalStateException("No se encontró un préstamo de referencia."));

        PrestamoDAO dao = new PrestamoDAO();

        try {
            DatabaseConnection.getInstance().ejecutarTransaccion(conexion -> {
                // Estos registros se crean únicamente para comprobar la consulta del ranking.
                for (int i = 0; i < 2; i++) {
                    dao.guardar(conexion, new Prestamo(referencia.getEstudiante(),
                            referencia.getLibro(), referencia.getFechaPrestamo(),
                            referencia.getFechaDevolucion()));
                }

                List<LibroMasPrestado> rankingTemporal = reportes.listarLibrosMasPrestados();
                comprobarRanking(rankingTemporal, dao.listar());

                comprobar(rankingTemporal.get(0).getIdLibro() == primero.getIdLibro()
                                && rankingTemporal.get(0).getCantidadPrestamos()
                                == primero.getCantidadPrestamos() + 2
                                && rankingTemporal.get(0).getCantidadPrestamos()
                                > rankingTemporal.get(1).getCantidadPrestamos(),
                        "El libro con más préstamos no quedó primero en el ranking.");

                throw new SQLException("Reversión intencional de los préstamos de prueba.", ERROR_PRUEBA);
            });

        } catch (SQLException e) {
            if (!ERROR_PRUEBA.equals(e.getSQLState())) {
                throw e;
            }

            comprobarMismosPrestamos(dao.listar(), prestamosOriginales,
                    "Los préstamos de prueba no fueron revertidos.");
            comprobarRanking(reportes.listarLibrosMasPrestados(), prestamosOriginales);
            return;
        }

        throw new IllegalStateException("La transacción de prueba debía revertirse.");
    }

    private static List<Prestamo> filtrarPorEstudiante(List<Prestamo> prestamos, int idEstudiante) {
        return prestamos.stream()
                .filter(p -> p.getEstudiante().getId() == idEstudiante)
                .toList();
    }

    private static void comprobarMismosPrestamos(List<Prestamo> actuales,
                                                 List<Prestamo> esperados, String mensaje) {
        List<Integer> idsActuales = actuales.stream().map(Prestamo::getId).sorted().toList();
        List<Integer> idsEsperados = esperados.stream().map(Prestamo::getId).sorted().toList();

        comprobar(idsActuales.equals(idsEsperados), mensaje);
    }

    private static void comprobarRechazo(OperacionPrueba operacion, String mensajeEsperado)
            throws SQLException {
        try {
            operacion.ejecutar();

        } catch (IllegalArgumentException | IllegalStateException e) {
            comprobar(mensajeEsperado.equals(e.getMessage()),
                    "El rechazo produjo un mensaje inesperado: " + e.getMessage());
            return;
        }

        throw new IllegalStateException("Se permitió una operación que debía rechazarse.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }

    @FunctionalInterface
    private interface OperacionPrueba {
        void ejecutar() throws SQLException;
    }
}