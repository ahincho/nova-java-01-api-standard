package pe.edu.nova.java.libs.api.standard.error;

import java.io.Serializable;

/**
 * Un error de un campo de la entrada: qué campo falló, con qué código y qué se le dice a la persona.
 * <p>
 * Solo lo lleva un {@link ApplicationError} de tipo {@code INVALID_INPUT}, o el fallo de una excepción
 * de un framework que valida, y llega al cliente como una entrada de {@code errors} por campo. El
 * código es opcional: sin él, la entrada lleva el código del fallo, el que decide el
 * {@link ErrorCatalog}.
 * <p>
 * Una restricción sobre el objeto entero, como la que compara dos campos, no tiene campo: su
 * {@code field} es la cadena vacía. Es lo mismo que responde NestJS, así que los dos stacks contestan
 * igual.
 *
 * @param field   campo que falló, como {@code email} o {@code address.zipCode}; la cadena vacía si el
 *                error es del objeto entero (nunca null)
 * @param code    código propio del campo, como {@code REQUIRED} (opcional, puede ser null)
 * @param message mensaje para la persona (obligatorio)
 */
public record FieldError(String field, String code, String message) implements Serializable {

    /**
     * Constructor compacto con validación. Un campo nulo o en blanco es el del objeto entero y queda
     * como la cadena vacía; un código en blanco se trata como ausente.
     *
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public FieldError {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es obligatorio");
        }
        field = field == null || field.isBlank() ? "" : field;
        code = code == null || code.isBlank() ? null : code;
    }

    /**
     * Crea un FieldError sin código propio.
     *
     * @param field   campo que falló; nulo, vacío o en blanco si el error es del objeto entero
     * @param message mensaje para la persona
     * @return nueva instancia de FieldError
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static FieldError of(String field, String message) {
        return new FieldError(field, null, message);
    }

    /**
     * Crea un FieldError con código propio.
     *
     * @param field   campo que falló; nulo, vacío o en blanco si el error es del objeto entero
     * @param code    código propio del campo
     * @param message mensaje para la persona
     * @return nueva instancia de FieldError
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public static FieldError of(String field, String code, String message) {
        return new FieldError(field, code, message);
    }
}
