package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.modelo.SesionUsuario;
import cl.biblioteca.servicio.AutenticacionServicio;

import java.sql.SQLException;

/**
 * Comprueba el acceso y los permisos con las cuentas del script original.
 */
public final class PruebaAutenticacionServicio {

    private PruebaAutenticacionServicio() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        AutenticacionServicio servicio = new AutenticacionServicio();
        int resultado = 0;

        System.out.println("PRUEBA DE AUTENTICACIÓN Y PERMISOS");

        try {
            SesionUsuario bibliotecaria = servicio.iniciarSesion("12.345.678-9", "clave123");
            comprobar("Antonia Pérez".equals(bibliotecaria.getNombre())
                            && "12345678-9".equals(bibliotecaria.getRut())
                            && "bibliotecario".equals(bibliotecaria.getRol())
                            && bibliotecaria.puedeAdministrar()
                            && !bibliotecaria.tieneFichaEstudiante(),
                    "La sesión de la bibliotecaria no es correcta.");
            bibliotecaria.exigirAdministracion();
            System.out.println("Acceso de bibliotecario y normalización del RUT: OK");

            baseDatos.cerrar();
            SesionUsuario carlos = servicio.iniciarSesion("98.765.432-1", "clave123");
            comprobar("Carlos Ruiz".equals(carlos.getNombre())
                            && "98765432-1".equals(carlos.getRut())
                            && "estudiante".equals(carlos.getRol())
                            && !carlos.puedeAdministrar()
                            && carlos.tieneFichaEstudiante(),
                    "La sesión de Carlos no es correcta.");

            // Estos son los IDs de Carlos en los scripts originales.
            comprobar(carlos.getIdUsuario() == 2 && carlos.getIdEstudiante() == 1,
                    "La cuenta y la ficha de Carlos no se vincularon correctamente por RUT.");
            carlos.exigirAccesoEstudiante(carlos.getIdEstudiante());
            System.out.println("Acceso de estudiante y vínculo por RUT: OK");

            SesionUsuario maria = servicio.iniciarSesion("11222333-4", "clave123");
            comprobar("María Torres".equals(maria.getNombre())
                            && maria.tieneFichaEstudiante()
                            && !maria.puedeAdministrar()
                            && maria.getIdEstudiante() != carlos.getIdEstudiante(),
                    "La sesión de María no es correcta.");

            comprobarPermisoRechazado(
                    carlos::exigirAdministracion,
                    "Esta operación requiere una cuenta de bibliotecario."
            );
            comprobarPermisoRechazado(
                    () -> carlos.exigirAccesoEstudiante(maria.getIdEstudiante()),
                    "Solo puede acceder a sus propios préstamos."
            );
            maria.exigirAccesoEstudiante(maria.getIdEstudiante());
            bibliotecaria.exigirAccesoEstudiante(carlos.getIdEstudiante());
            bibliotecaria.exigirAccesoEstudiante(maria.getIdEstudiante());
            System.out.println("Permisos administrativos y acceso a fichas propias: OK");

            comprobarAccesoRechazado(servicio, "98765432-1", "incorrecta",
                    "RUT o contraseña incorrectos.");
            comprobarAccesoRechazado(servicio, "98765432-1", "CLAVE123",
                    "RUT o contraseña incorrectos.");
            comprobarAccesoRechazado(servicio, "98765432-1", " clave123 ",
                    "RUT o contraseña incorrectos.");

            // Luis tiene una ficha de estudiante, pero no una cuenta en usuarios.
            comprobarAccesoRechazado(servicio, "19283746-5", "clave123",
                    "RUT o contraseña incorrectos.");
            System.out.println("Credenciales incorrectas y ficha sin cuenta: OK");

            comprobarAccesoRechazado(servicio, "", "clave123",
                    "El campo \"RUT\" es obligatorio.");
            comprobarAccesoRechazado(servicio, "98765432-1", "   ",
                    "El campo \"Contraseña\" es obligatorio.");
            comprobarAccesoRechazado(servicio, "incorrecto", "clave123",
                    "El RUT debe tener el formato 12345678-9.");
            System.out.println("Campos obligatorios y formato del RUT: OK");

        } catch (SQLException | IllegalArgumentException | IllegalStateException e) {
            resultado = 1;
            System.err.println("La prueba no se completó.");
            System.err.println(e.getMessage());

        } finally {
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

    private static void comprobarAccesoRechazado(AutenticacionServicio servicio,
                                                 String rut, String contrasena,
                                                 String mensajeEsperado) throws SQLException {
        try {
            servicio.iniciarSesion(rut, contrasena);
        } catch (IllegalArgumentException e) {
            comprobar(mensajeEsperado.equals(e.getMessage()),
                    "El rechazo del acceso produjo un mensaje inesperado: " + e.getMessage());
            return;
        }

        throw new IllegalStateException("Se aceptaron credenciales que debían rechazarse.");
    }

    private static void comprobarPermisoRechazado(Runnable operacion, String mensajeEsperado) {
        try {
            operacion.run();
        } catch (IllegalStateException e) {
            comprobar(mensajeEsperado.equals(e.getMessage()),
                    "El rechazo del permiso produjo un mensaje inesperado: " + e.getMessage());
            return;
        }

        throw new IllegalStateException("Se permitió una operación sin autorización.");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new IllegalStateException(mensaje);
        }
    }
}