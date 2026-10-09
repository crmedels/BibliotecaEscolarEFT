package cl.biblioteca.dao;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Estudiante;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona la persistencia de las fichas de estudiantes.
 */
public final class EstudianteDAO implements CrudDAO<Estudiante> {

    private static final String CONSULTA_BASE =
            "SELECT id, nombre, rut, curso, correo FROM estudiantes";

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();

    @Override
    public Estudiante guardar(Estudiante estudiante) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> guardar(conexion, estudiante)
        );
    }

    /**
     * Inserta la ficha dentro de una transacción gestionada por el llamador.
     */
    public Estudiante guardar(Connection conexion, Estudiante estudiante)
            throws SQLException {

        comprobarTransaccion(conexion);
        String sql = """
                INSERT INTO estudiantes (nombre, rut, curso, correo)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
        )) {
            asignarParametros(consulta, estudiante);

            if (consulta.executeUpdate() != 1) {
                throw new SQLException("No se pudo guardar el estudiante.");
            }

            try (ResultSet claves = consulta.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("No se pudo obtener el ID del estudiante.");
                }

                return new Estudiante(
                        claves.getInt(1), estudiante.getNombre(), estudiante.getRut(),
                        estudiante.getCurso(), estudiante.getCorreo()
                );
            }
        }
    }

    @Override
    public boolean actualizar(Estudiante estudiante) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> actualizar(conexion, estudiante)
        );
    }

    /**
     * Permite actualizar la ficha y su cuenta dentro de una misma transacción.
     */
    public boolean actualizar(Connection conexion, Estudiante estudiante)
            throws SQLException {

        comprobarTransaccion(conexion);
        String sql = """
                UPDATE estudiantes
                SET nombre = ?, rut = ?, curso = ?, correo = ?
                WHERE id = ?
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            asignarParametros(consulta, estudiante);
            consulta.setInt(5, estudiante.getId());

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
     * Permite eliminar la ficha y su cuenta dentro de una misma transacción.
     */
    public boolean eliminar(Connection conexion, int id) throws SQLException {
        comprobarTransaccion(conexion);
        String sql = "DELETE FROM estudiantes WHERE id = ?";

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);

            return consulta.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Estudiante> buscarPorId(int id) throws SQLException {
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

    public Optional<Estudiante> buscarPorRut(String rut) throws SQLException {
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

    @Override
    public List<Estudiante> listar() throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + " ORDER BY nombre, id";
            List<Estudiante> estudiantes = new ArrayList<>();

            try (PreparedStatement consulta = conexion.prepareStatement(sql);
                 ResultSet registros = consulta.executeQuery()) {

                while (registros.next()) {
                    estudiantes.add(mapear(registros));
                }
            }

            return estudiantes;
        });
    }

    private void comprobarTransaccion(Connection conexion) throws SQLException {
        if (conexion == null || conexion.getAutoCommit()) {
            throw new SQLException(
                    "Las operaciones de escritura requieren una transacción."
            );
        }
    }

    private void asignarParametros(PreparedStatement consulta, Estudiante estudiante)
            throws SQLException {

        consulta.setString(1, estudiante.getNombre());
        consulta.setString(2, estudiante.getRut());
        consulta.setString(3, estudiante.getCurso());
        consulta.setString(4, estudiante.getCorreo());
    }

    private Estudiante mapear(ResultSet registros) throws SQLException {
        return new Estudiante(
                registros.getInt("id"),
                registros.getString("nombre"),
                registros.getString("rut"),
                registros.getString("curso"),
                registros.getString("correo")
        );
    }
}