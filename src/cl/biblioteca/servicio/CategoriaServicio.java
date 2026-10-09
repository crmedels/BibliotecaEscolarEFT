package cl.biblioteca.servicio;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.CategoriaDAO;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.util.Validador;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Aplica las validaciones y los permisos a la gestión de categorías.
 */
public final class CategoriaServicio {

    private static final String CATEGORIA_INEXISTENTE =
            "La categoría seleccionada ya no existe.";

    private final DatabaseConnection baseDatos = DatabaseConnection.getInstance();
    private final CrudDAO<Categoria> categoriaDAO = new CategoriaDAO();
    private final SesionUsuario sesion;

    public CategoriaServicio(SesionUsuario sesion) {
        if (sesion == null) {
            throw new IllegalStateException("Debe iniciar sesión para utilizar el sistema.");
        }

        this.sesion = sesion;
    }

    public Categoria guardar(String nombre) throws SQLException {
        sesion.exigirAdministracion();
        String nombreValidado = Validador.textoObligatorio(
                nombre, "Nombre de categoría", 100
        );

        return baseDatos.ejecutar(conexion -> {
            comprobarNombreDisponible(nombreValidado, 0);
            return categoriaDAO.guardar(new Categoria(nombreValidado));
        });
    }

    public Categoria actualizar(int id, String nombre) throws SQLException {
        sesion.exigirAdministracion();
        Validador.validarId(id, "Categoría");
        String nombreValidado = Validador.textoObligatorio(
                nombre, "Nombre de categoría", 100
        );

        return baseDatos.ejecutar(conexion -> {
            buscarExistente(id);
            comprobarNombreDisponible(nombreValidado, id);
            Categoria categoria = new Categoria(id, nombreValidado);

            if (!categoriaDAO.actualizar(categoria)) {
                throw new IllegalStateException(CATEGORIA_INEXISTENTE);
            }

            return categoria;
        });
    }

    public void eliminar(int id) throws SQLException {
        sesion.exigirAdministracion();
        Validador.validarId(id, "Categoría");

        try {
            if (!categoriaDAO.eliminar(id)) {
                throw new IllegalStateException(CATEGORIA_INEXISTENTE);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalStateException(
                    "No puede eliminar una categoría que tiene libros asociados.", e
            );
        }
    }

    public Categoria buscarPorId(int id) throws SQLException {
        Validador.validarId(id, "Categoría");
        return buscarExistente(id);
    }

    public List<Categoria> listar() throws SQLException {
        return categoriaDAO.listar();
    }

    private Categoria buscarExistente(int id) throws SQLException {
        return categoriaDAO.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException(CATEGORIA_INEXISTENTE)
        );
    }

    private void comprobarNombreDisponible(String nombre, int idActual) throws SQLException {
        boolean repetido = categoriaDAO.listar().stream().anyMatch(categoria ->
                categoria.getId() != idActual
                        && categoria.getNombre() != null
                        && nombre.equalsIgnoreCase(categoria.getNombre().strip())
        );

        if (repetido) {
            throw new IllegalArgumentException("Ya existe una categoría con ese nombre.");
        }
    }
}