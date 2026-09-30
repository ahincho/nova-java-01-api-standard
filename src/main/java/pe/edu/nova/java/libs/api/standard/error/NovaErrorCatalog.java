package pe.edu.nova.java.libs.api.standard.error;

import java.util.Map;

/**
 * Implementación de Nova de {@link ErrorCatalog}, la misma en los tres stacks (ADR-031).
 * <ul>
 *   <li>Un 4xx lleva el código propio del error si lo trae, y si no, el de su status en el catálogo
 *       de la plataforma; el mensaje es el del error.</li>
 *   <li>Un 5xx lleva siempre el código de su status y el mensaje genérico. Nunca revela el código
 *       propio, el mensaje ni el proveedor: el cliente sabe si conviene reintentar, un 503 o un 504
 *       sí y un 500 no, sin conocer la topología.</li>
 * </ul>
 * <table>
 *   <caption>El catálogo de códigos de la plataforma</caption>
 *   <tr><th>Status</th><th>Código</th><th>Status</th><th>Código</th></tr>
 *   <tr><td>400</td><td>BAD_REQUEST</td><td>410</td><td>GONE</td></tr>
 *   <tr><td>401</td><td>UNAUTHORIZED</td><td>415</td><td>UNSUPPORTED_MEDIA_TYPE</td></tr>
 *   <tr><td>403</td><td>FORBIDDEN</td><td>422</td><td>UNPROCESSABLE_ENTITY</td></tr>
 *   <tr><td>404</td><td>NOT_FOUND</td><td>429</td><td>TOO_MANY_REQUESTS</td></tr>
 *   <tr><td>405</td><td>METHOD_NOT_ALLOWED</td><td>500</td><td>INTERNAL_SERVER_ERROR</td></tr>
 *   <tr><td>406</td><td>NOT_ACCEPTABLE</td><td>502</td><td>BAD_GATEWAY</td></tr>
 *   <tr><td>408</td><td>REQUEST_TIMEOUT</td><td>503</td><td>SERVICE_UNAVAILABLE</td></tr>
 *   <tr><td>409</td><td>CONFLICT</td><td>504</td><td>GATEWAY_TIMEOUT</td></tr>
 * </table>
 * Cualquier otro 4xx lleva {@code REQUEST_ERROR}, y cualquier otro status, {@code INTERNAL_SERVER_ERROR}.
 */
public final class NovaErrorCatalog implements ErrorCatalog {

    /** Mensaje genérico de todo error que no es 4xx; es el que ya usaba el starter de Spring Boot. */
    private static final String GENERIC_MESSAGE = "Error interno del servidor";

    /** Código de un 4xx que el catálogo no nombra. */
    private static final String REQUEST_ERROR = "REQUEST_ERROR";

    /** Código de cualquier otro status que el catálogo no nombra. */
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    /** El catálogo de códigos de la plataforma, por status. */
    private static final Map<Integer, String> CODES = Map.ofEntries(
            Map.entry(400, "BAD_REQUEST"),
            Map.entry(401, "UNAUTHORIZED"),
            Map.entry(403, "FORBIDDEN"),
            Map.entry(404, "NOT_FOUND"),
            Map.entry(405, "METHOD_NOT_ALLOWED"),
            Map.entry(406, "NOT_ACCEPTABLE"),
            Map.entry(408, "REQUEST_TIMEOUT"),
            Map.entry(409, "CONFLICT"),
            Map.entry(410, "GONE"),
            Map.entry(415, "UNSUPPORTED_MEDIA_TYPE"),
            Map.entry(422, "UNPROCESSABLE_ENTITY"),
            Map.entry(429, "TOO_MANY_REQUESTS"),
            Map.entry(500, INTERNAL_SERVER_ERROR),
            Map.entry(502, "BAD_GATEWAY"),
            Map.entry(503, "SERVICE_UNAVAILABLE"),
            Map.entry(504, "GATEWAY_TIMEOUT"));

    /** Crea el catálogo de Nova. */
    public NovaErrorCatalog() {
    }

    /**
     * Retorna el código y el mensaje que ve el cliente, según las reglas de la plataforma.
     *
     * @param error  el error
     * @param status el status con que se va a responder
     * @return la entrada del catálogo para ese error
     */
    @Override
    public CatalogEntry describe(NovaError error, int status) {
        if (isClientError(status)) {
            return new CatalogEntry(error.code().orElseGet(() -> platformCode(status)), error.getMessage());
        }
        return new CatalogEntry(platformCode(status), GENERIC_MESSAGE);
    }

    /**
     * Retorna el código del catálogo de la plataforma para un status, sin mirar ningún error.
     * <p>
     * Sirve a una integración que responde por status una excepción de su framework, como un 405,
     * con el mismo código que usaría Nova.
     *
     * @param status el status HTTP
     * @return el código de la tabla; {@code REQUEST_ERROR} para otro 4xx, y
     *         {@code INTERNAL_SERVER_ERROR} para cualquier otro status
     */
    public static String platformCode(int status) {
        String code = CODES.get(status);
        if (code != null) {
            return code;
        }
        return isClientError(status) ? REQUEST_ERROR : INTERNAL_SERVER_ERROR;
    }

    /**
     * Indica si el status es 4xx, el único rango que muestra el código y el mensaje del error.
     *
     * @param status el status HTTP
     * @return {@code true} si está entre 400 y 499
     */
    private static boolean isClientError(int status) {
        return status >= 400 && status < 500;
    }
}
