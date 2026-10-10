package cl.biblioteca.vista;

import cl.biblioteca.modelo.Categoria;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Presenta un formulario modal para crear o editar una categoría.
 * El nombre escrito se mantiene separado de la categoría del listado.
 */
public final class DialogoCategoria extends JDialog {

    private final JTextField campoNombre = new JTextField(30);
    private final JButton botonGuardar = new JButton();
    private final JButton botonCancelar = new JButton("Cancelar");
    private final JLabel etiquetaEstado = new JLabel("", SwingConstants.CENTER);

    private final int idCategoria;
    private boolean ocupada;

    /**
     * Una categoría nula abre la creación; una existente, la edición.
     */
    public DialogoCategoria(Window propietario, Categoria categoria) {
        super(propietario, "Biblioteca Escolar - Categoría", ModalityType.APPLICATION_MODAL);

        boolean edicion = categoria != null;

        if (edicion && categoria.getId() <= 0) {
            throw new IllegalArgumentException(
                    "Debe seleccionar una categoría existente para editar."
            );
        }

        idCategoria = edicion ? categoria.getId() : 0;

        setTitle(edicion
                ? "Biblioteca Escolar - Editar categoría"
                : "Biblioteca Escolar - Nueva categoría");

        botonGuardar.setText(edicion ? "Guardar cambios" : "Guardar");

        etiquetaEstado.setText(edicion
                ? "Modifique el nombre y pulse Guardar cambios."
                : "Escriba el nombre y pulse Guardar.");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(520, 220));

        if (edicion) {
            campoNombre.setText(categoria.getNombre());
            campoNombre.setCaretPosition(0);
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
        JLabel etiquetaNombre = new JLabel("Nombre:");
        etiquetaNombre.setLabelFor(campoNombre);

        JPanel fila = new JPanel(new BorderLayout(12, 0));
        fila.add(etiquetaNombre, BorderLayout.WEST);
        fila.add(campoNombre, BorderLayout.CENTER);

        JPanel formulario = new JPanel(new BorderLayout());
        formulario.add(fila, BorderLayout.NORTH);
        return formulario;
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

    public int getIdCategoria() {
        return idCategoria;
    }

    public String getNombre() {
        return campoNombre.getText();
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

        campoNombre.setEnabled(!ocupada);
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