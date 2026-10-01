package pe.edu.nova.java.libs.api.standard.error;

import java.util.Map;

/**
 * La tabla del catálogo de la plataforma (ADR-031): qué código y qué mensaje genérico lleva cada status.
 * <p>
 * La usan dos piezas, y por eso vive aparte: el {@link SanitizedFailure}, que arma el código y el
 * mensaje con los que llega cada fallo a los puertos, y el {@link NovaErrorCatalog}, que es la
 * implementación de Nova del puerto. Así la regla existe una sola vez.
 * <p>
 * Un 4xx lleva el código y el mensaje propios del error si los trae, y si no, los de su status. Un 5xx
 * lleva siempre los de su status: el código propio, el mensaje y el proveedor cuentan qué falló por
 * dentro, y eso va al log.
 */
final class PlatformCatalog {

    /** Código y mensaje de un 4xx que la tabla no nombra. */
    private static final CatalogEntry REQUEST_ERROR =
            new CatalogEntry("REQUEST_ERROR", "La solicitud no se pudo atender");

    /** Código y mensaje del 500, que también responde a cualquier otro 5xx que la tabla no nombra. */
    private static final CatalogEntry INTERNAL_SERVER_ERROR =
            new CatalogEntry("INTERNAL_SERVER_ERROR", "Error interno del servidor");

    /** La tabla, por status. */
    private static final Map<Integer, CatalogEntry> ENTRIES = Map.ofEntries(
            named(400, "BAD_REQUEST", "La solicitud no es válida"),
            named(401, "UNAUTHORIZED", "Hace falta autenticarse"),
            named(403, "FORBIDDEN", "No hay permiso para esta operación"),
            named(404, "NOT_FOUND", "El recurso no existe"),
            named(405, "METHOD_NOT_ALLOWED", "El método no está permitido en este recurso"),
            named(406, "NOT_ACCEPTABLE", "No hay una representación en el formato pedido"),
            named(408, "REQUEST_TIMEOUT", "La solicitud tardó demasiado en llegar"),
            named(409, "CONFLICT", "La operación choca con el estado actual del recurso"),
            named(410, "GONE", "El recurso ya no está disponible"),
            named(415, "UNSUPPORTED_MEDIA_TYPE", "El tipo de contenido no está soportado"),
            named(422, "UNPROCESSABLE_ENTITY", "La solicitud no se puede procesar"),
            named(429, "TOO_MANY_REQUESTS", "Demasiadas solicitudes; conviene esperar antes de reintentar"),
            Map.entry(500, INTERNAL_SERVER_ERROR),
            named(502, "BAD_GATEWAY", "Una dependencia respondió con un error"),
            named(503, "SERVICE_UNAVAILABLE", "El servicio no está disponible en este momento"),
            named(504, "GATEWAY_TIMEOUT", "Una dependencia no respondió a tiempo"));

    private PlatformCatalog() {
    }

    private static Map.Entry<Integer, CatalogEntry> named(int status, String code, String message) {
        return Map.entry(status, new CatalogEntry(code, message));
    }

    /**
     * El código y el mensaje de la plataforma para un status, sin mirar ningún error.
     *
     * @param status el status HTTP
     * @return la entrada de la tabla; {@code REQUEST_ERROR} para otro 4xx, y
     *         {@code INTERNAL_SERVER_ERROR} para cualquier otro status
     */
    static CatalogEntry entryOf(int status) {
        CatalogEntry named = ENTRIES.get(status);
        if (named != null) {
            return named;
        }
        return isClientError(status) ? REQUEST_ERROR : INTERNAL_SERVER_ERROR;
    }

    /**
     * Lo que ve el cliente: lo propio del error en un 4xx que lo trae, y lo de la plataforma en el resto.
     *
     * @param status     el status con que se responde
     * @param ownCode    el código propio del error, o null si no trae o no puede mostrarse
     * @param ownMessage el mensaje propio del error, o null si no trae o no puede mostrarse
     * @return el código y el mensaje de la respuesta
     */
    static CatalogEntry describe(int status, String ownCode, String ownMessage) {
        CatalogEntry platform = entryOf(status);
        if (!isClientError(status)) {
            return platform;
        }
        return new CatalogEntry(ownCode != null ? ownCode : platform.code(),
                ownMessage != null ? ownMessage : platform.message());
    }

    /**
     * Indica si el status es 4xx, el único rango que muestra lo propio del error.
     *
     * @param status el status HTTP
     * @return {@code true} si está entre 400 y 499
     */
    static boolean isClientError(int status) {
        return status >= 400 && status < 500;
    }
}
