package cl.biblioteca.modelo;

/**
 * Representa una cuenta con permisos de administración.
 */
public final class Bibliotecario extends Usuario {

    public Bibliotecario(int id, String nombre, String rut,
                         String correo, String contrasena) {

        super(id, nombre, rut, correo, contrasena);
    }

    public Bibliotecario(String nombre, String rut,
                         String correo, String contrasena) {

        super(nombre, rut, correo, contrasena);
    }

    @Override
    public String getRol() {
        return "bibliotecario";
    }

    @Override
    public boolean puedeAdministrar() {
        return true;
    }
}