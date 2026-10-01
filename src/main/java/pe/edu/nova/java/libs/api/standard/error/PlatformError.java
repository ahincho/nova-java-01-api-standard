package pe.edu.nova.java.libs.api.standard.error;

import java.io.Serial;
import java.util.List;

/**
 * Error de la capa {@code platform}: un defecto o una falla del propio servicio durante una
 * petición.
 * <p>
 * Es un incidente, y siempre sale con el mensaje genérico: ni el mensaje ni la causa llegan al
 * cliente. Es también la capa de cualquier excepción que no sea un {@link NovaError}: la
 * integración la envuelve con {@link #internal(Throwable)}.
 * <p>
 * Un error de configuración al arrancar no es de esta capa: no se responde, la aplicación no arranca.
 */
public final class PlatformError extends NovaError {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Mensaje para el log cuando no hay ni mensaje ni causa. */
    private static final String DEFAULT_MESSAGE = "Un defecto o una falla del propio servicio";

    /**
     * Los tipos de la capa {@code platform}. El status lo decide el {@link ErrorStatusMapper};
     * entre paréntesis, el de la implementación de Nova.
     */
    public enum Type implements ErrorType {

        /** Un defecto o una falla del propio servicio (500). */
        INTERNAL;

        /**
         * Retorna la capa de todos los tipos de esta enumeración.
         *
         * @return {@link Layer#PLATFORM}
         */
        @Override
        public Layer layer() {
            return Layer.PLATFORM;
        }
    }

    /**
     * Constructor privado: se crea con las fábricas.
     *
     * @param message mensaje para el log
     * @param cause   la excepción original (puede ser null)
     */
    private PlatformError(String message, Throwable cause) {
        super(null, message, List.of(), null, null, cause);
    }

    /**
     * Un defecto detectado por el propio servicio, sin excepción de origen.
     *
     * @param message qué se rompió; va solo al log
     * @return nuevo PlatformError de tipo {@link Type#INTERNAL}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static PlatformError internal(String message) {
        return new PlatformError(requireMessage(message), null);
    }

    /**
     * Un defecto o una falla del servicio, con su causa.
     *
     * @param message qué se rompió; va solo al log
     * @param cause   la excepción original (puede ser null)
     * @return nuevo PlatformError de tipo {@link Type#INTERNAL}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static PlatformError internal(String message, Throwable cause) {
        return new PlatformError(requireMessage(message), cause);
    }

    /**
     * Envuelve una excepción cualquiera. El mensaje para el log es el de la causa, como en
     * {@link RuntimeException#RuntimeException(Throwable)}.
     *
     * @param cause la excepción original (puede ser null)
     * @return nuevo PlatformError de tipo {@link Type#INTERNAL}
     */
    public static PlatformError internal(Throwable cause) {
        return new PlatformError(cause == null ? DEFAULT_MESSAGE : cause.toString(), cause);
    }

    /**
     * Retorna el tipo del error; la capa {@code platform} tiene uno solo.
     *
     * @return {@link Type#INTERNAL}
     */
    @Override
    public Type type() {
        return Type.INTERNAL;
    }
}
