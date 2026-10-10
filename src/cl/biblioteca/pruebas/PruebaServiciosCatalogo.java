package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.CategoriaDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.AutenticacionServicio;
import cl.biblioteca.servicio.CategoriaServicio;
import cl.biblioteca.servicio.LibroServicio;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Comprueba los servicios del catálogo utilizando registros temporales.
 */
public final class PruebaServiciosCatalogo {

    private PruebaServiciosCatalogo() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        int idCategoriaTemporal = 0;
        int idLibroTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA DE SERVICIOS DEL CATÁLOGO");

        try {
            AutenticacionServicio autenticacion = new AutenticacionServicio();
            SesionUsuario administrador = autenticacion.iniciarSesion("12345678-9", "clave123");
            SesionUsuario estudiante = autenticacion.iniciarSesion("98765432-1", "clave123");
            CategoriaServicio categorias = new CategoriaServicio(administrador);
            LibroServicio libros = new LibroServicio(administrador);
            int categoriasIniciales = categorias.listar().size();
            int librosIniciales = libros.listar().size();

            String nombre = "Categoría EFT " + UUID.randomUUID().toString().substring(0, 8);
            Categoria categoria = categorias.guardar("  " + nombre + "  ");
            idCategoriaTemporal = categoria.getId();
            baseDatos.cerrar();
            comprobar(nombre.equals(categorias.buscarPorId(categoria.getId()).getNombre()),
                    "La categoría no conservó el nombre normalizado.");

            comprobarRechazo(() -> categorias.guardar(nombre.toUpperCase(Locale.ROOT)),
                    IllegalArgumentException.class, "Ya existe una categoría con ese nombre.");
            categorias.actualizar(categoria.getId(), "  " + nombre + "  ");
            comprobar(categorias.listar().size() == categoriasIniciales + 1,
                    "El control de duplicados alteró la cantidad de categorías.");
            System.out.println("Categorías: normalización, duplicados y actualización: OK");

            String isbn = generarIsbnDisponible(libros);
            Libro datos = new Libro("  Libro EFT 'prueba'  ", "  Autor EFT  ",
                    " " + isbn.substring(0, 3) + "-" + isbn.substring(3) + " ",
                    "  Editorial EFT  ", 2, new Categoria(categoria.getId(), "Nombre desactualizado"));
            Libro libro = libros.guardar(datos);
            idLibroTemporal = libro.getId();
            baseDatos.cerrar();
            Libro guardado = libros.buscarPorId(libro.getId());
            comprobar("Libro EFT 'prueba'".equals(guardado.getTitulo())
                            && "Autor EFT".equals(guardado.getAutor())
                            && isbn.equals(guardado.getIsbn())
                            && "Editorial EFT".equals(guardado.getEditorial())
                            && guardado.getStock() == 2
                            && nombre.equals(guardado.getCategoria().getNombre()),
                    "El libro no conservó sus datos normalizados y su categoría actual.");
            comprobar(datos.getId() == 0 && datos.getTitulo().startsWith("  "),
                    "El servicio modificó los datos originales del llamador.");

            comprobarRechazo(() -> libros.guardar(libro), IllegalArgumentException.class,
                    "Ya existe un libro con ese ISBN.");
            Libro stockInvalido = new Libro("Libro inválido", "Autor", isbn,
                    "Editorial", -1, categoria);
            comprobarRechazo(() -> libros.guardar(stockInvalido), IllegalArgumentException.class,
                    "El stock no puede ser negativo.");
            comprobar(libros.listar().size() == librosIniciales + 1,
                    "Se guardó un libro que debía rechazarse.");
            System.out.println("Libros: normalización, ISBN duplicado y validación del stock: OK");

            comprobarPermisos(estudiante, categoria, libro);
            System.out.println("Consulta del estudiante y permisos administrativos: OK");

            comprobarRechazo(() -> categorias.eliminar(categoria.getId()),
                    IllegalStateException.class,
                    "No puede eliminar una categoría que tiene libros asociados.");
            String nombreActualizado = nombre + " actualizada";
            categorias.actualizar(categoria.getId(), nombreActualizado);
            baseDatos.cerrar();
            comprobar(nombreActualizado.equals(
                            libros.buscarPorId(libro.getId()).getCategoria().getNombre()),
                    "El libro no refleja el nombre actualizado de la categoría.");
            System.out.println("Protección y actualización de la categoría asociada: OK");

            comprobarEdicionStock(libros, libro.getId());

            libros.eliminar(libro.getId());
            categorias.eliminar(categoria.getId());
            baseDatos.cerrar();
            comprobar(new LibroDAO().buscarPorId(libro.getId()).isEmpty()
                            && new CategoriaDAO().buscarPorId(categoria.getId()).isEmpty(),
                    "Los registros temporales siguen existiendo después de eliminarlos.");
            comprobar(libros.listar().size() == librosIniciales
                            && categorias.listar().size() == categoriasIniciales,
                    "La cantidad final de registros no coincide con la inicial.");
            idLibroTemporal = 0;
            idCategoriaTemporal = 0;
            System.out.println("Eliminación y persistencia del catálogo: OK");

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó.");
            System.err.println(e.getMessage());

        } finally {
            // El libro se elimina primero por su relación con la categoría.
            if (idLibroTemporal > 0) {
                try {
                    new LibroDAO().eliminar(idLibroTemporal);
                } catch (SQLException e) {
                    resultado = 1;
                    System.err.println("No se pudo limpiar el libro temporal ID "
                            + idLibroTemporal + ": " + e.getMessage());
                }
            }
            if (idCategoriaTemporal > 0) {
                try {
                    new CategoriaDAO().eliminar(idCategoriaTemporal);
                } catch (SQLException e) {
                    resultado = 1;
                    System.err.println("No se pudo limpiar la categoría temporal ID "
                            + idCategoriaTemporal + ": " + e.getMessage());
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
        System.out.println("PRUEBA COMPLETADA CORRECTAMENTE");
    }

    private static void comprobarPermisos(SesionUsuario sesion, Categoria categoria,
                                          Libro libro) throws SQLException {
        CategoriaServicio categorias = new CategoriaServicio(sesion);
        LibroServicio libros = new LibroServicio(sesion);
        comprobar(categorias.buscarPorId(categoria.getId()).getId() == categoria.getId()
                        && categorias.listar().stream().anyMatch(c -> c.getId() == categoria.getId())
                        && libros.buscarPorId(libro.getId()).getId() == libro.getId()
                        && libros.listar().stream().anyMatch(l -> l.getId() == libro.getId())
                        && libros.listarDisponibles().stream().anyMatch(l -> l.getId() == libro.getId()),
                "El estudiante no pudo consultar el catálogo.");

        OperacionPrueba[] operaciones = {
                () -> categorias.guardar("Categoría sin permiso"),
                () -> categorias.actualizar(categoria.getId(), "Cambio sin permiso"),
                () -> categorias.eliminar(categoria.getId()),
                () -> libros.guardar(libro),
                () -> libros.actualizar(libro, libro.getStock()),
                () -> libros.eliminar(libro.getId())
        };
        for (OperacionPrueba operacion : operaciones) {
            comprobarRechazo(operacion, IllegalStateException.class,
                    "Esta operación requiere una cuenta de bibliotecario.");
        }
    }

    private static void comprobarEdicionStock(LibroServicio servicio, int idLibro)
            throws SQLException {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        Libro formulario = servicio.buscarPorId(idLibro);
        int stockOriginal = formulario.getStock();
        Libro editado = new Libro(idLibro, "Libro EFT actualizado", formulario.getAutor(),
                formulario.getIsbn(), formulario.getEditorial(), stockOriginal + 3,
                formulario.getCategoria());

        // Simula un descuento ocurrido después de abrir el formulario.
        comprobar(baseDatos.ejecutarTransaccion(
                        conexion -> new LibroDAO().descontarStock(conexion, idLibro)),
                "No se pudo preparar el cambio de stock.");
        comprobarRechazo(() -> servicio.actualizar(editado, stockOriginal),
                IllegalStateException.class,
                "El stock del libro cambió mientras editaba. "
                        + "Actualice el listado y vuelva a intentarlo.");
        baseDatos.cerrar();
        Libro actual = servicio.buscarPorId(idLibro);
        comprobar(actual.getStock() == stockOriginal - 1
                        && formulario.getTitulo().equals(actual.getTitulo()),
                "La edición desactualizada sobrescribió el libro o su stock.");
        System.out.println("Rechazo de edición con stock desactualizado: OK");

        servicio.actualizar(editado, actual.getStock());
        baseDatos.cerrar();
        Libro actualizado = servicio.buscarPorId(idLibro);
        comprobar(editado.getTitulo().equals(actualizado.getTitulo())
                        && editado.getStock() == actualizado.getStock(),
                "La edición con el stock vigente no quedó guardada.");
        System.out.println("Edición con stock vigente y persistencia: OK");

        int stockVigente = actualizado.getStock();
        actualizado.setStock(0);
        servicio.actualizar(actualizado, stockVigente);
        baseDatos.cerrar();
        comprobar(servicio.buscarPorId(idLibro).getStock() == 0
                        && servicio.listarDisponibles().stream().noneMatch(l -> l.getId() == idLibro),
                "Un libro sin stock aparece como disponible.");
        System.out.println("Listado de libros disponibles: OK");
    }

    private static String generarIsbnDisponible(LibroServicio servicio) throws SQLException {
        List<Libro> existentes = servicio.listar();
        while (true) {
            String isbn = String.format(Locale.ROOT, "978%010d",
                    ThreadLocalRandom.current().nextLong(10_000_000_000L));
            if (existentes.stream().noneMatch(l -> isbn.equals(l.getIsbn()))) {
                return isbn;
            }
        }
    }

    private static void comprobarRechazo(OperacionPrueba operacion,
                                         Class<? extends RuntimeException> tipoEsperado,
                                         String mensajeEsperado) throws SQLException {
        try {
            operacion.ejecutar();
        } catch (RuntimeException e) {
            comprobar(tipoEsperado.isInstance(e) && mensajeEsperado.equals(e.getMessage()),
                    "El rechazo produjo una excepción inesperada: " + e.getMessage());
            return;
        }
        throw new IllegalStateException("Se permitió una operación que debía rechazarse.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }

    @FunctionalInterface
    private interface OperacionPrueba {
        void ejecutar() throws SQLException;
    }
}