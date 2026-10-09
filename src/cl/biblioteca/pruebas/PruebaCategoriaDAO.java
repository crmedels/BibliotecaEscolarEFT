package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.CategoriaDAO;
import cl.biblioteca.interfaces.CrudDAO;
import cl.biblioteca.modelo.Categoria;

import java.sql.SQLException;
import java.util.List;

/**
 * Comprueba el CRUD real de categorías usando un registro temporal.
 */
public final class PruebaCategoriaDAO {

    private PruebaCategoriaDAO() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        CrudDAO<Categoria> dao = new CategoriaDAO();
        int idTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA CRUD DE CATEGORÍAS");

        try {
            int cantidadInicial = dao.listar().size();
            System.out.println("Categorías iniciales: " + cantidadInicial);

            String nombreInicial = "Prueba EFT 'categoría'";
            Categoria guardada = dao.guardar(new Categoria(nombreInicial));
            int idCreado = guardada.getId();
            idTemporal = idCreado;

            comprobar(idCreado > 0, "MySQL no generó un ID válido.");
            System.out.println("Guardar: OK");

            // Una conexión nueva permite comprobar que el INSERT se confirmó.
            baseDatos.cerrar();

            Categoria encontrada = dao.buscarPorId(idCreado).orElseThrow(
                    () -> new IllegalStateException("La categoría no quedó guardada.")
            );

            comprobar(nombreInicial.equals(encontrada.getNombre()),
                    "El nombre guardado no coincide.");
            System.out.println("Buscar y comprobar persistencia: OK");

            List<Categoria> categorias = dao.listar();
            comprobar(categorias.stream().anyMatch(c -> c.getId() == idCreado),
                    "La categoría guardada no aparece en el listado.");
            System.out.println("Listar: OK");

            String nombreActualizado = "Prueba EFT actualizada";
            comprobar(dao.actualizar(new Categoria(idCreado, nombreActualizado)),
                    "No se pudo actualizar la categoría.");

            baseDatos.cerrar();

            Categoria actualizada = dao.buscarPorId(idCreado).orElseThrow(
                    () -> new IllegalStateException("La categoría actualizada no existe.")
            );

            comprobar(nombreActualizado.equals(actualizada.getNombre()),
                    "La actualización no quedó guardada.");
            System.out.println("Actualizar y comprobar persistencia: OK");

            comprobar(dao.eliminar(idCreado), "No se pudo eliminar la categoría.");
            baseDatos.cerrar();

            comprobar(dao.buscarPorId(idCreado).isEmpty(),
                    "La categoría sigue existiendo después de eliminarla.");
            idTemporal = 0;
            System.out.println("Eliminar y comprobar persistencia: OK");

            comprobar(!dao.actualizar(new Categoria(idCreado, nombreActualizado)),
                    "Actualizar un ID inexistente debe devolver false.");
            comprobar(!dao.eliminar(idCreado),
                    "Eliminar un ID inexistente debe devolver false.");
            System.out.println("Operaciones sobre ID inexistente: OK");

            comprobar(dao.listar().size() == cantidadInicial,
                    "La cantidad final de categorías cambió.");
            System.out.println("PRUEBA COMPLETADA CORRECTAMENTE");

        } catch (SQLException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó.");
            System.err.println(e.getMessage());

        } finally {
            if (idTemporal > 0) {
                try {
                    dao.eliminar(idTemporal);
                } catch (SQLException e) {
                    resultado = 1;
                    System.err.println("No se pudo limpiar la categoría temporal ID "
                            + idTemporal + ": " + e.getMessage());
                }
            }

            try {
                baseDatos.cerrar();
            } catch (SQLException e) {
                resultado = 1;
                System.err.println("No se pudo cerrar la conexión: " + e.getMessage());
            }
        }

        if (resultado != 0) {
            System.exit(resultado);
        }
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }
}