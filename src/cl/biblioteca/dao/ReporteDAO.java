package cl.biblioteca.dao;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.modelo.LibroMasPrestado;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Obtiene el ranking de libros según su historial de préstamos.
 */
public final class ReporteDAO {

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();

    public List<LibroMasPrestado> listarLibrosMasPrestados() throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = """
                    SELECT l.id AS id_libro,
                           l.titulo,
                           l.autor,
                           l.isbn,
                           COUNT(p.id) AS cantidad_prestamos
                    FROM libros l
                    INNER JOIN prestamos p ON p.id_libro = l.id
                    GROUP BY l.id, l.titulo, l.autor, l.isbn
                    ORDER BY cantidad_prestamos DESC, l.titulo ASC, l.id ASC
                    """;

            List<LibroMasPrestado> libros = new ArrayList<>();

            try (PreparedStatement consulta = conexion.prepareStatement(sql);
                 ResultSet registros = consulta.executeQuery()) {

                while (registros.next()) {
                    libros.add(mapear(registros));
                }
            }

            return libros;
        });
    }

    private LibroMasPrestado mapear(ResultSet registros) throws SQLException {
        return new LibroMasPrestado(
                registros.getInt("id_libro"),
                registros.getString("titulo"),
                registros.getString("autor"),
                registros.getString("isbn"),
                registros.getLong("cantidad_prestamos")
        );
    }
}