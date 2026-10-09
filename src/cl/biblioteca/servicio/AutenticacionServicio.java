package cl.biblioteca.servicio;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.EstudianteDAO;
import cl.biblioteca.dao.UsuarioDAO;
import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.modelo.Usuario;
import cl.biblioteca.util.Validador;

import java.sql.SQLException;

/**
 * Valida las credenciales y construye la sesión con los datos persistidos.
 */
public final class AutenticacionServicio {

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final EstudianteDAO estudianteDAO = new EstudianteDAO();

    public SesionUsuario iniciarSesion(String rut, String contrasena) throws SQLException {
        String rutNormalizado = Validador.validarRut(rut);
        String clave = Validador.validarContrasena(contrasena);

        // Mantiene ambas lecturas juntas frente a otras operaciones de la aplicación.
        return baseDatos.ejecutar(conexion -> {
            Usuario usuario = usuarioDAO.autenticar(rutNormalizado, clave)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "RUT o contraseña incorrectos."
                    ));

            Estudiante estudiante = null;
            if (!usuario.puedeAdministrar()) {
                estudiante = estudianteDAO.buscarPorRut(usuario.getRut())
                        .orElseThrow(() -> new IllegalStateException(
                                "La cuenta de estudiante no tiene una ficha asociada. "
                                        + "Contacte al bibliotecario."
                        ));
            }

            return new SesionUsuario(usuario, estudiante);
        });
    }
}