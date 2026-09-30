package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class NovaErrorCatalogTest {

    private static final String GENERIC_MESSAGE = "Error interno del servidor";

    private final ErrorCatalog catalog = new NovaErrorCatalog();

    @Test
    void aClientErrorWithItsOwnCodeKeepsItAndItsMessage() {
        CatalogEntry entry = catalog.describe(DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe"), 404);

        assertEquals(new CatalogEntry("ORDER_NOT_FOUND", "El pedido 42 no existe"), entry);
    }

    @Test
    void aClientErrorWithoutCodeTakesThePlatformCodeOfItsStatus() {
        CatalogEntry entry = catalog.describe(DomainError.conflict("El pedido está cancelado"), 409);

        assertEquals(new CatalogEntry("CONFLICT", "El pedido está cancelado"), entry);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "400, BAD_REQUEST",
        "401, UNAUTHORIZED",
        "403, FORBIDDEN",
        "404, NOT_FOUND",
        "405, METHOD_NOT_ALLOWED",
        "406, NOT_ACCEPTABLE",
        "408, REQUEST_TIMEOUT",
        "409, CONFLICT",
        "410, GONE",
        "415, UNSUPPORTED_MEDIA_TYPE",
        "422, UNPROCESSABLE_ENTITY",
        "429, TOO_MANY_REQUESTS",
        "500, INTERNAL_SERVER_ERROR",
        "502, BAD_GATEWAY",
        "503, SERVICE_UNAVAILABLE",
        "504, GATEWAY_TIMEOUT"
    })
    void thePlatformCodesAreTheTableOfTheAdr(int status, String code) {
        assertEquals(code, NovaErrorCatalog.platformCode(status));
    }

    @ParameterizedTest
    @ValueSource(ints = {402, 407, 411, 418, 451, 499})
    void anyOtherClientStatusIsARequestError(int status) {
        assertEquals("REQUEST_ERROR", NovaErrorCatalog.platformCode(status));
    }

    @ParameterizedTest
    @ValueSource(ints = {501, 505, 507, 599, 200, 302})
    void anyOtherStatusIsAnInternalServerError(int status) {
        assertEquals("INTERNAL_SERVER_ERROR", NovaErrorCatalog.platformCode(status));
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {400, 499})
    void theWholeClientRangeShowsTheOwnCodeAndMessage(int status) {
        DomainError error = DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe");

        assertEquals(new CatalogEntry("ORDER_NOT_FOUND", "El pedido 42 no existe"), catalog.describe(error, status));
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = {399, 500, 599})
    void nothingOutsideTheClientRangeShowsTheOwnCodeNorTheMessage(int status) {
        // Los bordes: 399 tampoco es un 4xx, y el detalle del error solo sale en un 4xx.
        DomainError error = DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe");

        assertEquals(new CatalogEntry(NovaErrorCatalog.platformCode(status), GENERIC_MESSAGE),
                catalog.describe(error, status));
    }

    @Test
    void aServerErrorNeverShowsTheUpstreamNorTheMessage() {
        InfrastructureError timeout =
                InfrastructureError.timeout("pagos", new SocketTimeoutException("pagos.interno:8443"));

        assertEquals(new CatalogEntry("GATEWAY_TIMEOUT", GENERIC_MESSAGE), catalog.describe(timeout, 504));
        assertEquals(new CatalogEntry("SERVICE_UNAVAILABLE", GENERIC_MESSAGE),
                catalog.describe(InfrastructureError.unavailable("pagos", null), 503));
    }

    @Test
    void aServerErrorNeverShowsTheOwnCodeEvenIfTheErrorBringsOne() {
        // Un mapeador propio podría llevar un error de negocio a 500: el catálogo no lo delata.
        DomainError error = DomainError.ruleViolation("CREDIT_LIMIT_EXCEEDED", "Supera el crédito del cliente");

        assertEquals(new CatalogEntry("INTERNAL_SERVER_ERROR", GENERIC_MESSAGE), catalog.describe(error, 500));
    }

    @Test
    void aPlatformErrorGetsTheGenericMessage() {
        PlatformError error = PlatformError.internal(new IllegalStateException("el pool está cerrado"));

        assertEquals(new CatalogEntry("INTERNAL_SERVER_ERROR", GENERIC_MESSAGE), catalog.describe(error, 500));
    }
}
