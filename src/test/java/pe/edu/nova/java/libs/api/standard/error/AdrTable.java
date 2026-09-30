package pe.edu.nova.java.libs.api.standard.error;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * La tabla de capas y tipos de ADR-031, fila por fila: un error de cada tipo sin código propio, el
 * status con que responde, el código del catálogo que ve el cliente y el mensaje del fallo.
 * <p>
 * Un error de {@code domain} o {@code application} lleva el mensaje que escribió quien lo lanzó. Uno de
 * {@code infrastructure} o {@code platform} lleva el genérico de su status, escrito aquí a mano.
 */
final class AdrTable {

    private AdrTable() {
    }

    /**
     * Las 13 filas.
     *
     * @return el error, su status, su código y su mensaje
     */
    static Stream<Arguments> rows() {
        return Stream.of(
                Arguments.of(DomainError.notFound("El pedido no existe"), 404, "NOT_FOUND",
                        "El pedido no existe"),
                Arguments.of(DomainError.conflict("El pedido está cancelado"), 409, "CONFLICT",
                        "El pedido está cancelado"),
                Arguments.of(DomainError.ruleViolation("Supera el crédito"), 422, "UNPROCESSABLE_ENTITY",
                        "Supera el crédito"),
                Arguments.of(ApplicationError.invalidInput("Campos inválidos",
                        List.of(FieldError.of("email", "El correo no es válido"))), 400, "BAD_REQUEST",
                        "Campos inválidos"),
                Arguments.of(ApplicationError.conflict("La clave sigue en uso", Duration.ofSeconds(1)), 409,
                        "CONFLICT", "La clave sigue en uso"),
                Arguments.of(ApplicationError.unprocessable("La clave se reusó con otro contenido"), 422,
                        "UNPROCESSABLE_ENTITY", "La clave se reusó con otro contenido"),
                Arguments.of(ApplicationError.unauthenticated("Falta el token"), 401, "UNAUTHORIZED",
                        "Falta el token"),
                Arguments.of(ApplicationError.forbidden("Sin permiso"), 403, "FORBIDDEN", "Sin permiso"),
                Arguments.of(ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30)), 429,
                        "TOO_MANY_REQUESTS", "Superaste el límite"),
                Arguments.of(InfrastructureError.unavailable("pagos", null), 503, "SERVICE_UNAVAILABLE",
                        "El servicio no está disponible en este momento"),
                Arguments.of(InfrastructureError.timeout("pagos", null), 504, "GATEWAY_TIMEOUT",
                        "Una dependencia no respondió a tiempo"),
                Arguments.of(InfrastructureError.badGateway("pagos", null), 502, "BAD_GATEWAY",
                        "Una dependencia respondió con un error"),
                Arguments.of(PlatformError.internal(new IllegalStateException("boom")), 500,
                        "INTERNAL_SERVER_ERROR", "Error interno del servidor"));
    }
}
