package cl.biblioteca.controlador;

import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.EstudianteServicio;
import cl.biblioteca.util.ManejadorErrores;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.DialogoEstudiante;
import cl.biblioteca.vista.VentanaEstudiantes;

import javax.swing.JOptionPane;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina el listado, el formulario y las cuentas de estudiantes en segundo plano.
 */
public final class ControladorEstudiantes {

    private static final String ESTADO_INICIAL =
            "Pulse Nuevo estudiante o seleccione uno para editarlo.";

    private final VentanaEstudiantes ventana;
    private final EstudianteServicio servicio;

    private String estado = ESTADO_INICIAL;
    private String mensajeCambioPendiente;

    public ControladorEstudiantes(VentanaEstudiantes ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(
                ventana, "La ventana es obligatoria."
        );

        servicio = new EstudianteServicio(sesion);
        sesion.exigirAdministracion();

        ventana.alNuevo(evento -> abrirFormulario(null));
        ventana.alEditar(evento -> editar());
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

    private void editar() {
        if (!estaDisponible()) {
            return;
        }

        Estudiante seleccionado = ventana.getEstudianteSeleccionado();

        if (seleccionado != null) {
            abrirFormulario(seleccionado);
        }
    }

    private void abrirFormulario(Estudiante estudiante) {
        if (!estaDisponible()) {
            return;
        }

        DialogoEstudiante dialogo = null;
        ventana.establecerOcupada(true, "Formulario de estudiante abierto.");

        try {
            dialogo = new DialogoEstudiante(ventana, estudiante);

            DialogoEstudiante formulario = dialogo;
            formulario.alGuardar(evento -> guardar(formulario));
            formulario.setVisible(true);

        } catch (RuntimeException e) {
            ManejadorErrores.mostrar(ventana, e);

        } finally {
            if (dialogo != null) {
                dialogo.dispose();
            }

            finalizarTarea();
        }
    }

    private void seleccionarEstudiante() {
        if (!estaDisponible()) {
            return;
        }

        estado = ventana.getEstudianteSeleccionado() == null
                ? ESTADO_INICIAL
                : "Pulse Editar estudiante para modificar la ficha seleccionada.";

        ventana.establecerOcupada(false, estado);
    }

    private void guardar(DialogoEstudiante formulario) {
        if (!formulario.isDisplayable() || formulario.estaOcupada()) {
            return;
        }

        int id = formulario.getIdEstudiante();

        Estudiante datos = new Estudiante(
                id,
                formulario.getNombre(),
                formulario.getRut(),
                formulario.getCurso(),
                formulario.getCorreo()
        );

        char[] caracteres = formulario.getContrasena();
        String contrasena = new String(caracteres);
        Arrays.fill(caracteres, '\0');

        formulario.establecerOcupada(true, "Guardando estudiante...");

        try {
            new TareaBD<Estudiante>(
                    formulario,
                    () -> id > 0
                            ? servicio.actualizar(datos, contrasena)
                            : servicio.guardar(datos, contrasena),
                    resultado -> {
                        String mensaje = id > 0
                                ? "Estudiante actualizado."
                                : "Estudiante y cuenta creados.";

                        estado = mensaje
                                + " Pulse Recargar para actualizar el listado.";

                        mensajeCambioPendiente = mensaje;
                        formulario.dispose();
                    },
                    () -> finalizarGuardado(formulario)
            ).execute();

        } catch (RuntimeException e) {
            ManejadorErrores.mostrar(formulario, e);
            finalizarGuardado(formulario);
        }
    }

    private void finalizarGuardado(DialogoEstudiante formulario) {
        if (formulario.isDisplayable()) {
            formulario.establecerOcupada(
                    false,
                    "Revise los datos e intente guardar nuevamente."
            );

            formulario.enfocarNombre();
        }
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

                    estado = mensajeExito
                            + " Estudiantes: " + estudiantes.size() + ".";
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
                    estado = mensajeExito
                            + " Pulse Recargar para actualizar el listado.";

                    mensajeCambioPendiente = mensajeExito;
                    ventana.limpiarSeleccion();
                }
        );
    }

    private <T> void ejecutarTarea(String mensaje, Callable<T> operacion,
                                   Consumer<T> alCompletar) {
        if (!estaDisponible()) {
            return;
        }

        ventana.establecerOcupada(true, mensaje);

        try {
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

        } catch (RuntimeException e) {
            ManejadorErrores.mostrar(ventana, e);
            finalizarTarea();
        }
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
            ventana.enfocarTabla();
        }
    }

    private void cerrar() {
        if (estaDisponible()) {
            ventana.dispose();
        }
    }

    private boolean estaDisponible() {
        return ventana.isDisplayable() && !ventana.estaOcupada();
    }
}