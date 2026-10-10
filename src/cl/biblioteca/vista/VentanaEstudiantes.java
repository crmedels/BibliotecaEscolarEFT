package cl.biblioteca.vista;

import cl.biblioteca.modelo.Estudiante;

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
 * Presenta el listado de estudiantes y sus acciones.
 * La creación y edición se realizan en un formulario separado.
 */
public final class VentanaEstudiantes extends JDialog {

    private final JButton botonNuevo = new JButton("Nuevo estudiante");
    private final JButton botonEditar = new JButton("Editar estudiante");
    private final JButton botonEliminar = new JButton("Eliminar");
    private final JButton botonRecargar = new JButton("Recargar");
    private final JButton botonCerrar = new JButton("Cerrar");

    private final JLabel etiquetaEstado = new JLabel(
            "Pulse Nuevo estudiante o seleccione uno para editarlo.",
            SwingConstants.CENTER
    );

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "RUT", "Curso", "Correo"}, 0
    ) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }

        @Override
        public Class<?> getColumnClass(int columna) {
            return columna == 0 ? Integer.class : String.class;
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

    private List<Estudiante> estudiantes = List.of();
    private boolean ocupada;

    public VentanaEstudiantes(JFrame propietario) {
        super(propietario, "Biblioteca Escolar - Gestión de estudiantes", true);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
        tabla.setToolTipText("");

        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setMaxWidth(70);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(230);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(230);

        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                actualizarControles();
            }
        });

        JScrollPane listado = new JScrollPane(tabla);
        listado.setPreferredSize(new Dimension(820, 360));

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(new JLabel("Listado de estudiantes"), BorderLayout.NORTH);
        contenido.add(listado, BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarControles();
        pack();
        setLocationRelativeTo(propietario);
    }

    private JPanel crearPie() {
        JPanel acciones = new JPanel(new GridLayout(1, 5, 8, 0));
        acciones.add(botonNuevo);
        acciones.add(botonEditar);
        acciones.add(botonEliminar);
        acciones.add(botonRecargar);
        acciones.add(botonCerrar);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(acciones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
    }

    public Estudiante getEstudianteSeleccionado() {
        int fila = tabla.getSelectedRow();

        if (fila < 0 || fila >= tabla.getRowCount()) {
            return null;
        }

        int indice = tabla.convertRowIndexToModel(fila);
        return indice >= 0 && indice < estudiantes.size()
                ? estudiantes.get(indice) : null;
    }

    public void mostrarEstudiantes(List<Estudiante> datos) {
        List<Estudiante> nuevosEstudiantes = List.copyOf(datos);

        limpiarSeleccion();
        estudiantes = nuevosEstudiantes;
        modeloTabla.setRowCount(0);

        for (Estudiante estudiante : estudiantes) {
            modeloTabla.addRow(new Object[]{
                    estudiante.getId(),
                    estudiante.getNombre(),
                    estudiante.getRut(),
                    estudiante.getCurso(),
                    estudiante.getCorreo()
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
        boolean seleccionado = getEstudianteSeleccionado() != null;

        tabla.setEnabled(!ocupada);
        tabla.getTableHeader().setEnabled(!ocupada);
        botonNuevo.setEnabled(!ocupada);
        botonEditar.setEnabled(!ocupada && seleccionado);
        botonEliminar.setEnabled(!ocupada && seleccionado);
        botonRecargar.setEnabled(!ocupada);
        botonCerrar.setEnabled(!ocupada);
    }
}