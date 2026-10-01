package pe.edu.nova.java.libs.api.standard.error;

import java.io.Serial;
import java.time.Duration;
import java.util.List;

/**
 * Error de la capa {@code infrastructure}: una dependencia falló.
 * <p>
 * Es un incidente. Cada fábrica pide el nombre del proveedor y la causa, que van solo al log: el
 * cliente recibe el código genérico del status, que le dice si conviene reintentar sin contarle la
 * topología. Perder el nombre del proveedor es justo el defecto que este modelo evita, por eso es
 * obligatorio.
 * <pre>{@code
 * throw InfrastructureError.timeout("pagos", exception);
 * throw InfrastructureError.unavailable("pagos", exception, Duration.ofSeconds(5));
 * }</pre>
 * El módulo clasifica y reporta, y no reintenta ni degrada solo (ADR-029).
 */
public final class InfrastructureError extends NovaError {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Los tipos de la capa {@code infrastructure}. El status de cada uno lo decide el
     * {@link ErrorStatusMapper}; entre paréntesis, el de la implementación de Nova.
     */
    public enum Type implements ErrorType {

        /** Una dependencia no está disponible (503). */
        UNAVAILABLE,

        /** Una dependencia no respondió a tiempo (504). */
        TIMEOUT,

        /** Una dependencia respondió algo inválido (502). */
        BAD_GATEWAY;

        /**
         * Retorna la capa de todos los tipos de esta enumeración.
         *
         * @return {@link Layer#INFRASTRUCTURE}
         */
        @Override
        public Layer layer() {
            return Layer.INFRASTRUCTURE;
        }
    }

    /** Tipo del error. */
    private final Type type;

    /**
     * Constructor privado: se crea con las fábricas.
     * <p>
     * El mensaje dice qué pasó, sin nombrar al proveedor: el log lo lleva aparte, en el campo
     * {@code upstream}, y no dentro del mensaje.
     *
     * @param type       tipo del error
     * @param upstream   la dependencia que falló
     * @param cause      la excepción original (puede ser null)
     * @param retryAfter cuánto esperar antes de reintentar (puede ser null)
     */
    private InfrastructureError(Type type, String upstream, Throwable cause, Duration retryAfter) {
        super(null, messageOf(type), List.of(), retryAfter, requireUpstream(upstream), cause);
        this.type = type;
    }

    /**
     * Una dependencia no está disponible.
     *
     * @param upstream la dependencia que falló, como {@code pagos}
     * @param cause    la excepción original (puede ser null)
     * @return nuevo InfrastructureError de tipo {@link Type#UNAVAILABLE}
     * @throws IllegalArgumentException si upstream es nulo o en blanco
     */
    public static InfrastructureError unavailable(String upstream, Throwable cause) {
        return unavailable(upstream, cause, null);
    }

    /**
     * Una dependencia no está disponible, y se sabe cuándo conviene reintentar.
     *
     * @param upstream   la dependencia que falló, como {@code pagos}
     * @param cause      la excepción original (puede ser null)
     * @param retryAfter cuánto esperar antes de reintentar (puede ser null; una espera negativa se
     *                   descarta)
     * @return nuevo InfrastructureError de tipo {@link Type#UNAVAILABLE}
     * @throws IllegalArgumentException si upstream es nulo o en blanco
     */
    public static InfrastructureError unavailable(String upstream, Throwable cause, Duration retryAfter) {
        return new InfrastructureError(Type.UNAVAILABLE, upstream, cause, retryAfter);
    }

    /**
     * Una dependencia no respondió a tiempo.
     *
     * @param upstream la dependencia que falló, como {@code pagos}
     * @param cause    la excepción original (puede ser null)
     * @return nuevo InfrastructureError de tipo {@link Type#TIMEOUT}
     * @throws IllegalArgumentException si upstream es nulo o en blanco
     */
    public static InfrastructureError timeout(String upstream, Throwable cause) {
        return new InfrastructureError(Type.TIMEOUT, upstream, cause, null);
    }

    /**
     * Una dependencia respondió algo inválido.
     *
     * @param upstream la dependencia que falló, como {@code pagos}
     * @param cause    la excepción original (puede ser null)
     * @return nuevo InfrastructureError de tipo {@link Type#BAD_GATEWAY}
     * @throws IllegalArgumentException si upstream es nulo o en blanco
     */
    public static InfrastructureError badGateway(String upstream, Throwable cause) {
        return new InfrastructureError(Type.BAD_GATEWAY, upstream, cause, null);
    }

    /**
     * Retorna el tipo del error.
     *
     * @return tipo del error
     */
    @Override
    public Type type() {
        return type;
    }

    /**
     * Valida el nombre del proveedor.
     *
     * @param upstream la dependencia que falló
     * @return el mismo nombre
     * @throws IllegalArgumentException si upstream es nulo o en blanco
     */
    private static String requireUpstream(String upstream) {
        if (upstream == null || upstream.isBlank()) {
            throw new IllegalArgumentException("upstream es obligatorio");
        }
        return upstream;
    }

    /**
     * El mensaje para el log de cada tipo.
     *
     * @param type tipo del error
     * @return qué pasó, sin nombrar al proveedor
     */
    private static String messageOf(Type type) {
        return switch (type) {
            case UNAVAILABLE -> "Una dependencia no está disponible";
            case TIMEOUT -> "Una dependencia no respondió a tiempo";
            case BAD_GATEWAY -> "Una dependencia respondió algo inválido";
        };
    }
}
