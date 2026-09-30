package pe.edu.nova.java.libs.api.standard.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lo que una integración escribe en la respuesta de un error: el status, el cuerpo y los headers.
 * <p>
 * El cuerpo es {@code Object} a propósito. El de Nova es un
 * {@link pe.edu.nova.java.libs.api.standard.response.ApiResponse}, pero otro {@link ErrorSerializer},
 * como uno de RFC 7807, puede escribir otra forma; la integración lo entrega tal cual al
 * serializador JSON de su framework.
 *
 * @param status  status HTTP de la respuesta
 * @param body    cuerpo de la respuesta
 * @param headers headers de la respuesta, como {@code Retry-After} (copia defensiva inmutable)
 */
public record SerializedError(int status, Object body, Map<String, String> headers) {

    /**
     * Constructor compacto que realiza la copia defensiva de los headers.
     */
    public SerializedError {
        headers = headers == null || headers.isEmpty()
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
    }
}
