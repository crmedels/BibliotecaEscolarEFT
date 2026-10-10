package cl.biblioteca.controlador;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.AutenticacionServicio;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.VentanaLogin;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Coordina el acceso y el cierre de la ventana de inicio de sesión.
 */
public final class ControladorLogin {

    private static final String ESTADO_INICIAL =
            "Ingrese sus credenciales para continuar.";

    private final VentanaLogin ventana;
    private final AutenticacionServicio autenticacion = new AutenticacionServicio();
    private final Consumer<SesionUsuario> alAutenticar;
    private boolean cierreSolicitado;
    private boolean cerrando;

    public ControladorLogin(VentanaLogin ventana, Consumer<SesionUsuario> alAutenticar) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        this.alAutenticar = Objects.requireNonNull(
                alAutenticar, "La acción después del acceso es obligatoria."
        );

        ventana.alIngresar(evento -> ingresar());
        ventana.alCerrar(this::solicitarCierre);
    }

    public void mostrar() {
        ventana.setVisible(true);
        ventana.enfocarRut();
    }

    private void ingresar() {
        if (ventana.estaOcupada() || cierreSolicitado || !ventana.isDisplayable()) {
            return;
        }

        String rut = ventana.getRut();
        char[] caracteres = ventana.getContrasena();
        String contrasena = new String(caracteres);
        Arrays.fill(caracteres, '\0');

        ventana.establecerOcupada(true, "Verificando acceso...");

        new TareaBD<SesionUsuario>(
                ventana,
                () -> autenticacion.iniciarSesion(rut, contrasena),
                this::procesarAcceso,
                this::finalizarAcceso
        ).execute();
    }

    private void procesarAcceso(SesionUsuario sesion) {
        if (cierreSolicitado || !ventana.isDisplayable()) {
            return;
        }

        alAutenticar.accept(sesion);
        ventana.dispose();
    }

    private void finalizarAcceso() {
        ventana.limpiarContrasena();

        if (!ventana.isDisplayable()) {
            return;
        }

        if (cierreSolicitado) {
            cerrarAplicacion();
            return;
        }

        ventana.establecerOcupada(false, ESTADO_INICIAL);
        ventana.enfocarRut();
    }

    private void solicitarCierre() {
        if (cerrando || !ventana.isDisplayable()) {
            return;
        }

        cierreSolicitado = true;

        if (ventana.estaOcupada()) {
            ventana.establecerOcupada(true, "Terminando la operación para cerrar...");
            return;
        }

        cerrarAplicacion();
    }

    private void cerrarAplicacion() {
        cerrando = true;
        ventana.establecerOcupada(true, "Cerrando aplicación...");

        new TareaBD<Void>(
                ventana,
                () -> {
                    DatabaseConnection.getInstance().cerrar();
                    return null;
                },
                ignorado -> ventana.dispose(),
                this::finalizarCierre
        ).execute();
    }

    private void finalizarCierre() {
        ventana.limpiarContrasena();
        cerrando = false;

        if (ventana.isDisplayable()) {
            cierreSolicitado = false;
            ventana.establecerOcupada(false, ESTADO_INICIAL);
            ventana.enfocarRut();
        }
    }
}