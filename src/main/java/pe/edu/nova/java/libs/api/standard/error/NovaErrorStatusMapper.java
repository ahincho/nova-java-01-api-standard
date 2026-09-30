package pe.edu.nova.java.libs.api.standard.error;

/**
 * Implementación de Nova de {@link ErrorStatusMapper}: la tabla de capas y tipos de ADR-031.
 *
 * <table>
 *   <caption>Status de cada capa y tipo</caption>
 *   <tr><th>Capa</th><th>Tipo</th><th>HTTP</th></tr>
 *   <tr><td>domain</td><td>NOT_FOUND</td><td>404</td></tr>
 *   <tr><td>domain</td><td>CONFLICT</td><td>409</td></tr>
 *   <tr><td>domain</td><td>RULE_VIOLATION</td><td>422</td></tr>
 *   <tr><td>application</td><td>INVALID_INPUT</td><td>400</td></tr>
 *   <tr><td>application</td><td>CONFLICT</td><td>409</td></tr>
 *   <tr><td>application</td><td>UNPROCESSABLE</td><td>422</td></tr>
 *   <tr><td>application</td><td>UNAUTHENTICATED</td><td>401</td></tr>
 *   <tr><td>application</td><td>FORBIDDEN</td><td>403</td></tr>
 *   <tr><td>application</td><td>RATE_LIMITED</td><td>429</td></tr>
 *   <tr><td>infrastructure</td><td>UNAVAILABLE</td><td>503</td></tr>
 *   <tr><td>infrastructure</td><td>TIMEOUT</td><td>504</td></tr>
 *   <tr><td>infrastructure</td><td>BAD_GATEWAY</td><td>502</td></tr>
 *   <tr><td>platform</td><td>INTERNAL</td><td>500</td></tr>
 * </table>
 */
public final class NovaErrorStatusMapper implements ErrorStatusMapper {

    /** Crea el mapeador con la tabla de Nova. */
    public NovaErrorStatusMapper() {
    }

    /**
     * Retorna el status de la tabla para la capa y el tipo del error.
     *
     * @param error el error
     * @return el status HTTP
     */
    @Override
    public int statusOf(NovaError error) {
        // El switch es exhaustivo sobre los tipos sellados: un tipo nuevo no compila hasta tener su fila.
        return switch (error.type()) {
            case DomainError.Type.NOT_FOUND -> 404;
            case DomainError.Type.CONFLICT -> 409;
            case DomainError.Type.RULE_VIOLATION -> 422;
            case ApplicationError.Type.INVALID_INPUT -> 400;
            case ApplicationError.Type.CONFLICT -> 409;
            case ApplicationError.Type.UNPROCESSABLE -> 422;
            case ApplicationError.Type.UNAUTHENTICATED -> 401;
            case ApplicationError.Type.FORBIDDEN -> 403;
            case ApplicationError.Type.RATE_LIMITED -> 429;
            case InfrastructureError.Type.UNAVAILABLE -> 503;
            case InfrastructureError.Type.TIMEOUT -> 504;
            case InfrastructureError.Type.BAD_GATEWAY -> 502;
            case PlatformError.Type.INTERNAL -> 500;
        };
    }
}
