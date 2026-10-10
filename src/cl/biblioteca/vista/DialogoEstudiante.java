package cl.biblioteca.vista;

import cl.biblioteca.modelo.Estudiante;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Presenta un formulario modal para crear o editar un estudiante.
 * Los datos escritos se mantienen separados de la ficha del listado.
 */
public final class DialogoEstudiante extends JDialog {

    private final JTextField campoNombre = new JTextField(35);
    private final JTextField campoRut = new JTextField(20);
    private final JTextField campoCurso = new JTextField(20);
    private final JTextField campoCorreo = new JTextField(35);
    private final JPasswordField campoContrasena = new JPasswordField(25);

    private final JTextField[] campos = {
            campoNombre, campoRut, campoCurso, campoCorreo, campoContrasena
    };

    private final JButton botonGuardar = new JButton();
    private final JButton botonCancelar = new JButton("Cancelar");
    private final JLabel etiquetaEstado = new JLabel("", SwingConstants.CENTER);

    private final int idEstudiante;
    private final boolean edicion;
    private boolean ocupada;

    /**
     * Un estudiante nulo abre la creación; uno existente, la edición.
     */
    public DialogoEstudiante(Window propietario, Estudiante estudiante) {
        super(propietario, "Biblioteca Escolar - Estudiante", ModalityType.APPLICATION_MODAL);

        edicion = estudiante != null;

        if (edicion && estudiante.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar un estudiante existente para editar."
            );
        }

        idEstudiante = edicion ? estudiante.getId() : 0;

        setTitle(edicion
                ? "Biblioteca Escolar - Editar estudiante"
                : "Biblioteca Escolar - Nuevo estudiante");

        botonGuardar.setText(edicion ? "Guardar cambios" : "Guardar");

        etiquetaEstado.setText(edicion
                ? "Modifique los datos y pulse Guardar cambios."
                : "Complete la ficha y una contraseña para crear el acceso.");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(640, 360));

        campoRut.setToolTipText("Ejemplo: 12.345.678-9");

        campoContrasena.setToolTipText(edicion
                ? "Deje vacío para conservar la contraseña actual."
                : "Ingrese una contraseña para la cuenta del estudiante.");

        if (edicion) {
            cargarEstudiante(estudiante);
        }

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(crearFormulario(), BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        configurarCierre();
        getRootPane().setDefaultButton(botonGuardar);

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

        JLabel ayuda = new JLabel(edicion
                ? "<html>Deje la contraseña vacía para conservar la actual.<br>"
                  + "Si el estudiante no tiene cuenta, ingrese una contraseña para crearla.</html>"
                : "Ingrese una contraseña para crear la cuenta de acceso.");

        JPanel formulario = new JPanel(new BorderLayout(0, 10));
        formulario.add(camposFormulario, BorderLayout.CENTER);
        formulario.add(ayuda, BorderLayout.SOUTH);
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
        JPanel botones = new JPanel(new GridLayout(1, 2, 8, 0));
        botones.add(botonGuardar);
        botones.add(botonCancelar);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(botones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
    }

    private void cargarEstudiante(Estudiante estudiante) {
        campoNombre.setText(estudiante.getNombre());
        campoRut.setText(estudiante.getRut());
        campoCurso.setText(estudiante.getCurso());
        campoCorreo.setText(estudiante.getCorreo());

        for (JTextField campo : campos) {
            campo.setCaretPosition(0);
        }
    }

    private void configurarCierre() {
        botonCancelar.addActionListener(evento -> cancelar());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                cancelar();
            }

            @Override
            public void windowOpened(WindowEvent evento) {
                enfocarNombre();
            }
        });

        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke("ESCAPE"), "cancelar");

        getRootPane().getActionMap().put("cancelar", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evento) {
                cancelar();
            }
        });
    }

    private void cancelar() {
        if (!ocupada) {
            dispose();
        }
    }

    @Override
    public void dispose() {
        limpiarContrasena();
        super.dispose();
    }

    public int getIdEstudiante() {
        return idEstudiante;
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

    public void limpiarContrasena() {
        campoContrasena.setText("");
    }

    public void alGuardar(ActionListener accion) {
        botonGuardar.addActionListener(accion);
    }

    public boolean estaOcupada() {
        return ocupada;
    }

    public void establecerOcupada(boolean ocupada, String mensaje) {
        this.ocupada = ocupada;
        etiquetaEstado.setText(mensaje);

        for (JTextField campo : campos) {
            campo.setEnabled(!ocupada);
        }

        botonGuardar.setEnabled(!ocupada);
        botonCancelar.setEnabled(!ocupada);

        setCursor(Cursor.getPredefinedCursor(
                ocupada ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR
        ));
    }

    public void enfocarNombre() {
        if (!ocupada) {
            campoNombre.requestFocusInWindow();
        }
    }
}