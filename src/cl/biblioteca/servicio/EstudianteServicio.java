package cl.biblioteca.servicio;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.EstudianteDAO;
import cl.biblioteca.dao.UsuarioDAO;
import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.modelo.Usuario;
import cl.biblioteca.modelo.UsuarioEstudiante;
import cl.biblioteca.util.Validador;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Gestiona las fichas y sus cuentas de acceso mediante transacciones.
 */
public final class EstudianteServicio {

    private static final String ESTUDIANTE_INEXISTENTE =
            "El estudiante seleccionado ya no existe.";
    private static final String CUENTA_INEXISTENTE =
            "La cuenta del estudiante seleccionado ya no existe.";

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();
    private final EstudianteDAO estudianteDAO = new EstudianteDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();
    private final SesionUsuario sesion;

    public EstudianteServicio(SesionUsuario sesion) {
        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        this.sesion = sesion;
    }

    public Estudiante guardar(Estudiante datos, String contrasena) throws SQLException {
        sesion.exigirAdministracion();
        Estudiante estudiante = validarDatos(datos);
        String clave = Validador.validarContrasena(contrasena);

        return baseDatos.ejecutarTransaccion(conexion -> {
            comprobarRutDisponible(estudiante.getRut(), 0, 0);
            Estudiante guardado = estudianteDAO.guardar(conexion, estudiante);

            usuarioDAO.guardar(conexion, new UsuarioEstudiante(
                    guardado.getNombre(), guardado.getRut(), guardado.getCorreo(), clave
            ));

            return guardado;
        });
    }

    /**
     * Una clave vacía conserva la actual. Si la ficha no tiene cuenta,
     * se crea al proporcionar una clave.
     */
    public Estudiante actualizar(Estudiante datos, String nuevaContrasena)
            throws SQLException {

        sesion.exigirAdministracion();
        Validador.validarSeleccion(datos, "Estudiante");
        Validador.validarId(datos.getId(), "Estudiante");
        Estudiante estudiante = validarDatos(datos);
        String clave = nuevaContrasena == null || nuevaContrasena.isBlank()
                ? null : Validador.validarContrasena(nuevaContrasena);

        return baseDatos.ejecutarTransaccion(conexion -> {
            Estudiante actual = buscarExistente(estudiante.getId());
            Usuario cuenta = buscarCuentaVinculada(actual);
            int idCuenta = cuenta == null ? 0 : cuenta.getId();
            comprobarRutDisponible(estudiante.getRut(), estudiante.getId(), idCuenta);

            if (!estudianteDAO.actualizar(conexion, estudiante)) {
                throw new IllegalStateException(ESTUDIANTE_INEXISTENTE);
            }

            if (cuenta != null) {
                String claveActualizada = clave == null ? cuenta.getContrasena() : clave;
                UsuarioEstudiante cuentaActualizada = new UsuarioEstudiante(
                        cuenta.getId(), estudiante.getNombre(), estudiante.getRut(),
                        estudiante.getCorreo(), claveActualizada
                );

                if (!usuarioDAO.actualizar(conexion, cuentaActualizada)) {
                    throw new IllegalStateException(CUENTA_INEXISTENTE);
                }

            } else if (clave != null) {
                usuarioDAO.guardar(conexion, new UsuarioEstudiante(
                        estudiante.getNombre(), estudiante.getRut(),
                        estudiante.getCorreo(), clave
                ));
            }

            return estudiante;
        });
    }

    public void eliminar(int id) throws SQLException {
        sesion.exigirAdministracion();
        Validador.validarId(id, "Estudiante");

        try {
            baseDatos.ejecutarTransaccion(conexion -> {
                Estudiante estudiante = buscarExistente(id);
                Usuario cuenta = buscarCuentaVinculada(estudiante);

                // MySQL rechaza primero la eliminación si existe un historial de préstamos.
                if (!estudianteDAO.eliminar(conexion, id)) {
                    throw new IllegalStateException(ESTUDIANTE_INEXISTENTE);
                }

                if (cuenta != null && !usuarioDAO.eliminar(conexion, cuenta.getId())) {
                    throw new IllegalStateException(CUENTA_INEXISTENTE);
                }

                return null;
            });

        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalStateException(
                    "No puede eliminar un estudiante que tiene préstamos asociados.", e
            );
        }
    }

    public Estudiante buscarPorId(int id) throws SQLException {
        sesion.exigirAccesoEstudiante(id);
        return buscarExistente(id);
    }

    public List<Estudiante> listar() throws SQLException {
        sesion.exigirAdministracion();
        return estudianteDAO.listar();
    }

    private Estudiante buscarExistente(int id) throws SQLException {
        return estudianteDAO.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException(ESTUDIANTE_INEXISTENTE)
        );
    }

    private Usuario buscarCuentaVinculada(Estudiante estudiante) throws SQLException {
        // La asociación utiliza el RUT, nunca el ID de la ficha.
        Usuario cuenta = usuarioDAO.buscarPorRut(estudiante.getRut()).orElse(null);

        if (cuenta != null && cuenta.puedeAdministrar()) {
            throw new IllegalStateException(
                    "El RUT de esta ficha pertenece a una cuenta de bibliotecario."
            );
        }

        return cuenta;
    }

    private void comprobarRutDisponible(String rut, int idEstudiante, int idUsuario)
            throws SQLException {

        if (estudianteDAO.buscarPorRut(rut)
                .filter(estudiante -> estudiante.getId() != idEstudiante).isPresent()) {
            throw new IllegalArgumentException("Ya existe un estudiante con ese RUT.");
        }

        if (usuarioDAO.buscarPorRut(rut)
                .filter(usuario -> usuario.getId() != idUsuario).isPresent()) {
            throw new IllegalArgumentException("Ya existe una cuenta con ese RUT.");
        }
    }

    private Estudiante validarDatos(Estudiante datos) {
        Validador.validarSeleccion(datos, "Estudiante");
        String nombre = Validador.textoObligatorio(datos.getNombre(), "Nombre", 100);
        String rut = Validador.validarRut(datos.getRut());
        String curso = Validador.textoObligatorio(datos.getCurso(), "Curso", 20);
        String correo = Validador.validarCorreo(datos.getCorreo());

        return new Estudiante(datos.getId(), nombre, rut, curso, correo);
    }
}