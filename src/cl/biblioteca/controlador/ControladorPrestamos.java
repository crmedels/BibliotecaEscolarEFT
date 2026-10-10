package cl.biblioteca.controlador;

import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.EstudianteServicio;
import cl.biblioteca.servicio.LibroServicio;
import cl.biblioteca.servicio.PrestamoServicio;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.VentanaPrestamos;

import javax.swing.JOptionPane;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina préstamos, devoluciones e historial en segundo plano.
 */
public final class ControladorPrestamos {

    private static final String ESTADO_INICIAL =
            "Seleccione un libro para prestar o un préstamo pendiente para devolver.";

    private final VentanaPrestamos ventana;
    private final SesionUsuario sesion;
    private final PrestamoServicio prestamoServicio;
    private final EstudianteServicio estudianteServicio;
    private final LibroServicio libroServicio;
    private String estado = ESTADO_INICIAL;
    private String mensajeCambioPendiente;

    public ControladorPrestamos(VentanaPrestamos ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        prestamoServicio = new PrestamoServicio(sesion);
        estudianteServicio = new EstudianteServicio(sesion);
        libroServicio = new LibroServicio(sesion);
        this.sesion = sesion;

        ventana.alRegistrar(evento -> registrar());
        ventana.alDevolver(evento -> devolver());
        ventana.alRecargar(evento -> cargarDatos("Listado actualizado."));
        ventana.alCerrar(this::cerrar);
    }

    public void mostrar() {
        if (!ventana.isDisplayable()) {
            return;
        }

        cargarDatos("Listado cargado.");
        ventana.setVisible(true);
    }

    private void registrar() {
        if (!estaDisponible()) {
            return;
        }

        Estudiante estudiante = ventana.getEstudianteSeleccionado();
        Libro libro = ventana.getLibroSeleccionado();
        int idSeleccionado = estudiante == null ? 0 : estudiante.getId();
        int idEstudiante = sesion.puedeAdministrar()
                ? idSeleccionado : sesion.getIdEstudiante();
        int idLibro = libro == null ? 0 : libro.getId();

        ejecutarTarea(
                "Registrando préstamo...",
                () -> prestamoServicio.registrar(idEstudiante, idLibro),
                prestamo -> confirmarCambio(
                        "Préstamo N.º " + prestamo.getId()
                                + " registrado. Vencimiento: "
                                + prestamo.getFechaDevolucion() + "."
                )
        );
    }

    private void devolver() {
        if (!estaDisponible()) {
            return;
        }

        Prestamo seleccionado = ventana.getPrestamoSeleccionado();
        if (seleccionado == null || seleccionado.isDevuelto()) {
            return;
        }
        int idPrestamo = seleccionado.getId();

        int respuesta = JOptionPane.showConfirmDialog(
                ventana,
                "¿Desea registrar la devolución del préstamo N.º " + idPrestamo
                        + "?\nEl ejemplar volverá al stock disponible.",
                "Registrar devolución",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        ejecutarTarea(
                "Registrando devolución...",
                () -> prestamoServicio.devolver(idPrestamo),
                prestamo -> confirmarCambio(prestamo.fueDevueltoConAtraso()
                        ? "Devolución registrada con atraso."
                        : "Devolución registrada.")
        );
    }

    private void cargarDatos(String mensajeExito) {
        ejecutarTarea(
                "Cargando estudiantes, libros disponibles e historial...",
                () -> {
                    List<Estudiante> estudiantes = sesion.puedeAdministrar()
                            ? estudianteServicio.listar()
                            : List.of(estudianteServicio.buscarPorId(sesion.getIdEstudiante()));

                    return new DatosPrestamos(
                            estudiantes, libroServicio.listarDisponibles(),
                            prestamoServicio.listar()
                    );
                },
                datos -> {
                    ventana.mostrarEstudiantes(datos.estudiantes());
                    ventana.mostrarLibrosDisponibles(datos.librosDisponibles());
                    ventana.mostrarPrestamos(datos.prestamos());
                    estado = mensajeExito + " Préstamos: " + datos.prestamos().size() + ".";
                }
        );
    }

    private void confirmarCambio(String mensajeExito) {
        ventana.limpiarSeleccion();
        estado = mensajeExito + " Pulse Recargar para actualizar los datos.";
        mensajeCambioPendiente = mensajeExito;
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
            cargarDatos(mensajePendiente);
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

    private record DatosPrestamos(List<Estudiante> estudiantes,
                                  List<Libro> librosDisponibles,
                                  List<Prestamo> prestamos) {
    }
}