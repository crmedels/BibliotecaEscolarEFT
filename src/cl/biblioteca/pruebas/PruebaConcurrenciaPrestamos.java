package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.CategoriaDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.dao.PrestamoDAO;
import cl.biblioteca.modelo.Categoria;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.AutenticacionServicio;
import cl.biblioteca.servicio.LibroServicio;
import cl.biblioteca.servicio.PrestamoServicio;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Comprueba el stock frente a préstamos y devoluciones simultáneos.
 */
public final class PruebaConcurrenciaPrestamos {

    private static final int STOCK_INICIAL = 3;
    private static final int INTENTOS = 8;

    private PruebaConcurrenciaPrestamos() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        ExecutorService ejecutor = Executors.newFixedThreadPool(INTENTOS);
        int idLibroTemporal = 0;
        int librosIniciales = -1;
        int prestamosIniciales = -1;
        int resultado = 0;

        System.out.println("PRUEBA DE CONCURRENCIA DE PRÉSTAMOS");

        try {
            AutenticacionServicio autenticacion = new AutenticacionServicio();
            SesionUsuario bibliotecaria = autenticacion.iniciarSesion("12345678-9", "clave123");
            SesionUsuario carlos = autenticacion.iniciarSesion("98765432-1", "clave123");
            SesionUsuario maria = autenticacion.iniciarSesion("11222333-4", "clave123");

            LibroServicio libros = new LibroServicio(bibliotecaria);
            PrestamoServicio administracion = new PrestamoServicio(bibliotecaria);
            librosIniciales = libros.listar().size();
            prestamosIniciales = new PrestamoDAO().listar().size();

            List<Categoria> categorias = new CategoriaDAO().listar();
            comprobar(!categorias.isEmpty(), "Se necesita una categoría para la prueba.");

            Libro libro = libros.guardar(new Libro(
                    "Libro EFT de concurrencia", "Autor EFT",
                    generarIsbnDisponible(libros), "Editorial EFT",
                    STOCK_INICIAL, categorias.get(0)
            ));
            idLibroTemporal = libro.getId();

            List<Callable<Optional<Prestamo>>> solicitudes = new ArrayList<>();

            for (int i = 0; i < INTENTOS; i++) {
                SesionUsuario sesion = i % 2 == 0 ? carlos : maria;
                solicitudes.add(() -> intentarPrestamo(sesion, libro.getId()));
            }

            List<Optional<Prestamo>> respuestas = ejecutarEnParalelo(ejecutor, solicitudes);
            List<Prestamo> registrados = respuestas.stream()
                    .flatMap(Optional::stream)
                    .toList();
            int rechazados = INTENTOS - registrados.size();

            comprobar(registrados.size() == STOCK_INICIAL
                            && rechazados == INTENTOS - STOCK_INICIAL
                            && registrados.stream()
                            .map(Prestamo::getId).distinct().count() == STOCK_INICIAL,
                    "La cantidad de préstamos o rechazos no coincide con el stock disponible.");

            System.out.println("Préstamos registrados: " + registrados.size());
            System.out.println("Solicitudes rechazadas por falta de stock: " + rechazados);

            baseDatos.cerrar();
            List<Prestamo> persistidos = prestamosDelLibro(libro.getId());

            comprobar(libros.buscarPorId(libro.getId()).getStock() == 0
                            && persistidos.size() == STOCK_INICIAL
                            && persistidos.stream().allMatch(p -> registrados.stream()
                            .anyMatch(r -> r.getId() == p.getId()))
                            && persistidos.stream().allMatch(p -> !p.isDevuelto()
                            && p.getFechaDevolucionReal() == null
                            && p.getFechaDevolucion().equals(
                            p.getFechaPrestamo()
                                    .plusDays(PrestamoServicio.DIAS_PRESTAMO))
                            && (p.getEstudiante().getId() == carlos.getIdEstudiante()
                            || p.getEstudiante().getId() == maria.getIdEstudiante()))
                            && new PrestamoDAO().listar().size()
                            == prestamosIniciales + STOCK_INICIAL,
                    "Los préstamos simultáneos no conservaron el stock o sus datos persistidos.");

            System.out.println(
                    "Stock agotado y préstamos persistidos sin exceder disponibilidad: OK"
            );

            Prestamo objetivo = registrados.get(0);
            List<Callable<Boolean>> devoluciones = new ArrayList<>();

            for (int i = 0; i < INTENTOS; i++) {
                devoluciones.add(
                        () -> intentarDevolucion(bibliotecaria, objetivo.getId())
                );
            }

            long aceptadas = ejecutarEnParalelo(ejecutor, devoluciones).stream()
                    .filter(Boolean::booleanValue)
                    .count();

            comprobar(aceptadas == 1,
                    "Más de una devolución fue aceptada para el mismo préstamo.");

            baseDatos.cerrar();
            Prestamo devuelto = administracion.buscarPorId(objetivo.getId());

            comprobar(devuelto.isDevuelto()
                            && devuelto.getFechaDevolucionReal() != null
                            && objetivo.getFechaDevolucion().equals(devuelto.getFechaDevolucion())
                            && libros.buscarPorId(libro.getId()).getStock() == 1
                            && prestamosDelLibro(libro.getId()).stream()
                            .filter(p -> !p.isDevuelto()).count() == STOCK_INICIAL - 1,
                    "Las devoluciones simultáneas repusieron más de un ejemplar o alteraron el historial.");

            System.out.println("Devoluciones del mismo préstamo: 1 aceptada y "
                    + (INTENTOS - 1) + " rechazadas: OK");

            List<Callable<Prestamo>> restantes = new ArrayList<>();

            for (Prestamo prestamo : registrados) {
                if (prestamo.getId() != objetivo.getId()) {
                    restantes.add(
                            () -> new PrestamoServicio(bibliotecaria)
                                    .devolver(prestamo.getId())
                    );
                }
            }

            ejecutarEnParalelo(ejecutor, restantes);
            baseDatos.cerrar();

            comprobar(libros.buscarPorId(libro.getId()).getStock() == STOCK_INICIAL
                            && prestamosDelLibro(libro.getId()).size() == STOCK_INICIAL
                            && prestamosDelLibro(libro.getId()).stream()
                            .allMatch(p -> p.isDevuelto()
                                    && p.getFechaDevolucionReal() != null),
                    "Las devoluciones restantes no recuperaron el stock inicial.");

            System.out.println("Devoluciones de préstamos distintos y stock recuperado: OK");

        } catch (InterruptedException e) {
            resultado = 1;
            Thread.currentThread().interrupt();
            System.err.println("La prueba fue interrumpida.");

        } catch (ExecutionException e) {
            resultado = 1;
            System.err.println("Un hilo produjo un error: " + e.getCause());

        } catch (TimeoutException e) {
            resultado = 1;
            System.err.println("Los hilos excedieron el tiempo disponible para la prueba.");

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó: " + e.getMessage());

        } finally {
            // Conserva la interrupción y permite esperar el cierre antes de limpiar.
            boolean interrumpido = Thread.interrupted();
            ejecutor.shutdownNow();
            boolean terminado = ejecutor.isTerminated();

            try {
                terminado = ejecutor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                resultado = 1;
                interrumpido = true;
                terminado = ejecutor.isTerminated();
                System.err.println("Se interrumpió la espera del cierre de los hilos.");
            }

            if (terminado) {
                try {
                    if (idLibroTemporal > 0) {
                        limpiarLibroTemporal(idLibroTemporal);
                        baseDatos.cerrar();

                        comprobar(new LibroDAO().listar().size() == librosIniciales
                                        && new PrestamoDAO().listar().size() == prestamosIniciales,
                                "La limpieza no recuperó la cantidad inicial de registros.");

                        System.out.println(
                                "Cierre de hilos y limpieza de registros temporales: OK"
                        );
                    }

                } catch (SQLException | IllegalStateException e) {
                    resultado = 1;
                    System.err.println("No se pudo completar la limpieza del libro temporal ID "
                            + idLibroTemporal + ": " + e.getMessage());

                } finally {
                    try {
                        baseDatos.cerrar();
                    } catch (SQLException e) {
                        resultado = 1;
                        System.err.println("No se pudo cerrar la conexión: " + e.getMessage());
                    }
                }

            } else {
                resultado = 1;
                System.err.println(
                        "No terminaron todos los hilos. Quedó pendiente limpiar el libro temporal ID "
                                + idLibroTemporal + "."
                );
            }

            if (interrumpido) {
                Thread.currentThread().interrupt();
            }
        }

        if (resultado != 0) {
            System.exit(resultado);
        }

        System.out.println("PRUEBA COMPLETADA CORRECTAMENTE");
    }

    private static Optional<Prestamo> intentarPrestamo(SesionUsuario sesion, int idLibro)
            throws SQLException {
        try {
            return Optional.of(
                    new PrestamoServicio(sesion)
                            .registrar(sesion.getIdEstudiante(), idLibro)
            );

        } catch (IllegalStateException e) {
            if (!"El libro seleccionado no tiene stock disponible.".equals(e.getMessage())) {
                throw e;
            }
            return Optional.empty();
        }
    }

    private static boolean intentarDevolucion(SesionUsuario sesion, int idPrestamo)
            throws SQLException {
        try {
            new PrestamoServicio(sesion).devolver(idPrestamo);
            return true;

        } catch (IllegalStateException e) {
            if (!"El préstamo ya fue devuelto.".equals(e.getMessage())) {
                throw e;
            }
            return false;
        }
    }

    private static <T> List<T> ejecutarEnParalelo(
            ExecutorService ejecutor, List<Callable<T>> tareas
    ) throws InterruptedException, ExecutionException, TimeoutException {

        CountDownLatch preparados = new CountDownLatch(tareas.size());
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<T>> futuros = new ArrayList<>();

        try {
            for (Callable<T> tarea : tareas) {
                futuros.add(ejecutor.submit(() -> {
                    preparados.countDown();
                    inicio.await();
                    return tarea.call();
                }));
            }

            comprobar(preparados.await(10, TimeUnit.SECONDS),
                    "No se prepararon todos los hilos.");

            inicio.countDown();
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(45);
            List<T> resultados = new ArrayList<>();

            for (Future<T> futuro : futuros) {
                long restante = limite - System.nanoTime();

                if (restante <= 0) {
                    throw new TimeoutException("Se agotó el tiempo de la prueba.");
                }

                resultados.add(futuro.get(restante, TimeUnit.NANOSECONDS));
            }

            return resultados;

        } finally {
            inicio.countDown();

            for (Future<T> futuro : futuros) {
                if (!futuro.isDone()) {
                    futuro.cancel(true);
                }
            }
        }
    }

    private static List<Prestamo> prestamosDelLibro(int idLibro) throws SQLException {
        return new PrestamoDAO().listar().stream()
                .filter(p -> p.getLibro() != null
                        && p.getLibro().getId() == idLibro)
                .toList();
    }

    private static void limpiarLibroTemporal(int idLibro) throws SQLException {
        DatabaseConnection.getInstance().ejecutarTransaccion(conexion -> {
            PrestamoDAO dao = new PrestamoDAO();

            for (Prestamo prestamo : prestamosDelLibro(idLibro)) {
                comprobar(dao.eliminar(conexion, prestamo.getId()),
                        "No se pudo eliminar un préstamo del libro temporal.");
            }

            new LibroDAO().eliminar(idLibro);
            return null;
        });
    }

    private static String generarIsbnDisponible(LibroServicio servicio)
            throws SQLException {
        List<Libro> existentes = servicio.listar();

        while (true) {
            String isbn = String.format(
                    Locale.ROOT, "978%010d",
                    ThreadLocalRandom.current().nextLong(10_000_000_000L)
            );

            if (existentes.stream().noneMatch(l -> isbn.equals(l.getIsbn()))) {
                return isbn;
            }
        }
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }
}