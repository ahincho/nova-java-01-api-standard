package pe.edu.nova.java.libs.api.standard.error;

import java.io.Serial;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Base común de los errores de Nova (ADR-031): un error se clasifica por capa y tipo, y el mapeo a
 * HTTP lo hace la plataforma, no quien lo lanza.
 * <p>
 * Hay una clase por capa, con su tipo como enumeración y una fábrica por tipo:
 * {@link DomainError}, {@link ApplicationError}, {@link InfrastructureError} y
 * {@link PlatformError}. La jerarquía es cerrada, así que ningún servicio suma una capa.
 * <p>
 * El modelo no importa ningún framework web: el mismo caso de uso corre detrás de HTTP o de un
 * consumidor de cola. Qué llega al cliente lo deciden los puertos {@link ErrorStatusMapper},
 * {@link ErrorCatalog} y {@link ErrorSerializer}:
 * <ul>
 *   <li>el código propio y el mensaje, solo si la respuesta es 4xx;</li>
 *   <li>los errores por campo, en {@code INVALID_INPUT};</li>
 *   <li>{@link #retryAfter()}, como header {@code Retry-After} en segundos;</li>
 *   <li>el {@link #traceId()}, en {@code metadata.traceId}.</li>
 * </ul>
 * El proveedor ({@link #upstream()}) y la causa nunca llegan al cliente: son para el log.
 * <p>
 * Los puertos tampoco reciben este error. Lo ve solo el núcleo, que en Java es el handler de la
 * integración: lo registra en el log una sola vez, con el proveedor y la causa, y {@link ErrorPorts} les
 * pasa a los puertos un {@link SanitizedFailure}, que no los trae.
 * <p>
 * El {@code traceId} se toma en el constructor, de las {@link TraceIdSource} registradas, y no al
 * responder, cuando el contexto de la petición puede haberse perdido.
 */
public abstract sealed class NovaError extends RuntimeException
        permits DomainError, ApplicationError, InfrastructureError, PlatformError {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Código propio del error, como {@code ORDER_NOT_FOUND} (puede ser null). */
    private final String code;

    /** Errores por campo; solo los lleva {@code INVALID_INPUT}. */
    private final List<FieldError> fieldErrors;

    /** Cuánto esperar antes de reintentar (puede ser null; nunca es negativo). */
    private final Duration retryAfter;

    /** La dependencia que falló, solo para el log (puede ser null). */
    private final String upstream;

    /** Identificador de traza de la petición en la que nació el error (puede ser null). */
    private final String traceId;

    /**
     * Constructor con visibilidad de paquete: solo lo usan las cuatro clases de capa.
     *
     * @param code        código propio del error; en blanco se trata como ausente
     * @param message     mensaje del error
     * @param fieldErrors errores por campo (puede ser null)
     * @param retryAfter  cuánto esperar antes de reintentar (puede ser null; una espera negativa se
     *                    descarta)
     * @param upstream    la dependencia que falló (puede ser null)
     * @param cause       la excepción original (puede ser null)
     */
    NovaError(String code, String message, List<FieldError> fieldErrors,
              Duration retryAfter, String upstream, Throwable cause) {
        super(message, cause);
        this.code = code == null || code.isBlank() ? null : code;
        this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
        // Una espera negativa sale de un cálculo del servicio, como un reloj desfasado, y no de lo que
        // escribe quien lanza. Lanzar aquí reemplazaría el error que se iba a responder, un 429 por
        // ejemplo, por otro que nadie esperaba, así que se descarta y el error sale sin el header.
        this.retryAfter = retryAfter == null || retryAfter.isNegative() ? null : retryAfter;
        this.upstream = upstream;
        // Se captura al nacer: al responder, el contexto de la petición puede ya no estar.
        this.traceId = TraceIdCapture.current().orElse(null);
    }

    /**
     * Valida el mensaje que escribe quien lanza un error esperado.
     *
     * @param message mensaje del error
     * @return el mismo mensaje
     * @throws IllegalArgumentException si message es nulo o en blanco
     */
    static String requireMessage(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message es obligatorio");
        }
        return message;
    }

    /**
     * Retorna el tipo del error dentro de su capa.
     *
     * @return tipo del error
     */
    public abstract ErrorType type();

    /**
     * Retorna la capa del error.
     *
     * @return capa del error
     */
    public Layer layer() {
        return type().layer();
    }

    /**
     * Retorna el código propio del error, como {@code ORDER_NOT_FOUND}.
     * <p>
     * Llega al cliente solo si la respuesta es 4xx; si falta, el {@link ErrorCatalog} pone el de la
     * plataforma.
     *
     * @return código propio, o vacío si el error no lo trae
     */
    public Optional<String> code() {
        return Optional.ofNullable(code);
    }

    /**
     * Retorna los errores por campo.
     *
     * @return lista inmutable, vacía salvo en {@code INVALID_INPUT}
     */
    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }

    /**
     * Retorna cuánto esperar antes de reintentar.
     *
     * @return la espera, o vacío si el error no se puede reintentar o si la espera que recibió era
     *         negativa
     */
    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    /**
     * Retorna la dependencia que falló. Va solo al log, en el campo {@code upstream}.
     *
     * @return el nombre del proveedor, o vacío si el error no es de {@code infrastructure}
     */
    public Optional<String> upstream() {
        return Optional.ofNullable(upstream);
    }

    /**
     * Retorna el identificador de traza de la petición en la que nació el error.
     *
     * @return el traceId, o vacío si no había ninguna {@link TraceIdSource} con un valor
     */
    public Optional<String> traceId() {
        return Optional.ofNullable(traceId);
    }
}
