package cl.biblioteca.util;

import javax.swing.SwingWorker;
import java.awt.Component;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Ejecuta una operación en segundo plano y entrega su resultado al hilo de Swing.
 */
public final class TareaBD<T> extends SwingWorker<T, Void> {

    private final Component ventana;
    private final Callable<T> operacion;
    private final Consumer<T> alCompletar;
    private final Runnable alFinalizar;

    public TareaBD(Component ventana, Callable<T> operacion,
                   Consumer<T> alCompletar, Runnable alFinalizar) {
        this.ventana = ventana;
        this.operacion = Objects.requireNonNull(
                operacion, "La operación es obligatoria."
        );
        this.alCompletar = Objects.requireNonNull(
                alCompletar, "La acción al completar es obligatoria."
        );
        this.alFinalizar = Objects.requireNonNull(
                alFinalizar, "La acción al finalizar es obligatoria."
        );
    }

    @Override
    protected T doInBackground() throws Exception {
        return operacion.call();
    }

    @Override
    protected void done() {
        try {
            if (isCancelled()) {
                return;
            }

            alCompletar.accept(get());

        } catch (CancellationException e) {
            // Una tarea cancelada no presenta un resultado ni un mensaje de error.

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ManejadorErrores.mostrar(ventana, e);

        } catch (ExecutionException | RuntimeException e) {
            ManejadorErrores.mostrar(ventana, e);

        } finally {
            try {
                alFinalizar.run();
            } catch (RuntimeException e) {
                ManejadorErrores.mostrar(ventana, e);
            }
        }
    }
}