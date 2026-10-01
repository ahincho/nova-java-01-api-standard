package pe.edu.nova.java.libs.api.standard.error;

import java.util.Objects;

/**
 * Los tres puertos de ADR-031 juntos, con el orden en que se consultan.
 * <p>
 * Es la parte del núcleo que no se reemplaza: recibe el error completo, arma el {@link SanitizedFailure}
 * y les pasa a los puertos solo eso. Por eso un puerto propio no puede revelar el proveedor ni la causa:
 * nunca los ve.
 * <p>
 * El handler de la integración, que es el núcleo en Java, hace tres cosas y en este orden:
 * <ol>
 *   <li>registra el {@link NovaError} completo en el log, con el proveedor y la causa, una sola vez y
 *       antes de llamar a ningún puerto;</li>
 *   <li>llama a {@link #respond(NovaError)}, o a {@link #respond(SanitizedFailure)} con un fallo armado
 *       con {@link SanitizedFailure#ofStatus} si lo que llegó es la excepción de un framework;</li>
 *   <li>escribe en la respuesta del framework el status, el cuerpo y los headers del resultado.</li>
 * </ol>
 * En Spring Boot y en Quarkus cada puerto es un bean propio del servicio o, si no lo hay, el de Nova, y
 * este registro se arma con esos tres.
 *
 * @param statusMapper decide el status de un error de Nova
 * @param catalog      decide el código y el mensaje que ve el cliente
 * @param serializer   decide el cuerpo y los headers
 */
public record ErrorPorts(ErrorStatusMapper statusMapper, ErrorCatalog catalog, ErrorSerializer serializer) {

    /**
     * Valida que estén los tres puertos.
     *
     * @throws NullPointerException si alguno es nulo
     */
    public ErrorPorts {
        Objects.requireNonNull(statusMapper, "statusMapper es obligatorio");
        Objects.requireNonNull(catalog, "catalog es obligatorio");
        Objects.requireNonNull(serializer, "serializer es obligatorio");
    }

    /**
     * Crea el registro con la implementación de Nova de cada puerto.
     *
     * @return los tres puertos de Nova
     */
    public static ErrorPorts defaults() {
        return new ErrorPorts(new NovaErrorStatusMapper(), new NovaErrorCatalog(), new NovaErrorSerializer());
    }

    /**
     * Responde un error de Nova: el {@link ErrorStatusMapper} decide su status con solo su tipo, y
     * después {@link #respond(SanitizedFailure)} sigue con el fallo saneado.
     * <p>
     * El error completo no sale de aquí: el proveedor, la causa y el mensaje de un incidente quedan
     * para el log del handler.
     *
     * @param error el error, completo
     * @return el status, el cuerpo y los headers que la integración escribe
     * @throws NullPointerException     si error es nulo
     * @throws IllegalArgumentException si el mapeador responde un status fuera de 100 a 599
     */
    public SerializedError respond(NovaError error) {
        Objects.requireNonNull(error, "error es obligatorio");
        return respond(SanitizedFailure.of(error, statusMapper.statusOf(error.type())));
    }

    /**
     * Responde un fallo que ya tiene su status, como el de la excepción de un framework: el
     * {@link ErrorCatalog} decide el código y el mensaje, y el {@link ErrorSerializer} recibe el fallo
     * con esa decisión. El {@link ErrorStatusMapper} no se consulta.
     *
     * @param failure el fallo saneado
     * @return el status, el cuerpo y los headers que la integración escribe
     * @throws NullPointerException si failure es nulo
     */
    public SerializedError respond(SanitizedFailure failure) {
        Objects.requireNonNull(failure, "failure es obligatorio");
        CatalogEntry entry = catalog.describe(failure);
        return serializer.serialize(failure.decidedBy(entry));
    }
}
