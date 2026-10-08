package cl.biblioteca.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Lee las credenciales locales sin incluirlas en el código fuente.
 */
final class ConfiguracionBD {

    private final String url;
    private final String usuario;
    private final String contrasena;

    private ConfiguracionBD(String url, String usuario, String contrasena) {
        this.url = url;
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    static ConfiguracionBD cargar() throws SQLException {
        Path archivo = Path.of(System.getProperty(
                "biblioteca.config", "config/basedatos.properties"
        ));

        Properties propiedades = new Properties();

        try (Reader lector = Files.newBufferedReader(
                archivo, StandardCharsets.UTF_8
        )) {
            propiedades.load(lector);

        } catch (IOException e) {
            throw new SQLException(
                    "No se pudo leer " + archivo + ". Copie el archivo "
                            + "basedatos.properties.example y complete los datos de MySQL.",
                    "CONFIG", e
            );
        }

        String url = propiedades.getProperty("db.url", "").trim();
        String usuario = propiedades.getProperty("db.usuario", "").trim();
        String contrasena = propiedades.getProperty("db.contrasena");

        if (!url.startsWith("jdbc:mysql://") || usuario.isEmpty()
                || contrasena == null) {
            throw new SQLException(
                    "La configuración debe incluir db.url de MySQL, "
                            + "db.usuario y db.contrasena. La contraseña puede estar vacía.",
                    "CONFIG"
            );
        }

        return new ConfiguracionBD(url, usuario, contrasena);
    }

    String getUrl() {
        return url;
    }

    String getUsuario() {
        return usuario;
    }

    String getContrasena() {
        return contrasena;
    }
}