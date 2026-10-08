package cl.biblioteca.modelo;

/**
 * Representa una categoría utilizada para clasificar los libros.
 */
public class Categoria {

    private int id;
    private String nombre;

    public Categoria(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    /**
     * Crea una categoría cuyo ID será generado por MySQL.
     */
    public Categoria(String nombre) {
        this(0, nombre);
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

    @Override
    public String toString() {
        return nombre == null ? "" : nombre;
    }
}