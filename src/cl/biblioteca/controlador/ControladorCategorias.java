package cl.biblioteca.controlador;

import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.CategoriaServicio;
import cl.biblioteca.util.ManejadorErrores;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.DialogoCategoria;
import cl.biblioteca.vista.VentanaCategorias;

import javax.swing.JOptionPane;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina el listado y el formulario de categorías en segundo plano.
 */
public final class ControladorCategorias {

    private static final String ESTADO_INICIAL =
            "Pulse Nueva categoría o seleccione una para editarla.";

    private final VentanaCategorias ventana;
    private final CategoriaServicio servicio;

    private String estado = ESTADO_INICIAL;
    private String mensajeCambioPendiente;

    public ControladorCategorias(VentanaCategorias ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(
                ventana, "La ventana es obligatoria."
        );

        servicio = new CategoriaServicio(sesion);
        sesion.exigirAdministracion();

        ventana.alNuevo(evento -> abrirFormulario(null));
        ventana.alEditar(evento -> editar());
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

    private void editar() {
        if (!estaDisponible()) {
            return;
        }

        Categoria seleccionada = ventana.getCategoriaSeleccionada();

        if (seleccionada != null) {
            abrirFormulario(seleccionada);
        }
    }

    private void abrirFormulario(Categoria categoria) {
        if (!estaDisponible()) {
            return;
        }

        DialogoCategoria dialogo = null;
        ventana.establecerOcupada(true, "Formulario de categoría abierto.");

        try {
            dialogo = new DialogoCategoria(ventana, categoria);

            DialogoCategoria formulario = dialogo;
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

    private void seleccionarCategoria() {
        if (!estaDisponible()) {
            return;
        }

        estado = ventana.getCategoriaSeleccionada() == null
                ? ESTADO_INICIAL
                : "Pulse Editar categoría para modificar la categoría seleccionada.";

        ventana.establecerOcupada(false, estado);
    }

    private void guardar(DialogoCategoria formulario) {
        if (!formulario.isDisplayable() || formulario.estaOcupada()) {
            return;
        }

        int id = formulario.getIdCategoria();
        String nombre = formulario.getNombre();

        formulario.establecerOcupada(true, "Guardando categoría...");

        try {
            new TareaBD<Categoria>(
                    formulario,
                    () -> id > 0
                            ? servicio.actualizar(id, nombre)
                            : servicio.guardar(nombre),
                    resultado -> {
                        String mensaje = id > 0
                                ? "Categoría actualizada."
                                : "Categoría creada.";

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

    private void finalizarGuardado(DialogoCategoria formulario) {
        if (formulario.isDisplayable()) {
            formulario.establecerOcupada(
                    false,
                    "Revise el nombre e intente guardar nuevamente."
            );

            formulario.enfocarNombre();
        }
    }

    private void eliminar() {
        if (!estaDisponible()) {
            return;
        }

        Categoria seleccionada = ventana.getCategoriaSeleccionada();

        if (seleccionada == null) {
            return;
        }

        int id = seleccionada.getId();

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

                    estado = mensajeExito
                            + " Categorías: " + categorias.size() + ".";
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
            cargarCategorias(mensajePendiente);
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