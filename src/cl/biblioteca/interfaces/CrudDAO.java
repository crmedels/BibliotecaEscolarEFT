package cl.biblioteca.interfaces;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Define las operaciones CRUD comunes para los modelos.
 * Los errores de JDBC se propagan mediante SQLException.
 */
public interface CrudDAO<T> {

    /**
     * Inserta una entidad y la devuelve con el ID generado.
     */
    T guardar(T entidad) throws SQLException;

    /**
     * Actualiza una entidad.
     * Devuelve false si el registro no existe.
     */
    boolean actualizar(T entidad) throws SQLException;

    /**
     * Elimina un registro por su ID.
     * Devuelve false si el registro no existe.
     */
    boolean eliminar(int id) throws SQLException;

    /**
     * Busca un registro por su ID.
     * Devuelve Optional.empty() si no existe.
     */
    Optional<T> buscarPorId(int id) throws SQLException;

    /**
     * Consulta los registros.
     * Devuelve una lista vacía si no hay resultados.
     */
    List<T> listar() throws SQLException;
}