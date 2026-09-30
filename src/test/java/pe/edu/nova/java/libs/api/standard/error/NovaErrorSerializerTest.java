package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.bodyOf;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyCodeOf;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyMessageOf;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

class NovaErrorSerializerTest {

    private final ErrorPorts ports = ErrorPorts.defaults();

    @Test
    void theBodyIsAFailedEnvelopeWithTheHttpStatus() {
        SerializedError serialized = ports.respond(DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe"));
        ApiResponse<?> body = bodyOf(serialized);

        assertEquals(404, serialized.status());
        assertFalse(body.success());
        assertEquals(404, body.status());
        assertNull(body.data());
        assertEquals(List.of(ApiError.of("ORDER_NOT_FOUND", "El pedido 42 no existe")), body.errors());
    }

    @ParameterizedTest(name = "{0} -> {1} {2}")
    @MethodSource("pe.edu.nova.java.libs.api.standard.error.AdrTable#rows")
    void eachTypeAnswersWithTheStatusAndCodeOfTheAdr(NovaError error, int status, String code) {
        SerializedError serialized = ports.respond(error);

        assertEquals(status, serialized.status());
        assertEquals(status, bodyOf(serialized).status());
        assertEquals(List.of(code), bodyOf(serialized).errors().stream().map(ApiError::code).distinct().toList());
    }

    @Test
    void invalidInputHasOneEntryPerFieldWithItsField() {
        ApplicationError error = ApplicationError.invalidInput("La solicitud tiene campos inválidos", List.of(
                FieldError.of("email", "El correo no es válido"),
                FieldError.of("name", "REQUIRED", "El nombre es obligatorio")));

        ApiResponse<?> body = bodyOf(ports.respond(error));

        // Un campo sin código propio toma el del catálogo; uno con código conserva el suyo.
        assertEquals(List.of(
                ApiError.of("BAD_REQUEST", "El correo no es válido", "email"),
                ApiError.of("REQUIRED", "El nombre es obligatorio", "name")), body.errors());
    }

    @Test
    void aFieldWithoutCodeTakesTheOwnCodeOfTheError() {
        ApplicationError error = ApplicationError.invalidInput("INVALID_ORDER", "El pedido no es válido",
                List.of(FieldError.of("quantity", "La cantidad debe ser positiva")));

        assertEquals(List.of(ApiError.of("INVALID_ORDER", "La cantidad debe ser positiva", "quantity")),
                bodyOf(ports.respond(error)).errors());
    }

    @Test
    void invalidInputWithoutFieldsHasASingleEntryWithItsMessage() {
        SerializedError serialized = ports.respond(ApplicationError.invalidInput("El cuerpo no es legible", null));

        assertEquals(400, serialized.status());
        assertEquals("BAD_REQUEST", onlyCodeOf(serialized));
        assertEquals("El cuerpo no es legible", onlyMessageOf(serialized));
    }

    @Test
    void fieldErrorsNeverReachAServerError() {
        // Un mapeador propio que lleve INVALID_INPUT a 500 no puede filtrar el detalle por campo.
        ErrorPorts toServerError = new ErrorPorts(type -> 500, new NovaErrorCatalog(), new NovaErrorSerializer());
        ApplicationError error = ApplicationError.invalidInput("Campos inválidos",
                List.of(FieldError.of("cardNumber", "El número 4111 no es válido")));

        SerializedError serialized = toServerError.respond(error);

        assertEquals("INTERNAL_SERVER_ERROR", onlyCodeOf(serialized));
        assertEquals("Error interno del servidor", onlyMessageOf(serialized));
        assertNull(bodyOf(serialized).errors().getFirst().field());
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {400, 499})
    void fieldErrorsAreListedInTheWholeClientRange(int status) {
        ErrorPorts atStatus = new ErrorPorts(type -> status, new NovaErrorCatalog(), new NovaErrorSerializer());

        List<ApiError> entries = bodyOf(atStatus.respond(twoInvalidFields())).errors();

        assertEquals(List.of("email", "name"), entries.stream().map(ApiError::field).toList());
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {399, 599})
    void fieldErrorsAreNotListedOutsideTheClientRange(int status) {
        // Los bordes del rango: solo un 4xx muestra el detalle de la entrada.
        ErrorPorts atStatus = new ErrorPorts(type -> status, new NovaErrorCatalog(), new NovaErrorSerializer());

        List<ApiError> entries = bodyOf(atStatus.respond(twoInvalidFields())).errors();

        assertEquals(1, entries.size());
        assertNull(entries.getFirst().field());
    }

    private static ApplicationError twoInvalidFields() {
        return ApplicationError.invalidInput("Campos inválidos", List.of(
                FieldError.of("email", "El correo no es válido"),
                FieldError.of("name", "El nombre es obligatorio")));
    }

    @ParameterizedTest(name = "{0} -> Retry-After: {1}")
    @CsvSource({
        "PT1S, 1",
        "PT30S, 30",
        "PT0S, 0",
        "PT1.5S, 2",
        "PT0.000000001S, 1",
        "PT2M, 120"
    })
    void retryAfterBecomesAHeaderInWholeSecondsRoundedUp(Duration retryAfter, String header) {
        SerializedError serialized = ports.respond(ApplicationError.rateLimited("Superaste el límite", retryAfter));

        assertEquals(Map.of("Retry-After", header), serialized.headers());
    }

    @Test
    void aNegativeWaitStillAnswersTheErrorWithoutTheHeader() {
        // El 429 se responde igual: una espera calculada mal no puede convertirlo en un 500.
        SerializedError serialized =
                ports.respond(ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(-3)));

        assertEquals(429, serialized.status());
        assertEquals("TOO_MANY_REQUESTS", onlyCodeOf(serialized));
        assertTrue(serialized.headers().isEmpty());
    }

    @Test
    void anErrorThatCannotBeRetriedHasNoHeaders() {
        assertTrue(ports.respond(DomainError.conflict("El pedido está cancelado")).headers().isEmpty());
        assertTrue(ports.respond(ApplicationError.rateLimited("Superaste el límite", null)).headers().isEmpty());
    }

    @Test
    void unavailableCarriesRetryAfterToo() {
        SerializedError serialized =
                ports.respond(InfrastructureError.unavailable("pagos", null, Duration.ofSeconds(5)));

        assertEquals(503, serialized.status());
        assertEquals(Map.of("Retry-After", "5"), serialized.headers());
    }

    @Test
    void theBodyNeverNamesTheUpstream() {
        InfrastructureError error =
                InfrastructureError.timeout("pagos-core", new SocketTimeoutException("pagos-core.interno:8443"));

        ApiResponse<?> body = bodyOf(ports.respond(error));

        assertEquals(List.of(ApiError.of("GATEWAY_TIMEOUT", "Una dependencia no respondió a tiempo")), body.errors());
        assertFalse(body.toString().contains("pagos"), body::toString);
        assertTrue(body.metadata().customFields().isEmpty());
    }

    @Test
    void theMetadataCarriesTheTraceIdCapturedAtBirth() {
        DomainError error = FakeTraceIdSource.withTraceId("4bf92f3577b34da6", () -> DomainError.notFound("No existe"));

        // Al responder ya no hay petición en curso: vale lo que se capturó al nacer.
        assertEquals("4bf92f3577b34da6", bodyOf(ports.respond(error)).metadata().traceId());
    }

    @Test
    void theTraceIdOfBirthWinsOverTheOneAtResponseTime() {
        DomainError error = FakeTraceIdSource.withTraceId("al-nacer", () -> DomainError.notFound("No existe"));

        SerializedError serialized = FakeTraceIdSource.withTraceId("al-responder", () -> ports.respond(error));

        assertEquals("al-nacer", bodyOf(serialized).metadata().traceId());
    }

    @Test
    void anErrorBornWithoutTraceTakesTheOneOfTheResponse() {
        // Nació fuera de la petición, por ejemplo en otro hilo; al responder el contexto sí está.
        DomainError error = DomainError.notFound("No existe");

        SerializedError serialized = FakeTraceIdSource.withTraceId("al-responder", () -> ports.respond(error));

        assertEquals("al-responder", bodyOf(serialized).metadata().traceId());
    }

    @Test
    void withoutAnyTraceTheMetadataStillCarriesOne() {
        String traceId = bodyOf(ports.respond(DomainError.notFound("No existe"))).metadata().traceId();

        assertNotNull(traceId);
        assertFalse(traceId.isBlank());
    }

    @Test
    void thePortsAreAllRequired() {
        NovaErrorStatusMapper mapper = new NovaErrorStatusMapper();
        NovaErrorCatalog catalog = new NovaErrorCatalog();
        NovaErrorSerializer serializer = new NovaErrorSerializer();

        assertThrows(NullPointerException.class, () -> new ErrorPorts(null, catalog, serializer));
        assertThrows(NullPointerException.class, () -> new ErrorPorts(mapper, null, serializer));
        assertThrows(NullPointerException.class, () -> new ErrorPorts(mapper, catalog, null));
        assertThrows(NullPointerException.class, () -> ports.respond((NovaError) null));
        assertThrows(NullPointerException.class, () -> ports.respond((SanitizedFailure) null));
    }
}
