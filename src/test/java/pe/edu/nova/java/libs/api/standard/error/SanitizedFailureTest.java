package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SanitizedFailureTest {

    private static final FieldError EMAIL = FieldError.of("email", "El correo no es válido");

    @Test
    void anExpectedErrorKeepsWhatItsAuthorWroteForThePerson() {
        ApplicationError error = ApplicationError.invalidInput("ORDER_INVALID", "El pedido no es válido", List.of(EMAIL));

        SanitizedFailure failure = SanitizedFailure.of(error, 400);

        assertEquals(Layer.APPLICATION, failure.layer());
        assertEquals(Optional.of(ApplicationError.Type.INVALID_INPUT), failure.type());
        assertEquals(400, failure.status());
        assertEquals("ORDER_INVALID", failure.code());
        assertEquals("El pedido no es válido", failure.message());
        assertEquals(Optional.of("ORDER_INVALID"), failure.ownCode());
        assertEquals(Optional.of("El pedido no es válido"), failure.ownMessage());
        assertEquals(List.of(EMAIL), failure.fieldErrors());
    }

    @Test
    void withoutACodeOfItsOwnTheCodeIsTheOneOfTheStatus() {
        SanitizedFailure failure = SanitizedFailure.of(DomainError.notFound("El pedido 42 no existe"), 404);

        assertEquals("NOT_FOUND", failure.code());
        assertEquals(Optional.empty(), failure.ownCode());
        assertEquals("El pedido 42 no existe", failure.message());
    }

    static Stream<Arguments> incidents() {
        return Stream.of(
                Arguments.of(InfrastructureError.timeout("pagos", new SocketTimeoutException("Read timed out")), 504,
                        "GATEWAY_TIMEOUT", "Una dependencia no respondió a tiempo"),
                Arguments.of(InfrastructureError.unavailable("pagos", null, Duration.ofSeconds(5)), 503,
                        "SERVICE_UNAVAILABLE", "El servicio no está disponible en este momento"),
                Arguments.of(InfrastructureError.badGateway("pagos", null), 502,
                        "BAD_GATEWAY", "Una dependencia respondió con un error"),
                Arguments.of(PlatformError.internal("La conexión a db-orders-01 se cerró"), 500,
                        "INTERNAL_SERVER_ERROR", "Error interno del servidor"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("incidents")
    void anIncidentKeepsNothingOfWhatItWroteForTheLog(NovaError incident, int status, String code, String message) {
        SanitizedFailure failure = SanitizedFailure.of(incident, status);

        assertEquals(incident.layer(), failure.layer());
        assertEquals(Optional.of(incident.type()), failure.type());
        assertEquals(Optional.empty(), failure.ownCode());
        assertEquals(Optional.empty(), failure.ownMessage());
        assertEquals(List.of(), failure.fieldErrors());
        assertEquals(code, failure.code());
        assertEquals(message, failure.message());
    }

    @Test
    void anIncidentAnsweredAsAClientErrorStillKeepsNothingOfItsOwn() {
        // Un mapeador propio podría responder un incidente con un 4xx: su mensaje sigue siendo del log.
        SanitizedFailure failure = SanitizedFailure.of(PlatformError.internal("db-orders-01 caído"), 400);

        assertEquals(Optional.empty(), failure.ownMessage());
        assertEquals("BAD_REQUEST", failure.code());
        assertEquals("La solicitud no es válida", failure.message());
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {100, 399, 500, 502, 599})
    void anExpectedErrorAnsweredOutsideTheClientRangeKeepsNothingOfItsOwn(int status) {
        ApplicationError error = ApplicationError.invalidInput("ORDER_INVALID", "El pedido no es válido", List.of(EMAIL));

        SanitizedFailure failure = SanitizedFailure.of(error, status);

        assertEquals(Optional.empty(), failure.ownCode());
        assertEquals(Optional.empty(), failure.ownMessage());
        assertEquals(List.of(), failure.fieldErrors());
        assertEquals(PlatformTable.of(status).code(), failure.code());
        assertEquals(PlatformTable.of(status).message(), failure.message());
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {400, 404, 499})
    void anExpectedErrorAnsweredInTheClientRangeKeepsItsOwn(int status) {
        ApplicationError error = ApplicationError.invalidInput("ORDER_INVALID", "El pedido no es válido", List.of(EMAIL));

        SanitizedFailure failure = SanitizedFailure.of(error, status);

        assertEquals(Optional.of("ORDER_INVALID"), failure.ownCode());
        assertEquals(Optional.of("El pedido no es válido"), failure.ownMessage());
        assertEquals(List.of(EMAIL), failure.fieldErrors());
    }

    @Test
    void theWaitToRetryReachesEveryLayerThatHasOne() {
        assertEquals(Optional.of(Duration.ofSeconds(30)),
                SanitizedFailure.of(ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30)), 429)
                        .retryAfter());
        assertEquals(Optional.of(Duration.ofSeconds(5)),
                SanitizedFailure.of(InfrastructureError.unavailable("pagos", null, Duration.ofSeconds(5)), 503)
                        .retryAfter());
        assertEquals(Optional.empty(), SanitizedFailure.of(DomainError.notFound("No existe"), 404).retryAfter());
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {99, 600, -1, 0})
    void ofRejectsAStatusOutsideHttp(int status) {
        assertThrows(IllegalArgumentException.class, () -> SanitizedFailure.of(DomainError.notFound("No existe"), status));
    }

    @Test
    void ofRequiresTheError() {
        assertThrows(NullPointerException.class, () -> SanitizedFailure.of(null, 404));
    }

    @ParameterizedTest(name = "status {0} -> {1}")
    @CsvSource({
        "400, APPLICATION", "404, APPLICATION", "405, APPLICATION", "415, APPLICATION", "499, APPLICATION",
        "502, INFRASTRUCTURE", "503, INFRASTRUCTURE", "504, INFRASTRUCTURE",
        "500, PLATFORM", "501, PLATFORM", "505, PLATFORM", "599, PLATFORM"
    })
    void aFrameworkExceptionIsReadByItsStatus(int status, Layer layer) {
        SanitizedFailure failure = SanitizedFailure.ofStatus(status, null, null, null, null);

        assertEquals(layer, failure.layer());
        assertEquals(status, failure.status());
        assertEquals(Optional.empty(), failure.type());
    }

    @Test
    void aFrameworkClientFailureKeepsItsCodeItsMessageAndItsFieldErrors() {
        FieldError wholeObject = FieldError.of("", "La fecha de fin debe ser posterior a la de inicio");

        SanitizedFailure failure = SanitizedFailure.ofStatus(400, "BAD_FORM", "El formulario no es válido",
                List.of(EMAIL, wholeObject), Duration.ofSeconds(2));

        assertEquals("BAD_FORM", failure.code());
        assertEquals("El formulario no es válido", failure.message());
        assertEquals(Optional.of("BAD_FORM"), failure.ownCode());
        assertEquals(List.of(EMAIL, wholeObject), failure.fieldErrors());
        assertEquals(Optional.of(Duration.ofSeconds(2)), failure.retryAfter());
    }

    @Test
    void aFrameworkServerFailureKeepsNothingOfItsOwn() {
        SanitizedFailure failure = SanitizedFailure.ofStatus(503, "DB_DOWN", "No conecta con db-orders-01",
                List.of(EMAIL), null);

        assertEquals(Optional.empty(), failure.ownCode());
        assertEquals(Optional.empty(), failure.ownMessage());
        assertEquals(List.of(), failure.fieldErrors());
        assertEquals("SERVICE_UNAVAILABLE", failure.code());
        assertEquals("El servicio no está disponible en este momento", failure.message());
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {0, 200, 302, 399, 600})
    void ofStatusRejectsAStatusThatIsNotAnError(int status) {
        assertThrows(IllegalArgumentException.class, () -> SanitizedFailure.ofStatus(status, null, null, null, null));
    }

    @Test
    void aBlankCodeOrMessageCountsAsAbsent() {
        SanitizedFailure failure = SanitizedFailure.ofStatus(404, "  ", "", null, null);

        assertEquals(Optional.empty(), failure.ownCode());
        assertEquals(Optional.empty(), failure.ownMessage());
        assertEquals("NOT_FOUND", failure.code());
        assertEquals("El recurso no existe", failure.message());
    }

    @Test
    void aNegativeWaitIsDiscardedInAFrameworkFailureToo() {
        assertEquals(Optional.empty(),
                SanitizedFailure.ofStatus(429, null, null, null, Duration.ofSeconds(-1)).retryAfter());
        assertEquals(Optional.of(Duration.ZERO),
                SanitizedFailure.ofStatus(429, null, null, null, Duration.ZERO).retryAfter());
    }

    @Test
    void theFieldErrorsAreAnImmutableCopy() {
        List<FieldError> fields = new ArrayList<>(List.of(EMAIL));
        SanitizedFailure failure = SanitizedFailure.ofStatus(400, null, null, fields, null);

        fields.add(FieldError.of("name", "El nombre es obligatorio"));

        assertEquals(List.of(EMAIL), failure.fieldErrors());
        assertThrows(UnsupportedOperationException.class, () -> failure.fieldErrors().add(EMAIL));
    }

    @Test
    void theTraceIdOfBirthWinsOverTheOneOfTheRequestThatAnswers() {
        DomainError error = FakeTraceIdSource.withTraceId("al-nacer", () -> DomainError.notFound("No existe"));

        SanitizedFailure failure = FakeTraceIdSource.withTraceId("al-responder", () -> SanitizedFailure.of(error, 404));

        assertEquals(Optional.of("al-nacer"), failure.traceId());
    }

    @Test
    void anErrorBornWithoutTraceTakesTheOneOfTheRequestThatAnswers() {
        // Nació fuera de la petición, por ejemplo en otro hilo; al responder el contexto sí está.
        DomainError error = DomainError.notFound("No existe");

        SanitizedFailure failure = FakeTraceIdSource.withTraceId("al-responder", () -> SanitizedFailure.of(error, 404));

        assertEquals(Optional.of("al-responder"), failure.traceId());
    }

    @Test
    void aFrameworkFailureTakesTheTraceIdOfTheRequestInFlight() {
        SanitizedFailure failure = FakeTraceIdSource.withTraceId("en-curso",
                () -> SanitizedFailure.ofStatus(405, null, null, null, null));

        assertEquals(Optional.of("en-curso"), failure.traceId());
    }

    @Test
    void withoutAnyTraceThereIsNone() {
        assertEquals(Optional.empty(), SanitizedFailure.of(DomainError.notFound("No existe"), 404).traceId());
        assertEquals(Optional.empty(), SanitizedFailure.ofStatus(404, null, null, null, null).traceId());
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {504, 500, 400, 404})
    void neitherTheProviderNorTheCauseSurvivesInAnyAccessor(int status) {
        InfrastructureError error = InfrastructureError.timeout("pagos-core",
                new SocketTimeoutException("pagos-core.interno:8443 Read timed out"));
        PlatformError defect = PlatformError.internal("La conexión a db-orders-01 se cerró",
                new IllegalStateException("db-orders-01"));

        for (NovaError incident : List.of(error, defect)) {
            SanitizedFailure failure = SanitizedFailure.of(incident, status);
            List<String> everything = List.of(failure.toString(), failure.code(), failure.message(),
                    failure.layer().toString(), failure.type().toString(), failure.ownCode().toString(),
                    failure.ownMessage().toString(), failure.fieldErrors().toString(),
                    failure.retryAfter().toString(), failure.traceId().toString());

            everything.forEach(text -> {
                assertFalse(text.contains("pagos-core"), text);
                assertFalse(text.contains("8443"), text);
                assertFalse(text.contains("Read timed out"), text);
                assertFalse(text.contains("db-orders-01"), text);
            });
        }
    }

    @Test
    void itHasNoWayToReachTheProviderNorTheCause() {
        for (Method method : SanitizedFailure.class.getDeclaredMethods()) {
            assertFalse(Throwable.class.isAssignableFrom(method.getReturnType()), method.toString());
            assertFalse(NovaError.class.isAssignableFrom(method.getReturnType()), method.toString());
            assertFalse(mentionsTheProviderOrTheCause(method.getName()), method.toString());
        }
        for (Field field : SanitizedFailure.class.getDeclaredFields()) {
            assertFalse(Throwable.class.isAssignableFrom(field.getType()), field.toString());
            assertFalse(NovaError.class.isAssignableFrom(field.getType()), field.toString());
            assertFalse(mentionsTheProviderOrTheCause(field.getName()), field.toString());
        }
    }

    @Test
    void theClassCannotBeBuiltOutsideItsFactories() {
        assertTrue(Arrays.stream(SanitizedFailure.class.getDeclaredConstructors())
                .allMatch(constructor -> Modifier.isPrivate(constructor.getModifiers())));
    }

    private static boolean mentionsTheProviderOrTheCause(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("upstream") || lower.contains("cause") || lower.contains("provider");
    }
}
