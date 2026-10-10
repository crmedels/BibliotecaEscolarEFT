package cl.biblioteca.vista;

import cl.biblioteca.modelo.SesionUsuario;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Presenta las opciones del sistema según la sesión del usuario.
 */
public final class VentanaPrincipal extends JFrame {

    private final JButton botonLibros = new JButton();
    private final JButton botonCategorias = new JButton("Gestionar categorías");
    private final JButton botonEstudiantes = new JButton("Gestionar estudiantes");
    private final JButton botonPrestamos = new JButton();
    private final JButton botonReportes = new JButton();
    private final JButton botonCerrarSesion = new JButton("Cerrar sesión");
    private final JButton botonSalir = new JButton("Salir");

    private final JButton[] botones = {
            botonLibros, botonCategorias, botonEstudiantes, botonPrestamos,
            botonReportes, botonCerrarSesion, botonSalir
    };

    private final JLabel etiquetaEstado = new JLabel(
            "Seleccione una opción para continuar.", SwingConstants.CENTER
    );

    private final boolean administrador;
    private boolean ocupada;

    public VentanaPrincipal(SesionUsuario sesion) {
        super("Biblioteca Escolar - Menú principal");

        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        administrador = sesion.puedeAdministrar();
        botonLibros.setText(administrador ? "Gestionar libros" : "Consultar catálogo");
        botonPrestamos.setText(administrador
                ? "Préstamos y devoluciones" : "Mis préstamos y devoluciones");
        botonReportes.setText(administrador ? "Reportes" : "Mi historial");

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(480, 320));

        JPanel contenido = new JPanel(new BorderLayout(0, 20));
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        contenido.add(crearCabecera(sesion), BorderLayout.NORTH);
        contenido.add(crearMenu(), BorderLayout.CENTER);
        contenido.add(crearPie(), BorderLayout.SOUTH);

        setContentPane(contenido);
        actualizarBotones();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel crearCabecera(SesionUsuario sesion) {
        JPanel cabecera = new JPanel(new GridLayout(3, 1, 0, 6));
        JLabel titulo = new JLabel("Biblioteca Escolar", SwingConstants.CENTER);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 22f));

        cabecera.add(titulo);
        cabecera.add(new JLabel("Usuario: " + sesion.getNombre(), SwingConstants.CENTER));
        cabecera.add(new JLabel(
                "RUT: " + sesion.getRut() + " | Perfil: "
                        + (administrador ? "Bibliotecario" : "Estudiante"),
                SwingConstants.CENTER
        ));
        return cabecera;
    }

    private JPanel crearMenu() {
        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 10));
        menu.add(botonLibros);

        if (administrador) {
            menu.add(botonCategorias);
            menu.add(botonEstudiantes);
        }

        menu.add(botonPrestamos);
        menu.add(botonReportes);
        return menu;
    }

    private JPanel crearPie() {
        JPanel acciones = new JPanel(new GridLayout(1, 2, 12, 0));
        acciones.add(botonCerrarSesion);
        acciones.add(botonSalir);

        JPanel pie = new JPanel(new BorderLayout(0, 12));
        pie.add(acciones, BorderLayout.CENTER);
        pie.add(etiquetaEstado, BorderLayout.SOUTH);
        return pie;
    }

    public void alLibros(ActionListener accion) {
        registrarAccion(botonLibros, accion);
    }

    public void alCategorias(ActionListener accion) {
        registrarAccion(botonCategorias, accion);
    }

    public void alEstudiantes(ActionListener accion) {
        registrarAccion(botonEstudiantes, accion);
    }

    public void alPrestamos(ActionListener accion) {
        registrarAccion(botonPrestamos, accion);
    }

    public void alReportes(ActionListener accion) {
        registrarAccion(botonReportes, accion);
    }

    public void alCerrarSesion(ActionListener accion) {
        registrarAccion(botonCerrarSesion, accion);
    }

    public void alSalir(Runnable accion) {
        registrarAccion(botonSalir, evento -> accion.run());
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
        actualizarBotones();
        setCursor(Cursor.getPredefinedCursor(
                ocupada ? Cursor.WAIT_CURSOR : Cursor.DEFAULT_CURSOR
        ));
    }

    private void registrarAccion(JButton boton, ActionListener accion) {
        boton.addActionListener(accion);
        actualizarBotones();
    }

    private void actualizarBotones() {
        for (JButton boton : botones) {
            boton.setEnabled(!ocupada && boton.getActionListeners().length > 0);
        }
    }
}