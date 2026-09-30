package pe.edu.nova.java.libs.api.standard.error;

import java.util.Locale;

/**
 * Capa de la que viene un error, según ADR-031.
 * <p>
 * La capa ordena cómo se trata el error: {@link #DOMAIN} y {@link #APPLICATION} son esperados, así
 * que se registran en {@code warn}, sin stack trace, y no despiertan a nadie; {@link #INFRASTRUCTURE}
 * y {@link #PLATFORM} son incidentes, van en {@code error} con la causa completa y alimentan la tasa
 * de error de los Four Golden Signals.
 */
public enum Layer {

    /** El negocio: el recurso no existe, su estado no admite la operación o una regla dijo que no. */
    DOMAIN,

    /** El caso de uso: la entrada, la identidad, los permisos o un límite. */
    APPLICATION,

    /** Una dependencia: no está disponible, no respondió a tiempo o respondió algo inválido. */
    INFRASTRUCTURE,

    /** Un defecto o una falla del propio servicio durante una petición. */
    PLATFORM;

    /**
     * El nombre de la capa como lo escriben el log y las métricas: {@code domain},
     * {@code application}, {@code infrastructure} o {@code platform}.
     * <p>
     * Es el mismo en los tres stacks, para que un tablero agrupe por la misma cadena.
     *
     * @return el nombre de la capa en minúsculas
     */
    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }

    /**
     * Indica si un error de esta capa es un incidente, y no un resultado esperado.
     *
     * @return {@code true} para {@link #INFRASTRUCTURE} y {@link #PLATFORM}
     */
    public boolean isIncident() {
        return this == INFRASTRUCTURE || this == PLATFORM;
    }
}
