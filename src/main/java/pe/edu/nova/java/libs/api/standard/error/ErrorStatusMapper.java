package pe.edu.nova.java.libs.api.standard.error;

/**
 * Puerto que decide el status HTTP de cada capa y tipo de error.
 * <p>
 * Nova trae {@link NovaErrorStatusMapper}, la tabla de ADR-031. Una organización pone el suyo sin
 * forkear, o envuelve el de Nova para cambiar un solo caso: en Spring Boot con un bean propio de
 * este tipo, porque el de Nova es {@code @ConditionalOnMissingBean}; en Quarkus con un bean propio,
 * porque el de Nova es {@code @DefaultBean}.
 * <p>
 * Recibe solo la clasificación, que es todo lo que necesita y todo lo que un mapeador puede ver del
 * error: el status es lo que decide, así que no puede recibir un {@link SanitizedFailure}, que ya lo
 * trae. Tampoco ve el proveedor, la causa ni ningún mensaje.
 * <p>
 * Se consulta solo para los errores de Nova. Una excepción propia de un framework ya trae su status, y
 * la integración lo respeta al armar su {@link SanitizedFailure#ofStatus fallo saneado}.
 */
@FunctionalInterface
public interface ErrorStatusMapper {

    /**
     * Retorna el status HTTP con que se responde un error de este tipo.
     *
     * @param type el tipo del error, que conoce su {@link ErrorType#layer() capa}
     * @return el status HTTP, entre 100 y 599
     */
    int statusOf(ErrorType type);
}
