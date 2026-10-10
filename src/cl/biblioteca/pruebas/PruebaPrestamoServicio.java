package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.CategoriaDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.dao.PrestamoDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.AutenticacionServicio;
import cl.biblioteca.servicio.LibroServicio;
import cl.biblioteca.servicio.PrestamoServicio;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Comprueba préstamos, devoluciones y stock utilizando un libro temporal.
 */
public final class PruebaPrestamoServicio {

    private static final String ERROR_PRUEBA = "PR001";

    private PruebaPrestamoServicio() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        PrestamoDAO prestamoDAO = new PrestamoDAO();
        int idLibroTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA DE SERVICIO DE PRÉSTAMOS");

        try {
            AutenticacionServicio autenticacion = new AutenticacionServicio();
            SesionUsuario bibliotecaria = autenticacion.iniciarSesion("12345678-9", "clave123");
            SesionUsuario carlos = autenticacion.iniciarSesion("98765432-1", "clave123");
            SesionUsuario maria = autenticacion.iniciarSesion("11222333-4", "clave123");

            PrestamoServicio administracion = new PrestamoServicio(bibliotecaria);
            PrestamoServicio prestamosCarlos = new PrestamoServicio(carlos);
            PrestamoServicio prestamosMaria = new PrestamoServicio(maria);
            LibroServicio libros = new LibroServicio(bibliotecaria);

            int librosIniciales = libros.listar().size();
            int prestamosIniciales = prestamoDAO.listar().size();

            List<Categoria> categorias = new CategoriaDAO().listar();
            comprobar(!categorias.isEmpty(), "Se necesita una categoría para la prueba.");

            Libro libro = libros.guardar(new Libro(
                    "Libro EFT de préstamos", "Autor EFT",
                    generarIsbnDisponible(libros), "Editorial EFT",
                    1, categorias.get(0)
            ));
            idLibroTemporal = libro.getId();

            LocalDate antes = LocalDate.now();
            Prestamo primero = prestamosCarlos.registrar(
                    carlos.getIdEstudiante(), libro.getId()
            );
            LocalDate despues = LocalDate.now();

            baseDatos.cerrar();
            Prestamo guardado = prestamosCarlos.buscarPorId(primero.getId());
            comprobar(guardado.getId() > 0
                            && guardado.getEstudiante().getId() == carlos.getIdEstudiante()
                            && guardado.getLibro().getId() == libro.getId()
                            && !guardado.getFechaPrestamo().isBefore(antes)
                            && !guardado.getFechaPrestamo().isAfter(despues)
                            && guardado.getFechaDevolucion().equals(
                            guardado.getFechaPrestamo()
                                    .plusDays(PrestamoServicio.DIAS_PRESTAMO))
                            && !guardado.isDevuelto()
                            && guardado.getFechaDevolucionReal() == null
                            && guardado.getLibro().getStock() == 0,
                    "El préstamo no conservó sus datos, fechas o descuento de stock.");

            comprobar(libros.buscarPorId(libro.getId()).getStock() == 0,
                    "El descuento de stock no quedó guardado.");
            System.out.println("Préstamo, vencimiento automático y descuento persistido: OK");

            comprobarRechazo(
                    () -> administracion.registrar(maria.getIdEstudiante(), libro.getId()),
                    "El libro seleccionado no tiene stock disponible."
            );

            baseDatos.cerrar();
            comprobar(prestamoDAO.listar().size() == prestamosIniciales + 1
                            && libros.buscarPorId(libro.getId()).getStock() == 0,
                    "El intento sin stock creó un préstamo o alteró el inventario.");
            System.out.println("Rechazo del préstamo sin stock: OK");

            comprobarPermisos(prestamosCarlos, prestamosMaria, carlos, maria, primero);
            comprobar(administracion.listar().stream()
                            .anyMatch(p -> p.getId() == primero.getId())
                            && administracion.listarPendientes().stream()
                            .anyMatch(p -> p.getId() == primero.getId()),
                    "El bibliotecario no pudo consultar el préstamo pendiente.");
            System.out.println("Permisos, historial propio y consulta administrativa: OK");

            comprobarRechazo(
                    () -> libros.eliminar(libro.getId()),
                    "No puede eliminar un libro que tiene préstamos asociados."
            );
            System.out.println("Protección del libro con historial: OK");

            antes = LocalDate.now();
            prestamosCarlos.devolver(primero.getId());
            despues = LocalDate.now();

            baseDatos.cerrar();
            Prestamo devuelto = prestamosCarlos.buscarPorId(primero.getId());
            comprobarDevolucion(devuelto, guardado, antes, despues);
            comprobar(!devuelto.fueDevueltoConAtraso()
                            && libros.buscarPorId(libro.getId()).getStock() == 1
                            && prestamosCarlos.listarPendientes().stream()
                            .noneMatch(p -> p.getId() == primero.getId())
                            && prestamosCarlos.listar().stream()
                            .anyMatch(p -> p.getId() == primero.getId()),
                    "La devolución no repuso el stock o alteró el historial.");
            System.out.println("Devolución, fecha real y reposición persistida: OK");

            comprobarRechazo(
                    () -> prestamosCarlos.devolver(primero.getId()),
                    "El préstamo ya fue devuelto."
            );

            baseDatos.cerrar();
            Prestamo sinCambios = prestamosCarlos.buscarPorId(primero.getId());
            comprobar(libros.buscarPorId(libro.getId()).getStock() == 1
                            && devuelto.getFechaDevolucionReal()
                            .equals(sinCambios.getFechaDevolucionReal())
                            && devuelto.getFechaDevolucion()
                            .equals(sinCambios.getFechaDevolucion()),
                    "La devolución repetida repuso otro ejemplar o cambió las fechas.");
            System.out.println("Rechazo de devolución repetida sin duplicar stock: OK");

            Prestamo segundo = administracion.registrar(
                    maria.getIdEstudiante(), libro.getId()
            );

            // Solo cambia las fechas del préstamo temporal para simular un atraso.
            segundo.setFechaPrestamo(
                    LocalDate.now().minusDays(PrestamoServicio.DIAS_PRESTAMO + 1L)
            );
            segundo.setFechaDevolucion(
                    segundo.getFechaPrestamo().plusDays(PrestamoServicio.DIAS_PRESTAMO)
            );
            comprobar(prestamoDAO.actualizar(segundo),
                    "No se pudo preparar el préstamo atrasado.");

            baseDatos.cerrar();
            comprobar(prestamosMaria.buscarPorId(segundo.getId()).estaAtrasado(),
                    "No se detectó el atraso del préstamo pendiente.");

            antes = LocalDate.now();
            administracion.devolver(segundo.getId());
            despues = LocalDate.now();

            baseDatos.cerrar();
            Prestamo atrasado = prestamosMaria.buscarPorId(segundo.getId());
            comprobarDevolucion(atrasado, segundo, antes, despues);
            comprobar(atrasado.fueDevueltoConAtraso()
                            && libros.buscarPorId(libro.getId()).getStock() == 1
                            && prestamosMaria.listar().stream()
                            .anyMatch(p -> p.getId() == segundo.getId())
                            && prestamosCarlos.listar().stream()
                            .noneMatch(p -> p.getId() == segundo.getId()),
                    "El atraso no quedó registrado o se mezclaron los historiales.");
            System.out.println("Devolución administrativa y constancia del atraso: OK");

            comprobarRollback(primero, libros.buscarPorId(libro.getId()));

            baseDatos.cerrar();
            comprobar(libros.buscarPorId(libro.getId()).getStock() == 1
                            && prestamoDAO.listar().size() == prestamosIniciales + 2,
                    "El rollback no recuperó el stock y la cantidad de préstamos.");
            System.out.println("Rollback de préstamo y stock: OK");

            limpiarLibroTemporal(libro.getId());
            baseDatos.cerrar();

            comprobar(new LibroDAO().buscarPorId(libro.getId()).isEmpty()
                            && libros.listar().size() == librosIniciales
                            && prestamoDAO.listar().size() == prestamosIniciales,
                    "La limpieza no recuperó la cantidad inicial de registros.");
            idLibroTemporal = 0;
            System.out.println("Limpieza de registros temporales: OK");

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó.");
            System.err.println(e.getMessage());

        } finally {
            if (idLibroTemporal > 0) {
                try {
                    limpiarLibroTemporal(idLibroTemporal);
                } catch (SQLException | IllegalStateException e) {
                    resultado = 1;
                    System.err.println("No se pudo limpiar el libro temporal ID "
                            + idLibroTemporal + ": " + e.getMessage());
                }
            }

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

    private static void comprobarPermisos(PrestamoServicio propios,
                                          PrestamoServicio ajenos,
                                          SesionUsuario carlos,
                                          SesionUsuario maria,
                                          Prestamo prestamo) throws SQLException {
        String mensaje = "Solo puede acceder a sus propios préstamos.";

        comprobarRechazo(
                () -> propios.registrar(
                        maria.getIdEstudiante(), prestamo.getLibro().getId()
                ),
                mensaje
        );
        comprobarRechazo(
                () -> propios.listarPorEstudiante(maria.getIdEstudiante()),
                mensaje
        );
        comprobarRechazo(() -> ajenos.buscarPorId(prestamo.getId()), mensaje);
        comprobarRechazo(() -> ajenos.devolver(prestamo.getId()), mensaje);

        comprobar(propios.listar().stream()
                        .allMatch(p -> p.getEstudiante().getId() == carlos.getIdEstudiante())
                        && propios.listarPorEstudiante(carlos.getIdEstudiante()).stream()
                        .anyMatch(p -> p.getId() == prestamo.getId())
                        && propios.listarPendientes().stream()
                        .anyMatch(p -> p.getId() == prestamo.getId())
                        && ajenos.listar().stream()
                        .allMatch(p -> p.getEstudiante().getId() == maria.getIdEstudiante())
                        && ajenos.listarPendientes().stream()
                        .allMatch(p -> p.getEstudiante().getId() == maria.getIdEstudiante()),
                "Las consultas no respetan el estudiante de la sesión.");
    }

    private static void comprobarDevolucion(Prestamo actual, Prestamo original,
                                            LocalDate desde, LocalDate hasta) {
        comprobar(actual.isDevuelto()
                        && actual.getFechaDevolucionReal() != null
                        && actual.getId() == original.getId()
                        && actual.getEstudiante().getId() == original.getEstudiante().getId()
                        && actual.getLibro().getId() == original.getLibro().getId()
                        && original.getFechaPrestamo().equals(actual.getFechaPrestamo())
                        && original.getFechaDevolucion().equals(actual.getFechaDevolucion())
                        && !actual.getFechaDevolucionReal().isBefore(desde)
                        && !actual.getFechaDevolucionReal().isAfter(hasta)
                        && !actual.estaAtrasado(),
                "La devolución no conservó las asociaciones o las fechas correctas.");
    }

    private static void comprobarRollback(Prestamo referencia, Libro libro)
            throws SQLException {
        int[] idSinConfirmar = {0};

        try {
            DatabaseConnection.getInstance().ejecutarTransaccion(conexion -> {
                comprobar(new LibroDAO().descontarStock(conexion, libro.getId()),
                        "No se pudo preparar el descuento temporal.");

                LocalDate fecha = LocalDate.now();
                Prestamo temporal = new PrestamoDAO().guardar(conexion, new Prestamo(
                        referencia.getEstudiante(), libro, fecha,
                        fecha.plusDays(PrestamoServicio.DIAS_PRESTAMO)
                ));
                idSinConfirmar[0] = temporal.getId();

                throw new SQLException(
                        "Fallo intencional después del descuento y del préstamo.",
                        ERROR_PRUEBA
                );
            });

        } catch (SQLException e) {
            if (!ERROR_PRUEBA.equals(e.getSQLState())) {
                throw e;
            }

            comprobar(idSinConfirmar[0] > 0
                            && new PrestamoDAO().buscarPorId(idSinConfirmar[0]).isEmpty(),
                    "El préstamo sin confirmar no fue revertido.");
            return;
        }

        throw new IllegalStateException("La transacción de prueba debía fallar.");
    }

    private static void limpiarLibroTemporal(int idLibro) throws SQLException {
        DatabaseConnection.getInstance().ejecutarTransaccion(conexion -> {
            PrestamoDAO dao = new PrestamoDAO();

            for (Prestamo prestamo : dao.listar()) {
                if (prestamo.getLibro() != null
                        && prestamo.getLibro().getId() == idLibro) {
                    comprobar(dao.eliminar(conexion, prestamo.getId()),
                            "No se pudo eliminar un préstamo del libro temporal.");
                }
            }

            new LibroDAO().eliminar(idLibro);
            return null;
        });
    }

    private static String generarIsbnDisponible(LibroServicio servicio)
            throws SQLException {
        List<Libro> existentes = servicio.listar();

        while (true) {
            String isbn = String.format(
                    Locale.ROOT, "978%010d",
                    ThreadLocalRandom.current().nextLong(10_000_000_000L)
            );

            if (existentes.stream().noneMatch(l -> isbn.equals(l.getIsbn()))) {
                return isbn;
            }
        }
    }

    private static void comprobarRechazo(OperacionPrueba operacion,
                                         String mensajeEsperado) throws SQLException {
        try {
            operacion.ejecutar();
        } catch (IllegalStateException e) {
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