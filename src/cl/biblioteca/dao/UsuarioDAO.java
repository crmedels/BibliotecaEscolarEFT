package cl.biblioteca.dao;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Bibliotecario;
import cl.biblioteca.modelo.Usuario;
import cl.biblioteca.modelo.UsuarioEstudiante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona las cuentas y recupera el tipo de usuario según su rol.
 */
public final class UsuarioDAO implements CrudDAO<Usuario> {

    private static final String CONSULTA_BASE = """
            SELECT id, nombre, rut, correo, `contraseña` AS contrasena, rol
            FROM usuarios
            """;

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();

    @Override
    public Usuario guardar(Usuario usuario) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> guardar(conexion, usuario)
        );
    }

    /**
     * Inserta la cuenta usando una transacción gestionada por el llamador.
     */
    public Usuario guardar(Connection conexion, Usuario usuario) throws SQLException {
        comprobarTransaccion(conexion);
        String sql = """
                INSERT INTO usuarios (nombre, rut, correo, `contraseña`, rol)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
        )) {
            asignarParametros(consulta, usuario);

            if (consulta.executeUpdate() != 1) {
                throw new SQLException("No se pudo guardar el usuario.");
            }

            try (ResultSet claves = consulta.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("No se pudo obtener el ID del usuario.");
                }

                return crearUsuario(
                        claves.getInt(1), usuario.getNombre(), usuario.getRut(),
                        usuario.getCorreo(), usuario.getContrasena(), usuario.getRol()
                );
            }
        }
    }

    @Override
    public boolean actualizar(Usuario usuario) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> actualizar(conexion, usuario)
        );
    }

    /**
     * Actualiza la cuenta usando una transacción gestionada por el llamador.
     */
    public boolean actualizar(Connection conexion, Usuario usuario) throws SQLException {
        comprobarTransaccion(conexion);
        String sql = """
                UPDATE usuarios
                SET nombre = ?, rut = ?, correo = ?, `contraseña` = ?, rol = ?
                WHERE id = ?
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            asignarParametros(consulta, usuario);
            consulta.setInt(6, usuario.getId());

            return consulta.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> eliminar(conexion, id)
        );
    }

    /**
     * Elimina la cuenta usando una transacción gestionada por el llamador.
     */
    public boolean eliminar(Connection conexion, int id) throws SQLException {
        comprobarTransaccion(conexion);
        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);

            return consulta.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + " WHERE id = ?";

            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setInt(1, id);

                try (ResultSet registros = consulta.executeQuery()) {
                    if (registros.next()) {
                        return Optional.of(mapear(registros));
                    }

                    return Optional.empty();
                }
            }
        });
    }

    public Optional<Usuario> buscarPorRut(String rut) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + " WHERE rut = ?";

            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setString(1, rut);

                try (ResultSet registros = consulta.executeQuery()) {
                    if (registros.next()) {
                        return Optional.of(mapear(registros));
                    }

                    return Optional.empty();
                }
            }
        });
    }

    /**
     * Comprueba las credenciales respetando mayúsculas y minúsculas en la clave.
     */
    public Optional<Usuario> autenticar(String rut, String contrasena)
            throws SQLException {

        if (rut == null || contrasena == null) {
            return Optional.empty();
        }

        return buscarPorRut(rut).filter(
                usuario -> contrasena.equals(usuario.getContrasena())
        );
    }

    @Override
    public List<Usuario> listar() throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + " ORDER BY nombre, id";
            List<Usuario> usuarios = new ArrayList<>();

            try (PreparedStatement consulta = conexion.prepareStatement(sql);
                 ResultSet registros = consulta.executeQuery()) {

                while (registros.next()) {
                    usuarios.add(mapear(registros));
                }
            }

            return usuarios;
        });
    }

    private void comprobarTransaccion(Connection conexion) throws SQLException {
        if (conexion == null || conexion.getAutoCommit()) {
            throw new SQLException(
                    "Las operaciones de escritura requieren una transacción."
            );
        }
    }

    private void asignarParametros(PreparedStatement consulta, Usuario usuario)
            throws SQLException {

        consulta.setString(1, usuario.getNombre());
        consulta.setString(2, usuario.getRut());
        consulta.setString(3, usuario.getCorreo());
        consulta.setString(4, usuario.getContrasena());
        consulta.setString(5, usuario.getRol());
    }

    private Usuario mapear(ResultSet registros) throws SQLException {
        return crearUsuario(
                registros.getInt("id"), registros.getString("nombre"),
                registros.getString("rut"), registros.getString("correo"),
                registros.getString("contrasena"), registros.getString("rol")
        );
    }

    private Usuario crearUsuario(int id, String nombre, String rut, String correo,
                                 String contrasena, String rol) throws SQLException {

        if ("bibliotecario".equals(rol)) {
            return new Bibliotecario(id, nombre, rut, correo, contrasena);
        }

        if ("estudiante".equals(rol)) {
            return new UsuarioEstudiante(id, nombre, rut, correo, contrasena);
        }

        throw new SQLException("El rol del usuario no es válido: " + rol);
    }
}