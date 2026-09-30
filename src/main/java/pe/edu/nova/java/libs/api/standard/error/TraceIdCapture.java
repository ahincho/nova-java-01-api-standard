package pe.edu.nova.java.libs.api.standard.error;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Function;

/**
 * Toma el {@code traceId} de la petición en curso de las {@link TraceIdSource} registradas.
 * <p>
 * Nunca lanza: corre dentro del constructor de un {@link NovaError}, y una falla aquí reemplazaría
 * al error que se estaba construyendo por otro que nadie esperaba.
 */
final class TraceIdCapture {

    /** Logger de la plataforma; sin dependencias, llega al backend de logging del servicio. */
    private static final Logger LOGGER = System.getLogger(TraceIdCapture.class.getName());

    private TraceIdCapture() {
    }

    /**
     * El {@code traceId} de la petición en curso, según las fuentes registradas.
     *
     * @return el identificador, o vacío si ninguna fuente tiene uno
     */
    static Optional<String> current() {
        return current(Discovered.SOURCES);
    }

    /**
     * El {@code traceId} que da la primera fuente con un valor.
     *
     * @param sources las fuentes, en orden
     * @return el identificador, o vacío si ninguna tiene uno
     */
    static Optional<String> current(List<TraceIdSource> sources) {
        for (TraceIdSource source : sources) {
            Optional<String> traceId = read(source);
            if (traceId.isPresent()) {
                return traceId;
            }
        }
        return Optional.empty();
    }

    /**
     * Busca las fuentes registradas en un class loader.
     * <p>
     * Una fuente mal registrada, como una clase que no existe o que no implementa la interfaz, se
     * salta y queda en el log; las demás se cargan igual.
     *
     * @param classLoader el class loader donde buscar
     * @return las fuentes encontradas, en el orden del {@link ServiceLoader}
     */
    static List<TraceIdSource> discover(ClassLoader classLoader) {
        List<TraceIdSource> sources = new ArrayList<>();
        try {
            Iterator<TraceIdSource> iterator = ServiceLoader.load(TraceIdSource.class, classLoader).iterator();
            while (iterator.hasNext()) {
                try {
                    sources.add(iterator.next());
                } catch (ServiceConfigurationError broken) {
                    LOGGER.log(Level.WARNING, "Se ignora una TraceIdSource mal registrada", broken);
                }
            }
        } catch (ServiceConfigurationError broken) {
            LOGGER.log(Level.WARNING, "No se pudieron buscar las TraceIdSource registradas", broken);
        }
        return List.copyOf(sources);
    }

    /**
     * Lee una fuente sin dejar que su falla escape.
     *
     * @param source la fuente
     * @return el identificador, o vacío si la fuente no tiene uno, devuelve un valor en blanco o
     *         lanza
     */
    private static Optional<String> read(TraceIdSource source) {
        try {
            return Optional.ofNullable(source.currentTraceId())
                    .flatMap(Function.identity())
                    .filter(traceId -> !traceId.isBlank());
        } catch (RuntimeException failure) {
            LOGGER.log(Level.WARNING, () -> "La TraceIdSource " + source.getClass().getName()
                    + " falló; el error queda sin traceId", failure);
            return Optional.empty();
        }
    }

    /**
     * Las fuentes del class loader de esta librería, buscadas una sola vez: un error nace en cada
     * petición que falla, y recorrer el classpath en cada una no tiene sentido.
     */
    private static final class Discovered {

        /** Las fuentes registradas junto a esta librería. */
        static final List<TraceIdSource> SOURCES = discover(TraceIdSource.class.getClassLoader());

        private Discovered() {
        }
    }
}
