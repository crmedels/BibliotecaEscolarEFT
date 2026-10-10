package cl.biblioteca.controlador;

import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.EstudianteServicio;
import cl.biblioteca.servicio.ReporteServicio;
import cl.biblioteca.util.TareaBD;
import cl.biblioteca.vista.VentanaReportes;
import cl.biblioteca.vista.VentanaReportes.TipoReporte;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/**
 * Coordina los reportes y el historial mediante consultas en segundo plano.
 */
public final class ControladorReportes {

    private final VentanaReportes ventana;
    private final SesionUsuario sesion;
    private final ReporteServicio reporteServicio;
    private final EstudianteServicio estudianteServicio;
    private String estado = "Seleccione un reporte y pulse Consultar.";
    private boolean consultaPendiente;

    public ControladorReportes(VentanaReportes ventana, SesionUsuario sesion) {
        this.ventana = Objects.requireNonNull(ventana, "La ventana es obligatoria.");
        reporteServicio = new ReporteServicio(sesion);
        estudianteServicio = new EstudianteServicio(sesion);
        this.sesion = sesion;

        ventana.alConsultar(evento -> consultar());
        ventana.alRecargar(evento -> cargarEstudiantes());
        ventana.alCambiarFiltros(this::cambiarFiltros);
        ventana.alCerrar(this::cerrar);
    }

    public void mostrar() {
        if (!ventana.isDisplayable()) {
            return;
        }

        cargarEstudiantes();
        ventana.setVisible(true);
    }

    private void cambiarFiltros() {
        if (!estaDisponible()) {
            return;
        }

        ventana.limpiarReporte();
        estado = obtenerIndicacion();
        ventana.establecerOcupada(false, estado);
    }

    private void cargarEstudiantes() {
        ejecutarTarea(
                "Cargando estudiantes...",
                () -> sesion.puedeAdministrar()
                        ? estudianteServicio.listar()
                        : List.of(estudianteServicio.buscarPorId(sesion.getIdEstudiante())),
                estudiantes -> {
                    ventana.mostrarEstudiantes(estudiantes);
                    ventana.limpiarReporte();
                    estado = obtenerIndicacion();
                    consultaPendiente = puedeConsultar();
                }
        );
    }

    private void consultar() {
        if (!estaDisponible()) {
            return;
        }

        TipoReporte tipo = ventana.getTipoReporte();
        if (tipo == null) {
            return;
        }

        switch (tipo) {
            case LIBROS_MAS_PRESTADOS -> consultarLibrosMasPrestados();
            case HISTORIAL_ESTUDIANTE -> {
                Estudiante estudiante = ventana.getEstudianteSeleccionado();
                int idSeleccionado = estudiante == null ? 0 : estudiante.getId();
                int idEstudiante = sesion.puedeAdministrar()
                        ? idSeleccionado : sesion.getIdEstudiante();
                consultarHistorial(idEstudiante);
            }
            case LIBROS_EN_PRESTAMO -> consultarPrestamosPendientes();
        }
    }

    private void consultarLibrosMasPrestados() {
        ejecutarTarea(
                "Consultando libros más prestados...",
                reporteServicio::listarLibrosMasPrestados,
                libros -> {
                    ventana.mostrarLibrosMasPrestados(libros);
                    estado = libros.isEmpty()
                            ? "No hay libros con préstamos registrados."
                            : "Libros con préstamos: " + libros.size()
                              + ". El total incluye préstamos pendientes y devueltos.";
                }
        );
    }

    private void consultarHistorial(int idEstudiante) {
        ejecutarTarea(
                "Consultando historial del estudiante...",
                () -> reporteServicio.listarHistorialPorEstudiante(idEstudiante),
                prestamos -> {
                    ventana.mostrarPrestamos(prestamos);
                    estado = prestamos.isEmpty()
                            ? "El estudiante seleccionado no tiene préstamos registrados."
                            : "Préstamos del estudiante: " + prestamos.size() + ".";
                }
        );
    }

    private void consultarPrestamosPendientes() {
        ejecutarTarea(
                "Consultando préstamos pendientes...",
                reporteServicio::listarLibrosEnPrestamo,
                prestamos -> {
                    ventana.mostrarPrestamos(prestamos);
                    estado = prestamos.isEmpty()
                            ? "No hay préstamos pendientes de devolución."
                            : "Préstamos pendientes: " + prestamos.size() + ".";
                }
        );
    }

    private boolean puedeConsultar() {
        TipoReporte tipo = ventana.getTipoReporte();
        if (tipo == null) {
            return false;
        }
        if (tipo != TipoReporte.HISTORIAL_ESTUDIANTE) {
            return true;
        }

        Estudiante estudiante = ventana.getEstudianteSeleccionado();
        return estudiante != null && estudiante.getId() > 0;
    }

    private String obtenerIndicacion() {
        TipoReporte tipo = ventana.getTipoReporte();
        if (tipo == null) {
            return "Seleccione un reporte y pulse Consultar.";
        }

        return switch (tipo) {
            case LIBROS_MAS_PRESTADOS ->
                    "Pulse Consultar. El ranking incluye préstamos pendientes y devueltos.";
            case HISTORIAL_ESTUDIANTE -> puedeConsultar()
                    ? "Pulse Consultar para cargar el historial del estudiante."
                    : "Seleccione un estudiante y pulse Consultar.";
            case LIBROS_EN_PRESTAMO ->
                    "Pulse Consultar para ver los préstamos pendientes de devolución.";
        };
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
        boolean debeConsultar = consultaPendiente;
        consultaPendiente = false;

        if (!ventana.isDisplayable()) {
            return;
        }

        ventana.establecerOcupada(false, estado);
        if (debeConsultar) {
            consultar();
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