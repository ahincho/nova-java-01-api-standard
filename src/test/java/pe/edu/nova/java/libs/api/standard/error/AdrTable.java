package pe.edu.nova.java.libs.api.standard.error;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * La tabla de capas y tipos de ADR-031, fila por fila: un error de cada tipo sin código propio, el
 * status con que responde y el código del catálogo que ve el cliente.
 */
final class AdrTable {

    private AdrTable() {
    }

    static Stream<Arguments> rows() {
        return Stream.of(
                Arguments.of(DomainError.notFound("El pedido no existe"), 404, "NOT_FOUND"),
                Arguments.of(DomainError.conflict("El pedido está cancelado"), 409, "CONFLICT"),
                Arguments.of(DomainError.ruleViolation("Supera el crédito"), 422, "UNPROCESSABLE_ENTITY"),
                Arguments.of(ApplicationError.invalidInput("Campos inválidos",
                        List.of(FieldError.of("email", "El correo no es válido"))), 400, "BAD_REQUEST"),
                Arguments.of(ApplicationError.conflict("La clave sigue en uso", Duration.ofSeconds(1)), 409, "CONFLICT"),
                Arguments.of(ApplicationError.unprocessable("La clave se reusó con otro contenido"), 422,
                        "UNPROCESSABLE_ENTITY"),
                Arguments.of(ApplicationError.unauthenticated("Falta el token"), 401, "UNAUTHORIZED"),
                Arguments.of(ApplicationError.forbidden("Sin permiso"), 403, "FORBIDDEN"),
                Arguments.of(ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30)), 429,
                        "TOO_MANY_REQUESTS"),
                Arguments.of(InfrastructureError.unavailable("pagos", null), 503, "SERVICE_UNAVAILABLE"),
                Arguments.of(InfrastructureError.timeout("pagos", null), 504, "GATEWAY_TIMEOUT"),
                Arguments.of(InfrastructureError.badGateway("pagos", null), 502, "BAD_GATEWAY"),
                Arguments.of(PlatformError.internal(new IllegalStateException("boom")), 500, "INTERNAL_SERVER_ERROR"));
    }
}
