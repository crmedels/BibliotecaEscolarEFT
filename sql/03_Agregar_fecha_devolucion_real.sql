USE biblioteca;

ALTER TABLE prestamos
    ADD COLUMN fecha_devolucion_real DATE NULL
    AFTER fecha_devolucion;