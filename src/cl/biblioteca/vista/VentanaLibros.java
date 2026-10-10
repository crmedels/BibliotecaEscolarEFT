package cl.biblioteca.vista;

import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.SesionUsuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Presenta el catálogo y sus acciones según el perfil del usuario.
 * La creación y edición se realizan en un formulario separado.
 */
public final class VentanaLibros extends JDialog {

    private final JButton botonNuevo = new JButton("Nuevo libro");
    private final JButton botonEditar = new JButton("Editar libro");
    private final JButton botonEliminar = new JButton("Eliminar");
    private final JButton botonRecargar = new JButton("Recargar");
    private final JButton botonCerrar = new JButton("Cerrar");
    private final JLabel etiquetaEstado = new JLabel("", SwingConstants.CENTER);

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Título", "Autor", "ISBN", "Editorial", "Stock", "Categoría"},
            0
    ) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            return columna == 0 || columna == 5 ? Integer.class : String.class;
        }
    };

    private final JTable tabla = new JTable(modeloTabla) {
        @Override
        public String getToolTipText(MouseEvent evento) {
            int fila = rowAtPoint(evento.getPoint());
            int columna = columnAtPoint(evento.getPoint());

            if (fila < 0 || columna < 0) {
                return null;
            }

            Object valor = getValueAt(fila, columna);
            return valor == null ? null : valor.toString();
        }
    };

    private final boolean administrador;
    private List<Libro> libros = List.of();
    private boolean ocupada;

    public VentanaLibros(JFrame propietario, SesionUsuario sesion) {
        super(propietario, "Biblioteca Escolar - Libros", true);

        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        administrador = sesion.puedeAdministrar();

        setTitle(administrador
                ? "Biblioteca Escolar - Gestión de libros"
                : "Biblioteca Escolar - Catálogo");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));

        etiquetaEstado.setText(administrador
                ? "Pulse Nuevo libro o seleccione uno para editarlo."
                : "Seleccione un libro para consultar sus datos.");

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
        tabla.setToolTipText("");

        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setMaxWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(220);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(5).setMaxWidth(80);

        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                actualizarControles();
            }
        });

        JScrollPane listado = new JScrollPane(tabla);
        listado.setPreferredSize(new Dimension(880, 360));

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(new JLabel("Catálogo de libros"), BorderLayout.NORTH);
        contenido.add(listado, BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarControles();
        pack();
        setLocationRelativeTo(propietario);
    }

    private JPanel crearPie() {
        JPanel acciones = new JPanel(
                new GridLayout(1, administrador ? 5 : 2, 8, 0)
        );

        if (administrador) {
            acciones.add(botonNuevo);
            acciones.add(botonEditar);
            acciones.add(botonEliminar);
        }

        acciones.add(botonRecargar);
        acciones.add(botonCerrar);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(acciones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
    }

    public Libro getLibroSeleccionado() {
        int fila = tabla.getSelectedRow();

        if (fila < 0 || fila >= tabla.getRowCount()) {
            return null;
        }

        int indice = tabla.convertRowIndexToModel(fila);
        return indice >= 0 && indice < libros.size() ? libros.get(indice) : null;
    }

    public void mostrarLibros(List<Libro> datos) {
        List<Libro> nuevosLibros = List.copyOf(datos);

        limpiarSeleccion();
        libros = nuevosLibros;
        modeloTabla.setRowCount(0);

        for (Libro libro : libros) {
            modeloTabla.addRow(new Object[]{
                    libro.getId(),
                    libro.getTitulo(),
                    libro.getAutor(),
                    libro.getIsbn(),
                    libro.getEditorial(),
                    libro.getStock(),
                    libro.getCategoria() == null ? "" : libro.getCategoria().getNombre()
            });
        }

        actualizarControles();
    }

    public void limpiarSeleccion() {
        tabla.clearSelection();
        tabla.getSelectionModel().setAnchorSelectionIndex(-1);
        tabla.getSelectionModel().setLeadSelectionIndex(-1);
    }

    public void enfocarTabla() {
        if (!ocupada) {
            tabla.requestFocusInWindow();
        }
    }

    public void alNuevo(ActionListener accion) {
        botonNuevo.addActionListener(accion);
    }

    public void alEditar(ActionListener accion) {
        botonEditar.addActionListener(accion);
    }

    public void alEliminar(ActionListener accion) {
        botonEliminar.addActionListener(accion);
    }

    public void alRecargar(ActionListener accion) {
        botonRecargar.addActionListener(accion);
    }

    public void alSeleccionar(Runnable accion) {
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                accion.run();
            }
        });
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
        boolean seleccionado = getLibroSeleccionado() != null;

        tabla.setEnabled(!ocupada);
        tabla.getTableHeader().setEnabled(!ocupada);
        botonNuevo.setEnabled(administrador && !ocupada);
        botonEditar.setEnabled(administrador && !ocupada && seleccionado);
        botonEliminar.setEnabled(administrador && !ocupada && seleccionado);
        botonRecargar.setEnabled(!ocupada);
        botonCerrar.setEnabled(!ocupada);
    }
}