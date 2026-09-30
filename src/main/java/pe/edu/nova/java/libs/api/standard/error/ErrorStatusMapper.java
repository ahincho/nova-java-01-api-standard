package pe.edu.nova.java.libs.api.standard.error;

/**
 * Puerto que decide el status HTTP de cada capa y tipo de error.
 * <p>
 * Nova trae {@link NovaErrorStatusMapper}, la tabla de ADR-031. Una organización pone el suyo sin
 * forkear, o envuelve el de Nova para cambiar un solo caso: en Spring Boot con un bean propio de
 * este tipo, porque el de Nova es {@code @ConditionalOnMissingBean}; en Quarkus con un bean propio,
 * porque el de Nova es {@code @DefaultBean}.
 * <p>
 * Se consulta solo para los errores de Nova. Una excepción propia de un framework ya trae su status, y
 * la integración lo respeta.
 */
@FunctionalInterface
public interface ErrorStatusMapper {

    /**
     * Retorna el status HTTP con que se responde el error.
     *
     * @param error el error
     * @return el status HTTP, entre 100 y 599
     */
    int statusOf(NovaError error);
}
