package cl.biblioteca.main;

import cl.biblioteca.controlador.ControladorLogin;
import cl.biblioteca.controlador.ControladorPrincipal;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.util.ManejadorErrores;
import cl.biblioteca.vista.VentanaLogin;
import cl.biblioteca.vista.VentanaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 * Inicia la interfaz y conecta el acceso con el menú principal.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            configurarApariencia();

            try {
                mostrarLogin();
            } catch (RuntimeException error) {
                ManejadorErrores.mostrar(null, error);
            }
        });
    }

    private static void configurarApariencia() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException | UnsupportedLookAndFeelException error) {
            System.err.println(
                    "Se utilizará la apariencia predeterminada de Swing."
            );
        }
    }

    private static void mostrarLogin() {
        VentanaLogin ventana = new VentanaLogin();
        ControladorLogin controlador = new ControladorLogin(
                ventana, Main::mostrarPrincipal
        );
        controlador.mostrar();
    }

    private static void mostrarPrincipal(SesionUsuario sesion) {
        VentanaPrincipal ventana = new VentanaPrincipal(sesion);
        ControladorPrincipal controlador = new ControladorPrincipal(
                ventana, Main::mostrarLogin
        );
        controlador.mostrar();
    }
}