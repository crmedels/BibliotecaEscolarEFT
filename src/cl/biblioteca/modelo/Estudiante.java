package cl.biblioteca.modelo;

/**
 * Representa a un estudiante que puede solicitar préstamos.
 */
public class Estudiante {

    private int id;
    private String nombre;
    private String rut;
    private String curso;
    private String correo;

    public Estudiante(int id, String nombre, String rut,
                      String curso, String correo) {

        this.id = id;
        this.nombre = nombre;
        this.rut = rut;
        this.curso = curso;
        this.correo = correo;
    }

    /**
     * Crea un estudiante cuyo ID será generado por MySQL.
     */
    public Estudiante(String nombre, String rut,
                      String curso, String correo) {

        this(0, nombre, rut, curso, correo);
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

    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    @Override
    public String toString() {
        return nombre == null ? "" : nombre;
    }
}