package pe.edu.nova.java.libs.api.standard.error;

/**
 * Implementación de Nova de {@link ErrorCatalog}, la misma en los tres stacks (ADR-031).
 * <ul>
 *   <li>Un 4xx lleva el código y el mensaje propios del error si los trae, y si no, los de su status en
 *       el catálogo de la plataforma: un 4xx sin mensaje propio lleva el de su código.</li>
 *   <li>Un 5xx lleva siempre el código y el mensaje genérico de su status. Nunca revela el código
 *       propio, el mensaje ni el proveedor: el cliente sabe si conviene reintentar, un 503 o un 504
 *       sí y un 500 no, sin conocer la topología.</li>
 * </ul>
 * <table>
 *   <caption>El catálogo de la plataforma</caption>
 *   <tr><th>Status</th><th>Código</th><th>Mensaje</th></tr>
 *   <tr><td>400</td><td>BAD_REQUEST</td><td>La solicitud no es válida</td></tr>
 *   <tr><td>401</td><td>UNAUTHORIZED</td><td>Hace falta autenticarse</td></tr>
 *   <tr><td>403</td><td>FORBIDDEN</td><td>No hay permiso para esta operación</td></tr>
 *   <tr><td>404</td><td>NOT_FOUND</td><td>El recurso no existe</td></tr>
 *   <tr><td>405</td><td>METHOD_NOT_ALLOWED</td><td>El método no está permitido en este recurso</td></tr>
 *   <tr><td>406</td><td>NOT_ACCEPTABLE</td><td>No hay una representación en el formato pedido</td></tr>
 *   <tr><td>408</td><td>REQUEST_TIMEOUT</td><td>La solicitud tardó demasiado en llegar</td></tr>
 *   <tr><td>409</td><td>CONFLICT</td><td>La operación choca con el estado actual del recurso</td></tr>
 *   <tr><td>410</td><td>GONE</td><td>El recurso ya no está disponible</td></tr>
 *   <tr><td>415</td><td>UNSUPPORTED_MEDIA_TYPE</td><td>El tipo de contenido no está soportado</td></tr>
 *   <tr><td>422</td><td>UNPROCESSABLE_ENTITY</td><td>La solicitud no se puede procesar</td></tr>
 *   <tr><td>429</td><td>TOO_MANY_REQUESTS</td><td>Demasiadas solicitudes; conviene esperar antes de
 *       reintentar</td></tr>
 *   <tr><td>500</td><td>INTERNAL_SERVER_ERROR</td><td>Error interno del servidor</td></tr>
 *   <tr><td>502</td><td>BAD_GATEWAY</td><td>Una dependencia respondió con un error</td></tr>
 *   <tr><td>503</td><td>SERVICE_UNAVAILABLE</td><td>El servicio no está disponible en este momento</td></tr>
 *   <tr><td>504</td><td>GATEWAY_TIMEOUT</td><td>Una dependencia no respondió a tiempo</td></tr>
 *   <tr><td>otro 4xx</td><td>REQUEST_ERROR</td><td>La solicitud no se pudo atender</td></tr>
 *   <tr><td>otro 5xx</td><td>INTERNAL_SERVER_ERROR</td><td>Error interno del servidor</td></tr>
 * </table>
 * Los textos están en español, como el resto de los mensajes que ve una persona. Una organización que
 * necesite otro idioma o sus propios códigos pone su {@link ErrorCatalog}, o envuelve este.
 */
public final class NovaErrorCatalog implements ErrorCatalog {

    /** Crea el catálogo de Nova. */
    public NovaErrorCatalog() {
    }

    /**
     * Retorna el código y el mensaje que ve el cliente, según las reglas de la plataforma.
     * <p>
     * Se calcula desde lo propio del fallo y su status, no desde lo que ya traen
     * {@link SanitizedFailure#code()} y {@link SanitizedFailure#message()}: así responde lo mismo aunque
     * el fallo ya haya pasado por otro catálogo.
     *
     * @param failure el fallo saneado, con el status ya decidido
     * @return la entrada del catálogo para ese fallo
     */
    @Override
    public CatalogEntry describe(SanitizedFailure failure) {
        return PlatformCatalog.describe(failure.status(),
                failure.ownCode().orElse(null), failure.ownMessage().orElse(null));
    }

    /**
     * Retorna el código del catálogo de la plataforma para un status, sin mirar ningún error.
     * <p>
     * Sirve a una integración o a un catálogo propio que quiere responder con el mismo código que
     * usaría Nova.
     *
     * @param status el status HTTP
     * @return el código de la tabla; {@code REQUEST_ERROR} para otro 4xx, y
     *         {@code INTERNAL_SERVER_ERROR} para cualquier otro status
     */
    public static String platformCode(int status) {
        return PlatformCatalog.entryOf(status).code();
    }
}
