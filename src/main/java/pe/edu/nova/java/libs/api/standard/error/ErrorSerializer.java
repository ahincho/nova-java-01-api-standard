package pe.edu.nova.java.libs.api.standard.error;

/**
 * Puerto que decide el cuerpo y los headers de la respuesta de un error.
 * <p>
 * Nova trae {@link NovaErrorSerializer}: el sobre de Nova, con {@code metadata.traceId} y el header
 * {@code Retry-After}. Es lo que escribe cada integración, como el starter de Spring Boot o la
 * extensión de Quarkus, que solo copia el resultado a la respuesta de su framework. Otro formato,
 * como RFC 7807, es otra implementación de este puerto; también se puede envolver el de Nova para
 * sumar un header.
 * <p>
 * La regla del 5xx pasa a ser de quien escribe el puerto: el error trae el proveedor y la causa, que
 * son solo para el log. Con un serializador propio, el código y el mensaje de un 5xx se toman del
 * {@link ErrorCatalog}, y no del error.
 */
@FunctionalInterface
public interface ErrorSerializer {

    /**
     * Construye la respuesta de un error.
     *
     * @param error el error
     * @return el status, el cuerpo y los headers que la integración escribe
     */
    SerializedError serialize(NovaError error);
}
