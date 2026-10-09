package cl.biblioteca.modelo;

/**
 * Define los datos comunes de las cuentas del sistema.
 * Cada tipo de usuario establece su rol y sus permisos.
 */
public abstract class Usuario {

    private int id;
    private String nombre;
    private String rut;
    private String correo;
    private String contrasena;

    protected Usuario(int id, String nombre, String rut,
                      String correo, String contrasena) {

        this.id = id;
        this.nombre = nombre;
        this.rut = rut;
        this.correo = correo;
        this.contrasena = contrasena;
    }

    /**
     * Prepara una cuenta cuyo ID será generado por MySQL.
     */
    protected Usuario(String nombre, String rut,
                      String correo, String contrasena) {

        this(0, nombre, rut, correo, contrasena);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getRut() {
        return rut;
    }

    public void setRut(String rut) {
        this.rut = rut;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    /**
     * Devuelve el rol utilizado en la base de datos.
     */
    public abstract String getRol();

    /**
     * Indica si puede administrar el catálogo, estudiantes y usuarios.
     */
    public abstract boolean puedeAdministrar();

    @Override
    public String toString() {
        return nombre == null ? "" : nombre;
    }
}