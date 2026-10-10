package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.EstudianteDAO;
import cl.biblioteca.dao.LibroDAO;
import cl.biblioteca.dao.PrestamoDAO;
import cl.biblioteca.dao.UsuarioDAO;
import cl.biblioteca.modelo.Estudiante;
import cl.biblioteca.modelo.Libro;
import cl.biblioteca.modelo.Prestamo;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.modelo.Usuario;
import cl.biblioteca.modelo.UsuarioEstudiante;
import cl.biblioteca.servicio.AutenticacionServicio;
import cl.biblioteca.servicio.EstudianteServicio;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Comprueba la gestión coordinada de una ficha y su cuenta temporal.
 */
public final class PruebaEstudianteServicio {

    private static final String ERROR_PRUEBA = "PR001";

    private PruebaEstudianteServicio() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        EstudianteDAO estudianteDAO = new EstudianteDAO();
        UsuarioDAO usuarioDAO = new UsuarioDAO();
        PrestamoDAO prestamoDAO = new PrestamoDAO();
        int idEstudianteTemporal = 0;
        int idUsuarioTemporal = 0;
        int idPrestamoTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA DE SERVICIO DE ESTUDIANTES");

        try {
            AutenticacionServicio autenticacion = new AutenticacionServicio();
            SesionUsuario administrador = autenticacion.iniciarSesion("12345678-9", "clave123");
            SesionUsuario carlos = autenticacion.iniciarSesion("98765432-1", "clave123");
            EstudianteServicio servicio = new EstudianteServicio(administrador);
            int estudiantesIniciales = estudianteDAO.listar().size();
            int usuariosIniciales = usuarioDAO.listar().size();
            int prestamosIniciales = prestamoDAO.listar().size();

            String rutInicial = generarRutDisponible();
            String rutFormateado = rutInicial.substring(0, 2) + "."
                    + rutInicial.substring(2, 5) + "." + rutInicial.substring(5);
            Estudiante datos = new Estudiante("  Estudiante EFT 'prueba'  ",
                    rutFormateado, "  1° Medio  ", "  estudiante@eft.test  ");
            Estudiante guardado = servicio.guardar(datos, "claveEFT123");
            idEstudianteTemporal = guardado.getId();
            idUsuarioTemporal = leerCuenta(rutInicial).getId();
            baseDatos.cerrar();
            comprobar("Estudiante EFT 'prueba'".equals(guardado.getNombre())
                            && rutInicial.equals(guardado.getRut())
                            && "1° Medio".equals(guardado.getCurso())
                            && "estudiante@eft.test".equals(guardado.getCorreo())
                            && datos.getId() == 0 && datos.getNombre().startsWith("  "),
                    "La normalización alteró los datos originales o produjo datos incorrectos.");
            comprobarVinculo(guardado, "claveEFT123", idUsuarioTemporal);
            System.out.println("Registro, normalización y autenticación por RUT: OK");

            comprobarRechazo(() -> servicio.guardar(guardado, "otraClave"),
                    IllegalArgumentException.class, "Ya existe un estudiante con ese RUT.");
            Estudiante rutOcupado = new Estudiante("Prueba de duplicado", administrador.getRut(),
                    "1° Medio", "duplicado@eft.test");
            comprobarRechazo(() -> servicio.guardar(rutOcupado, "otraClave"),
                    IllegalArgumentException.class, "Ya existe una cuenta con ese RUT.");
            comprobar(estudianteDAO.listar().size() == estudiantesIniciales + 1
                            && usuarioDAO.listar().size() == usuariosIniciales + 1,
                    "El rechazo de duplicados dejó registros adicionales.");
            System.out.println("RUT duplicado en fichas y cuentas: OK");

            SesionUsuario alumno = autenticacion.iniciarSesion(rutInicial, "claveEFT123");
            comprobarPermisos(alumno, guardado, carlos.getIdEstudiante());
            System.out.println("Consulta de ficha propia y permisos administrativos: OK");

            String rutNuevo = generarRutDisponible();
            Estudiante editado = new Estudiante(guardado.getId(), "Estudiante EFT actualizado",
                    rutNuevo, "2° Medio", "actualizado@eft.test");
            servicio.actualizar(editado, "");
            baseDatos.cerrar();
            comprobarVinculo(editado, "claveEFT123", idUsuarioTemporal);
            comprobar(estudianteDAO.buscarPorRut(rutInicial).isEmpty()
                            && usuarioDAO.buscarPorRut(rutInicial).isEmpty(),
                    "El RUT anterior sigue asociado a la ficha o a la cuenta.");
            comprobarRechazo(() -> autenticacion.iniciarSesion(rutInicial, "claveEFT123"),
                    IllegalArgumentException.class, "RUT o contraseña incorrectos.");
            System.out.println("Actualización conjunta del RUT y conservación de contraseña: OK");

            servicio.actualizar(editado, "nuevaClaveEFT456");
            baseDatos.cerrar();
            comprobarVinculo(editado, "nuevaClaveEFT456", idUsuarioTemporal);
            comprobarRechazo(() -> autenticacion.iniciarSesion(rutNuevo, "claveEFT123"),
                    IllegalArgumentException.class, "RUT o contraseña incorrectos.");
            System.out.println("Cambio de contraseña y rechazo de la anterior: OK");

            comprobarRollback(editado, idUsuarioTemporal);
            baseDatos.cerrar();
            comprobarVinculo(editado, "nuevaClaveEFT456", idUsuarioTemporal);
            System.out.println("Rollback de cambios en ficha y cuenta: OK");

            // Solo se retira la cuenta temporal para simular una ficha antigua sin acceso.
            comprobar(usuarioDAO.eliminar(idUsuarioTemporal),
                    "No se pudo preparar la ficha sin cuenta.");
            idUsuarioTemporal = 0;
            servicio.actualizar(editado, "");
            comprobar(usuarioDAO.buscarPorRut(rutNuevo).isEmpty(),
                    "Se creó una cuenta sin indicar una contraseña.");
            servicio.actualizar(editado, "cuentaRecuperada789");
            idUsuarioTemporal = leerCuenta(rutNuevo).getId();
            baseDatos.cerrar();
            comprobarVinculo(editado, "cuentaRecuperada789", idUsuarioTemporal);
            System.out.println("Creación de cuenta para una ficha existente: OK");

            List<Libro> libros = new LibroDAO().listar();
            comprobar(!libros.isEmpty(),
                    "Se necesita un libro para comprobar la protección del historial.");
            LocalDate fecha = LocalDate.now();

            // Este registro DAO solo comprueba la restricción; no modifica el stock del libro.
            Prestamo prestamo = prestamoDAO.guardar(new Prestamo(
                    editado, libros.get(0), fecha, fecha.plusDays(7)
            ));
            idPrestamoTemporal = prestamo.getId();
            comprobarRechazo(() -> servicio.eliminar(editado.getId()),
                    IllegalStateException.class,
                    "No puede eliminar un estudiante que tiene préstamos asociados.");
            baseDatos.cerrar();
            comprobarVinculo(editado, "cuentaRecuperada789", idUsuarioTemporal);
            comprobar(prestamoDAO.buscarPorId(prestamo.getId()).isPresent(),
                    "El rechazo de la eliminación alteró el historial.");
            System.out.println("Protección del historial y conservación de la cuenta: OK");

            comprobar(prestamoDAO.eliminar(prestamo.getId()),
                    "No se pudo retirar el préstamo temporal.");
            idPrestamoTemporal = 0;
            servicio.eliminar(editado.getId());
            baseDatos.cerrar();
            comprobar(estudianteDAO.buscarPorId(editado.getId()).isEmpty()
                            && usuarioDAO.buscarPorId(idUsuarioTemporal).isEmpty(),
                    "La eliminación no retiró la ficha y su cuenta.");
            comprobarRechazo(() -> autenticacion.iniciarSesion(rutNuevo, "cuentaRecuperada789"),
                    IllegalArgumentException.class, "RUT o contraseña incorrectos.");
            comprobar(estudianteDAO.listar().size() == estudiantesIniciales
                            && usuarioDAO.listar().size() == usuariosIniciales
                            && prestamoDAO.listar().size() == prestamosIniciales,
                    "La cantidad final de registros no coincide con la inicial.");
            idEstudianteTemporal = 0;
            idUsuarioTemporal = 0;
            System.out.println("Eliminación conjunta y persistencia: OK");

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó.");
            System.err.println(e.getMessage());

        } finally {
            if (idEstudianteTemporal > 0 || idUsuarioTemporal > 0 || idPrestamoTemporal > 0) {
                try {
                    limpiarTemporales(idEstudianteTemporal, idUsuarioTemporal, idPrestamoTemporal);
                } catch (SQLException | IllegalStateException e) {
                    resultado = 1;
                    System.err.println("No se pudieron limpiar los registros temporales: "
                            + e.getMessage());
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

    private static void comprobarPermisos(SesionUsuario sesion, Estudiante estudiante,
                                          int idOtroEstudiante) throws SQLException {
        EstudianteServicio servicio = new EstudianteServicio(sesion);
        comprobar(servicio.buscarPorId(estudiante.getId()).getId() == estudiante.getId(),
                "El estudiante no pudo consultar su propia ficha.");

        OperacionPrueba[] operaciones = {
                () -> servicio.guardar(estudiante, "sinPermiso"),
                () -> servicio.actualizar(estudiante, ""),
                () -> servicio.eliminar(estudiante.getId()),
                servicio::listar
        };

        for (OperacionPrueba operacion : operaciones) {
            comprobarRechazo(operacion, IllegalStateException.class,
                    "Esta operación requiere una cuenta de bibliotecario.");
        }

        comprobarRechazo(() -> servicio.buscarPorId(idOtroEstudiante),
                IllegalStateException.class,
                "Solo puede acceder a sus propios préstamos.");
    }

    private static void comprobarVinculo(Estudiante esperado, String clave, int idUsuario)
            throws SQLException {
        Estudiante actual = new EstudianteDAO().buscarPorId(esperado.getId()).orElseThrow(
                () -> new IllegalStateException("La ficha temporal no existe.")
        );
        Usuario cuenta = leerCuenta(esperado.getRut());

        comprobar(esperado.getNombre().equals(actual.getNombre())
                        && esperado.getRut().equals(actual.getRut())
                        && esperado.getCurso().equals(actual.getCurso())
                        && esperado.getCorreo().equals(actual.getCorreo())
                        && cuenta.getId() == idUsuario
                        && esperado.getNombre().equals(cuenta.getNombre())
                        && esperado.getRut().equals(cuenta.getRut())
                        && esperado.getCorreo().equals(cuenta.getCorreo())
                        && clave.equals(cuenta.getContrasena())
                        && "estudiante".equals(cuenta.getRol())
                        && !cuenta.puedeAdministrar(),
                "Los datos de la ficha y su cuenta no coinciden con lo esperado.");

        SesionUsuario sesion = new AutenticacionServicio().iniciarSesion(esperado.getRut(), clave);
        comprobar(sesion.getIdUsuario() == idUsuario
                        && sesion.getIdEstudiante() == esperado.getId()
                        && !sesion.puedeAdministrar(),
                "La autenticación no vinculó los IDs correctos por RUT.");
    }

    private static Usuario leerCuenta(String rut) throws SQLException {
        return new UsuarioDAO().buscarPorRut(rut).orElseThrow(
                () -> new IllegalStateException("La cuenta temporal no existe.")
        );
    }

    private static void comprobarRollback(Estudiante estudiante, int idUsuario)
            throws SQLException {
        try {
            DatabaseConnection.getInstance().ejecutarTransaccion(conexion -> {
                Estudiante cambio = new Estudiante(estudiante.getId(), "Cambio sin confirmar",
                        estudiante.getRut(), "Curso temporal", "rollback@eft.test");
                comprobar(new EstudianteDAO().actualizar(conexion, cambio),
                        "No se pudo preparar el cambio temporal de la ficha.");

                Usuario cuenta = new UsuarioEstudiante(
                        idUsuario, cambio.getNombre(), cambio.getRut(),
                        cambio.getCorreo(), "claveSinConfirmar"
                );
                comprobar(new UsuarioDAO().actualizar(conexion, cuenta),
                        "No se pudo preparar el cambio temporal de la cuenta.");

                throw new SQLException(
                        "Fallo intencional después de ambas actualizaciones.", ERROR_PRUEBA
                );
            });

        } catch (SQLException e) {
            if (!ERROR_PRUEBA.equals(e.getSQLState())) {
                throw e;
            }
            return;
        }

        throw new IllegalStateException("La transacción de prueba debía fallar.");
    }

    private static void limpiarTemporales(int idEstudiante, int idUsuario, int idPrestamo)
            throws SQLException {
        DatabaseConnection.getInstance().ejecutarTransaccion(conexion -> {
            if (idPrestamo > 0) {
                new PrestamoDAO().eliminar(conexion, idPrestamo);
            }

            int idCuenta = idUsuario;
            if (idEstudiante > 0) {
                Estudiante ficha = new EstudianteDAO().buscarPorId(idEstudiante).orElse(null);
                if (ficha != null) {
                    Usuario cuenta = new UsuarioDAO().buscarPorRut(ficha.getRut()).orElse(null);
                    if (cuenta != null) {
                        comprobar(!cuenta.puedeAdministrar(),
                                "La limpieza no puede eliminar una cuenta de bibliotecario.");
                        idCuenta = cuenta.getId();
                    }
                }
                new EstudianteDAO().eliminar(conexion, idEstudiante);
            }

            if (idCuenta > 0) {
                new UsuarioDAO().eliminar(conexion, idCuenta);
            }
            return null;
        });
    }

    private static String generarRutDisponible() throws SQLException {
        for (int intento = 0; intento < 100; intento++) {
            int numero = ThreadLocalRandom.current().nextInt(50_000_000, 100_000_000);
            int restante = numero;
            int suma = 0;
            int factor = 2;

            while (restante > 0) {
                suma += (restante % 10) * factor;
                restante /= 10;
                factor = factor == 7 ? 2 : factor + 1;
            }

            int digito = 11 - suma % 11;
            String verificador = switch (digito) {
                case 11 -> "0";
                case 10 -> "K";
                default -> Integer.toString(digito);
            };

            String rut = numero + "-" + verificador;
            if (new EstudianteDAO().buscarPorRut(rut).isEmpty()
                    && new UsuarioDAO().buscarPorRut(rut).isEmpty()) {
                return rut;
            }
        }

        throw new IllegalStateException("No se pudo generar un RUT de prueba disponible.");
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