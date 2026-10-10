package cl.biblioteca.servicio;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.CategoriaDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.util.Validador;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Valida el catálogo y protege sus modificaciones mediante permisos y control de stock.
 */
public final class LibroServicio {

    private static final String LIBRO_INEXISTENTE =
            "El libro seleccionado ya no existe.";

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();
    private final CrudDAO<Libro> libroDAO = new LibroDAO();
    private final CrudDAO<Categoria> categoriaDAO = new CategoriaDAO();
    private final SesionUsuario sesion;

    public LibroServicio(SesionUsuario sesion) {
        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        this.sesion = sesion;
    }

    public Libro guardar(Libro datos) throws SQLException {
        sesion.exigirAdministracion();
        Libro libro = validarDatos(datos);

        return baseDatos.ejecutar(conexion -> {
            libro.setCategoria(buscarCategoria(libro.getCategoria().getId()));
            comprobarIsbnDisponible(libro.getIsbn(), 0);
            return libroDAO.guardar(libro);
        });
    }

    /**
     * El stock original debe ser el valor obtenido al cargar el formulario de edición.
     */
    public Libro actualizar(Libro datos, int stockOriginal) throws SQLException {
        sesion.exigirAdministracion();
        Validador.validarSeleccion(datos, "Libro");
        Validador.validarId(datos.getId(), "Libro");
        Validador.validarStock(stockOriginal);
        Libro libro = validarDatos(datos);

        return baseDatos.ejecutar(conexion -> {
            Libro actual = buscarExistente(libro.getId());
            if (actual.getStock() != stockOriginal) {
                throw new IllegalStateException(
                        "El stock del libro cambió mientras editaba. "
                                + "Actualice el listado y vuelva a intentarlo."
                );
            }

            libro.setCategoria(buscarCategoria(libro.getCategoria().getId()));
            comprobarIsbnDisponible(libro.getIsbn(), libro.getId());

            if (!libroDAO.actualizar(libro)) {
                throw new IllegalStateException(LIBRO_INEXISTENTE);
            }

            return libro;
        });
    }

    public void eliminar(int id) throws SQLException {
        sesion.exigirAdministracion();
        Validador.validarId(id, "Libro");

        try {
            if (!libroDAO.eliminar(id)) {
                throw new IllegalStateException(LIBRO_INEXISTENTE);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalStateException(
                    "No puede eliminar un libro que tiene préstamos asociados.", e
            );
        }
    }

    public Libro buscarPorId(int id) throws SQLException {
        Validador.validarId(id, "Libro");
        return buscarExistente(id);
    }

    public List<Libro> listar() throws SQLException {
        return libroDAO.listar();
    }

    public List<Libro> listarDisponibles() throws SQLException {
        return listar().stream().filter(Libro::tieneStockDisponible).toList();
    }

    private Libro buscarExistente(int id) throws SQLException {
        return libroDAO.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException(LIBRO_INEXISTENTE)
        );
    }

    private Categoria buscarCategoria(int id) throws SQLException {
        return categoriaDAO.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException("La categoría seleccionada ya no existe.")
        );
    }

    private Libro validarDatos(Libro datos) {
        Validador.validarSeleccion(datos, "Libro");
        String titulo = Validador.textoObligatorio(datos.getTitulo(), "Título", 200);
        String autor = Validador.textoObligatorio(datos.getAutor(), "Autor", 100);
        String isbn = Validador.validarIsbn(datos.getIsbn());
        String editorial = Validador.textoObligatorio(datos.getEditorial(), "Editorial", 100);
        int stock = Validador.validarStock(datos.getStock());

        Categoria categoria = datos.getCategoria();
        Validador.validarSeleccion(categoria, "Categoría");
        int idCategoria = Validador.validarId(categoria.getId(), "Categoría");

        // Trabaja con una copia para conservar intactos los datos del llamador.
        return new Libro(
                datos.getId(), titulo, autor, isbn, editorial, stock,
                new Categoria(idCategoria, categoria.getNombre())
        );
    }

    private void comprobarIsbnDisponible(String isbn, int idActual) throws SQLException {
        boolean repetido = libroDAO.listar().stream().anyMatch(libro ->
                libro.getId() != idActual
                        && libro.getIsbn() != null
                        && isbn.equalsIgnoreCase(libro.getIsbn())
        );

        if (repetido) {
            throw new IllegalArgumentException("Ya existe un libro con ese ISBN.");
        }
    }
}