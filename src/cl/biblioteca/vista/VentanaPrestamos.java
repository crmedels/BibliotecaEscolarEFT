package cl.biblioteca.vista;

import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Libro;
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
 * Presenta el registro de préstamos, su historial y las devoluciones.
 */
public final class VentanaPrestamos extends JDialog {

    private final JComboBox<Estudiante> campoEstudiante = new JComboBox<>();
    private final JComboBox<Libro> campoLibro = new JComboBox<>();
    private final JButton botonRegistrar = new JButton("Registrar préstamo");
    private final JButton botonDevolver = new JButton("Registrar devolución");
    private final JButton botonRecargar = new JButton("Recargar");
    private final JButton botonCerrar = new JButton("Cerrar");
    private final JLabel etiquetaEstado = new JLabel("", SwingConstants.CENTER);
    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Estudiante", "RUT", "Libro", "Fecha préstamo",
                    "Vencimiento", "Devolución real", "Estado"}, 0
    ) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            if (columna == 0) {
                return Integer.class;
            }
            if (columna >= 4 && columna <= 6) {
                return LocalDate.class;
            }
            return String.class;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);
    private final boolean administrador;
    private List<Prestamo> prestamos = List.of();
    private boolean ocupada;

    public VentanaPrestamos(JFrame propietario, SesionUsuario sesion) {
        super(propietario, "Biblioteca Escolar - Préstamos y devoluciones", true);
        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        administrador = sesion.puedeAdministrar();
        setTitle(administrador
                ? "Biblioteca Escolar - Préstamos y devoluciones"
                : "Biblioteca Escolar - Mis préstamos y devoluciones");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(950, 480));
        configurarCombos();
        etiquetaEstado.setText("Seleccione un libro para registrar un préstamo.");

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setMaxWidth(60);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(170);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(105);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(105);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(120);
        tabla.getColumnModel().getColumn(7).setPreferredWidth(190);
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                actualizarControles();
            }
        });

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(crearFormulario(), BorderLayout.NORTH);

        JScrollPane listado = new JScrollPane(tabla);
        listado.setPreferredSize(new Dimension(1080, 300));
        contenido.add(listado, BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarControles();
        pack();
        setLocationRelativeTo(propietario);
    }

    private void configurarCombos() {
        DefaultListCellRenderer renderEstudiante = new DefaultListCellRenderer();
        campoEstudiante.setRenderer((lista, estudiante, indice, seleccionado, foco) -> {
            JLabel etiqueta = (JLabel) renderEstudiante.getListCellRendererComponent(
                    lista, estudiante, indice, seleccionado, foco
            );
            etiqueta.setText(estudiante == null
                    ? "Seleccione un estudiante"
                    : estudiante.getNombre() + " | RUT: " + estudiante.getRut());
            return etiqueta;
        });

        DefaultListCellRenderer renderLibro = new DefaultListCellRenderer();
        campoLibro.setRenderer((lista, libro, indice, seleccionado, foco) -> {
            JLabel etiqueta = (JLabel) renderLibro.getListCellRendererComponent(
                    lista, libro, indice, seleccionado, foco
            );
            etiqueta.setText(libro == null
                    ? "Seleccione un libro disponible"
                    : libro.getTitulo() + " | ISBN: " + libro.getIsbn()
                      + " | Stock: " + libro.getStock());
            return etiqueta;
        });

        campoEstudiante.addItem(null);
        campoLibro.addItem(null);
        campoEstudiante.addActionListener(evento -> actualizarControles());
        campoLibro.addActionListener(evento -> actualizarControles());
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints posicion = new GridBagConstraints();
        agregarCampo(formulario, posicion, 0, "Estudiante:", campoEstudiante);
        agregarCampo(formulario, posicion, 1, "Libro disponible:", campoLibro);

        posicion.gridx = 0;
        posicion.gridy = 2;
        posicion.gridwidth = 2;
        posicion.insets = new Insets(10, 0, 0, 0);
        formulario.add(new JLabel(
                "Las fechas se asignan automáticamente. "
                        + "Para devolver, seleccione un préstamo pendiente en la tabla."
        ), posicion);
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
        JPanel acciones = new JPanel(new GridLayout(1, 4, 8, 0));
        acciones.add(botonRegistrar);
        acciones.add(botonDevolver);
        acciones.add(botonRecargar);
        acciones.add(botonCerrar);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(acciones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
    }

    public Estudiante getEstudianteSeleccionado() {
        return (Estudiante) campoEstudiante.getSelectedItem();
    }

    public Libro getLibroSeleccionado() {
        return (Libro) campoLibro.getSelectedItem();
    }

    public Prestamo getPrestamoSeleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : prestamos.get(tabla.convertRowIndexToModel(fila));
    }

    public void mostrarEstudiantes(List<Estudiante> estudiantes) {
        campoEstudiante.removeAllItems();
        if (administrador) {
            campoEstudiante.addItem(null);
        }
        for (Estudiante estudiante : estudiantes) {
            campoEstudiante.addItem(estudiante);
        }
        if (campoEstudiante.getItemCount() > 0) {
            campoEstudiante.setSelectedIndex(0);
        }
        actualizarControles();
    }

    public void mostrarLibrosDisponibles(List<Libro> libros) {
        campoLibro.removeAllItems();
        campoLibro.addItem(null);
        for (Libro libro : libros) {
            campoLibro.addItem(libro);
        }
        campoLibro.setSelectedIndex(0);
        actualizarControles();
    }

    public void mostrarPrestamos(List<Prestamo> datos) {
        tabla.clearSelection();
        tabla.getSelectionModel().setAnchorSelectionIndex(-1);
        tabla.getSelectionModel().setLeadSelectionIndex(-1);

        prestamos = List.copyOf(datos);
        modeloTabla.setRowCount(0);

        for (Prestamo prestamo : prestamos) {
            Estudiante estudiante = prestamo.getEstudiante();
            Libro libro = prestamo.getLibro();

            modeloTabla.addRow(new Object[]{
                    prestamo.getId(),
                    estudiante == null ? "" : estudiante.getNombre(),
                    estudiante == null ? "" : estudiante.getRut(),
                    libro == null ? "" : libro.getTitulo(),
                    prestamo.getFechaPrestamo(),
                    prestamo.getFechaDevolucion(),
                    prestamo.getFechaDevolucionReal(),
                    obtenerEstado(prestamo)
            });
        }
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

    public void limpiarSeleccion() {
        tabla.clearSelection();
        if (administrador && campoEstudiante.getItemCount() > 0) {
            campoEstudiante.setSelectedIndex(0);
        }
        campoLibro.setSelectedIndex(0);
        actualizarControles();
    }

    public void alRegistrar(ActionListener accion) {
        botonRegistrar.addActionListener(accion);
    }

    public void alDevolver(ActionListener accion) {
        botonDevolver.addActionListener(accion);
    }

    public void alRecargar(ActionListener accion) {
        botonRecargar.addActionListener(accion);
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
        Estudiante estudiante = getEstudianteSeleccionado();
        Libro libro = getLibroSeleccionado();
        Prestamo prestamo = getPrestamoSeleccionado();

        campoEstudiante.setEnabled(administrador && !ocupada);
        campoLibro.setEnabled(!ocupada);
        tabla.setEnabled(!ocupada);
        tabla.getTableHeader().setEnabled(!ocupada);
        botonRegistrar.setEnabled(!ocupada && estudiante != null && estudiante.getId() > 0
                && libro != null && libro.getId() > 0 && libro.tieneStockDisponible());
        botonDevolver.setEnabled(!ocupada && prestamo != null && !prestamo.isDevuelto());
        botonRecargar.setEnabled(!ocupada);
        botonCerrar.setEnabled(!ocupada);
    }
}