package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class NovaErrorCatalogTest {

    private final ErrorCatalog catalog = new NovaErrorCatalog();

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @MethodSource("pe.edu.nova.java.libs.api.standard.error.PlatformTable#allRows")
    void aFailureWithNothingOfItsOwnGetsTheCodeAndTheGenericMessageOfItsStatus(
            int status, String code, String message) {
        SanitizedFailure failure = SanitizedFailure.ofStatus(status, null, null, null, null);

        assertEquals(new CatalogEntry(code, message), catalog.describe(failure));
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {402, 407, 411, 418, 451, 499})
    void anyOtherClientStatusIsARequestError(int status) {
        SanitizedFailure failure = SanitizedFailure.ofStatus(status, null, null, null, null);

        assertEquals(new CatalogEntry("REQUEST_ERROR", "La solicitud no se pudo atender"),
                catalog.describe(failure));
        assertEquals("REQUEST_ERROR", NovaErrorCatalog.platformCode(status));
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {501, 505, 507, 599})
    void anyOtherServerStatusIsAnInternalServerError(int status) {
        SanitizedFailure failure = SanitizedFailure.ofStatus(status, null, null, null, null);

        assertEquals(new CatalogEntry("INTERNAL_SERVER_ERROR", "Error interno del servidor"),
                catalog.describe(failure));
        assertEquals("INTERNAL_SERVER_ERROR", NovaErrorCatalog.platformCode(status));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("pe.edu.nova.java.libs.api.standard.error.PlatformTable#allRows")
    void thePlatformCodesAreTheTableOfTheAdr(int status, String code, String message) {
        assertEquals(code, NovaErrorCatalog.platformCode(status));
    }

    @Test
    void aClientErrorWithItsOwnCodeAndMessageKeepsBoth() {
        SanitizedFailure failure = SanitizedFailure.of(
                DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe"), 404);

        assertEquals(new CatalogEntry("ORDER_NOT_FOUND", "El pedido 42 no existe"), catalog.describe(failure));
    }

    @Test
    void aClientErrorWithoutCodeTakesTheCodeOfItsStatusAndKeepsItsMessage() {
        SanitizedFailure failure = SanitizedFailure.of(DomainError.conflict("El pedido está cancelado"), 409);

        assertEquals(new CatalogEntry("CONFLICT", "El pedido está cancelado"), catalog.describe(failure));
    }

    @Test
    void aClientFailureWithoutAMessageOfItsOwnCarriesTheMessageOfItsCode() {
        SanitizedFailure withCode = SanitizedFailure.ofStatus(404, "ORDER_NOT_FOUND", null, null, null);
        SanitizedFailure withMessage = SanitizedFailure.ofStatus(404, null, "No hay ningún pedido 42", null, null);

        assertEquals(new CatalogEntry("ORDER_NOT_FOUND", "El recurso no existe"), catalog.describe(withCode));
        assertEquals(new CatalogEntry("NOT_FOUND", "No hay ningún pedido 42"), catalog.describe(withMessage));
    }

    @ParameterizedTest(name = "{0} -> {1}: {2}")
    @MethodSource("pe.edu.nova.java.libs.api.standard.error.PlatformTable#serverRows")
    void aServerErrorAlwaysCarriesTheGenericOfItsStatusWhateverItBrings(int status, String code, String message) {
        // Un mapeador propio podría llevar un error de negocio a un 5xx: el catálogo no lo delata.
        DomainError error = DomainError.ruleViolation("CREDIT_LIMIT_EXCEEDED", "Supera el crédito del cliente");

        assertEquals(new CatalogEntry(code, message), catalog.describe(SanitizedFailure.of(error, status)));
        assertEquals(new CatalogEntry(code, message),
                catalog.describe(SanitizedFailure.ofStatus(status, "OWN_CODE", "Mensaje propio", null, null)));
    }

    @Test
    void aServerErrorNeverShowsTheUpstreamNorTheMessage() {
        InfrastructureError timeout =
                InfrastructureError.timeout("pagos", new SocketTimeoutException("pagos.interno:8443"));

        CatalogEntry entry = catalog.describe(SanitizedFailure.of(timeout, 504));

        assertEquals(new CatalogEntry("GATEWAY_TIMEOUT", "Una dependencia no respondió a tiempo"), entry);
        assertFalse(entry.toString().contains("pagos"), entry::toString);
    }

    @Test
    void anIncidentAnsweredAsAClientErrorNeverShowsItsMessage() {
        // El mensaje de un incidente cuenta qué falló por dentro, sea cual sea el status con que se lo responda.
        PlatformError incident = PlatformError.internal("La conexión a db-orders-01 se cerró");

        CatalogEntry entry = catalog.describe(SanitizedFailure.of(incident, 400));

        assertEquals(new CatalogEntry("BAD_REQUEST", "La solicitud no es válida"), entry);
    }

    @Test
    void itDescribesFromWhatIsOwnEvenIfAnotherCatalogAlreadyDecided() {
        SanitizedFailure failure = SanitizedFailure.of(DomainError.notFound("El pedido 42 no existe"), 404);
        SanitizedFailure decidedByAnother = failure.decidedBy(new CatalogEntry("ORG-404", "Texto de otro"));

        assertEquals(new CatalogEntry("NOT_FOUND", "El pedido 42 no existe"), catalog.describe(decidedByAnother));
    }

    @Test
    void theTableHasTheSixteenRowsOfTheAdr() {
        assertEquals(List.of(400, 401, 403, 404, 405, 406, 408, 409, 410, 415, 422, 429, 500, 502, 503, 504),
                PlatformTable.NAMED.stream().map(PlatformTable.Row::status).toList());
    }
}
