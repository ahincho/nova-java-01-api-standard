package pe.edu.nova.java.libs.api.standard.error;

import java.io.Serial;
import java.time.Duration;
import java.util.List;

/**
 * Error de la capa {@code application}: el caso de uso no puede atender la petición tal como llegó.
 * <p>
 * Es un resultado esperado, no un incidente. Se crea con una fábrica por tipo; los que se pueden
 * reintentar, {@code CONFLICT} por una operación en curso y {@code RATE_LIMITED}, reciben además
 * cuánto esperar:
 * <pre>{@code
 * throw ApplicationError.invalidInput("La solicitud tiene campos inválidos",
 *         List.of(FieldError.of("email", "El correo no es válido")));
 * throw ApplicationError.conflict("IDEMPOTENCY_KEY_IN_USE",
 *         "La operación con esta clave sigue en curso", Duration.ofSeconds(1));
 * }</pre>
 */
public final class ApplicationError extends NovaError {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Los tipos de la capa {@code application}. El status de cada uno lo decide el
     * {@link ErrorStatusMapper}; entre paréntesis, el de la implementación de Nova.
     */
    public enum Type implements ErrorType {

        /** La entrada no es válida; lleva los errores por campo (400). */
        INVALID_INPUT,

        /** La operación choca con otra en curso, como una clave de idempotencia en uso (409). */
        CONFLICT,

        /** La entrada es válida pero no se puede procesar, como una clave reusada con otro contenido (422). */
        UNPROCESSABLE,

        /** Falta la identidad o no es válida (401). */
        UNAUTHENTICATED,

        /** La identidad no tiene permiso (403). */
        FORBIDDEN,

        /** Se superó un límite (429). */
        RATE_LIMITED;

        /**
         * Retorna la capa de todos los tipos de esta enumeración.
         *
         * @return {@link Layer#APPLICATION}
         */
        @Override
        public Layer layer() {
            return Layer.APPLICATION;
        }
    }

    /** Tipo del error. */
    private final Type type;

    /**
     * Constructor privado: se crea con las fábricas.
     *
     * @param type        tipo del error
     * @param code        código propio (puede ser null)
     * @param message     mensaje para la persona
     * @param fieldErrors errores por campo (puede ser null)
     * @param retryAfter  cuánto esperar antes de reintentar (puede ser null)
     */
    private ApplicationError(Type type, String code, String message,
                             List<FieldError> fieldErrors, Duration retryAfter) {
        super(code, requireMessage(message), fieldErrors, retryAfter, null, null);
        this.type = type;
    }

    /**
     * La entrada no es válida.
     *
     * @param message     mensaje para la persona
     * @param fieldErrors errores por campo; cada uno llega al cliente como una entrada propia
     * @return nuevo ApplicationError de tipo {@link Type#INVALID_INPUT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError invalidInput(String message, List<FieldError> fieldErrors) {
        return invalidInput(null, message, fieldErrors);
    }

    /**
     * La entrada no es válida, con código propio.
     *
     * @param code        código propio; también es el de cada campo que no trae el suyo
     * @param message     mensaje para la persona
     * @param fieldErrors errores por campo; cada uno llega al cliente como una entrada propia
     * @return nuevo ApplicationError de tipo {@link Type#INVALID_INPUT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError invalidInput(String code, String message, List<FieldError> fieldErrors) {
        return new ApplicationError(Type.INVALID_INPUT, code, message, fieldErrors, null);
    }

    /**
     * La operación choca con otra.
     *
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#CONFLICT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError conflict(String message) {
        return conflict(null, message, null);
    }

    /**
     * La operación choca con otra, con código propio.
     *
     * @param code    código propio, como {@code IDEMPOTENCY_KEY_IN_USE}
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#CONFLICT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError conflict(String code, String message) {
        return conflict(code, message, null);
    }

    /**
     * La operación choca con otra que sigue en curso, y se puede reintentar.
     *
     * @param message    mensaje para la persona
     * @param retryAfter cuánto esperar antes de reintentar; una espera negativa se descarta
     * @return nuevo ApplicationError de tipo {@link Type#CONFLICT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError conflict(String message, Duration retryAfter) {
        return conflict(null, message, retryAfter);
    }

    /**
     * La operación choca con otra que sigue en curso, con código propio, y se puede reintentar.
     *
     * @param code       código propio, como {@code IDEMPOTENCY_KEY_IN_USE}
     * @param message    mensaje para la persona
     * @param retryAfter cuánto esperar antes de reintentar (puede ser null; una espera negativa se
     *                   descarta)
     * @return nuevo ApplicationError de tipo {@link Type#CONFLICT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError conflict(String code, String message, Duration retryAfter) {
        return new ApplicationError(Type.CONFLICT, code, message, null, retryAfter);
    }

    /**
     * La entrada es válida pero no se puede procesar.
     *
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#UNPROCESSABLE}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError unprocessable(String message) {
        return unprocessable(null, message);
    }

    /**
     * La entrada es válida pero no se puede procesar, con código propio.
     *
     * @param code    código propio, como {@code IDEMPOTENCY_KEY_REUSED}
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#UNPROCESSABLE}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError unprocessable(String code, String message) {
        return new ApplicationError(Type.UNPROCESSABLE, code, message, null, null);
    }

    /**
     * Falta la identidad o no es válida.
     *
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#UNAUTHENTICATED}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError unauthenticated(String message) {
        return unauthenticated(null, message);
    }

    /**
     * Falta la identidad o no es válida, con código propio.
     *
     * @param code    código propio, como {@code TOKEN_EXPIRED}
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#UNAUTHENTICATED}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError unauthenticated(String code, String message) {
        return new ApplicationError(Type.UNAUTHENTICATED, code, message, null, null);
    }

    /**
     * La identidad no tiene permiso.
     *
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#FORBIDDEN}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError forbidden(String message) {
        return forbidden(null, message);
    }

    /**
     * La identidad no tiene permiso, con código propio.
     *
     * @param code    código propio, como {@code COURSE_NOT_ENROLLED}
     * @param message mensaje para la persona
     * @return nuevo ApplicationError de tipo {@link Type#FORBIDDEN}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError forbidden(String code, String message) {
        return new ApplicationError(Type.FORBIDDEN, code, message, null, null);
    }

    /**
     * Se superó un límite.
     *
     * @param message    mensaje para la persona
     * @param retryAfter cuánto esperar antes de reintentar (puede ser null si el límite no lo dice; una
     *                   espera negativa se descarta)
     * @return nuevo ApplicationError de tipo {@link Type#RATE_LIMITED}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError rateLimited(String message, Duration retryAfter) {
        return rateLimited(null, message, retryAfter);
    }

    /**
     * Se superó un límite, con código propio.
     *
     * @param code       código propio, como {@code ORDER_QUOTA_EXCEEDED}
     * @param message    mensaje para la persona
     * @param retryAfter cuánto esperar antes de reintentar (puede ser null si el límite no lo dice; una
     *                   espera negativa se descarta)
     * @return nuevo ApplicationError de tipo {@link Type#RATE_LIMITED}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static ApplicationError rateLimited(String code, String message, Duration retryAfter) {
        return new ApplicationError(Type.RATE_LIMITED, code, message, null, retryAfter);
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
}
