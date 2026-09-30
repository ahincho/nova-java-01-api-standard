package pe.edu.nova.java.libs.api.standard.error;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * El fallo saneado: lo único que ven los puertos de un error (ADR-031, con el reparto de ADR-034).
 * <p>
 * El {@link NovaError} completo lo ve solo el núcleo, que en Java es el handler de la integración. Ese
 * handler lo registra en el log una sola vez, con el proveedor y la causa, y recién entonces llama a
 * {@link ErrorPorts}, que arma este fallo. Un puerto propio, como el de una organización, no puede
 * revelar lo que nunca recibe:
 * <ul>
 *   <li>no lleva el proveedor ({@link NovaError#upstream()}) ni la causa;</li>
 *   <li>el código propio, el mensaje propio y los errores por campo solo llegan si el status es 4xx y el
 *       error es de {@code domain} o {@code application}, que escriben para la persona. El mensaje de un
 *       incidente ({@code infrastructure} o {@code platform}) cuenta qué falló por dentro, y no llega
 *       nunca, sea cual sea el status con que se lo responda;</li>
 *   <li>en un 5xx, {@link #message()} es el mensaje genérico del status y {@link #code()} es el código
 *       del catálogo de la plataforma;</li>
 *   <li>la espera para reintentar y el {@code traceId} sí llegan: son para el cliente.</li>
 * </ul>
 * Una excepción propia de un framework entra por {@link #ofStatus}, con su status y su capa y sin tipo:
 * el {@link ErrorStatusMapper} solo se consulta para los errores de Nova.
 * <p>
 * {@link #code()} y {@link #message()} son lo que vería el cliente con los valores de la plataforma:
 * lo propio si el error es un 4xx que lo trae, y si no, el código y el mensaje de su status. El
 * {@link ErrorCatalog} puede cambiarlos, y el {@link ErrorSerializer} recibe los que quedaron. Un
 * catálogo que necesita distinguir lo propio de lo que puso la plataforma tiene {@link #ownCode()} y
 * {@link #ownMessage()}.
 */
public final class SanitizedFailure {

    /** Capa del error. */
    private final Layer layer;

    /** Tipo del error; null en una excepción del framework. */
    private final ErrorType type;

    /** Status con que se responde. */
    private final int status;

    /** Código que vería el cliente. */
    private final String code;

    /** Mensaje que vería el cliente. */
    private final String message;

    /** Código propio del error, solo si puede llegar al cliente (puede ser null). */
    private final String ownCode;

    /** Mensaje propio del error, solo si puede llegar al cliente (puede ser null). */
    private final String ownMessage;

    /** Errores por campo, solo en un 4xx. */
    private final List<FieldError> fieldErrors;

    /** Cuánto esperar antes de reintentar (puede ser null). */
    private final Duration retryAfter;

    /** Identificador de traza de la petición (puede ser null). */
    private final String traceId;

    /**
     * Lo propio del error que puede llegar al cliente.
     *
     * @param code    código propio (puede ser null)
     * @param message mensaje propio (puede ser null)
     */
    private record Own(String code, String message) {

        /** Nada propio: lo que queda de un 5xx o de un incidente. */
        static final Own NONE = new Own(null, null);
    }

    /**
     * Constructor privado: se crea con las fábricas.
     *
     * @param layer       capa del error
     * @param type        tipo del error (puede ser null)
     * @param status      status con que se responde
     * @param own         lo propio que puede llegar al cliente
     * @param fieldErrors errores por campo que pueden llegar al cliente
     * @param retryAfter  cuánto esperar antes de reintentar (puede ser null)
     * @param traceId     identificador de traza (puede ser null)
     */
    private SanitizedFailure(Layer layer, ErrorType type, int status, Own own,
                             List<FieldError> fieldErrors, Duration retryAfter, String traceId) {
        CatalogEntry platform = PlatformCatalog.describe(status, own.code(), own.message());
        this.layer = layer;
        this.type = type;
        this.status = status;
        this.code = platform.code();
        this.message = platform.message();
        this.ownCode = own.code();
        this.ownMessage = own.message();
        this.fieldErrors = fieldErrors;
        this.retryAfter = retryAfter;
        this.traceId = traceId;
    }

    /**
     * Copia de otro fallo con el código y el mensaje que decidió el catálogo.
     *
     * @param base  el fallo
     * @param entry lo que decidió el {@link ErrorCatalog}
     */
    private SanitizedFailure(SanitizedFailure base, CatalogEntry entry) {
        this.layer = base.layer;
        this.type = base.type;
        this.status = base.status;
        this.code = entry.code();
        this.message = entry.message();
        this.ownCode = base.ownCode;
        this.ownMessage = base.ownMessage;
        this.fieldErrors = base.fieldErrors;
        this.retryAfter = base.retryAfter;
        this.traceId = base.traceId;
    }

    /**
     * Sanea un error de Nova para el status que decidió el {@link ErrorStatusMapper}.
     * <p>
     * El {@code traceId} es el que el error capturó al nacer; si nació fuera de la petición, el de la
     * petición en curso, y si tampoco hay, ninguno.
     *
     * @param error  el error, completo
     * @param status el status con que se responde, entre 100 y 599
     * @return el fallo que ven los puertos
     * @throws NullPointerException     si error es nulo
     * @throws IllegalArgumentException si status está fuera de 100 a 599
     */
    public static SanitizedFailure of(NovaError error, int status) {
        Objects.requireNonNull(error, "error es obligatorio");
        requireStatus(status, 100);
        // Lo propio solo llega en un 4xx de una capa que escribe para la persona: el mensaje de un
        // incidente cuenta qué falló por dentro, aunque un mapeador propio lo responda con un 4xx.
        boolean shown = PlatformCatalog.isClientError(status) && !error.layer().isIncident();
        Own own = shown ? new Own(blankToNull(error.code().orElse(null)), blankToNull(error.getMessage())) : Own.NONE;
        return new SanitizedFailure(error.layer(), error.type(), status, own,
                shown ? error.fieldErrors() : List.of(),
                error.retryAfter().orElse(null),
                error.traceId().or(TraceIdCapture::current).orElse(null));
    }

    /**
     * Sanea una excepción propia de un framework, que ya trae su status: un 404, un 405 o un 415, por
     * ejemplo. No tiene tipo, y su capa sale del status: un 4xx es {@code application}; un 502, 503 o
     * 504, {@code infrastructure}; y cualquier otro 5xx, {@code platform}.
     * <p>
     * El código, el mensaje y los errores por campo solo llegan en un 4xx; en un 5xx se descartan y el
     * fallo lleva el código y el mensaje genéricos del status. El {@code traceId} es el de la petición
     * en curso.
     *
     * @param status      el status de la excepción, entre 400 y 599
     * @param code        código propio (puede ser null o en blanco)
     * @param message     mensaje propio (puede ser null o en blanco)
     * @param fieldErrors errores por campo (puede ser null)
     * @param retryAfter  cuánto esperar antes de reintentar (puede ser null; una espera negativa se
     *                    descarta)
     * @return el fallo que ven los puertos
     * @throws IllegalArgumentException si status está fuera de 400 a 599
     */
    public static SanitizedFailure ofStatus(int status, String code, String message,
                                            List<FieldError> fieldErrors, Duration retryAfter) {
        requireStatus(status, 400);
        boolean shown = PlatformCatalog.isClientError(status);
        Own own = shown ? new Own(blankToNull(code), blankToNull(message)) : Own.NONE;
        return new SanitizedFailure(layerOf(status), null, status, own,
                shown && fieldErrors != null ? List.copyOf(fieldErrors) : List.of(),
                retryAfter == null || retryAfter.isNegative() ? null : retryAfter,
                TraceIdCapture.current().orElse(null));
    }

    /**
     * Retorna la capa del error.
     *
     * @return capa del error
     */
    public Layer layer() {
        return layer;
    }

    /**
     * Retorna el tipo del error dentro de su capa.
     *
     * @return el tipo, o vacío si el fallo viene de una excepción del framework
     */
    public Optional<ErrorType> type() {
        return Optional.ofNullable(type);
    }

    /**
     * Retorna el status con que se responde.
     *
     * @return status HTTP
     */
    public int status() {
        return status;
    }

    /**
     * Retorna el código que vería el cliente: el propio si el error es un 4xx que lo trae, y si no, el
     * del catálogo de la plataforma para el status.
     *
     * @return el código, nunca vacío
     */
    public String code() {
        return code;
    }

    /**
     * Retorna el mensaje que vería el cliente: el propio si el error es un 4xx que lo trae, y si no, el
     * genérico del status. En un 5xx es siempre el genérico.
     *
     * @return el mensaje, nunca vacío
     */
    public String message() {
        return message;
    }

    /**
     * Retorna el código propio del error, tal como lo escribió quien lo lanzó.
     *
     * @return el código propio, o vacío si el error no lo trae o no puede mostrarse (un 5xx o un
     *         incidente)
     */
    public Optional<String> ownCode() {
        return Optional.ofNullable(ownCode);
    }

    /**
     * Retorna el mensaje propio del error, tal como lo escribió quien lo lanzó.
     *
     * @return el mensaje propio, o vacío si el error no lo trae o no puede mostrarse (un 5xx o un
     *         incidente)
     */
    public Optional<String> ownMessage() {
        return Optional.ofNullable(ownMessage);
    }

    /**
     * Retorna los errores por campo. Un {@link FieldError} con el campo vacío es del objeto entero.
     *
     * @return lista inmutable, vacía en un 5xx y en todo lo que no sea una entrada inválida
     */
    public List<FieldError> fieldErrors() {
        return fieldErrors;
    }

    /**
     * Retorna cuánto esperar antes de reintentar.
     *
     * @return la espera, o vacío si el error no se puede reintentar
     */
    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }

    /**
     * Retorna el identificador de traza de la petición.
     *
     * @return el traceId, o vacío si no hay ninguno
     */
    public Optional<String> traceId() {
        return Optional.ofNullable(traceId);
    }

    /**
     * Retorna una copia con el código y el mensaje que decidió el {@link ErrorCatalog}; es lo que recibe
     * el {@link ErrorSerializer}.
     *
     * @param entry lo que decidió el catálogo
     * @return el fallo con el código y el mensaje del catálogo
     */
    SanitizedFailure decidedBy(CatalogEntry entry) {
        return new SanitizedFailure(this, Objects.requireNonNull(entry, "entry es obligatorio"));
    }

    @Override
    public String toString() {
        return "SanitizedFailure{layer=" + layer.label()
                + ", type=" + (type == null ? null : type.name())
                + ", status=" + status
                + ", code=" + code
                + ", message=" + message
                + ", fieldErrors=" + fieldErrors
                + ", retryAfter=" + retryAfter
                + ", traceId=" + traceId
                + '}';
    }

    /**
     * La capa de una excepción del framework según su status.
     *
     * @param status el status, de 400 a 599
     * @return {@code application} para un 4xx, {@code infrastructure} para un 502, 503 o 504, y
     *         {@code platform} para cualquier otro 5xx
     */
    private static Layer layerOf(int status) {
        if (PlatformCatalog.isClientError(status)) {
            return Layer.APPLICATION;
        }
        return status == 502 || status == 503 || status == 504 ? Layer.INFRASTRUCTURE : Layer.PLATFORM;
    }

    /**
     * Valida que el status esté entre el mínimo y 599.
     *
     * @param status el status
     * @param min    el menor status válido
     * @throws IllegalArgumentException si el status está fuera del rango
     */
    private static void requireStatus(int status, int min) {
        if (status < min || status > 599) {
            throw new IllegalArgumentException(
                    "status debe estar en el rango " + min + "-599, recibido: " + status);
        }
    }

    /**
     * Un texto en blanco se trata como ausente.
     *
     * @param text el texto (puede ser null)
     * @return el mismo texto, o null si es nulo o está en blanco
     */
    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
