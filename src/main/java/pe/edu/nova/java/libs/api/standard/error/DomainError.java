package pe.edu.nova.java.libs.api.standard.error;

import java.io.Serial;
import java.util.List;

/**
 * Error de la capa {@code domain}: el negocio dijo que no.
 * <p>
 * Es un resultado esperado, no un incidente. Se crea con una fábrica por tipo, con o sin código
 * propio:
 * <pre>{@code
 * throw DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe");
 * throw DomainError.conflict("El pedido está cancelado y no se puede confirmar");
 * }</pre>
 */
public final class DomainError extends NovaError {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Los tipos de la capa {@code domain}. El status de cada uno lo decide el
     * {@link ErrorStatusMapper}; entre paréntesis, el de la implementación de Nova.
     */
    public enum Type implements ErrorType {

        /** El recurso de negocio no existe (404). */
        NOT_FOUND,

        /** El estado del recurso no admite la operación, como confirmar un pedido cancelado (409). */
        CONFLICT,

        /** Una regla de negocio dijo que no (422). */
        RULE_VIOLATION;

        /**
         * Retorna la capa de todos los tipos de esta enumeración.
         *
         * @return {@link Layer#DOMAIN}
         */
        @Override
        public Layer layer() {
            return Layer.DOMAIN;
        }
    }

    /** Tipo del error. */
    private final Type type;

    /**
     * Constructor privado: se crea con las fábricas.
     *
     * @param type    tipo del error
     * @param code    código propio (puede ser null)
     * @param message mensaje para la persona
     */
    private DomainError(Type type, String code, String message) {
        super(code, requireMessage(message), List.of(), null, null, null);
        this.type = type;
    }

    /**
     * El recurso de negocio no existe.
     *
     * @param message mensaje para la persona
     * @return nuevo DomainError de tipo {@link Type#NOT_FOUND}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static DomainError notFound(String message) {
        return notFound(null, message);
    }

    /**
     * El recurso de negocio no existe, con código propio.
     *
     * @param code    código propio, como {@code ORDER_NOT_FOUND}
     * @param message mensaje para la persona
     * @return nuevo DomainError de tipo {@link Type#NOT_FOUND}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static DomainError notFound(String code, String message) {
        return new DomainError(Type.NOT_FOUND, code, message);
    }

    /**
     * El estado del recurso no admite la operación.
     *
     * @param message mensaje para la persona
     * @return nuevo DomainError de tipo {@link Type#CONFLICT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static DomainError conflict(String message) {
        return conflict(null, message);
    }

    /**
     * El estado del recurso no admite la operación, con código propio.
     *
     * @param code    código propio, como {@code ORDER_CANCELLED}
     * @param message mensaje para la persona
     * @return nuevo DomainError de tipo {@link Type#CONFLICT}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static DomainError conflict(String code, String message) {
        return new DomainError(Type.CONFLICT, code, message);
    }

    /**
     * Una regla de negocio dijo que no.
     *
     * @param message mensaje para la persona
     * @return nuevo DomainError de tipo {@link Type#RULE_VIOLATION}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static DomainError ruleViolation(String message) {
        return ruleViolation(null, message);
    }

    /**
     * Una regla de negocio dijo que no, con código propio.
     *
     * @param code    código propio, como {@code CREDIT_LIMIT_EXCEEDED}
     * @param message mensaje para la persona
     * @return nuevo DomainError de tipo {@link Type#RULE_VIOLATION}
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static DomainError ruleViolation(String code, String message) {
        return new DomainError(Type.RULE_VIOLATION, code, message);
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
