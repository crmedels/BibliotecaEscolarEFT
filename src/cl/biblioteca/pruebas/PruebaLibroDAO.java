package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.CategoriaDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Comprueba el CRUD, la categoría y las transacciones de stock.
 */
public final class PruebaLibroDAO {

    private static final String ERROR_PRUEBA = "PR001";

    private PruebaLibroDAO() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        LibroDAO dao = new LibroDAO();
        int idTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA CRUD Y STOCK DE LIBROS");

        try {
            int cantidadInicial = dao.listar().size();
            System.out.println("Libros iniciales: " + cantidadInicial);

            List<Categoria> categorias = new CategoriaDAO().listar();
            comprobar(!categorias.isEmpty(), "Se necesita una categoría para la prueba.");
            Categoria categoria = categorias.get(0);
            Categoria otraCategoria = categorias.get(categorias.size() - 1);

            String isbn = "EFT-" + UUID.randomUUID().toString().substring(0, 12);
            Libro guardado = dao.guardar(new Libro(
                    "Prueba EFT 'libro'", "Autor de prueba", isbn,
                    "Editorial EFT", 1, categoria
            ));

            int idCreado = guardado.getId();
            idTemporal = idCreado;
            comprobar(idCreado > 0, "MySQL no generó un ID válido.");
            baseDatos.cerrar();

            Libro encontrado = leer(dao, idCreado);
            comprobar("Prueba EFT 'libro'".equals(encontrado.getTitulo())
                            && "Autor de prueba".equals(encontrado.getAutor())
                            && isbn.equals(encontrado.getIsbn())
                            && "Editorial EFT".equals(encontrado.getEditorial())
                            && encontrado.getStock() == 1,
                    "Los datos del libro no quedaron guardados correctamente.");
            comprobarCategoria(encontrado, categoria);
            System.out.println("Guardar, buscar y categoría: OK");

            comprobar(dao.listar().stream().anyMatch(l -> l.getId() == idCreado),
                    "El libro temporal no aparece en el listado.");
            System.out.println("Listar: OK");

            Libro editado = new Libro(
                    idCreado, "Prueba EFT actualizada", "Autor actualizado", isbn,
                    "Editorial actualizada", 2, otraCategoria
            );
            comprobar(dao.actualizar(editado), "No se pudo actualizar el libro.");
            baseDatos.cerrar();

            Libro actualizado = leer(dao, idCreado);
            comprobar("Prueba EFT actualizada".equals(actualizado.getTitulo())
                            && "Autor actualizado".equals(actualizado.getAutor())
                            && isbn.equals(actualizado.getIsbn())
                            && "Editorial actualizada".equals(actualizado.getEditorial())
                            && actualizado.getStock() == 2,
                    "La actualización no quedó guardada correctamente.");
            comprobarCategoria(actualizado, otraCategoria);
            System.out.println("Actualizar y comprobar persistencia: OK");

            for (int intento = 0; intento < 2; intento++) {
                comprobar(baseDatos.ejecutarTransaccion(
                        conexion -> dao.descontarStock(conexion, idCreado)
                ), "No se pudo descontar un ejemplar disponible.");
            }

            comprobar(!baseDatos.ejecutarTransaccion(
                    conexion -> dao.descontarStock(conexion, idCreado)
            ), "Se permitió descontar un libro sin stock.");

            baseDatos.cerrar();
            comprobar(leer(dao, idCreado).getStock() == 0,
                    "El stock debe quedar en cero.");
            System.out.println("Stock agotado sin valores negativos: OK");

            comprobar(baseDatos.ejecutarTransaccion(
                    conexion -> dao.reponerStock(conexion, idCreado)
            ), "No se pudo reponer un ejemplar.");
            baseDatos.cerrar();
            comprobar(leer(dao, idCreado).getStock() == 1,
                    "La reposición no quedó guardada.");
            System.out.println("Reponer stock: OK");

            try {
                baseDatos.ejecutarTransaccion(conexion -> {
                    comprobar(dao.descontarStock(conexion, idCreado),
                            "No se pudo preparar la prueba de rollback.");
                    throw new SQLException("Fallo intencional de la prueba.", ERROR_PRUEBA);
                });

                throw new IllegalStateException("La transacción debía fallar.");

            } catch (SQLException e) {
                if (!ERROR_PRUEBA.equals(e.getSQLState())) {
                    throw e;
                }
            }

            baseDatos.cerrar();
            comprobar(leer(dao, idCreado).getStock() == 1,
                    "El rollback no recuperó el stock anterior.");
            System.out.println("Rollback del descuento: OK");

            comprobar(dao.eliminar(idCreado), "No se pudo eliminar el libro temporal.");
            baseDatos.cerrar();
            comprobar(dao.buscarPorId(idCreado).isEmpty(),
                    "El libro sigue existiendo después de eliminarlo.");
            idTemporal = 0;
            System.out.println("Eliminar y comprobar persistencia: OK");

            comprobar(!dao.actualizar(editado),
                    "Actualizar un ID inexistente debe devolver false.");
            comprobar(!dao.eliminar(idCreado),
                    "Eliminar un ID inexistente debe devolver false.");
            System.out.println("Operaciones sobre ID inexistente: OK");

            comprobar(dao.listar().size() == cantidadInicial,
                    "La cantidad final de libros cambió.");
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
                    System.err.println("No se pudo limpiar el libro temporal ID "
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

    private static Libro leer(LibroDAO dao, int id) throws SQLException {
        return dao.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException("No se encontró el libro temporal.")
        );
    }

    private static void comprobarCategoria(Libro libro, Categoria esperada) {
        comprobar(libro.getCategoria() != null
                        && libro.getCategoria().getId() == esperada.getId()
                        && esperada.getNombre().equals(libro.getCategoria().getNombre()),
                "La categoría del libro no coincide.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }
}