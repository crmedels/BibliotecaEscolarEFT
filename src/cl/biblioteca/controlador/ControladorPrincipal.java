package cl.biblioteca.controlador;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.VentanaPrincipal;

import java.util.Objects;

/**
 * Coordina el cierre de sesión y la salida desde el menú principal.
 */
public final class ControladorPrincipal {

    private final VentanaPrincipal ventana;
    private final Runnable alCerrarSesion;
    private boolean cerrando;

    public ControladorPrincipal(VentanaPrincipal ventana, Runnable alCerrarSesion) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        this.alCerrarSesion = Objects.requireNonNull(
                alCerrarSesion, "La acción para volver al acceso es obligatoria."
        );

        ventana.alCerrarSesion(evento -> cerrar(true));
        ventana.alSalir(() -> cerrar(false));
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    private void cerrar(boolean volverAlAcceso) {
        if (cerrando || ventana.estaOcupada() || !ventana.isDisplayable()) {
            return;
        }

        cerrando = true;
        ventana.establecerOcupada(true, volverAlAcceso
                ? "Cerrando sesión..." : "Cerrando la aplicación...");

        TareaBD<Void> tarea = new TareaBD<>(
                ventana,
                () -> {
                    DatabaseConnection.getInstance().cerrar();
                    return null;
                },
                resultado -> completarCierre(volverAlAcceso),
                this::finalizarCierre
        );
        tarea.execute();
    }

    private void completarCierre(boolean volverAlAcceso) {
        if (volverAlAcceso) {
            alCerrarSesion.run();
        }
        ventana.dispose();
    }

    private void finalizarCierre() {
        cerrando = false;

        if (ventana.isDisplayable()) {
            ventana.establecerOcupada(false, "Seleccione una opción para continuar.");
        }
    }
}