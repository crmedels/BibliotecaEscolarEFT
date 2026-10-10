package cl.biblioteca.vista;

import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.LibroMasPrestado;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.List;

/**
 * Presenta los reportes disponibles según el perfil del usuario.
 */
public final class VentanaReportes extends JDialog {

    public enum TipoReporte {
        LIBROS_MAS_PRESTADOS("Libros más prestados"),
        HISTORIAL_ESTUDIANTE("Historial por estudiante"),
        LIBROS_EN_PRESTAMO("Libros actualmente en préstamo");

        private final String nombre;

        TipoReporte(String nombre) {
            this.nombre = nombre;
        }

        @Override
        public String toString() {
            return nombre;
        }
    }

    private final JComboBox<TipoReporte> campoReporte = new JComboBox<>();
    private final JComboBox<Estudiante> campoEstudiante = new JComboBox<>();
    private final JButton botonConsultar = new JButton("Consultar");
    private final JButton botonRecargar = new JButton("Recargar");
    private final JButton botonCerrar = new JButton("Cerrar");
    private final JLabel etiquetaEstado = new JLabel(
            "Seleccione un reporte y pulse Consultar.", SwingConstants.CENTER
    );
    private final JTable tabla = new JTable();
    private DefaultTableModel modeloTabla;
    private final boolean administrador;
    private boolean ocupada;

    public VentanaReportes(JFrame propietario, SesionUsuario sesion) {
        super(propietario, "Biblioteca Escolar - Reportes", true);
        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        administrador = sesion.puedeAdministrar();
        setTitle(administrador
                ? "Biblioteca Escolar - Reportes"
                : "Biblioteca Escolar - Mi historial y préstamos pendientes");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1050, 480));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
        tabla.getTableHeader().setReorderingAllowed(false);
        configurarSelectores();
        limpiarReporte();

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(crearFormulario(), BorderLayout.NORTH);

        JScrollPane listado = new JScrollPane(tabla);
        listado.setPreferredSize(new Dimension(1160, 300));
        contenido.add(listado, BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarControles();
        pack();
        setLocationRelativeTo(propietario);
    }

    private void configurarSelectores() {
        if (administrador) {
            campoReporte.addItem(TipoReporte.LIBROS_MAS_PRESTADOS);
        }
        campoReporte.addItem(TipoReporte.HISTORIAL_ESTUDIANTE);
        campoReporte.addItem(TipoReporte.LIBROS_EN_PRESTAMO);

        DefaultListCellRenderer renderEstudiante = new DefaultListCellRenderer();
        campoEstudiante.setRenderer((lista, estudiante, indice, seleccionado, foco) -> {
            JLabel etiqueta = (JLabel) renderEstudiante.getListCellRendererComponent(
                    lista, estudiante, indice, seleccionado, foco
            );
            etiqueta.setText(estudiante == null
                    ? "Seleccione un estudiante para consultar su historial"
                    : estudiante.getNombre() + " | RUT: " + estudiante.getRut());
            return etiqueta;
        });

        campoEstudiante.addItem(null);
        campoReporte.addActionListener(evento -> actualizarControles());
        campoEstudiante.addActionListener(evento -> actualizarControles());
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints posicion = new GridBagConstraints();
        agregarCampo(formulario, posicion, 0, "Reporte:", campoReporte);
        agregarCampo(formulario, posicion, 1, "Estudiante:", campoEstudiante);
        return formulario;
    }

    private void agregarCampo(JPanel formulario, GridBagConstraints posicion,
                              int fila, String texto, JComponent campo) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setLabelFor(campo);

        posicion.gridy = fila;
        posicion.gridx = 0;
        posicion.weightx = 0;
        posicion.anchor = GridBagConstraints.LINE_START;
        posicion.fill = GridBagConstraints.NONE;
        posicion.insets = new Insets(4, 0, 4, 12);
        formulario.add(etiqueta, posicion);

        posicion.gridx = 1;
        posicion.weightx = 1;
        posicion.fill = GridBagConstraints.HORIZONTAL;
        posicion.insets = new Insets(4, 0, 4, 0);
        formulario.add(campo, posicion);
    }

    private JPanel crearPie() {
        JPanel acciones = new JPanel(new GridLayout(1, 3, 8, 0));
        acciones.add(botonConsultar);
        acciones.add(botonRecargar);
        acciones.add(botonCerrar);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(acciones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
    }

    public TipoReporte getTipoReporte() {
        return (TipoReporte) campoReporte.getSelectedItem();
    }

    public Estudiante getEstudianteSeleccionado() {
        return (Estudiante) campoEstudiante.getSelectedItem();
    }

    public void mostrarEstudiantes(List<Estudiante> estudiantes) {
        Estudiante seleccionado = getEstudianteSeleccionado();
        int idSeleccionado = seleccionado == null ? 0 : seleccionado.getId();

        campoEstudiante.removeAllItems();
        if (administrador) {
            campoEstudiante.addItem(null);
        }
        for (Estudiante estudiante : estudiantes) {
            campoEstudiante.addItem(estudiante);
        }
        if (campoEstudiante.getItemCount() > 0) {
            campoEstudiante.setSelectedIndex(0);
            for (int indice = 0; indice < campoEstudiante.getItemCount(); indice++) {
                Estudiante estudiante = campoEstudiante.getItemAt(indice);
                if (estudiante != null && estudiante.getId() == idSeleccionado) {
                    campoEstudiante.setSelectedIndex(indice);
                    break;
                }
            }
        }
        actualizarControles();
    }

    public void mostrarLibrosMasPrestados(List<LibroMasPrestado> libros) {
        cambiarModelo(crearModeloLibros());
        for (LibroMasPrestado libro : libros) {
            modeloTabla.addRow(new Object[]{
                    libro.getIdLibro(), libro.getTitulo(), libro.getAutor(),
                    libro.getIsbn(), libro.getCantidadPrestamos()
            });
        }
    }

    public void mostrarPrestamos(List<Prestamo> prestamos) {
        cambiarModelo(crearModeloPrestamos());
        for (Prestamo prestamo : prestamos) {
            Estudiante estudiante = prestamo.getEstudiante();
            Libro libro = prestamo.getLibro();
            modeloTabla.addRow(new Object[]{
                    prestamo.getId(), estudiante == null ? "" : estudiante.getNombre(),
                    estudiante == null ? "" : estudiante.getRut(),
                    libro == null ? "" : libro.getTitulo(),
                    libro == null ? "" : libro.getIsbn(),
                    prestamo.getFechaPrestamo(), prestamo.getFechaDevolucion(),
                    prestamo.getFechaDevolucionReal(), obtenerEstado(prestamo)
            });
        }
    }

    public void limpiarReporte() {
        cambiarModelo(getTipoReporte() == TipoReporte.LIBROS_MAS_PRESTADOS
                ? crearModeloLibros() : crearModeloPrestamos());
    }

    private DefaultTableModel crearModeloLibros() {
        return crearModelo(
                new String[]{"ID libro", "Título", "Autor", "ISBN", "Total préstamos"},
                new Class<?>[]{Integer.class, String.class, String.class,
                        String.class, Long.class}
        );
    }

    private DefaultTableModel crearModeloPrestamos() {
        return crearModelo(
                new String[]{"ID", "Estudiante", "RUT", "Libro", "ISBN",
                        "Fecha préstamo", "Vencimiento", "Devolución real", "Estado"},
                new Class<?>[]{Integer.class, String.class, String.class,
                        String.class, String.class, LocalDate.class, LocalDate.class,
                        LocalDate.class, String.class}
        );
    }

    private DefaultTableModel crearModelo(String[] columnas, Class<?>[] tipos) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columna) {
                return tipos[columna];
            }
        };
    }

    private void cambiarModelo(DefaultTableModel modelo) {
        modeloTabla = modelo;
        tabla.setModel(modeloTabla);
        int[] anchos = modeloTabla.getColumnCount() == 5
                ? new int[]{70, 320, 230, 170, 140}
                : new int[]{55, 160, 110, 210, 135, 105, 105, 120, 190};

        for (int columna = 0; columna < anchos.length; columna++) {
            tabla.getColumnModel().getColumn(columna).setPreferredWidth(anchos[columna]);
        }
        tabla.getColumnModel().getColumn(0).setMaxWidth(75);
        tabla.getTableHeader().setReorderingAllowed(false);
        actualizarControles();
    }

    private String obtenerEstado(Prestamo prestamo) {
        if (prestamo.isDevuelto()) {
            if (prestamo.getFechaDevolucionReal() == null) {
                return "Devuelto (sin fecha real)";
            }
            return prestamo.fueDevueltoConAtraso() ? "Devuelto con atraso" : "Devuelto";
        }
        return prestamo.estaAtrasado() ? "Pendiente con atraso" : "Pendiente";
    }

    public void alConsultar(ActionListener accion) {
        botonConsultar.addActionListener(accion);
    }

    public void alRecargar(ActionListener accion) {
        botonRecargar.addActionListener(accion);
    }

    public void alCambiarFiltros(Runnable accion) {
        campoReporte.addActionListener(evento -> accion.run());
        campoEstudiante.addActionListener(evento -> accion.run());
    }

    public void alCerrar(Runnable accion) {
        botonCerrar.addActionListener(evento -> accion.run());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                accion.run();
            }
        });
    }

    public boolean estaOcupada() {
        return ocupada;
    }

    public void establecerOcupada(boolean ocupada, String mensaje) {
        this.ocupada = ocupada;
        etiquetaEstado.setText(mensaje);
        actualizarControles();
        setCursor(Cursor.getPredefinedCursor(
                ocupada ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR
        ));
    }

    private void actualizarControles() {
        TipoReporte tipo = getTipoReporte();
        boolean historial = tipo == TipoReporte.HISTORIAL_ESTUDIANTE;
        Estudiante estudiante = getEstudianteSeleccionado();
        boolean seleccionValida = estudiante != null && estudiante.getId() > 0;

        campoReporte.setEnabled(!ocupada);
        campoEstudiante.setEnabled(administrador && historial && !ocupada);
        tabla.setEnabled(!ocupada);
        tabla.getTableHeader().setEnabled(!ocupada);
        botonConsultar.setEnabled(!ocupada && tipo != null && (!historial || seleccionValida));
        botonRecargar.setEnabled(!ocupada);
        botonCerrar.setEnabled(!ocupada);
    }
}