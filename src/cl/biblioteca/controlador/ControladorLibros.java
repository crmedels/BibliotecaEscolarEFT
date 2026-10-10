package cl.biblioteca.controlador;

import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.CategoriaServicio;
import cl.biblioteca.servicio.LibroServicio;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.util.Validador;
import cl.biblioteca.vista.VentanaLibros;

import javax.swing.JOptionPane;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina la gestión de libros y la consulta del catálogo en segundo plano.
 */
public final class ControladorLibros {

    private final VentanaLibros ventana;
    private final LibroServicio libroServicio;
    private final CategoriaServicio categoriaServicio;
    private final boolean administrador;
    private final String estadoInicial;
    private String estado;
    private String mensajeCambioPendiente;

    public ControladorLibros(VentanaLibros ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        libroServicio = new LibroServicio(sesion);
        categoriaServicio = new CategoriaServicio(sesion);
        administrador = sesion.puedeAdministrar();
        estadoInicial = administrador
                ? "Seleccione un libro para editarlo, o pulse Nuevo."
                : "Seleccione un libro para consultar sus datos.";
        estado = estadoInicial;

        if (administrador) {
            ventana.alNuevo(evento -> nuevo());
            ventana.alGuardar(evento -> guardar());
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

    private void nuevo() {
        if (!administrador || !estaDisponible()) {
            return;
        }

        ventana.limpiarFormulario();
        estado = "Complete los datos del nuevo libro y pulse Guardar.";
        ventana.establecerOcupada(false, estado);
        ventana.enfocarTitulo();
    }

    private void seleccionarLibro() {
        if (!estaDisponible()) {
            return;
        }

        ventana.mostrarLibroSeleccionado();
        estado = ventana.getLibroSeleccionado() == null ? estadoInicial
                : administrador ? "Edite los datos y pulse Guardar para actualizar el libro."
                  : "Consulta del libro seleccionado.";
        ventana.establecerOcupada(false, estado);
    }

    private void guardar() {
        if (!administrador || !estaDisponible()) {
            return;
        }

        Libro seleccionado = ventana.getLibroSeleccionado();
        int id = seleccionado == null ? 0 : seleccionado.getId();
        int stockOriginal = seleccionado == null ? 0 : seleccionado.getStock();

        String titulo = ventana.getTitulo();
        String autor = ventana.getAutor();
        String isbn = ventana.getIsbn();
        String editorial = ventana.getEditorial();
        String textoStock = ventana.getStock();

        Categoria seleccion = ventana.getCategoriaSeleccionada();
        Categoria categoria = seleccion == null ? null
                : new Categoria(seleccion.getId(), seleccion.getNombre());

        ejecutarCambio(
                "Guardando libro...",
                () -> {
                    int stock = Validador.validarStock(textoStock);
                    Libro datos = new Libro(
                            id, titulo, autor, isbn, editorial, stock, categoria
                    );

                    return id > 0
                            ? libroServicio.actualizar(datos, stockOriginal)
                            : libroServicio.guardar(datos);
                },
                id > 0 ? "Libro actualizado." : "Libro creado."
        );
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
                () -> new DatosCatalogo(categoriaServicio.listar(), libroServicio.listar()),
                datos -> {
                    ventana.mostrarCategorias(datos.categorias());
                    ventana.mostrarLibros(datos.libros());
                    estado = mensajeExito + " Libros: " + datos.libros().size() + ".";
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
                    estado = mensajeExito + " Pulse Recargar para actualizar el catálogo.";
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
            cargarCatalogo(mensajePendiente);
        } else {
            ventana.enfocarTitulo();
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