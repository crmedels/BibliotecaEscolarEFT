package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.EstudianteDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.dao.PrestamoDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.Prestamo;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Comprueba el CRUD, el historial y las devoluciones con un préstamo temporal.
 */
public final class PruebaPrestamoDAO {

    private static final String ERROR_PRUEBA = "PR001";

    private PruebaPrestamoDAO() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        PrestamoDAO dao = new PrestamoDAO();
        int idTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA CRUD E HISTORIAL DE PRÉSTAMOS");

        try {
            int cantidadInicial = dao.listar().size();
            System.out.println("Préstamos iniciales: " + cantidadInicial);

            List<Estudiante> estudiantes = new EstudianteDAO().listar();
            List<Libro> libros = new LibroDAO().listar();
            comprobar(estudiantes.size() >= 2 && libros.size() >= 2,
                    "La prueba requiere al menos dos estudiantes y dos libros.");

            Estudiante estudiante = estudiantes.get(0);
            Estudiante otroEstudiante = estudiantes.get(estudiantes.size() - 1);
            Libro libro = libros.get(0);
            Libro otroLibro = libros.get(libros.size() - 1);

            LocalDate hoy = LocalDate.now();
            LocalDate fechaPrestamo = hoy.minusDays(10);
            LocalDate fechaLimite = fechaPrestamo.plusDays(7);
            Prestamo inicial = new Prestamo(
                    estudiante, libro, fechaPrestamo, fechaLimite
            );

            Prestamo guardado = dao.guardar(inicial);
            int idCreado = guardado.getId();
            idTemporal = idCreado;
            comprobar(idCreado > 0, "MySQL no generó un ID válido.");

            baseDatos.cerrar();
            Prestamo encontrado = leerPrestamo(dao, idCreado);
            comprobarDatos(encontrado, inicial);
            comprobar(encontrado.estaAtrasado(),
                    "El préstamo pendiente debe aparecer atrasado.");
            System.out.println("Guardar, asociaciones, fechas y persistencia: OK");

            comprobar(dao.listar().stream().anyMatch(p -> p.getId() == idCreado),
                    "El préstamo temporal no aparece en el listado.");

            List<Prestamo> historial = dao.listarPorEstudiante(estudiante.getId());
            comprobar(historial.stream().anyMatch(p -> p.getId() == idCreado),
                    "El préstamo no aparece en el historial del estudiante.");
            comprobar(historial.stream().allMatch(p -> p.getEstudiante() != null
                            && p.getEstudiante().getId() == estudiante.getId()),
                    "El historial incluye préstamos de otro estudiante.");
            comprobar(dao.listarPorEstudiante(otroEstudiante.getId()).stream()
                            .noneMatch(p -> p.getId() == idCreado),
                    "El préstamo aparece en el historial de otro estudiante.");

            List<Prestamo> pendientes = dao.listarPendientes();
            comprobar(pendientes.stream().anyMatch(p -> p.getId() == idCreado)
                            && pendientes.stream().noneMatch(Prestamo::isDevuelto),
                    "El listado de pendientes no es correcto.");
            System.out.println("Listar, historial por estudiante y pendientes: OK");

            Prestamo nuevosDatos = new Prestamo(
                    idCreado, otroEstudiante, otroLibro,
                    fechaPrestamo.plusDays(1), fechaLimite.plusDays(1), null, false
            );
            comprobar(dao.actualizar(nuevosDatos),
                    "No se pudo actualizar el préstamo.");

            baseDatos.cerrar();
            comprobarDatos(leerPrestamo(dao, idCreado), nuevosDatos);
            comprobar(dao.listarPorEstudiante(estudiante.getId()).stream()
                            .noneMatch(p -> p.getId() == idCreado),
                    "El préstamo sigue asociado al estudiante anterior.");
            comprobar(dao.listarPorEstudiante(otroEstudiante.getId()).stream()
                            .anyMatch(p -> p.getId() == idCreado),
                    "El préstamo no aparece en el nuevo historial.");
            System.out.println("Actualizar asociaciones y fechas con persistencia: OK");

            try {
                baseDatos.ejecutarTransaccion(conexion -> {
                    comprobar(dao.marcarDevuelto(conexion, idCreado, hoy),
                            "No se pudo preparar la devolución temporal.");
                    throw new SQLException("Fallo intencional de prueba.", ERROR_PRUEBA);
                });
                throw new IllegalStateException("La transacción debía fallar.");

            } catch (SQLException e) {
                if (!ERROR_PRUEBA.equals(e.getSQLState())) {
                    throw e;
                }
            }

            baseDatos.cerrar();
            comprobarDatos(leerPrestamo(dao, idCreado), nuevosDatos);
            comprobar(dao.listarPendientes().stream().anyMatch(p -> p.getId() == idCreado),
                    "El rollback no recuperó el préstamo pendiente.");
            System.out.println("Rollback del estado y de la fecha real: OK");

            comprobar(baseDatos.ejecutarTransaccion(
                    conexion -> dao.marcarDevuelto(conexion, idCreado, hoy)
            ), "No se pudo registrar la devolución.");

            Prestamo datosDevueltos = new Prestamo(
                    idCreado, otroEstudiante, otroLibro,
                    nuevosDatos.getFechaPrestamo(), nuevosDatos.getFechaDevolucion(),
                    hoy, true
            );

            baseDatos.cerrar();
            Prestamo devuelto = leerPrestamo(dao, idCreado);
            comprobarDatos(devuelto, datosDevueltos);
            comprobar(!devuelto.estaAtrasado() && devuelto.fueDevueltoConAtraso(),
                    "Debe conservarse el atraso histórico de la devolución.");
            comprobar(dao.listarPendientes().stream().noneMatch(p -> p.getId() == idCreado),
                    "El préstamo devuelto sigue apareciendo pendiente.");
            comprobar(dao.listarPorEstudiante(otroEstudiante.getId()).stream()
                            .anyMatch(p -> p.getId() == idCreado && p.isDevuelto()),
                    "La devolución no aparece en el historial.");
            System.out.println("Devolución, fecha límite y atraso histórico: OK");

            comprobar(!baseDatos.ejecutarTransaccion(
                    conexion -> dao.marcarDevuelto(conexion, idCreado, hoy.plusDays(1))
            ), "Se aceptó una devolución repetida.");

            baseDatos.cerrar();
            comprobarDatos(leerPrestamo(dao, idCreado), datosDevueltos);
            System.out.println("Devolución repetida rechazada sin cambiar las fechas: OK");

            comprobar(dao.eliminar(idCreado),
                    "No se pudo eliminar el préstamo temporal.");
            baseDatos.cerrar();
            comprobar(dao.buscarPorId(idCreado).isEmpty(),
                    "El préstamo sigue existiendo después de eliminarlo.");
            comprobar(dao.listarPorEstudiante(otroEstudiante.getId()).stream()
                            .noneMatch(p -> p.getId() == idCreado),
                    "El préstamo eliminado sigue apareciendo en el historial.");
            idTemporal = 0;
            System.out.println("Eliminar y comprobar persistencia: OK");

            comprobar(!dao.actualizar(datosDevueltos),
                    "Actualizar un ID inexistente debe devolver false.");
            comprobar(!dao.eliminar(idCreado),
                    "Eliminar un ID inexistente debe devolver false.");
            comprobar(!baseDatos.ejecutarTransaccion(
                    conexion -> dao.marcarDevuelto(conexion, idCreado, hoy)
            ), "Se aceptó devolver un préstamo inexistente.");
            System.out.println("Operaciones sobre ID inexistente: OK");

            comprobar(dao.listar().size() == cantidadInicial,
                    "La cantidad final de préstamos cambió.");

        } catch (SQLException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó.");
            System.err.println(e.getMessage());

        } finally {
            if (idTemporal > 0) {
                try {
                    dao.eliminar(idTemporal);
                } catch (SQLException e) {
                    resultado = 1;
                    System.err.println("No se pudo limpiar el préstamo temporal ID "
                            + idTemporal + ": " + e.getMessage());
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

    private static Prestamo leerPrestamo(PrestamoDAO dao, int id) throws SQLException {
        return dao.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException("El préstamo temporal no existe.")
        );
    }

    private static void comprobarDatos(Prestamo actual, Prestamo esperado) {
        Estudiante estudiante = actual.getEstudiante();
        Estudiante estudianteEsperado = esperado.getEstudiante();
        comprobar(estudiante != null && estudiante.getId() == estudianteEsperado.getId()
                        && Objects.equals(estudiante.getNombre(), estudianteEsperado.getNombre())
                        && Objects.equals(estudiante.getRut(), estudianteEsperado.getRut()),
                "Los datos del estudiante asociado no coinciden.");

        Libro libro = actual.getLibro();
        Libro libroEsperado = esperado.getLibro();
        comprobar(libro != null && libro.getId() == libroEsperado.getId()
                        && Objects.equals(libro.getTitulo(), libroEsperado.getTitulo())
                        && Objects.equals(libro.getIsbn(), libroEsperado.getIsbn()),
                "Los datos del libro asociado no coinciden.");

        Categoria categoria = libro.getCategoria();
        Categoria categoriaEsperada = libroEsperado.getCategoria();
        if (categoriaEsperada == null) {
            comprobar(categoria == null, "La categoría debe ser nula.");
        } else {
            comprobar(categoria != null && categoria.getId() == categoriaEsperada.getId()
                            && Objects.equals(categoria.getNombre(), categoriaEsperada.getNombre()),
                    "La categoría del libro no coincide.");
        }

        comprobar(Objects.equals(actual.getFechaPrestamo(), esperado.getFechaPrestamo())
                        && Objects.equals(actual.getFechaDevolucion(), esperado.getFechaDevolucion())
                        && Objects.equals(actual.getFechaDevolucionReal(), esperado.getFechaDevolucionReal())
                        && actual.isDevuelto() == esperado.isDevuelto(),
                "Las fechas o el estado del préstamo no coinciden.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }
}