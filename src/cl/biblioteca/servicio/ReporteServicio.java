package cl.biblioteca.servicio;

import cl.biblioteca.dao.ReporteDAO;
import cl.biblioteca.modelo.LibroMasPrestado;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;

import java.sql.SQLException;
import java.util.List;

/**
 * Organiza los reportes y aplica los permisos del usuario conectado.
 */
public final class ReporteServicio {

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private final PrestamoServicio prestamoServicio;
    private final SesionUsuario sesion;

    public ReporteServicio(SesionUsuario sesion) {
        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        this.sesion = sesion;
        this.prestamoServicio = new PrestamoServicio(sesion);
    }

    public List<LibroMasPrestado> listarLibrosMasPrestados() throws SQLException {
        sesion.exigirAdministracion();
        return reporteDAO.listarLibrosMasPrestados();
    }

    public List<Prestamo> listarHistorialPorEstudiante(int idEstudiante)
            throws SQLException {
        return prestamoServicio.listarPorEstudiante(idEstudiante);
    }

    public List<Prestamo> listarLibrosEnPrestamo() throws SQLException {
        return prestamoServicio.listarPendientes();
    }
}