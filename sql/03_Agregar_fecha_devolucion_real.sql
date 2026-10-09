USE biblioteca;

ALTER TABLE prestamos
    ADD COLUMN fecha_devolucion_real DATE NULL
AFTER fecha_devolucion;

SHOW COLUMNS FROM prestamos LIKE 'fecha_devolucion_real';