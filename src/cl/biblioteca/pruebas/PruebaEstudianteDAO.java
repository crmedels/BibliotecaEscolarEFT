package cl.biblioteca.pruebas;

import cl.biblioteca.config.DatabaseConnection;
import cl.biblioteca.dao.EstudianteDAO;
import cl.biblioteca.modelo.Estudiante;

import java.sql.SQLException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Comprueba el CRUD de estudiantes con un registro temporal.
 */
public final class PruebaEstudianteDAO {

    private static final String ERROR_PRUEBA = "PR001";

    private PruebaEstudianteDAO() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        EstudianteDAO dao = new EstudianteDAO();
        int idTemporal = 0;
        int resultado = 0;

        System.out.println("PRUEBA CRUD DE ESTUDIANTES");

        try {
            int cantidadInicial = dao.listar().size();
            System.out.println("Estudiantes iniciales: " + cantidadInicial);

            String rutInicial = crearRutDisponible(dao);
            Estudiante inicial = new Estudiante(
                    "Prueba EFT 'estudiante'", rutInicial,
                    "1° Medio", "estudiante@eft.test"
            );

            Estudiante guardado = dao.guardar(inicial);
            int idCreado = guardado.getId();
            idTemporal = idCreado;

            comprobar(idCreado > 0, "MySQL no generó un ID válido.");
            System.out.println("Guardar: OK");

            baseDatos.cerrar();
            comprobarDatos(leerEstudiante(dao, idCreado), inicial);
            System.out.println("Buscar y comprobar persistencia: OK");

            Estudiante encontradoPorRut = dao.buscarPorRut(rutInicial).orElseThrow(
                    () -> new IllegalStateException("No se encontró el RUT guardado.")
            );
            comprobar(encontradoPorRut.getId() == idCreado,
                    "La búsqueda por RUT devolvió otro estudiante.");
            System.out.println("Buscar por RUT: OK");

            comprobar(dao.listar().stream().anyMatch(e -> e.getId() == idCreado),
                    "El estudiante no aparece en el listado.");
            System.out.println("Listar: OK");

            String rutActualizado = crearRutDisponible(dao);
            Estudiante nuevosDatos = new Estudiante(
                    idCreado, "Prueba EFT actualizada", rutActualizado,
                    "2° Medio", "actualizado@eft.test"
            );

            comprobar(dao.actualizar(nuevosDatos),
                    "No se pudo actualizar el estudiante.");

            baseDatos.cerrar();
            comprobarDatos(leerEstudiante(dao, idCreado), nuevosDatos);
            comprobar(dao.buscarPorRut(rutInicial).isEmpty(),
                    "El RUT anterior sigue asociado al estudiante.");

            Estudiante actualizadoPorRut = dao.buscarPorRut(rutActualizado)
                    .orElseThrow(() -> new IllegalStateException(
                            "No se encontró el RUT actualizado."
                    ));
            comprobar(actualizadoPorRut.getId() == idCreado,
                    "El RUT actualizado no corresponde al estudiante.");
            System.out.println("Actualizar datos y RUT con persistencia: OK");

            Estudiante sinConfirmar = new Estudiante(
                    idCreado, "Cambio sin confirmar", rutActualizado,
                    "Curso temporal", "rollback@eft.test"
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
            comprobarDatos(leerEstudiante(dao, idCreado), nuevosDatos);
            System.out.println("Rollback de la actualización: OK");

            comprobar(dao.eliminar(idCreado),
                    "No se pudo eliminar el estudiante temporal.");

            baseDatos.cerrar();
            comprobar(dao.buscarPorId(idCreado).isEmpty(),
                    "El estudiante sigue existiendo después de eliminarlo.");
            comprobar(dao.buscarPorRut(rutActualizado).isEmpty(),
                    "La búsqueda por RUT encontró al estudiante eliminado.");
            idTemporal = 0;
            System.out.println("Eliminar y comprobar persistencia: OK");

            comprobar(!dao.actualizar(nuevosDatos),
                    "Actualizar un ID inexistente debe devolver false.");
            comprobar(!dao.eliminar(idCreado),
                    "Eliminar un ID inexistente debe devolver false.");
            System.out.println("Operaciones sobre ID inexistente: OK");

            comprobar(dao.listar().size() == cantidadInicial,
                    "La cantidad final de estudiantes cambió.");

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
                    System.err.println("No se pudo limpiar el estudiante temporal ID "
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

    private static Estudiante leerEstudiante(EstudianteDAO dao, int id)
            throws SQLException {

        return dao.buscarPorId(id).orElseThrow(
                () -> new IllegalStateException("El estudiante guardado no existe.")
        );
    }

    private static void comprobarDatos(Estudiante actual, Estudiante esperado) {
        comprobar(esperado.getNombre().equals(actual.getNombre()),
                "El nombre guardado no coincide.");
        comprobar(esperado.getRut().equals(actual.getRut()),
                "El RUT guardado no coincide.");
        comprobar(esperado.getCurso().equals(actual.getCurso()),
                "El curso guardado no coincide.");
        comprobar(esperado.getCorreo().equals(actual.getCorreo()),
                "El correo guardado no coincide.");
    }

    private static String crearRutDisponible(EstudianteDAO dao) throws SQLException {
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