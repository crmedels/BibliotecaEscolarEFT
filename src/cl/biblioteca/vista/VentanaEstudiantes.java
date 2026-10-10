package cl.biblioteca.vista;

import cl.biblioteca.modelo.Estudiante;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
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
import java.util.List;

/**
 * Presenta las fichas de estudiantes y el formulario para gestionarlas.
 */
public final class VentanaEstudiantes extends JDialog {

    private final JTextField campoNombre = new JTextField(35);
    private final JTextField campoRut = new JTextField(20);
    private final JTextField campoCurso = new JTextField(20);
    private final JTextField campoCorreo = new JTextField(35);
    private final JTextField[] campos = {
            campoNombre, campoRut, campoCurso, campoCorreo
    };
    private final JPasswordField campoContrasena = new JPasswordField(25);
    private final JButton botonNuevo = new JButton("Nuevo");
    private final JButton botonGuardar = new JButton("Guardar");
    private final JButton botonEliminar = new JButton("Eliminar");
    private final JButton botonRecargar = new JButton("Recargar");
    private final JButton botonCerrar = new JButton("Cerrar");
    private final JLabel etiquetaEstado = new JLabel(
            "Seleccione un estudiante para editarlo, o pulse Nuevo.",
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
    private final JTable tabla = new JTable(modeloTabla);
    private List<Estudiante> estudiantes = List.of();
    private boolean ocupada;

    public VentanaEstudiantes(JFrame propietario) {
        super(propietario, "Biblioteca Escolar - Gestión de estudiantes", true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(740, 500));

        campoRut.setToolTipText("Ejemplo: 12.345.678-9");
        campoContrasena.setToolTipText(
                "Al editar, una contraseña vacía conserva la actual."
        );

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
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

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(crearFormulario(), BorderLayout.NORTH);

        JScrollPane listado = new JScrollPane(tabla);
        listado.setPreferredSize(new Dimension(820, 260));
        contenido.add(listado, BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarControles();
        pack();
        setLocationRelativeTo(propietario);
    }

    private JPanel crearFormulario() {
        JPanel camposFormulario = new JPanel(new GridBagLayout());
        GridBagConstraints posicion = new GridBagConstraints();

        agregarCampo(camposFormulario, posicion, 0, "Nombre:", campoNombre);
        agregarCampo(camposFormulario, posicion, 1, "RUT:", campoRut);
        agregarCampo(camposFormulario, posicion, 2, "Curso:", campoCurso);
        agregarCampo(camposFormulario, posicion, 3, "Correo:", campoCorreo);
        agregarCampo(camposFormulario, posicion, 4, "Contraseña:", campoContrasena);

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        formulario.add(camposFormulario, BorderLayout.CENTER);
        formulario.add(new JLabel(
                "Al crear, ingrese una contraseña. Al editar, deje vacío para conservar la actual."
        ), BorderLayout.SOUTH);
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

    public String getRut() {
        return campoRut.getText();
    }

    public String getCurso() {
        return campoCurso.getText();
    }

    public String getCorreo() {
        return campoCorreo.getText();
    }

    public char[] getContrasena() {
        return campoContrasena.getPassword();
    }

    public Estudiante getEstudianteSeleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : estudiantes.get(tabla.convertRowIndexToModel(fila));
    }

    public void mostrarEstudiantes(List<Estudiante> datos) {
        limpiarFormulario();
        estudiantes = List.copyOf(datos);
        modeloTabla.setRowCount(0);

        for (Estudiante estudiante : estudiantes) {
            modeloTabla.addRow(new Object[]{
                    estudiante.getId(), estudiante.getNombre(), estudiante.getRut(),
                    estudiante.getCurso(), estudiante.getCorreo()
            });
        }
    }

    public void mostrarEstudianteSeleccionado() {
        limpiarContrasena();
        Estudiante estudiante = getEstudianteSeleccionado();

        if (estudiante == null) {
            limpiarFormulario();
            return;
        }

        campoNombre.setText(estudiante.getNombre());
        campoRut.setText(estudiante.getRut());
        campoCurso.setText(estudiante.getCurso());
        campoCorreo.setText(estudiante.getCorreo());
    }

    public void limpiarFormulario() {
        tabla.clearSelection();

        for (JTextField campo : campos) {
            campo.setText("");
        }

        limpiarContrasena();
    }

    public void limpiarContrasena() {
        campoContrasena.setText("");
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
        for (JTextField campo : campos) {
            campo.setEnabled(!ocupada);
        }

        campoContrasena.setEnabled(!ocupada);
        tabla.setEnabled(!ocupada);
        tabla.getTableHeader().setEnabled(!ocupada);
        botonNuevo.setEnabled(!ocupada);
        botonGuardar.setEnabled(!ocupada);
        botonEliminar.setEnabled(!ocupada && getEstudianteSeleccionado() != null);
        botonRecargar.setEnabled(!ocupada);
        botonCerrar.setEnabled(!ocupada);
    }
}