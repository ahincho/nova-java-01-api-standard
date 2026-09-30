package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

/**
 * Las reglas del catálogo de Nova, sobre todos los status y no solo sobre los de la tabla.
 */
class NovaErrorCatalogPropertyTest {

    private static final Set<Integer> CLIENT_STATUSES_IN_TABLE =
            Set.of(400, 401, 403, 404, 405, 406, 408, 409, 410, 415, 422, 429);

    private static final Set<Integer> SERVER_STATUSES_IN_TABLE = Set.of(500, 502, 503, 504);

    private final ErrorCatalog catalog = new NovaErrorCatalog();

    /**
     * Un 5xx lleva el código de su status y el mensaje genérico, traiga lo que traiga el error.
     */
    @Property(tries = 200)
    void aServerStatusNeverExposesTheError(
            @ForAll @IntRange(min = 500, max = 599) int status,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String ownCode,
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String message) {
        CatalogEntry entry = catalog.describe(DomainError.notFound("OWN_" + ownCode, message), status);

        assertEquals(NovaErrorCatalog.platformCode(status), entry.code());
        assertEquals("Error interno del servidor", entry.message());
        assertNotEquals("OWN_" + ownCode, entry.code());
    }

    /**
     * Un 4xx conserva el código propio y el mensaje del error.
     */
    @Property(tries = 200)
    void aClientStatusKeepsTheOwnCodeAndMessage(
            @ForAll @IntRange(min = 400, max = 499) int status,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String ownCode,
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String message) {
        CatalogEntry entry = catalog.describe(ApplicationError.forbidden(ownCode, message), status);

        assertEquals(new CatalogEntry(ownCode, message), entry);
    }

    /**
     * Un 4xx que no está en la tabla lleva {@code REQUEST_ERROR}.
     */
    @Property(tries = 100)
    void aClientStatusOutsideTheTableIsARequestError(@ForAll("clientStatusesOutsideTheTable") int status) {
        assertEquals("REQUEST_ERROR", NovaErrorCatalog.platformCode(status));
    }

    /**
     * Un 5xx que no está en la tabla lleva {@code INTERNAL_SERVER_ERROR}.
     */
    @Property(tries = 100)
    void aServerStatusOutsideTheTableIsAnInternalServerError(@ForAll("serverStatusesOutsideTheTable") int status) {
        assertEquals("INTERNAL_SERVER_ERROR", NovaErrorCatalog.platformCode(status));
    }

    @Provide
    Arbitrary<Integer> clientStatusesOutsideTheTable() {
        return Arbitraries.integers().between(400, 499).filter(status -> !CLIENT_STATUSES_IN_TABLE.contains(status));
    }

    @Provide
    Arbitrary<Integer> serverStatusesOutsideTheTable() {
        return Arbitraries.integers().between(500, 599).filter(status -> !SERVER_STATUSES_IN_TABLE.contains(status));
    }
}
