package cl.biblioteca.interfaces;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Define una operación que utiliza la conexión compartida.
 * Cada operación debe cerrar sus sentencias y resultados,
 * pero no debe cerrar la conexión.
 */
@FunctionalInterface
public interface OperacionBD<T> {

    T ejecutar(Connection conexion) throws SQLException;
}