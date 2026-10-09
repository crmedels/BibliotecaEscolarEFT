package cl.biblioteca.modelo;

import cl.biblioteca.util.Validador;

/**
 * Conserva la identidad y los permisos del usuario que inició sesión.
 */
public final class SesionUsuario {

    private final int idUsuario;
    private final String nombre;
    private final String rut;
    private final String rol;
    private final boolean administrador;
    private final int idEstudiante;

    public SesionUsuario(Usuario usuario, Estudiante estudiante) {
        Validador.validarSeleccion(usuario, "Usuario");

        idUsuario = Validador.validarId(usuario.getId(), "Usuario");
        nombre = Validador.textoObligatorio(usuario.getNombre(), "Nombre", 100);
        rut = Validador.validarRut(usuario.getRut());
        rol = Validador.textoObligatorio(usuario.getRol(), "Rol", 20);
        administrador = usuario.puedeAdministrar();

        if (estudiante == null) {
            if (!administrador) {
                throw new IllegalStateException(
                        "La cuenta de estudiante no tiene una ficha asociada."
                );
            }

            idEstudiante = 0;
        } else {
            String rutEstudiante = Validador.validarRut(estudiante.getRut());
            if (!rut.equals(rutEstudiante)) {
                throw new IllegalArgumentException(
                        "La ficha del estudiante no corresponde al RUT de la cuenta."
                );
            }

            idEstudiante = Validador.validarId(estudiante.getId(), "Estudiante");
        }
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRut() {
        return rut;
    }

    public String getRol() {
        return rol;
    }

    public boolean puedeAdministrar() {
        return administrador;
    }

    public boolean tieneFichaEstudiante() {
        return idEstudiante > 0;
    }

    public int getIdEstudiante() {
        if (!tieneFichaEstudiante()) {
            throw new IllegalStateException(
                    "La sesión no tiene una ficha de estudiante asociada."
            );
        }

        return idEstudiante;
    }

    public boolean puedeAccederAEstudiante(int idConsultado) {
        return idConsultado > 0 && (administrador || idConsultado == idEstudiante);
    }

    /**
     * Los servicios usarán esta comprobación para las operaciones administrativas.
     */
    public void exigirAdministracion() {
        if (!puedeAdministrar()) {
            throw new IllegalStateException(
                    "Esta operación requiere una cuenta de bibliotecario."
            );
        }
    }

    /**
     * Autoriza al bibliotecario o al estudiante dueño de la ficha consultada.
     */
    public void exigirAccesoEstudiante(int idConsultado) {
        Validador.validarId(idConsultado, "Estudiante");

        if (!puedeAccederAEstudiante(idConsultado)) {
            throw new IllegalStateException("Solo puede acceder a sus propios préstamos.");
        }
    }
}