package pe.edu.nova.java.libs.api.standard.error;

import java.io.Serializable;

/**
 * Un error de un campo de la entrada: qué campo falló, con qué código y qué se le dice a la persona.
 * <p>
 * Solo lo lleva un {@link ApplicationError} de tipo {@code INVALID_INPUT}, y llega al cliente como
 * una entrada de {@code errors} por campo. El código es opcional: sin él, la entrada lleva el código
 * del error, el que decide el {@link ErrorCatalog}.
 *
 * @param field   campo que falló, como {@code email} o {@code address.zipCode} (obligatorio)
 * @param code    código propio del campo, como {@code REQUIRED} (opcional, puede ser null)
 * @param message mensaje para la persona (obligatorio)
 */
public record FieldError(String field, String code, String message) implements Serializable {

    /**
     * Constructor compacto con validación. Un código en blanco se trata como ausente.
     *
     * @throws IllegalArgumentException si field es nulo o en blanco
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public FieldError {
        if (field == null || field.isBlank()) {
            throw new IllegalArgumentException("field es obligatorio");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es obligatorio");
        }
        code = code == null || code.isBlank() ? null : code;
    }

    /**
     * Crea un FieldError sin código propio.
     *
     * @param field   campo que falló
     * @param message mensaje para la persona
     * @return nueva instancia de FieldError
     * @throws IllegalArgumentException si field o message son nulos o en blanco
     */
    public static FieldError of(String field, String message) {
        return new FieldError(field, null, message);
    }

    /**
     * Crea un FieldError con código propio.
     *
     * @param field   campo que falló
     * @param code    código propio del campo
     * @param message mensaje para la persona
     * @return nueva instancia de FieldError
     * @throws IllegalArgumentException si field o message son nulos o en blanco
     */
    public static FieldError of(String field, String code, String message) {
        return new FieldError(field, code, message);
    }
}
