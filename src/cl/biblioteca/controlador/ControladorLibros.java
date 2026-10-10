package cl.biblioteca.controlador;

import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.CategoriaServicio;
import cl.biblioteca.servicio.LibroServicio;
import cl.biblioteca.util.ManejadorErrores;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.util.Validador;
import cl.biblioteca.vista.DialogoLibro;
import cl.biblioteca.vista.VentanaLibros;

import javax.swing.JOptionPane;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina el catálogo y el formulario de libros con operaciones en segundo plano.
 */
public final class ControladorLibros {

    private final VentanaLibros ventana;
    private final LibroServicio libroServicio;
    private final CategoriaServicio categoriaServicio;
    private final boolean administrador;
    private final String estadoInicial;

    private List<Categoria> categorias = List.of();
    private String estado;
    private String mensajeCambioPendiente;

    public ControladorLibros(VentanaLibros ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(
                ventana, "La ventana es obligatoria."
        );

        libroServicio = new LibroServicio(sesion);
        categoriaServicio = new CategoriaServicio(sesion);
        administrador = sesion.puedeAdministrar();

        estadoInicial = administrador
                ? "Pulse Nuevo libro o seleccione uno para editarlo."
                : "Seleccione un libro para consultar sus datos.";

        estado = estadoInicial;

        if (administrador) {
            ventana.alNuevo(evento -> abrirFormulario(null));
            ventana.alEditar(evento -> editar());
            ventana.alEliminar(evento -> eliminar());
        }

        ventana.alRecargar(evento -> cargarCatalogo("Catálogo actualizado."));
        ventana.alSeleccionar(this::seleccionarLibro);
        ventana.alCerrar(this::cerrar);
    }

    public void mostrar() {
        if (!ventana.isDisplayable()) {
            return;
        }

        cargarCatalogo("Catálogo cargado.");
        ventana.setVisible(true);
    }

    private void editar() {
        if (!administrador || !estaDisponible()) {
            return;
        }

        Libro seleccionado = ventana.getLibroSeleccionado();

        if (seleccionado != null) {
            abrirFormulario(seleccionado);
        }
    }

    private void abrirFormulario(Libro libro) {
        if (!administrador || !estaDisponible()) {
            return;
        }

        DialogoLibro dialogo = null;
        ventana.establecerOcupada(true, "Formulario de libro abierto.");

        try {
            dialogo = new DialogoLibro(ventana, categorias, libro);

            DialogoLibro formulario = dialogo;
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

    private void seleccionarLibro() {
        if (!estaDisponible()) {
            return;
        }

        estado = ventana.getLibroSeleccionado() == null ? estadoInicial
                : administrador ? "Pulse Editar libro para modificar el libro seleccionado."
                  : "Consulta del libro seleccionado.";

        ventana.establecerOcupada(false, estado);
    }

    private void guardar(DialogoLibro formulario) {
        if (!administrador || !formulario.isDisplayable() || formulario.estaOcupada()) {
            return;
        }

        int id = formulario.getIdLibro();
        int stockOriginal = formulario.getStockOriginal();

        String titulo = formulario.getTitulo();
        String autor = formulario.getAutor();
        String isbn = formulario.getIsbn();
        String editorial = formulario.getEditorial();
        String textoStock = formulario.getStock();

        Categoria seleccion = formulario.getCategoriaSeleccionada();
        Categoria categoria = seleccion == null ? null
                : new Categoria(seleccion.getId(), seleccion.getNombre());

        formulario.establecerOcupada(true, "Guardando libro...");

        try {
            new TareaBD<Libro>(
                    formulario,
                    () -> {
                        int stock = Validador.validarStock(textoStock);

                        Libro datos = new Libro(
                                id, titulo, autor, isbn, editorial, stock, categoria
                        );

                        return id > 0
                                ? libroServicio.actualizar(datos, stockOriginal)
                                : libroServicio.guardar(datos);
                    },
                    resultado -> {
                        String mensaje = id > 0
                                ? "Libro actualizado."
                                : "Libro creado.";

                        estado = mensaje
                                + " Pulse Recargar para actualizar el catálogo.";

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

    private void finalizarGuardado(DialogoLibro formulario) {
        if (formulario.isDisplayable()) {
            formulario.establecerOcupada(
                    false,
                    "Revise los datos e intente guardar nuevamente."
            );

            formulario.enfocarTitulo();
        }
    }

    private void eliminar() {
        if (!administrador || !estaDisponible()) {
            return;
        }

        Libro seleccionado = ventana.getLibroSeleccionado();

        if (seleccionado == null) {
            return;
        }

        int id = seleccionado.getId();

        int respuesta = JOptionPane.showConfirmDialog(
                ventana,
                "¿Desea eliminar el libro seleccionado?",
                "Eliminar libro",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        ejecutarCambio(
                "Eliminando libro...",
                () -> {
                    libroServicio.eliminar(id);
                    return null;
                },
                "Libro eliminado."
        );
    }

    private void cargarCatalogo(String mensajeExito) {
        ejecutarTarea(
                "Cargando catálogo...",
                () -> new DatosCatalogo(
                        categoriaServicio.listar(),
                        libroServicio.listar()
                ),
                datos -> {
                    categorias = List.copyOf(datos.categorias());
                    ventana.mostrarLibros(datos.libros());

                    estado = mensajeExito
                            + " Libros: " + datos.libros().size() + ".";
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
                            + " Pulse Recargar para actualizar el catálogo.";

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
            cargarCatalogo(mensajePendiente);
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

    private record DatosCatalogo(List<Categoria> categorias, List<Libro> libros) {
    }
}