package cl.biblioteca.modelo;

public final class LibroMasPrestado {

    private final int idLibro;
    private final String titulo;
    private final String autor;
    private final String isbn;
    private final long cantidadPrestamos;

    public LibroMasPrestado(int idLibro, String titulo, String autor,
                            String isbn, long cantidadPrestamos) {
        this.idLibro = idLibro;
        this.titulo = titulo;
        this.autor = autor;
        this.isbn = isbn;
        this.cantidadPrestamos = cantidadPrestamos;
    }

    public int getIdLibro() {
        return idLibro;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAutor() {
        return autor;
    }

    public String getIsbn() {
        return isbn;
    }

    public long getCantidadPrestamos() {
        return cantidadPrestamos;
    }
}