package pe.edu.nova.java.libs.api.standard.error;

import java.util.Optional;

/**
 * De dónde sale el {@code traceId} de la petición en curso, sin que el modelo de errores conozca el
 * framework.
 * <p>
 * Cada integración, como el starter de Spring Boot o la extensión de Quarkus, trae la suya y la
 * registra en {@code META-INF/services/pe.edu.nova.java.libs.api.standard.error.TraceIdSource}. Un
 * {@link NovaError} la consulta al construirse, así que el identificador es el del contexto vigente
 * en el instante en que nace el error. Sin ninguna fuente registrada el error queda sin
 * {@code traceId}, nunca con una excepción.
 * <p>
 * Las fuentes se buscan una sola vez, con {@link java.util.ServiceLoader} y en el class loader de
 * esta librería: una fuente vive en una dependencia, junto a esta, como el starter o la extensión.
 * Si hay varias, gana la primera que devuelva un valor.
 * <p>
 * Una fuente se llama cada vez que nace un error, desde cualquier hilo, así que tiene que ser
 * barata, segura entre hilos y no lanzar. Si lanza igual, el error nace sin {@code traceId} y la
 * falla queda en el log.
 */
@FunctionalInterface
public interface TraceIdSource {

    /**
     * Retorna el {@code traceId} de la petición que atiende el hilo actual.
     *
     * @return el identificador, o vacío si no hay una petición en curso
     */
    Optional<String> currentTraceId();
}
