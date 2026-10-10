package cl.biblioteca.vista;

import cl.biblioteca.modelo.Categoria;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Presenta el formulario y el listado de categorías.
 */
public final class VentanaCategorias extends JDialog {

    private final JTextField campoNombre = new JTextField(30);
    private final JButton botonNuevo = new JButton("Nuevo");
    private final JButton botonGuardar = new JButton("Guardar");
    private final JButton botonEliminar = new JButton("Eliminar");
    private final JButton botonRecargar = new JButton("Recargar");
    private final JButton botonCerrar = new JButton("Cerrar");
    private final JLabel etiquetaEstado = new JLabel(
            "Seleccione una categoría o pulse Nuevo.", SwingConstants.CENTER
    );
    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre"}, 0
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
    private final JTable tabla = new JTable(modeloTabla);
    private boolean ocupada;

    public VentanaCategorias(JFrame propietario) {
        super(propietario, "Biblioteca Escolar - Categorías", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(580, 380));

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.getColumnModel().getColumn(0).setMaxWidth(80);
        tabla.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                actualizarControles();
            }
        });

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(crearFormulario(), BorderLayout.NORTH);

        JScrollPane listado = new JScrollPane(tabla);
        listado.setPreferredSize(new Dimension(600, 260));
        contenido.add(listado, BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarControles();
        pack();
        setLocationRelativeTo(propietario);
    }

    private JPanel crearFormulario() {
        JLabel etiquetaNombre = new JLabel("Nombre:");
        etiquetaNombre.setLabelFor(campoNombre);

        JPanel campo = new JPanel(new BorderLayout(12, 0));
        campo.add(etiquetaNombre, BorderLayout.WEST);
        campo.add(campoNombre, BorderLayout.CENTER);

        JPanel formulario = new JPanel(new BorderLayout(0, 12));
        formulario.add(new JLabel(
                "Seleccione una categoría para editarla, o pulse Nuevo."
        ), BorderLayout.NORTH);
        formulario.add(campo, BorderLayout.CENTER);
        return formulario;
    }

    private JPanel crearPie() {
        JPanel acciones = new JPanel(new GridLayout(1, 5, 8, 0));
        acciones.add(botonNuevo);
        acciones.add(botonGuardar);
        acciones.add(botonEliminar);
        acciones.add(botonRecargar);
        acciones.add(botonCerrar);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(acciones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
    }

    public String getNombre() {
        return campoNombre.getText();
    }

    public int getIdSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return 0;
        }
        int filaModelo = tabla.convertRowIndexToModel(fila);
        return ((Number) modeloTabla.getValueAt(filaModelo, 0)).intValue();
    }

    public void mostrarNombreSeleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            campoNombre.setText("");
            return;
        }
        int filaModelo = tabla.convertRowIndexToModel(fila);
        campoNombre.setText((String) modeloTabla.getValueAt(filaModelo, 1));
    }

    public void mostrarCategorias(List<Categoria> categorias) {
        limpiarFormulario();
        modeloTabla.setRowCount(0);
        for (Categoria categoria : categorias) {
            modeloTabla.addRow(new Object[]{categoria.getId(), categoria.getNombre()});
        }
    }

    public void limpiarFormulario() {
        tabla.clearSelection();
        campoNombre.setText("");
    }

    public void enfocarNombre() {
        campoNombre.requestFocusInWindow();
    }

    public void alNuevo(ActionListener accion) {
        botonNuevo.addActionListener(accion);
    }

    public void alGuardar(ActionListener accion) {
        botonGuardar.addActionListener(accion);
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
        campoNombre.setEnabled(!ocupada);
        tabla.setEnabled(!ocupada);
        tabla.getTableHeader().setEnabled(!ocupada);
        botonNuevo.setEnabled(!ocupada);
        botonGuardar.setEnabled(!ocupada);
        botonEliminar.setEnabled(!ocupada && getIdSeleccionado() > 0);
        botonRecargar.setEnabled(!ocupada);
        botonCerrar.setEnabled(!ocupada);
    }
}