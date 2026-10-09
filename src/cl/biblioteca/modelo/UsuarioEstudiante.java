package cl.biblioteca.modelo;

/**
 * Representa una cuenta de estudiante con acceso a consultas,
 * préstamos y devoluciones propios.
 */
public final class UsuarioEstudiante extends Usuario {

    public UsuarioEstudiante(int id, String nombre, String rut,
                             String correo, String contrasena) {

        super(id, nombre, rut, correo, contrasena);
    }

    public UsuarioEstudiante(String nombre, String rut,
                             String correo, String contrasena) {

        super(nombre, rut, correo, contrasena);
    }

    @Override
    public String getRol() {
        return "estudiante";
    }

    @Override
    public boolean puedeAdministrar() {
        return false;
    }
}