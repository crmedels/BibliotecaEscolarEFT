package cl.biblioteca.pruebas;

import cl.biblioteca.main.Main;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Diagnostico temporal para localizar el aviso del ordenador de tablas.
 */
public final class PruebaAvisoTabla {

    private PruebaAvisoTabla() {
    }

    public static void main(String[] args) {
        PrintStream consolaOriginal = System.err;

        System.setErr(new PrintStream(consolaOriginal, true, StandardCharsets.UTF_8) {
            @Override
            public void println(String mensaje) {
                super.println(mensaje);

                if (mensaje != null
                        && mensaje.startsWith("WARNING: row index is bigger")) {
                    new Throwable("Origen del aviso de ordenacion (diagnostico)")
                            .printStackTrace(consolaOriginal);
                }
            }
        });

        Main.main(args);
    }
}