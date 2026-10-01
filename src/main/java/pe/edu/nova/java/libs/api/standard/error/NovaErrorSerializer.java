package pe.edu.nova.java.libs.api.standard.error;

import pe.edu.nova.java.libs.api.standard.metadata.ApiMetadata;
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementación de Nova de {@link ErrorSerializer}: el sobre de Nova (ADR-031).
 * <p>
 * El cuerpo es un {@link ApiResponse} con:
 * <ul>
 *   <li>{@code success} en {@code false} y {@code status} igual al status HTTP del fallo;</li>
 *   <li>{@code errors} con una entrada, el código y el mensaje que decidió el {@link ErrorCatalog}; o,
 *       si el fallo trae errores por campo, que solo los trae un 4xx, una entrada por campo, con su
 *       {@code field}, su código propio o el del fallo, y su mensaje. El {@code field} de un error de
 *       todo el objeto es la cadena vacía;</li>
 *   <li>{@code metadata.traceId}, el del fallo.</li>
 * </ul>
 * Si el fallo tiene {@code retryAfter}, los headers llevan {@code Retry-After} en segundos enteros,
 * redondeados hacia arriba para que el cliente nunca reintente antes de tiempo.
 */
public final class NovaErrorSerializer implements ErrorSerializer {

    /** Header que dice cuántos segundos esperar antes de reintentar. */
    private static final String RETRY_AFTER = "Retry-After";

    /** Crea el serializador de Nova. */
    public NovaErrorSerializer() {
    }

    /**
     * Construye el sobre de Nova y los headers de un fallo.
     *
     * @param failure el fallo saneado, con el status, el código y el mensaje decididos
     * @return el status, un {@link ApiResponse} como cuerpo y los headers
     */
    @Override
    public SerializedError serialize(SanitizedFailure failure) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .status(failure.status())
                .errors(entriesOf(failure))
                .metadata(ApiMetadata.builder().traceId(failure.traceId().orElse(null)).build())
                .build();
        return new SerializedError(failure.status(), body, headersOf(failure));
    }

    /**
     * Las entradas de {@code errors}: una por campo si el fallo los trae, y si no, una sola.
     * <p>
     * El fallo saneado no trae errores por campo en un 5xx, así que ahí sale siempre una sola entrada,
     * con el código y el mensaje genéricos.
     *
     * @param failure el fallo
     * @return las entradas, al menos una
     */
    private static List<ApiError> entriesOf(SanitizedFailure failure) {
        if (failure.fieldErrors().isEmpty()) {
            return List.of(ApiError.of(failure.code(), failure.message()));
        }
        return failure.fieldErrors().stream()
                .map(field -> ApiError.of(
                        Objects.requireNonNullElse(field.code(), failure.code()), field.message(), field.field()))
                .toList();
    }

    /**
     * Los headers de la respuesta.
     *
     * @param failure el fallo
     * @return {@code Retry-After} si el fallo se puede reintentar; si no, ninguno
     */
    private static Map<String, String> headersOf(SanitizedFailure failure) {
        return failure.retryAfter()
                .map(retryAfter -> Map.of(RETRY_AFTER, Long.toString(seconds(retryAfter))))
                .orElse(Map.of());
    }

    /**
     * Una espera en segundos enteros, redondeada hacia arriba.
     *
     * @param retryAfter la espera, nunca negativa
     * @return los segundos
     */
    private static long seconds(Duration retryAfter) {
        long seconds = retryAfter.getSeconds();
        return retryAfter.getNano() > 0 ? seconds + 1 : seconds;
    }
}
