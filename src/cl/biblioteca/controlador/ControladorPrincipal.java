package cl.biblioteca.controlador;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.util.ManejadorErrores;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.VentanaCategorias;
import cl.biblioteca.vista.VentanaEstudiantes;
import cl.biblioteca.vista.VentanaLibros;
import cl.biblioteca.vista.VentanaPrincipal;

import java.util.Objects;

/**
 * Coordina las opciones del menú principal y el cierre de la sesión.
 */
public final class ControladorPrincipal {

    private static final String ESTADO_INICIAL =
            "Seleccione una opción para continuar.";

    private final VentanaPrincipal ventana;
    private final SesionUsuario sesion;
    private final Runnable alCerrarSesion;
    private boolean cerrando;

    public ControladorPrincipal(VentanaPrincipal ventana, SesionUsuario sesion,
                                Runnable alCerrarSesion) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");

        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        this.sesion = sesion;
        this.alCerrarSesion = Objects.requireNonNull(
                alCerrarSesion, "La acción para volver al acceso es obligatoria."
        );

        ventana.alLibros(evento -> abrirLibros());
        ventana.alCerrarSesion(evento -> cerrar(true));
        ventana.alSalir(() -> cerrar(false));

        if (sesion.puedeAdministrar()) {
            ventana.alCategorias(evento -> abrirCategorias());
            ventana.alEstudiantes(evento -> abrirEstudiantes());
        }
    }

    public void mostrar() {
        ventana.setVisible(true);
    }

    private void abrirLibros() {
        if (!estaDisponible()) {
            return;
        }

        try {
            ventana.establecerOcupada(true, sesion.puedeAdministrar()
                    ? "Gestionando libros..." : "Consultando catálogo...");

            VentanaLibros ventanaLibros = new VentanaLibros(ventana, sesion);

            try {
                ControladorLibros controlador = new ControladorLibros(
                        ventanaLibros, sesion
                );
                controlador.mostrar();
            } finally {
                ventanaLibros.dispose();
            }

        } catch (RuntimeException error) {
            ManejadorErrores.mostrar(ventana, error);

        } finally {
            if (ventana.isDisplayable()) {
                ventana.establecerOcupada(false, ESTADO_INICIAL);
            }
        }
    }

    private void abrirCategorias() {
        if (!estaDisponible()) {
            return;
        }

        try {
            sesion.exigirAdministracion();
            ventana.establecerOcupada(true, "Gestionando categorías...");

            VentanaCategorias ventanaCategorias = new VentanaCategorias(ventana);

            try {
                ControladorCategorias controlador = new ControladorCategorias(
                        ventanaCategorias, sesion
                );
                controlador.mostrar();
            } finally {
                ventanaCategorias.dispose();
            }

        } catch (RuntimeException error) {
            ManejadorErrores.mostrar(ventana, error);

        } finally {
            if (ventana.isDisplayable()) {
                ventana.establecerOcupada(false, ESTADO_INICIAL);
            }
        }
    }

    private void abrirEstudiantes() {
        if (!estaDisponible()) {
            return;
        }

        try {
            sesion.exigirAdministracion();
            ventana.establecerOcupada(true, "Gestionando estudiantes...");

            VentanaEstudiantes ventanaEstudiantes = new VentanaEstudiantes(ventana);

            try {
                ControladorEstudiantes controlador = new ControladorEstudiantes(
                        ventanaEstudiantes, sesion
                );
                controlador.mostrar();
            } finally {
                ventanaEstudiantes.dispose();
            }

        } catch (RuntimeException error) {
            ManejadorErrores.mostrar(ventana, error);

        } finally {
            if (ventana.isDisplayable()) {
                ventana.establecerOcupada(false, ESTADO_INICIAL);
            }
        }
    }

    private void cerrar(boolean volverAlAcceso) {
        if (!estaDisponible()) {
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
            ventana.establecerOcupada(false, ESTADO_INICIAL);
        }
    }

    private boolean estaDisponible() {
        return !cerrando && !ventana.estaOcupada() && ventana.isDisplayable();
    }
}