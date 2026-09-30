package pe.edu.nova.java.libs.api.standard.error;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Fuente de traceId de las pruebas, registrada en
 * {@code src/test/resources/META-INF/services} como lo hará el starter de cada framework.
 * <p>
 * Imita el contexto de una petición con un {@link ThreadLocal}, igual que el MDC: fuera de
 * {@link #withTraceId} no hay ninguna petición en curso.
 */
public final class FakeTraceIdSource implements TraceIdSource {

    /** Lo que contesta la fuente en el hilo actual; null fuera de una petición. */
    private static final ThreadLocal<Supplier<Optional<String>>> BEHAVIOR = new ThreadLocal<>();

    /** {@link java.util.ServiceLoader} pide un constructor público sin argumentos. */
    public FakeTraceIdSource() {
    }

    /**
     * Corre el bloque como si el hilo atendiera la petición con ese traceId.
     */
    static <T> T withTraceId(String traceId, Supplier<T> block) {
        return with(() -> Optional.of(traceId), block);
    }

    /**
     * Corre el bloque con una fuente que lanza, como un contexto de trazas roto.
     */
    static <T> T failing(Supplier<T> block) {
        return with(() -> {
            throw new IllegalStateException("el contexto de trazas no está listo");
        }, block);
    }

    private static <T> T with(Supplier<Optional<String>> behavior, Supplier<T> block) {
        BEHAVIOR.set(behavior);
        try {
            return block.get();
        } finally {
            BEHAVIOR.remove();
        }
    }

    @Override
    public Optional<String> currentTraceId() {
        Supplier<Optional<String>> behavior = BEHAVIOR.get();
        return behavior == null ? Optional.empty() : behavior.get();
    }
}
