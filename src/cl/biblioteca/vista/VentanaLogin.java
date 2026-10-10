package cl.biblioteca.vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Presenta el formulario de acceso y los eventos que utilizará el controlador.
 */
public final class VentanaLogin extends JFrame {

    private final JTextField campoRut = new JTextField(22);
    private final JPasswordField campoContrasena = new JPasswordField(22);
    private final JButton botonIngresar = new JButton("Ingresar");
    private final JLabel etiquetaEstado = new JLabel(
            "Ingrese sus credenciales para continuar.", SwingConstants.CENTER
    );
    private boolean ocupada;

    public VentanaLogin() {
        super("Biblioteca Escolar - Inicio de sesión");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);

        JPanel contenido = new JPanel(new BorderLayout(0, 20));
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titulo = new JLabel("Biblioteca Escolar", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));
        contenido.add(titulo, BorderLayout.NORTH);
        contenido.add(crearFormulario(), BorderLayout.CENTER);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(botonIngresar, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        contenido.add(pie, BorderLayout.SOUTH);

        setContentPane(contenido);
        getRootPane().setDefaultButton(botonIngresar);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel crearFormulario() {
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints posicion = new GridBagConstraints();
        posicion.anchor = GridBagConstraints.LINE_START;
        posicion.insets = new Insets(6, 0, 6, 12);

        JLabel etiquetaRut = new JLabel("RUT:");
        etiquetaRut.setLabelFor(campoRut);
        JLabel etiquetaContrasena = new JLabel("Contraseña:");
        etiquetaContrasena.setLabelFor(campoContrasena);

        posicion.gridx = 0;
        posicion.gridy = 0;
        formulario.add(etiquetaRut, posicion);
        posicion.gridy = 1;
        formulario.add(etiquetaContrasena, posicion);

        posicion.gridx = 1;
        posicion.gridy = 0;
        posicion.weightx = 1;
        posicion.fill = GridBagConstraints.HORIZONTAL;
        posicion.insets = new Insets(6, 0, 6, 0);
        formulario.add(campoRut, posicion);
        posicion.gridy = 1;
        formulario.add(campoContrasena, posicion);

        campoRut.setToolTipText("Ejemplo: 12.345.678-9");
        return formulario;
    }

    public String getRut() {
        return campoRut.getText();
    }

    public char[] getContrasena() {
        return campoContrasena.getPassword();
    }

    public void alIngresar(ActionListener accion) {
        botonIngresar.addActionListener(accion);
    }

    public void alCerrar(Runnable accion) {
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
        campoRut.setEnabled(!ocupada);
        campoContrasena.setEnabled(!ocupada);
        botonIngresar.setEnabled(!ocupada);
        etiquetaEstado.setText(mensaje);
        setCursor(Cursor.getPredefinedCursor(
                ocupada ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR
        ));
    }

    public void limpiarContrasena() {
        campoContrasena.setText("");
    }

    public void enfocarRut() {
        campoRut.requestFocusInWindow();
    }
}