package cl.biblioteca.main;

import cl.biblioteca.config.DatabaseConnection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Verifica inicialmente la conexión y las tablas de la biblioteca.
 * Posteriormente iniciará la interfaz gráfica.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        DatabaseConnection baseDatos = DatabaseConnection.getInstance();
        int resultado = 0;

        System.out.println("BIBLIOTECA ESCOLAR - VERIFICACIÓN INICIAL");

        try {
            String resumen = baseDatos.ejecutar(conexion -> {
                StringBuilder texto = new StringBuilder();

                try (PreparedStatement consulta = conexion.prepareStatement(
                        "SELECT DATABASE()"
                ); ResultSet registros = consulta.executeQuery()) {

                    if (!registros.next()
                            || !"biblioteca".equals(registros.getString(1))) {

                        throw new SQLException(
                                "La conexión debe seleccionar la base biblioteca."
                        );
                    }

                    texto.append("Conexión correcta a la base biblioteca.\n");
                }

                // Los nombres provienen de esta lista fija, no del usuario.
                String[] tablas = {
                        "usuarios",
                        "estudiantes",
                        "categorias",
                        "libros",
                        "prestamos"
                };

                for (String tabla : tablas) {
                    try (PreparedStatement consulta = conexion.prepareStatement(
                            "SELECT COUNT(*) FROM " + tabla
                    ); ResultSet registros = consulta.executeQuery()) {

                        registros.next();

                        texto.append(tabla)
                                .append(": ")
                                .append(registros.getInt(1))
                                .append(" registros\n");
                    }
                }

                return texto.toString();
            });

            System.out.print(resumen);

        } catch (SQLException e) {
            resultado = 1;
            System.err.println("No fue posible verificar la base de datos.");
            System.err.println(e.getMessage());

        } finally {
            try {
                baseDatos.cerrar();
            } catch (SQLException e) {
                resultado = 1;
                System.err.println("No fue posible cerrar la conexión de MySQL.");
            }
        }

        if (resultado != 0) {
            System.exit(resultado);
        }
    }
}