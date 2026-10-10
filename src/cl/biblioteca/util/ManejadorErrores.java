package cl.biblioteca.util;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

/**
 * Presenta los errores de forma uniforme en las ventanas Swing.
 */
public final class ManejadorErrores {

    private ManejadorErrores() {
    }

    public static void mostrar(Component ventana, Throwable error) {
        Throwable causa = desenvolver(error);
        boolean aviso = causa instanceof IllegalArgumentException
                || causa instanceof IllegalStateException;
        String mensaje = obtenerMensaje(causa);

        if (!aviso && causa != null) {
            causa.printStackTrace(System.err);
        }

        Runnable mostrarDialogo = () -> JOptionPane.showMessageDialog(
                ventana,
                mensaje,
                aviso ? "Biblioteca Escolar - Aviso" : "Biblioteca Escolar - Error",
                aviso ? JOptionPane.WARNING_MESSAGE : JOptionPane.ERROR_MESSAGE
        );

        if (SwingUtilities.isEventDispatchThread()) {
            mostrarDialogo.run();
        } else {
            SwingUtilities.invokeLater(mostrarDialogo);
        }
    }

    public static String obtenerMensaje(Throwable error) {
        Throwable causa = desenvolver(error);

        if (causa instanceof IllegalArgumentException
                || causa instanceof IllegalStateException) {
            String mensaje = causa.getMessage();
            if (mensaje != null && !mensaje.isBlank()) {
                return mensaje;
            }
        }

        if (causa instanceof SQLException errorSQL) {
            String estado = errorSQL.getSQLState();

            if ("CONFIG".equals(estado)) {
                return "No se pudo cargar la configuración de la base de datos. "
                        + "Revise el archivo config/basedatos.properties.";
            }

            if ("08006".equals(estado)) {
                return "La conexión se interrumpió. Actualice el listado para comprobar "
                        + "los cambios antes de repetir la operación.";
            }

            if (estado != null && estado.startsWith("08")) {
                return "No fue posible conectarse con la base de datos. "
                        + "Compruebe que MySQL esté iniciado y revise su configuración.";
            }

            if (estado != null && estado.startsWith("28")) {
                return "No se pudo acceder a la base de datos. "
                        + "Revise el usuario y la contraseña de MySQL en la configuración.";
            }

            if (estado != null && estado.startsWith("23")) {
                return "No se pudo guardar el cambio porque existen datos duplicados "
                        + "o registros relacionados.";
            }

            return "No se pudo completar la operación en la base de datos.";
        }

        return "Ocurrió un error inesperado al realizar la operación.";
    }

    private static Throwable desenvolver(Throwable error) {
        Throwable causa = error;

        while (causa instanceof ExecutionException && causa.getCause() != null) {
            causa = causa.getCause();
        }

        return causa;
    }
}