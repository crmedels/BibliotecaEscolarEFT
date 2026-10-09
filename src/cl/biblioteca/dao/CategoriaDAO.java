package cl.biblioteca.dao;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Categoria;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona la persistencia de las categorías mediante JDBC.
 */
public final class CategoriaDAO implements CrudDAO<Categoria> {

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();

    @Override
    public Categoria guardar(Categoria categoria) throws SQLException {
        return baseDatos.ejecutarTransaccion(conexion -> {
            String sql = "INSERT INTO categorias (nombre) VALUES (?)";

            try (PreparedStatement consulta = conexion.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            )) {
                consulta.setString(1, categoria.getNombre());

                if (consulta.executeUpdate() != 1) {
                    throw new SQLException("No se pudo guardar la categoría.");
                }

                try (ResultSet claves = consulta.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException(
                                "No se pudo obtener el ID de la categoría."
                        );
                    }

                    return new Categoria(claves.getInt(1), categoria.getNombre());
                }
            }
        });
    }

    @Override
    public boolean actualizar(Categoria categoria) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = "UPDATE categorias SET nombre = ? WHERE id = ?";

            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setString(1, categoria.getNombre());
                consulta.setInt(2, categoria.getId());

                return consulta.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = "DELETE FROM categorias WHERE id = ?";

            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setInt(1, id);

                return consulta.executeUpdate() > 0;
            }
        });
    }

    @Override
    public Optional<Categoria> buscarPorId(int id) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = "SELECT id, nombre FROM categorias WHERE id = ?";

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

    @Override
    public List<Categoria> listar() throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = "SELECT id, nombre FROM categorias ORDER BY nombre, id";
            List<Categoria> categorias = new ArrayList<>();

            try (PreparedStatement consulta = conexion.prepareStatement(sql);
                 ResultSet registros = consulta.executeQuery()) {

                while (registros.next()) {
                    categorias.add(mapear(registros));
                }
            }

            return categorias;
        });
    }

    private Categoria mapear(ResultSet registros) throws SQLException {
        return new Categoria(
                registros.getInt("id"),
                registros.getString("nombre")
        );
    }
}