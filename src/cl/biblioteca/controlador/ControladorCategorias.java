package cl.biblioteca.controlador;

import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.CategoriaServicio;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.VentanaCategorias;

import javax.swing.JOptionPane;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina el formulario de categorías y las operaciones en segundo plano.
 */
public final class ControladorCategorias {

    private static final String ESTADO_INICIAL =
            "Seleccione una categoría o pulse Nuevo.";

    private final VentanaCategorias ventana;
    private final CategoriaServicio servicio;
    private String estado = ESTADO_INICIAL;
    private String mensajeCambioPendiente;

    public ControladorCategorias(VentanaCategorias ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        servicio = new CategoriaServicio(sesion);
        sesion.exigirAdministracion();

        ventana.alNuevo(evento -> nuevo());
        ventana.alGuardar(evento -> guardar());
        ventana.alEliminar(evento -> eliminar());
        ventana.alRecargar(evento -> cargarCategorias("Listado actualizado."));
        ventana.alSeleccionar(this::seleccionarCategoria);
        ventana.alCerrar(this::cerrar);
    }

    public void mostrar() {
        if (!ventana.isDisplayable()) {
            return;
        }

        cargarCategorias("Listado cargado.");
        ventana.setVisible(true);
    }

    private void nuevo() {
        if (!estaDisponible()) {
            return;
        }

        ventana.limpiarFormulario();
        estado = "Escriba el nombre de la nueva categoría y pulse Guardar.";
        ventana.establecerOcupada(false, estado);
        ventana.enfocarNombre();
    }

    private void seleccionarCategoria() {
        if (!estaDisponible()) {
            return;
        }

        ventana.mostrarNombreSeleccionado();
        estado = ventana.getIdSeleccionado() > 0
                ? "Edite el nombre y pulse Guardar para actualizar la categoría."
                : ESTADO_INICIAL;
        ventana.establecerOcupada(false, estado);
    }

    private void guardar() {
        if (!estaDisponible()) {
            return;
        }

        int id = ventana.getIdSeleccionado();
        String nombre = ventana.getNombre();
        String mensajeExito = id > 0
                ? "Categoría actualizada." : "Categoría creada.";

        ejecutarCambio(
                "Guardando categoría...",
                () -> id > 0
                        ? servicio.actualizar(id, nombre)
                        : servicio.guardar(nombre),
                mensajeExito
        );
    }

    private void eliminar() {
        if (!estaDisponible()) {
            return;
        }

        int id = ventana.getIdSeleccionado();
        if (id <= 0) {
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(
                ventana,
                "¿Desea eliminar la categoría seleccionada?",
                "Eliminar categoría",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        ejecutarCambio(
                "Eliminando categoría...",
                () -> {
                    servicio.eliminar(id);
                    return null;
                },
                "Categoría eliminada."
        );
    }

    private void cargarCategorias(String mensajeExito) {
        ejecutarTarea(
                "Cargando categorías...",
                servicio::listar,
                categorias -> {
                    ventana.mostrarCategorias(categorias);
                    estado = mensajeExito + " Categorías: " + categorias.size() + ".";
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
            cargarCategorias(mensajePendiente);
        } else {
            ventana.enfocarNombre();
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