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
 *   <li>{@code success} en {@code false} y {@code status} igual al status HTTP, el que decide el
 *       {@link ErrorStatusMapper};</li>
 *   <li>{@code errors} con una entrada, el código y el mensaje que decide el {@link ErrorCatalog}; o,
 *       si el error trae errores por campo y la respuesta es 4xx, una entrada por campo, con su
 *       {@code field}, su código propio o el del catálogo, y su mensaje;</li>
 *   <li>{@code metadata.traceId}, el que el error capturó al nacer.</li>
 * </ul>
 * Si el error tiene {@code retryAfter}, los headers llevan {@code Retry-After} en segundos enteros,
 * redondeados hacia arriba para que el cliente nunca reintente antes de tiempo.
 */
public final class NovaErrorSerializer implements ErrorSerializer {

    /** Header que dice cuántos segundos esperar antes de reintentar. */
    private static final String RETRY_AFTER = "Retry-After";

    /** Puerto que decide el status. */
    private final ErrorStatusMapper statusMapper;

    /** Puerto que decide el código y el mensaje. */
    private final ErrorCatalog catalog;

    /**
     * Crea el serializador con las implementaciones de Nova de los otros dos puertos.
     */
    public NovaErrorSerializer() {
        this(new NovaErrorStatusMapper(), new NovaErrorCatalog());
    }

    /**
     * Crea el serializador con los puertos que registró el servicio, propios o de Nova.
     *
     * @param statusMapper el que decide el status
     * @param catalog      el que decide el código y el mensaje
     * @throws NullPointerException si alguno es nulo
     */
    public NovaErrorSerializer(ErrorStatusMapper statusMapper, ErrorCatalog catalog) {
        this.statusMapper = Objects.requireNonNull(statusMapper, "statusMapper es obligatorio");
        this.catalog = Objects.requireNonNull(catalog, "catalog es obligatorio");
    }

    /**
     * Construye el sobre de Nova y los headers de un error.
     *
     * @param error el error
     * @return el status, un {@link ApiResponse} como cuerpo y los headers
     */
    @Override
    public SerializedError serialize(NovaError error) {
        int status = statusMapper.statusOf(error);
        CatalogEntry entry = catalog.describe(error, status);
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .status(status)
                .errors(entriesOf(error, entry, status))
                .metadata(ApiMetadata.builder().traceId(traceIdOf(error)).build())
                .build();
        return new SerializedError(status, body, headersOf(error));
    }

    /**
     * Las entradas de {@code errors}: una por campo en un 4xx que los trae, y si no, una sola.
     * <p>
     * Los errores por campo nunca salen en un 5xx, aunque un mapeador propio llevara ahí un error
     * que los tiene: un 5xx no revela nada del error.
     *
     * @param error  el error
     * @param entry  lo que decidió el catálogo
     * @param status el status de la respuesta
     * @return las entradas, al menos una
     */
    private static List<ApiError> entriesOf(NovaError error, CatalogEntry entry, int status) {
        boolean clientError = status >= 400 && status < 500;
        if (!clientError || error.fieldErrors().isEmpty()) {
            return List.of(ApiError.of(entry.code(), entry.message()));
        }
        return error.fieldErrors().stream()
                .map(field -> ApiError.of(
                        Objects.requireNonNullElse(field.code(), entry.code()), field.message(), field.field()))
                .toList();
    }

    /**
     * El {@code traceId} del cuerpo.
     * <p>
     * Es el que el error capturó al nacer. Si nació sin uno, por ejemplo en un hilo sin el
     * contexto de la petición, se toma el del contexto vigente al responder; y si tampoco hay,
     * {@link ApiMetadata} genera uno, como hace siempre.
     *
     * @param error el error
     * @return el traceId, o null si no hay ninguno
     */
    private static String traceIdOf(NovaError error) {
        return error.traceId().or(TraceIdCapture::current).orElse(null);
    }

    /**
     * Los headers de la respuesta.
     *
     * @param error el error
     * @return {@code Retry-After} si el error se puede reintentar; si no, ninguno
     */
    private static Map<String, String> headersOf(NovaError error) {
        return error.retryAfter()
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
