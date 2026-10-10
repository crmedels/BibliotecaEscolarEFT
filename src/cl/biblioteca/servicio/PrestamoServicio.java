package cl.biblioteca.servicio;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.EstudianteDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.dao.PrestamoDAO;
import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.util.Validador;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Coordina los préstamos y las devoluciones con sus cambios de stock.
 */
public final class PrestamoServicio {

    public static final int DIAS_PRESTAMO = 7;

    private static final String PRESTAMO_INEXISTENTE =
            "El préstamo seleccionado ya no existe.";
    private static final String PRESTAMO_DEVUELTO =
            "El préstamo ya fue devuelto.";

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();
    private final EstudianteDAO estudianteDAO = new EstudianteDAO();
    private final LibroDAO libroDAO = new LibroDAO();
    private final PrestamoDAO prestamoDAO = new PrestamoDAO();
    private final SesionUsuario sesion;

    public PrestamoServicio(SesionUsuario sesion) {
        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        this.sesion = sesion;
    }

    public Prestamo registrar(int idEstudiante, int idLibro) throws SQLException {
        sesion.exigirAccesoEstudiante(idEstudiante);
        Validador.validarId(idLibro, "Libro");

        return baseDatos.ejecutarTransaccion(conexion -> {
            Estudiante estudiante = buscarEstudiante(idEstudiante);
            Libro libro = buscarLibro(idLibro);

            // El DAO descuenta únicamente si todavía existe un ejemplar disponible.
            if (!libroDAO.descontarStock(conexion, idLibro)) {
                throw new IllegalStateException(
                        "El libro seleccionado no tiene stock disponible."
                );
            }

            LocalDate fechaPrestamo = LocalDate.now();
            LocalDate fechaLimite = fechaPrestamo.plusDays(DIAS_PRESTAMO);
            Prestamo guardado = prestamoDAO.guardar(conexion, new Prestamo(
                    estudiante, libro, fechaPrestamo, fechaLimite
            ));

            // Recupera el registro con el stock actualizado dentro de la misma transacción.
            return buscarExistente(guardado.getId());
        });
    }

    public Prestamo devolver(int idPrestamo) throws SQLException {
        Validador.validarId(idPrestamo, "Préstamo");

        return baseDatos.ejecutarTransaccion(conexion -> {
            Prestamo prestamo = buscarExistente(idPrestamo);
            comprobarAcceso(prestamo);

            if (prestamo.isDevuelto()) {
                throw new IllegalStateException(PRESTAMO_DEVUELTO);
            }

            Libro libro = prestamo.getLibro();
            if (libro == null) {
                throw new IllegalStateException(
                        "El préstamo no tiene un libro asociado."
                );
            }

            LocalDate fechaReal = LocalDate.now();
            Validador.validarFechaDevolucion(prestamo.getFechaPrestamo(), fechaReal);

            // Solo quien cambia el préstamo de pendiente a devuelto puede reponer el stock.
            if (!prestamoDAO.marcarDevuelto(conexion, idPrestamo, fechaReal)) {
                throw new IllegalStateException(PRESTAMO_DEVUELTO);
            }

            if (!libroDAO.reponerStock(conexion, libro.getId())) {
                throw new IllegalStateException(
                        "El libro asociado al préstamo ya no existe."
                );
            }

            // Mantiene la fecha límite y recupera también la fecha real de devolución.
            return buscarExistente(idPrestamo);
        });
    }

    public Prestamo buscarPorId(int id) throws SQLException {
        Validador.validarId(id, "Préstamo");

        return baseDatos.ejecutar(conexion -> {
            Prestamo prestamo = buscarExistente(id);
            comprobarAcceso(prestamo);
            return prestamo;
        });
    }

    public List<Prestamo> listar() throws SQLException {
        if (sesion.puedeAdministrar()) {
            return prestamoDAO.listar();
        }

        return prestamoDAO.listarPorEstudiante(sesion.getIdEstudiante());
    }

    public List<Prestamo> listarPorEstudiante(int idEstudiante) throws SQLException {
        sesion.exigirAccesoEstudiante(idEstudiante);
        return prestamoDAO.listarPorEstudiante(idEstudiante);
    }

    public List<Prestamo> listarPendientes() throws SQLException {
        if (sesion.puedeAdministrar()) {
            return prestamoDAO.listarPendientes();
        }

        return prestamoDAO.listarPorEstudiante(sesion.getIdEstudiante()).stream()
                .filter(prestamo -> !prestamo.isDevuelto())
                .toList();
    }

    private Prestamo buscarExistente(int id) throws SQLException {
        return prestamoDAO.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException(PRESTAMO_INEXISTENTE)
        );
    }

    private Estudiante buscarEstudiante(int id) throws SQLException {
        return estudianteDAO.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException("El estudiante seleccionado ya no existe.")
        );
    }

    private Libro buscarLibro(int id) throws SQLException {
        return libroDAO.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException("El libro seleccionado ya no existe.")
        );
    }

    private void comprobarAcceso(Prestamo prestamo) {
        Estudiante estudiante = prestamo.getEstudiante();
        if (estudiante == null) {
            throw new IllegalStateException(
                    "El préstamo no tiene un estudiante asociado."
            );
        }

        sesion.exigirAccesoEstudiante(estudiante.getId());
    }
}