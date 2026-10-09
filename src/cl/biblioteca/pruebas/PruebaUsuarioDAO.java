package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.UsuarioDAO;
import cl.biblioteca.modelo.Bibliotecario;
import cl.biblioteca.modelo.Usuario;
import cl.biblioteca.modelo.UsuarioEstudiante;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Comprueba el CRUD, la autenticación y los roles con una cuenta temporal.
 */
public final class PruebaUsuarioDAO {

    private static final String ERROR_PRUEBA = "PR001";

    private PruebaUsuarioDAO() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        UsuarioDAO dao = new UsuarioDAO();
        int idTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA CRUD Y AUTENTICACIÓN DE USUARIOS");

        try {
            int cantidadInicial = dao.listar().size();
            System.out.println("Usuarios iniciales: " + cantidadInicial);

            String rutInicial = crearRutDisponible(dao);
            Usuario inicial = new UsuarioEstudiante(
                    "Prueba EFT 'usuario'", rutInicial,
                    "usuario@eft.test", "ClaveEFT123"
            );

            Usuario guardado = dao.guardar(inicial);
            int idCreado = guardado.getId();
            idTemporal = idCreado;
            comprobar(idCreado > 0, "MySQL no generó un ID válido.");

            baseDatos.cerrar();
            Usuario encontrado = leerUsuario(dao, idCreado);
            comprobarDatos(encontrado, inicial);
            comprobar(encontrado instanceof UsuarioEstudiante,
                    "No se recuperó el tipo UsuarioEstudiante.");
            comprobar(!encontrado.puedeAdministrar(),
                    "Un estudiante no debe tener permisos de administración.");
            System.out.println("Guardar, persistencia y rol estudiante: OK");

            Usuario porRut = dao.buscarPorRut(rutInicial).orElseThrow(
                    () -> new IllegalStateException("No se encontró el RUT guardado.")
            );
            comprobar(porRut.getId() == idCreado,
                    "La búsqueda por RUT devolvió otra cuenta.");
            comprobar(dao.listar().stream().anyMatch(u -> u.getId() == idCreado),
                    "El usuario no aparece en el listado.");
            System.out.println("Buscar por RUT y listar: OK");

            Usuario autenticado = dao.autenticar(rutInicial, inicial.getContrasena())
                    .orElseThrow(() -> new IllegalStateException(
                            "Las credenciales correctas fueron rechazadas."
                    ));
            comprobar(autenticado.getId() == idCreado,
                    "Se autenticó otra cuenta.");
            comprobar(!autenticado.puedeAdministrar(),
                    "La cuenta autenticada tiene permisos incorrectos.");
            comprobar(dao.autenticar(rutInicial, "incorrecta").isEmpty(),
                    "Se aceptó una contraseña incorrecta.");
            comprobar(dao.autenticar(rutInicial, "claveeft123").isEmpty(),
                    "La contraseña debe distinguir mayúsculas y minúsculas.");
            comprobar(dao.autenticar("' OR '1'='1", inicial.getContrasena()).isEmpty(),
                    "Se aceptó un RUT que no corresponde a la cuenta.");
            System.out.println("Autenticación correcta e intentos inválidos: OK");

            String rutActualizado = crearRutDisponible(dao);
            Usuario nuevosDatos = new Bibliotecario(
                    idCreado, "Prueba EFT actualizada", rutActualizado,
                    "actualizado@eft.test", "NuevaClave456"
            );
            comprobar(dao.actualizar(nuevosDatos),
                    "No se pudo actualizar el usuario.");

            baseDatos.cerrar();
            Usuario actualizado = leerUsuario(dao, idCreado);
            comprobarDatos(actualizado, nuevosDatos);
            comprobar(actualizado instanceof Bibliotecario,
                    "No se recuperó el tipo Bibliotecario.");
            comprobar(actualizado.puedeAdministrar(),
                    "El bibliotecario debe tener permisos de administración.");
            comprobar(dao.buscarPorRut(rutInicial).isEmpty(),
                    "El RUT anterior sigue asociado a la cuenta.");

            Usuario nuevoAcceso = dao.autenticar(rutActualizado, nuevosDatos.getContrasena())
                    .orElseThrow(() -> new IllegalStateException(
                            "La contraseña actualizada no permite acceder."
                    ));
            comprobar(nuevoAcceso.getId() == idCreado && nuevoAcceso.puedeAdministrar(),
                    "El nuevo acceso no recuperó la cuenta con su rol actualizado.");
            comprobar(dao.autenticar(rutActualizado, inicial.getContrasena()).isEmpty(),
                    "La contraseña anterior sigue permitiendo acceder.");
            System.out.println("Actualizar datos, contraseña y rol bibliotecario: OK");

            Usuario sinConfirmar = new UsuarioEstudiante(
                    idCreado, "Cambio sin confirmar", rutActualizado,
                    "rollback@eft.test", "NoConfirmada321"
            );

            try {
                baseDatos.ejecutarTransaccion(conexion -> {
                    comprobar(dao.actualizar(conexion, sinConfirmar),
                            "No se pudo preparar la actualización temporal.");
                    throw new SQLException("Fallo intencional de prueba.", ERROR_PRUEBA);
                });
            } catch (SQLException e) {
                if (!ERROR_PRUEBA.equals(e.getSQLState())) {
                    throw e;
                }
            }

            baseDatos.cerrar();
            comprobarDatos(leerUsuario(dao, idCreado), nuevosDatos);
            System.out.println("Rollback de la actualización: OK");

            comprobar(dao.eliminar(idCreado),
                    "No se pudo eliminar la cuenta temporal.");
            baseDatos.cerrar();
            comprobar(dao.buscarPorId(idCreado).isEmpty(),
                    "El usuario sigue existiendo después de eliminarlo.");
            comprobar(dao.buscarPorRut(rutActualizado).isEmpty(),
                    "La búsqueda por RUT encontró al usuario eliminado.");
            comprobar(dao.autenticar(rutActualizado, nuevosDatos.getContrasena()).isEmpty(),
                    "La cuenta eliminada sigue permitiendo acceder.");
            idTemporal = 0;
            System.out.println("Eliminar, persistencia y rechazo del acceso: OK");

            comprobar(!dao.actualizar(nuevosDatos),
                    "Actualizar un ID inexistente debe devolver false.");
            comprobar(!dao.eliminar(idCreado),
                    "Eliminar un ID inexistente debe devolver false.");
            System.out.println("Operaciones sobre ID inexistente: OK");

            comprobar(dao.listar().size() == cantidadInicial,
                    "La cantidad final de usuarios cambió.");

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
                    System.err.println("No se pudo limpiar el usuario temporal ID "
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

        System.out.println("PRUEBA COMPLETADA CORRECTAMENTE");
    }

    private static Usuario leerUsuario(UsuarioDAO dao, int id) throws SQLException {
        return dao.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException("El usuario guardado no existe.")
        );
    }

    private static void comprobarDatos(Usuario actual, Usuario esperado) {
        comprobar(esperado.getNombre().equals(actual.getNombre()),
                "El nombre guardado no coincide.");
        comprobar(esperado.getRut().equals(actual.getRut()),
                "El RUT guardado no coincide.");
        comprobar(esperado.getCorreo().equals(actual.getCorreo()),
                "El correo guardado no coincide.");
        comprobar(esperado.getContrasena().equals(actual.getContrasena()),
                "La contraseña guardada no coincide.");
        comprobar(esperado.getRol().equals(actual.getRol()),
                "El rol guardado no coincide.");
    }

    private static String crearRutDisponible(UsuarioDAO dao) throws SQLException {
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
            if (dao.buscarPorRut(rut).isEmpty()) {
                return rut;
            }
        }

        throw new IllegalStateException("No se pudo generar un RUT de prueba disponible.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }
}