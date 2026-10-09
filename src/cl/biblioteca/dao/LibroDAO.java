package cl.biblioteca.dao;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona el catálogo y los cambios de stock mediante JDBC.
 */
public final class LibroDAO implements CrudDAO<Libro> {

    private static final String CONSULTA_BASE = """
            SELECT l.id, l.titulo, l.autor, l.isbn, l.editorial,
                   l.stock, l.id_categoria, c.nombre AS nombre_categoria
            FROM libros l
            LEFT JOIN categorias c ON c.id = l.id_categoria
            """;

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();

    @Override
    public Libro guardar(Libro libro) throws SQLException {
        return baseDatos.ejecutarTransaccion(conexion -> {
            String sql = """
                    INSERT INTO libros
                        (titulo, autor, isbn, editorial, stock, id_categoria)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement consulta = conexion.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS
            )) {
                asignarParametros(consulta, libro);

                if (consulta.executeUpdate() != 1) {
                    throw new SQLException("No se pudo guardar el libro.");
                }

                try (ResultSet claves = consulta.getGeneratedKeys()) {
                    if (!claves.next()) {
                        throw new SQLException("No se pudo obtener el ID del libro.");
                    }

                    return new Libro(
                            claves.getInt(1), libro.getTitulo(), libro.getAutor(),
                            libro.getIsbn(), libro.getEditorial(), libro.getStock(),
                            libro.getCategoria()
                    );
                }
            }
        });
    }

    @Override
    public boolean actualizar(Libro libro) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = """
                    UPDATE libros
                    SET titulo = ?, autor = ?, isbn = ?, editorial = ?,
                        stock = ?, id_categoria = ?
                    WHERE id = ?
                    """;

            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                asignarParametros(consulta, libro);
                consulta.setInt(7, libro.getId());

                return consulta.executeUpdate() > 0;
            }
        });
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = "DELETE FROM libros WHERE id = ?";

            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                consulta.setInt(1, id);

                return consulta.executeUpdate() > 0;
            }
        });
    }

    @Override
    public Optional<Libro> buscarPorId(int id) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + " WHERE l.id = ?";

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
    public List<Libro> listar() throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + " ORDER BY l.titulo, l.id";
            List<Libro> libros = new ArrayList<>();

            try (PreparedStatement consulta = conexion.prepareStatement(sql);
                 ResultSet registros = consulta.executeQuery()) {

                while (registros.next()) {
                    libros.add(mapear(registros));
                }
            }

            return libros;
        });
    }

    /**
     * Descuenta un ejemplar dentro de la transacción de un préstamo.
     * Devuelve false si el libro no existe o no tiene stock disponible.
     */
    public synchronized boolean descontarStock(Connection conexion, int idLibro)
            throws SQLException {

        comprobarTransaccion(conexion);
        String sql = "UPDATE libros SET stock = stock - 1 WHERE id = ? AND stock > 0";

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, idLibro);

            return consulta.executeUpdate() > 0;
        }
    }

    /**
     * Repone un ejemplar dentro de la transacción de una devolución.
     * El servicio comprobará primero que el préstamo siga pendiente.
     */
    public synchronized boolean reponerStock(Connection conexion, int idLibro)
            throws SQLException {

        comprobarTransaccion(conexion);
        String sql = "UPDATE libros SET stock = stock + 1 WHERE id = ?";

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, idLibro);

            return consulta.executeUpdate() > 0;
        }
    }

    private void comprobarTransaccion(Connection conexion) throws SQLException {
        if (conexion == null || conexion.getAutoCommit()) {
            throw new SQLException(
                    "Los cambios de stock requieren una transacción."
            );
        }
    }

    private void asignarParametros(PreparedStatement consulta, Libro libro)
            throws SQLException {

        consulta.setString(1, libro.getTitulo());
        consulta.setString(2, libro.getAutor());
        consulta.setString(3, libro.getIsbn());
        consulta.setString(4, libro.getEditorial());
        consulta.setInt(5, libro.getStock());

        if (libro.getCategoria() == null) {
            consulta.setNull(6, Types.INTEGER);
        } else {
            consulta.setInt(6, libro.getCategoria().getId());
        }
    }

    private Libro mapear(ResultSet registros) throws SQLException {
        int idCategoria = registros.getInt("id_categoria");
        Categoria categoria = registros.wasNull() ? null : new Categoria(
                idCategoria, registros.getString("nombre_categoria")
        );

        return new Libro(
                registros.getInt("id"),
                registros.getString("titulo"),
                registros.getString("autor"),
                registros.getString("isbn"),
                registros.getString("editorial"),
                registros.getInt("stock"),
                categoria
        );
    }
}