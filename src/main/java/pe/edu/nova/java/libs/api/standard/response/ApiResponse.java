package pe.edu.nova.java.libs.api.standard.response;

import pe.edu.nova.java.libs.api.standard.error.ApiError;
import pe.edu.nova.java.libs.api.standard.link.ApiLink;
import pe.edu.nova.java.libs.api.standard.metadata.ApiMetadata;
import pe.edu.nova.java.libs.api.standard.page.PageInfo;
import pe.edu.nova.java.libs.api.standard.ratelimit.RateLimitInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Envelope genérico inmutable para respuestas de API REST.
 * Encapsula toda respuesta en una estructura uniforme con datos, errores,
 * metadatos, enlaces HATEOAS, rate limiting y paginación.
 * <p>
 * Usa el patrón Builder ({@link ResponseBuilder}) para construcción fluida.
 * Los factory methods proporcionan atajos para respuestas comunes.
 *
 * @param success       indica si la respuesta fue exitosa (sin errores)
 * @param status        código de estado HTTP de la respuesta
 * @param data          dato contenido en la respuesta (puede ser null)
 * @param errors        lista inmutable de errores de la respuesta
 * @param metadata      metadatos de la respuesta (puede ser null)
 * @param links         lista inmutable de enlaces HATEOAS
 * @param rateLimitInfo información de rate limiting (puede ser null)
 * @param pageInfo      información de paginación (puede ser null)
 * @param <T>           tipo del dato contenido en la respuesta
 */
public record ApiResponse<T>(
        boolean success,
        int status,
        T data,
        List<ApiError> errors,
        ApiMetadata metadata,
        List<ApiLink> links,
        RateLimitInfo rateLimitInfo,
        PageInfo pageInfo
) {

    /**
     * Constructor compacto que realiza copias defensivas de las listas.
     *
     * @param success       indica si la respuesta fue exitosa
     * @param status        código de estado HTTP
     * @param data          dato contenido en la respuesta
     * @param errors        lista de errores
     * @param metadata      metadatos de la respuesta
     * @param links         lista de enlaces HATEOAS
     * @param rateLimitInfo información de rate limiting
     * @param pageInfo      información de paginación
     */
    public ApiResponse {
        errors = (errors == null || errors.isEmpty())
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(errors));
        links = (links == null || links.isEmpty())
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(links));
    }

    /**
     * Crea una respuesta exitosa con status 200 y datos.
     *
     * @param data dato a incluir en la respuesta
     * @param <T>  tipo del dato
     * @return ApiResponse exitoso con status 200
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, 200, data, List.of(), null, List.of(), null, null);
    }

    /**
     * Crea una respuesta exitosa con status 201 y datos.
     *
     * @param data dato creado
     * @param <T>  tipo del dato
     * @return ApiResponse exitoso con status 201
     */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, 201, data, List.of(), null, List.of(), null, null);
    }

    /**
     * Crea una respuesta exitosa con status 204 sin datos.
     *
     * @param <T> tipo del dato (inferido)
     * @return ApiResponse exitoso con status 204 y data null
     */
    public static <T> ApiResponse<T> noContent() {
        return new ApiResponse<>(true, 204, null, List.of(), null, List.of(), null, null);
    }

    /**
     * Crea una respuesta de error con status y mensaje.
     * <p>
     * La entrada lleva siempre el código {@code ERROR}, y se conserva así por compatibilidad. Para
     * responder un {@link pe.edu.nova.java.libs.api.standard.error.NovaError} conviene
     * {@link pe.edu.nova.java.libs.api.standard.error.ErrorSerializer}, que pone el código del
     * catálogo de la plataforma y el {@code traceId} (ADR-031).
     *
     * @param status  código de estado HTTP
     * @param message mensaje de error
     * @param <T>     tipo del dato (inferido)
     * @return ApiResponse de error
     */
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(false, status, null,
                List.of(ApiError.of("ERROR", message)), null, List.of(), null, null);
    }

    /**
     * Crea una respuesta de error con status y lista de errores.
     * <p>
     * Para responder un {@link pe.edu.nova.java.libs.api.standard.error.NovaError} conviene
     * {@link pe.edu.nova.java.libs.api.standard.error.ErrorSerializer} (ADR-031).
     *
     * @param status código de estado HTTP
     * @param errors lista de errores
     * @param <T>    tipo del dato (inferido)
     * @return ApiResponse de error
     */
    public static <T> ApiResponse<T> error(int status, List<ApiError> errors) {
        return new ApiResponse<>(false, status, null,
                Collections.unmodifiableList(List.copyOf(errors)), null, List.of(), null, null);
    }

    /**
     * Retorna un nuevo Builder para construir instancias de ApiResponse.
     *
     * @param <T> tipo del dato
     * @return nuevo ResponseBuilder
     */
    public static <T> ResponseBuilder<T> builder() {
        return new ResponseBuilder<>();
    }
}
