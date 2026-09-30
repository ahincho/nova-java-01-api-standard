package pe.edu.nova.java.libs.api.standard.error;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * El catálogo de la plataforma tal como lo fija la enmienda de ADR-031, escrito aparte del código: si
 * alguien cambia un texto de {@code PlatformCatalog}, las pruebas lo notan porque esta tabla no cambia.
 */
final class PlatformTable {

    /** Una fila de la tabla: el status, su código y su mensaje. */
    record Row(int status, String code, String message) {
    }

    /** Las 16 filas con nombre. */
    static final List<Row> NAMED = List.of(
            new Row(400, "BAD_REQUEST", "La solicitud no es válida"),
            new Row(401, "UNAUTHORIZED", "Hace falta autenticarse"),
            new Row(403, "FORBIDDEN", "No hay permiso para esta operación"),
            new Row(404, "NOT_FOUND", "El recurso no existe"),
            new Row(405, "METHOD_NOT_ALLOWED", "El método no está permitido en este recurso"),
            new Row(406, "NOT_ACCEPTABLE", "No hay una representación en el formato pedido"),
            new Row(408, "REQUEST_TIMEOUT", "La solicitud tardó demasiado en llegar"),
            new Row(409, "CONFLICT", "La operación choca con el estado actual del recurso"),
            new Row(410, "GONE", "El recurso ya no está disponible"),
            new Row(415, "UNSUPPORTED_MEDIA_TYPE", "El tipo de contenido no está soportado"),
            new Row(422, "UNPROCESSABLE_ENTITY", "La solicitud no se puede procesar"),
            new Row(429, "TOO_MANY_REQUESTS", "Demasiadas solicitudes; conviene esperar antes de reintentar"),
            new Row(500, "INTERNAL_SERVER_ERROR", "Error interno del servidor"),
            new Row(502, "BAD_GATEWAY", "Una dependencia respondió con un error"),
            new Row(503, "SERVICE_UNAVAILABLE", "El servicio no está disponible en este momento"),
            new Row(504, "GATEWAY_TIMEOUT", "Una dependencia no respondió a tiempo"));

    /** Código y mensaje de cualquier otro 4xx. */
    static final Row OTHER_CLIENT_ERROR = new Row(0, "REQUEST_ERROR", "La solicitud no se pudo atender");

    /** Código y mensaje de cualquier otro 5xx. */
    static final Row OTHER_SERVER_ERROR = new Row(0, "INTERNAL_SERVER_ERROR", "Error interno del servidor");

    private PlatformTable() {
    }

    /**
     * Las 4xx con nombre, como argumentos de una prueba parametrizada.
     *
     * @return status, código y mensaje
     */
    static Stream<Arguments> clientRows() {
        return NAMED.stream().filter(row -> row.status() < 500)
                .map(row -> Arguments.of(row.status(), row.code(), row.message()));
    }

    /**
     * Las 5xx con nombre, como argumentos de una prueba parametrizada.
     *
     * @return status, código y mensaje
     */
    static Stream<Arguments> serverRows() {
        return NAMED.stream().filter(row -> row.status() >= 500)
                .map(row -> Arguments.of(row.status(), row.code(), row.message()));
    }

    /**
     * Todas las filas con nombre, como argumentos de una prueba parametrizada.
     *
     * @return status, código y mensaje
     */
    static Stream<Arguments> allRows() {
        return NAMED.stream().map(row -> Arguments.of(row.status(), row.code(), row.message()));
    }

    /**
     * Lo que responde la tabla para un status cualquiera.
     *
     * @param status el status
     * @return la fila con nombre, o la de otro 4xx o la de otro 5xx
     */
    static Row of(int status) {
        return NAMED.stream().filter(row -> row.status() == status).findFirst()
                .orElse(status >= 400 && status < 500 ? OTHER_CLIENT_ERROR : OTHER_SERVER_ERROR);
    }
}
