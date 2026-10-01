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
 * Recibe el {@link SanitizedFailure} con el status, el código y el mensaje ya decididos por el
 * {@link ErrorStatusMapper} y el {@link ErrorCatalog}: en un 5xx, el código y el mensaje genéricos del
 * status. No recibe el proveedor ni la causa, que van solo al log, así que un serializador propio no
 * puede revelarlos. Lo alcanzan igual un error de Nova y la excepción de un framework.
 */
@FunctionalInterface
public interface ErrorSerializer {

    /**
     * Construye la respuesta de un fallo.
     *
     * @param failure el fallo saneado, con el status, el código y el mensaje decididos
     * @return el status, el cuerpo y los headers que la integración escribe
     */
    SerializedError serialize(SanitizedFailure failure);
}
