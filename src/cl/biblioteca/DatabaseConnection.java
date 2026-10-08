package cl.biblioteca.config;

import cl.biblioteca.interfaces.OperacionBD;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Singleton responsable de la conexión JDBC.
 * Coordina el acceso para que los hilos no compartan una transacción.
 */
public final class DatabaseConnection {

    private static final DatabaseConnection INSTANCIA =
            new DatabaseConnection();

    private Connection conexion;

    private DatabaseConnection() {
    }

    public static DatabaseConnection getInstance() {
        return INSTANCIA;
    }

    /**
     * Ejecuta una operación con acceso exclusivo a la conexión.
     */
    public synchronized <T> T ejecutar(OperacionBD<T> operacion)
            throws SQLException {

        Objects.requireNonNull(
                operacion, "La operación no puede ser nula."
        );

        return operacion.ejecutar(obtenerConexion());
    }

    /**
     * Confirma todas las modificaciones juntas o las revierte si fallan.
     * Los DAO participantes deben utilizar la conexión recibida.
     */
    public synchronized <T> T ejecutarTransaccion(OperacionBD<T> operacion)
            throws SQLException {

        Objects.requireNonNull(
                operacion, "La operación no puede ser nula."
        );

        Connection actual = obtenerConexion();

        if (!actual.getAutoCommit()) {
            throw new SQLException(
                    "No se permiten transacciones anidadas."
            );
        }

        try {
            actual.setAutoCommit(false);
        } catch (SQLException e) {
            descartarConexion(e);
            throw e;
        }

        Throwable fallo = null;

        try {
            T resultado = operacion.ejecutar(actual);
            actual.commit();
            return resultado;

        } catch (SQLException | RuntimeException | Error e) {
            fallo = e;

            try {
                actual.rollback();
            } catch (SQLException errorRollback) {
                e.addSuppressed(errorRollback);

                // Una reversión fallida deja la transacción en estado incierto.
                descartarConexion(e);
            }

            throw e;

        } finally {
            if (conexion == actual) {
                try {
                    actual.setAutoCommit(true);
                } catch (SQLException errorRestauracion) {
                    descartarConexion(errorRestauracion);

                    if (fallo != null) {
                        fallo.addSuppressed(errorRestauracion);
                    } else {
                        throw new SQLException(
                                "La operación se confirmó, pero fue necesario cerrar "
                                        + "la conexión. Compruebe los datos antes de repetirla.",
                                "08006",
                                errorRestauracion
                        );
                    }
                }
            }
        }
    }

    /**
     * Cierra la conexión al terminar la aplicación.
     */
    public synchronized void cerrar() throws SQLException {
        if (conexion != null) {
            Connection anterior = conexion;
            conexion = null;
            anterior.close();
        }
    }

    private void descartarConexion(Throwable fallo) {
        Connection anterior = conexion;
        conexion = null;

        if (anterior != null) {
            try {
                anterior.close();
            } catch (SQLException errorCierre) {
                fallo.addSuppressed(errorCierre);
            }
        }
    }

    private Connection obtenerConexion() throws SQLException {
        if (conexion == null || conexion.isClosed()) {
            ConfiguracionBD configuracion = ConfiguracionBD.cargar();

            conexion = DriverManager.getConnection(
                    configuracion.getUrl(),
                    configuracion.getUsuario(),
                    configuracion.getContrasena()
            );
        }

        return conexion;
    }
}