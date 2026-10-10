package cl.biblioteca.controlador;

import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.EstudianteServicio;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.VentanaEstudiantes;

import javax.swing.JOptionPane;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina la gestión de estudiantes y sus cuentas de acceso en segundo plano.
 */
public final class ControladorEstudiantes {

    private static final String ESTADO_INICIAL =
            "Seleccione un estudiante para editarlo, o pulse Nuevo.";

    private final VentanaEstudiantes ventana;
    private final EstudianteServicio servicio;
    private String estado = ESTADO_INICIAL;
    private String mensajeCambioPendiente;

    public ControladorEstudiantes(VentanaEstudiantes ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        servicio = new EstudianteServicio(sesion);
        sesion.exigirAdministracion();

        ventana.alNuevo(evento -> nuevo());
        ventana.alGuardar(evento -> guardar());
        ventana.alEliminar(evento -> eliminar());
        ventana.alRecargar(evento -> cargarEstudiantes("Listado actualizado."));
        ventana.alSeleccionar(this::seleccionarEstudiante);
        ventana.alCerrar(this::cerrar);
    }

    public void mostrar() {
        if (!ventana.isDisplayable()) {
            return;
        }

        cargarEstudiantes("Listado cargado.");
        ventana.setVisible(true);
    }

    private void nuevo() {
        if (!estaDisponible()) {
            return;
        }

        ventana.limpiarFormulario();
        estado = "Complete la ficha y una contraseña para crear el acceso del estudiante.";
        ventana.establecerOcupada(false, estado);
        ventana.enfocarNombre();
    }

    private void seleccionarEstudiante() {
        if (!estaDisponible()) {
            return;
        }

        ventana.mostrarEstudianteSeleccionado();
        estado = ventana.getEstudianteSeleccionado() == null ? ESTADO_INICIAL
                : "Edite la ficha. Una contraseña vacía conserva la actual.";
        ventana.establecerOcupada(false, estado);
    }

    private void guardar() {
        if (!estaDisponible()) {
            return;
        }

        Estudiante seleccionado = ventana.getEstudianteSeleccionado();
        int id = seleccionado == null ? 0 : seleccionado.getId();

        Estudiante datos = new Estudiante(
                id, ventana.getNombre(), ventana.getRut(),
                ventana.getCurso(), ventana.getCorreo()
        );

        char[] caracteres = ventana.getContrasena();
        String contrasena = new String(caracteres);
        Arrays.fill(caracteres, '\0');

        ejecutarCambio(
                "Guardando estudiante...",
                () -> id > 0
                        ? servicio.actualizar(datos, contrasena)
                        : servicio.guardar(datos, contrasena),
                id > 0 ? "Estudiante actualizado." : "Estudiante y cuenta creados."
        );
    }

    private void eliminar() {
        if (!estaDisponible()) {
            return;
        }

        Estudiante seleccionado = ventana.getEstudianteSeleccionado();
        if (seleccionado == null) {
            return;
        }
        int id = seleccionado.getId();

        int respuesta = JOptionPane.showConfirmDialog(
                ventana,
                "¿Desea eliminar esta ficha y su cuenta de acceso, si existe?",
                "Eliminar estudiante",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        ejecutarCambio(
                "Eliminando estudiante...",
                () -> {
                    servicio.eliminar(id);
                    return null;
                },
                "Estudiante eliminado."
        );
    }

    private void cargarEstudiantes(String mensajeExito) {
        ejecutarTarea(
                "Cargando estudiantes...",
                servicio::listar,
                estudiantes -> {
                    ventana.mostrarEstudiantes(estudiantes);
                    estado = mensajeExito + " Estudiantes: " + estudiantes.size() + ".";
                }
        );
    }

    private void ejecutarCambio(String mensajeTrabajo, Callable<?> cambio,
                                String mensajeExito) {
        ejecutarTarea(
                mensajeTrabajo,
                () -> {
                    cambio.call();
                    return null;
                },
                resultado -> {
                    ventana.limpiarFormulario();
                    estado = mensajeExito + " Pulse Recargar para actualizar el listado.";
                    mensajeCambioPendiente = mensajeExito;
                }
        );
    }

    private <T> void ejecutarTarea(String mensaje, Callable<T> operacion,
                                   Consumer<T> alCompletar) {
        if (!estaDisponible()) {
            return;
        }

        ventana.establecerOcupada(true, mensaje);
        new TareaBD<T>(
                ventana,
                operacion,
                resultado -> {
                    if (ventana.isDisplayable()) {
                        alCompletar.accept(resultado);
                    }
                },
                this::finalizarTarea
        ).execute();
    }

    private void finalizarTarea() {
        String mensajePendiente = mensajeCambioPendiente;
        mensajeCambioPendiente = null;

        if (!ventana.isDisplayable()) {
            return;
        }

        ventana.establecerOcupada(false, estado);

        if (mensajePendiente != null) {
            cargarEstudiantes(mensajePendiente);
        } else {
            ventana.enfocarNombre();
        }
    }

    private void cerrar() {
        if (estaDisponible()) {
            ventana.limpiarContrasena();
            ventana.dispose();
        }
    }

    private boolean estaDisponible() {
        return ventana.isDisplayable() && !ventana.estaOcupada();
    }
}