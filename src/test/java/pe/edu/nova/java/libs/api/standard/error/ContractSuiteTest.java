package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.bodyOf;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyCodeOf;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyMessageOf;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * La suite de contrato de ADR-031, al nivel del modelo y del serializador.
 * <p>
 * Los tres stacks corren estos mismos casos. En todos: {@code success: false}, {@code status} igual
 * al HTTP y {@code metadata.traceId} presente, el de la petición en la que nació el error.
 */
class ContractSuiteTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    private final ErrorSerializer serializer = new NovaErrorSerializer();

    @Test
    void domainNotFoundWithItsOwnCode() {
        SerializedError serialized = respond(() -> DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe"));

        assertContract(404, serialized);
        assertEquals("ORDER_NOT_FOUND", onlyCodeOf(serialized));
        assertEquals("El pedido 42 no existe", onlyMessageOf(serialized));
    }

    @Test
    void domainConflictWithoutCode() {
        SerializedError serialized = respond(() -> DomainError.conflict("El pedido está cancelado"));

        assertContract(409, serialized);
        assertEquals("CONFLICT", onlyCodeOf(serialized));
    }

    @Test
    void applicationInvalidInputWithTwoFields() {
        SerializedError serialized = respond(() -> ApplicationError.invalidInput("La solicitud tiene campos inválidos",
                List.of(FieldError.of("email", "El correo no es válido"),
                        FieldError.of("quantity", "La cantidad debe ser positiva"))));

        assertContract(400, serialized);
        List<ApiError> errors = bodyOf(serialized).errors();
        assertEquals(List.of("email", "quantity"), errors.stream().map(ApiError::field).toList());
        assertEquals(List.of("BAD_REQUEST", "BAD_REQUEST"), errors.stream().map(ApiError::code).toList());
    }

    @Test
    void applicationConflictWithRetryAfterOfOneSecond() {
        SerializedError serialized =
                respond(() -> ApplicationError.conflict("La operación sigue en curso", Duration.ofSeconds(1)));

        assertContract(409, serialized);
        assertEquals("CONFLICT", onlyCodeOf(serialized));
        assertEquals(Map.of("Retry-After", "1"), serialized.headers());
    }

    @Test
    void applicationRateLimitedWithThirtySeconds() {
        SerializedError serialized =
                respond(() -> ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30)));

        assertContract(429, serialized);
        assertEquals("TOO_MANY_REQUESTS", onlyCodeOf(serialized));
        assertEquals(Map.of("Retry-After", "30"), serialized.headers());
    }

    @Test
    void infrastructureTimeoutOfAnUpstream() {
        InfrastructureError error = FakeTraceIdSource.withTraceId(TRACE_ID,
                () -> InfrastructureError.timeout("pagos", new SocketTimeoutException("Read timed out")));

        SerializedError serialized = serializer.serialize(error);

        assertContract(504, serialized);
        assertEquals("GATEWAY_TIMEOUT", onlyCodeOf(serialized));
        // El cuerpo no nombra al proveedor; el log sí, porque el error lo conserva.
        assertFalse(bodyOf(serialized).toString().contains("pagos"));
        assertEquals(Optional.of("pagos"), error.upstream());
    }

    @Test
    void infrastructureUnavailable() {
        SerializedError serialized = respond(() -> InfrastructureError.unavailable("pagos", null));

        assertContract(503, serialized);
        assertEquals("SERVICE_UNAVAILABLE", onlyCodeOf(serialized));
    }

    @Test
    void anyOtherException() {
        // La integración envuelve lo que no es un NovaError en un PlatformError.
        SerializedError serialized = respond(
                () -> PlatformError.internal(new IllegalStateException("Conexión rechazada por db-orders-01")));

        assertContract(500, serialized);
        assertEquals("INTERNAL_SERVER_ERROR", onlyCodeOf(serialized));
        assertEquals("Error interno del servidor", onlyMessageOf(serialized));
    }

    @Test
    void aCatalogOfItsOwnRegisteredByTheService() {
        ErrorCatalog own = (error, status) -> new CatalogEntry("SVC_" + NovaErrorCatalog.platformCode(status),
                "Mensaje del catálogo propio");
        ErrorSerializer withOwnCatalog = new NovaErrorSerializer(new NovaErrorStatusMapper(), own);
        DomainError error = FakeTraceIdSource.withTraceId(TRACE_ID,
                () -> DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe"));

        SerializedError serialized = withOwnCatalog.serialize(error);

        assertContract(404, serialized);
        assertEquals("SVC_NOT_FOUND", onlyCodeOf(serialized));
        assertEquals("Mensaje del catálogo propio", onlyMessageOf(serialized));
    }

    /** Crea el error dentro de una petición y lo responde fuera de ella, como una integración. */
    private SerializedError respond(Supplier<NovaError> error) {
        return serializer.serialize(FakeTraceIdSource.withTraceId(TRACE_ID, error));
    }

    private static void assertContract(int status, SerializedError serialized) {
        ApiResponse<?> body = bodyOf(serialized);
        assertEquals(status, serialized.status());
        assertFalse(body.success());
        assertEquals(status, body.status());
        assertEquals(TRACE_ID, body.metadata().traceId());
        assertFalse(body.errors().isEmpty(), "el sobre de un error lleva al menos una entrada");
    }
}
