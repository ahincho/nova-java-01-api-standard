package pe.edu.nova.java.libs.api.standard.error;

/**
 * Lo que el cliente ve de un error, según el {@link ErrorCatalog}: su código y su mensaje.
 *
 * @param code    código que ve el cliente, como {@code ORDER_NOT_FOUND} o {@code GATEWAY_TIMEOUT}
 *                (obligatorio)
 * @param message mensaje que ve el cliente (obligatorio)
 */
public record CatalogEntry(String code, String message) {

    /**
     * Constructor compacto con validación.
     *
     * @throws IllegalArgumentException si code es nulo o en blanco
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    public CatalogEntry {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code es obligatorio");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es obligatorio");
        }
    }
}
