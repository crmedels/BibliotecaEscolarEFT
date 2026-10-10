package cl.biblioteca.vista;

import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.SesionUsuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * Presenta la gestión de libros o la consulta del catálogo según el perfil.
 */
public final class VentanaLibros extends JDialog {

    private final JTextField campoTitulo = new JTextField(35);
    private final JTextField campoAutor = new JTextField(35);
    private final JTextField campoIsbn = new JTextField(20);
    private final JTextField campoEditorial = new JTextField(35);
    private final JTextField campoStock = new JTextField("0", 10);
    private final JTextField[] campos = {
            campoTitulo, campoAutor, campoIsbn, campoEditorial, campoStock
    };
    private final JComboBox<Categoria> campoCategoria = new JComboBox<>();
    private final JButton botonNuevo = new JButton("Nuevo");
    private final JButton botonGuardar = new JButton("Guardar");
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
    private final JTable tabla = new JTable(modeloTabla);
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
        setMinimumSize(new Dimension(760, 520));

        for (JTextField campo : campos) {
            campo.setEditable(administrador);
        }

        campoCategoria.addItem(new Categoria(0, "Seleccione una categoría"));
        etiquetaEstado.setText(administrador
                ? "Seleccione un libro para editarlo, o pulse Nuevo."
                : "Seleccione un libro para consultar sus datos.");

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.setAutoCreateRowSorter(true);
        tabla.setFillsViewportHeight(true);
        tabla.setRowHeight(24);
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

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        contenido.add(crearFormulario(), BorderLayout.NORTH);

        JScrollPane listado = new JScrollPane(tabla);
        listado.setPreferredSize(new Dimension(880, 260));
        contenido.add(listado, BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarControles();
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
        JPanel acciones = new JPanel(new GridLayout(1, administrador ? 5 : 2, 8, 0));

        if (administrador) {
            acciones.add(botonNuevo);
            acciones.add(botonGuardar);
            acciones.add(botonEliminar);
        }

        acciones.add(botonRecargar);
        acciones.add(botonCerrar);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(acciones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
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

    public Libro getLibroSeleccionado() {
        int fila = tabla.getSelectedRow();
        return fila < 0 ? null : libros.get(tabla.convertRowIndexToModel(fila));
    }

    public void mostrarCategorias(List<Categoria> categorias) {
        campoCategoria.removeAllItems();
        campoCategoria.addItem(new Categoria(0, "Seleccione una categoría"));

        for (Categoria categoria : categorias) {
            campoCategoria.addItem(categoria);
        }

        campoCategoria.setSelectedIndex(0);
    }

    public void mostrarLibros(List<Libro> datos) {
        limpiarFormulario();
        libros = List.copyOf(datos);
        modeloTabla.setRowCount(0);

        for (Libro libro : libros) {
            modeloTabla.addRow(new Object[]{
                    libro.getId(), libro.getTitulo(), libro.getAutor(), libro.getIsbn(),
                    libro.getEditorial(), libro.getStock(),
                    libro.getCategoria() == null ? "" : libro.getCategoria().getNombre()
            });
        }
    }

    public void mostrarLibroSeleccionado() {
        Libro libro = getLibroSeleccionado();

        if (libro == null) {
            limpiarFormulario();
            return;
        }

        campoTitulo.setText(libro.getTitulo());
        campoAutor.setText(libro.getAutor());
        campoIsbn.setText(libro.getIsbn());
        campoEditorial.setText(libro.getEditorial());
        campoStock.setText(String.valueOf(libro.getStock()));

        seleccionarCategoria(
                libro.getCategoria() == null ? 0 : libro.getCategoria().getId()
        );
    }

    private void seleccionarCategoria(int id) {
        for (int indice = 0; indice < campoCategoria.getItemCount(); indice++) {
            if (campoCategoria.getItemAt(indice).getId() == id) {
                campoCategoria.setSelectedIndex(indice);
                return;
            }
        }

        campoCategoria.setSelectedIndex(0);
    }

    public void limpiarFormulario() {
        tabla.clearSelection();

        for (JTextField campo : campos) {
            campo.setText("");
        }

        campoStock.setText("0");
        campoCategoria.setSelectedIndex(0);
    }

    public void enfocarTitulo() {
        campoTitulo.requestFocusInWindow();
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

        campoCategoria.setEnabled(administrador && !ocupada);
        tabla.setEnabled(!ocupada);
        tabla.getTableHeader().setEnabled(!ocupada);
        botonNuevo.setEnabled(administrador && !ocupada);
        botonGuardar.setEnabled(administrador && !ocupada);
        botonEliminar.setEnabled(
                administrador && !ocupada && getLibroSeleccionado() != null
        );
        botonRecargar.setEnabled(!ocupada);
        botonCerrar.setEnabled(!ocupada);
    }
}