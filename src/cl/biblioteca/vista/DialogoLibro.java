package cl.biblioteca.vista;

import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Presenta un formulario modal para crear o editar un libro.
 * Los cambios escritos se mantienen separados del libro del catálogo.
 */
public final class DialogoLibro extends JDialog {

    private final JTextField campoTitulo = new JTextField(35);
    private final JTextField campoAutor = new JTextField(35);
    private final JTextField campoIsbn = new JTextField(20);
    private final JTextField campoEditorial = new JTextField(35);
    private final JTextField campoStock = new JTextField("0", 10);
    private final JTextField[] campos = {
            campoTitulo, campoAutor, campoIsbn, campoEditorial, campoStock
    };
    private final JComboBox<Categoria> campoCategoria = new JComboBox<>();
    private final JButton botonGuardar = new JButton();
    private final JButton botonCancelar = new JButton("Cancelar");
    private final JLabel etiquetaEstado = new JLabel("", SwingConstants.CENTER);
    private final int idLibro;
    private final int stockOriginal;
    private boolean ocupada;

    /**
     * Un libro nulo abre el formulario de creación; uno existente, el de edición.
     */
    public DialogoLibro(Window propietario, List<Categoria> categorias, Libro libro) {
        super(propietario, "Biblioteca Escolar - Libro", ModalityType.APPLICATION_MODAL);

        boolean edicion = libro != null;
        if (edicion && libro.getId() <= 0) {
            throw new IllegalArgumentException("Debe seleccionar un libro existente para editar.");
        }

        idLibro = edicion ? libro.getId() : 0;
        stockOriginal = edicion ? libro.getStock() : 0;
        setTitle(edicion ? "Biblioteca Escolar - Editar libro"
                : "Biblioteca Escolar - Nuevo libro");
        botonGuardar.setText(edicion ? "Guardar cambios" : "Guardar");
        etiquetaEstado.setText(edicion
                ? "Modifique los datos y pulse Guardar cambios."
                : "Complete los datos y pulse Guardar.");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(640, 350));
        cargarCategorias(categorias);

        if (edicion) {
            cargarLibro(libro);
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
        JPanel formulario = new JPanel(new GridBagLayout());
        GridBagConstraints posicion = new GridBagConstraints();

        agregarCampo(formulario, posicion, 0, "Título:", campoTitulo);
        agregarCampo(formulario, posicion, 1, "Autor:", campoAutor);
        agregarCampo(formulario, posicion, 2, "ISBN:", campoIsbn);
        agregarCampo(formulario, posicion, 3, "Editorial:", campoEditorial);
        agregarCampo(formulario, posicion, 4, "Stock disponible:", campoStock);
        agregarCampo(formulario, posicion, 5, "Categoría:", campoCategoria);
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

    private void cargarCategorias(List<Categoria> categorias) {
        campoCategoria.addItem(new Categoria(0, "Seleccione una categoría"));
        for (Categoria categoria : List.copyOf(categorias)) {
            campoCategoria.addItem(new Categoria(categoria.getId(), categoria.getNombre()));
        }
        campoCategoria.setSelectedIndex(0);
    }

    private void cargarLibro(Libro libro) {
        campoTitulo.setText(libro.getTitulo());
        campoAutor.setText(libro.getAutor());
        campoIsbn.setText(libro.getIsbn());
        campoEditorial.setText(libro.getEditorial());
        campoStock.setText(String.valueOf(libro.getStock()));

        int idCategoria = libro.getCategoria() == null ? 0 : libro.getCategoria().getId();
        for (int indice = 0; indice < campoCategoria.getItemCount(); indice++) {
            if (campoCategoria.getItemAt(indice).getId() == idCategoria) {
                campoCategoria.setSelectedIndex(indice);
                break;
            }
        }

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
                enfocarTitulo();
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

    public int getIdLibro() {
        return idLibro;
    }

    public int getStockOriginal() {
        return stockOriginal;
    }

    public String getTitulo() {
        return campoTitulo.getText();
    }

    public String getAutor() {
        return campoAutor.getText();
    }

    public String getIsbn() {
        return campoIsbn.getText();
    }

    public String getEditorial() {
        return campoEditorial.getText();
    }

    public String getStock() {
        return campoStock.getText();
    }

    public Categoria getCategoriaSeleccionada() {
        return (Categoria) campoCategoria.getSelectedItem();
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
        campoCategoria.setEnabled(!ocupada);
        botonGuardar.setEnabled(!ocupada);
        botonCancelar.setEnabled(!ocupada);
        setCursor(Cursor.getPredefinedCursor(
                ocupada ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR
        ));
    }

    public void enfocarTitulo() {
        if (!ocupada) {
            campoTitulo.requestFocusInWindow();
        }
    }
}