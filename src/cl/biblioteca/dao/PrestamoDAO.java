package cl.biblioteca.dao;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.Prestamo;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Gestiona los préstamos y recupera sus estudiantes, libros y fechas.
 */
public final class PrestamoDAO implements CrudDAO<Prestamo> {

    private static final String CONSULTA_BASE = """
            SELECT p.id AS id_prestamo, p.fecha_prestamo,
                   p.fecha_devolucion, p.fecha_devolucion_real, p.devuelto,
                   e.id AS id_estudiante, e.nombre AS nombre_estudiante,
                   e.rut AS rut_estudiante, e.curso, e.correo AS correo_estudiante,
                   l.id AS id_libro, l.titulo, l.autor, l.isbn, l.editorial, l.stock,
                   c.id AS id_categoria, c.nombre AS nombre_categoria
            FROM prestamos p
            LEFT JOIN estudiantes e ON e.id = p.id_estudiante
            LEFT JOIN libros l ON l.id = p.id_libro
            LEFT JOIN categorias c ON c.id = l.id_categoria
            """;

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();

    @Override
    public Prestamo guardar(Prestamo prestamo) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> guardar(conexion, prestamo)
        );
    }

    /**
     * Inserta el préstamo en una transacción gestionada por el llamador.
     */
    public Prestamo guardar(Connection conexion, Prestamo prestamo) throws SQLException {
        comprobarTransaccion(conexion);
        String sql = """
                INSERT INTO prestamos
                    (id_estudiante, id_libro, fecha_prestamo,
                     fecha_devolucion, fecha_devolucion_real, devuelto)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS
        )) {
            asignarParametros(consulta, prestamo);

            if (consulta.executeUpdate() != 1) {
                throw new SQLException("No se pudo guardar el préstamo.");
            }

            try (ResultSet claves = consulta.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new SQLException("No se pudo obtener el ID del préstamo.");
                }

                return new Prestamo(
                        claves.getInt(1), prestamo.getEstudiante(), prestamo.getLibro(),
                        prestamo.getFechaPrestamo(), prestamo.getFechaDevolucion(),
                        prestamo.getFechaDevolucionReal(), prestamo.isDevuelto()
                );
            }
        }
    }

    @Override
    public boolean actualizar(Prestamo prestamo) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> actualizar(conexion, prestamo)
        );
    }

    public boolean actualizar(Connection conexion, Prestamo prestamo) throws SQLException {
        comprobarTransaccion(conexion);
        String sql = """
                UPDATE prestamos
                SET id_estudiante = ?, id_libro = ?, fecha_prestamo = ?,
                    fecha_devolucion = ?, fecha_devolucion_real = ?, devuelto = ?
                WHERE id = ?
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            asignarParametros(consulta, prestamo);
            consulta.setInt(7, prestamo.getId());

            return consulta.executeUpdate() > 0;
        }
    }

    @Override
    public boolean eliminar(int id) throws SQLException {
        return baseDatos.ejecutarTransaccion(
                conexion -> eliminar(conexion, id)
        );
    }

    public boolean eliminar(Connection conexion, int id) throws SQLException {
        comprobarTransaccion(conexion);
        String sql = "DELETE FROM prestamos WHERE id = ?";

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);

            return consulta.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Prestamo> buscarPorId(int id) throws SQLException {
        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + " WHERE p.id = ?";

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
    public List<Prestamo> listar() throws SQLException {
        return consultarLista("", null);
    }

    public List<Prestamo> listarPorEstudiante(int idEstudiante) throws SQLException {
        return consultarLista(" WHERE p.id_estudiante = ?", idEstudiante);
    }

    public List<Prestamo> listarPendientes() throws SQLException {
        return consultarLista(" WHERE p.devuelto = FALSE", null);
    }

    /**
     * Registra una devolución únicamente si el préstamo sigue pendiente.
     * El servicio repondrá el stock en esta misma transacción si devuelve true.
     */
    public boolean marcarDevuelto(Connection conexion, int id, LocalDate fechaReal)
            throws SQLException {

        comprobarTransaccion(conexion);
        if (fechaReal == null) {
            throw new SQLException("La devolución requiere su fecha real.");
        }

        String sql = """
                UPDATE prestamos
                SET devuelto = TRUE, fecha_devolucion_real = ?
                WHERE id = ? AND devuelto = FALSE
                """;

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setDate(1, Date.valueOf(fechaReal));
            consulta.setInt(2, id);

            return consulta.executeUpdate() > 0;
        }
    }

    private List<Prestamo> consultarLista(String filtro, Integer idEstudiante)
            throws SQLException {

        return baseDatos.ejecutar(conexion -> {
            String sql = CONSULTA_BASE + filtro
                    + " ORDER BY p.fecha_prestamo DESC, p.id DESC";
            List<Prestamo> prestamos = new ArrayList<>();

            try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
                if (idEstudiante != null) {
                    consulta.setInt(1, idEstudiante);
                }

                try (ResultSet registros = consulta.executeQuery()) {
                    while (registros.next()) {
                        prestamos.add(mapear(registros));
                    }
                }
            }

            return prestamos;
        });
    }

    private void comprobarTransaccion(Connection conexion) throws SQLException {
        if (conexion == null || conexion.getAutoCommit()) {
            throw new SQLException(
                    "Las operaciones de escritura requieren una transacción."
            );
        }
    }

    private void asignarParametros(PreparedStatement consulta, Prestamo prestamo)
            throws SQLException {

        if (prestamo.getEstudiante() == null) {
            consulta.setNull(1, Types.INTEGER);
        } else {
            consulta.setInt(1, prestamo.getEstudiante().getId());
        }

        if (prestamo.getLibro() == null) {
            consulta.setNull(2, Types.INTEGER);
        } else {
            consulta.setInt(2, prestamo.getLibro().getId());
        }

        asignarFecha(consulta, 3, prestamo.getFechaPrestamo());
        asignarFecha(consulta, 4, prestamo.getFechaDevolucion());
        asignarFecha(consulta, 5, prestamo.getFechaDevolucionReal());
        consulta.setBoolean(6, prestamo.isDevuelto());
    }

    private void asignarFecha(PreparedStatement consulta, int indice, LocalDate fecha)
            throws SQLException {

        consulta.setDate(indice, fecha == null ? null : Date.valueOf(fecha));
    }

    private LocalDate leerFecha(ResultSet registros, String columna) throws SQLException {
        Date fecha = registros.getDate(columna);
        return fecha == null ? null : fecha.toLocalDate();
    }

    private Prestamo mapear(ResultSet registros) throws SQLException {
        int idEstudiante = registros.getInt("id_estudiante");
        Estudiante estudiante = registros.wasNull() ? null : new Estudiante(
                idEstudiante, registros.getString("nombre_estudiante"),
                registros.getString("rut_estudiante"), registros.getString("curso"),
                registros.getString("correo_estudiante")
        );

        int idLibro = registros.getInt("id_libro");
        Libro libro = null;
        if (!registros.wasNull()) {
            int idCategoria = registros.getInt("id_categoria");
            Categoria categoria = registros.wasNull() ? null : new Categoria(
                    idCategoria, registros.getString("nombre_categoria")
            );

            libro = new Libro(
                    idLibro, registros.getString("titulo"), registros.getString("autor"),
                    registros.getString("isbn"), registros.getString("editorial"),
                    registros.getInt("stock"), categoria
            );
        }

        return new Prestamo(
                registros.getInt("id_prestamo"), estudiante, libro,
                leerFecha(registros, "fecha_prestamo"),
                leerFecha(registros, "fecha_devolucion"),
                leerFecha(registros, "fecha_devolucion_real"),
                registros.getBoolean("devuelto")
        );
    }
}