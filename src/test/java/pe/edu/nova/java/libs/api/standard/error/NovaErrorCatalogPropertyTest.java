package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

/**
 * Las reglas del catálogo de Nova, sobre todos los status y no solo sobre los de la tabla.
 */
class NovaErrorCatalogPropertyTest {

    private final ErrorCatalog catalog = new NovaErrorCatalog();

    /**
     * Un 5xx lleva el código y el mensaje de su status, traiga lo que traiga el error.
     */
    @Property(tries = 200)
    void aServerStatusNeverExposesTheError(
            @ForAll @IntRange(min = 500, max = 599) int status,
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String ownCode,
            @ForAll @AlphaChars @StringLength(min = 1, max = 40) String message) {
        SanitizedFailure failure = SanitizedFailure.of(DomainError.notFound("OWN_" + ownCode, message), status);

        CatalogEntry entry = catalog.describe(failure);

        assertEquals(new CatalogEntry(PlatformTable.of(status).code(), PlatformTable.of(status).message()), entry);
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
        SanitizedFailure failure = SanitizedFailure.of(ApplicationError.forbidden(ownCode, message), status);

        assertEquals(new CatalogEntry(ownCode, message), catalog.describe(failure));
    }

    /**
     * Un 4xx sin nada propio lleva la fila de su status, o la de otro 4xx si la tabla no lo nombra.
     */
    @Property(tries = 100)
    void aClientStatusWithNothingOfItsOwnTakesTheRowOfTheTable(@ForAll @IntRange(min = 400, max = 499) int status) {
        SanitizedFailure failure = SanitizedFailure.ofStatus(status, null, null, null, null);

        assertEquals(new CatalogEntry(PlatformTable.of(status).code(), PlatformTable.of(status).message()),
                catalog.describe(failure));
    }

    /**
     * Un 5xx sin nada propio lleva la fila de su status, o la de otro 5xx si la tabla no lo nombra.
     */
    @Property(tries = 100)
    void aServerStatusWithNothingOfItsOwnTakesTheRowOfTheTable(@ForAll @IntRange(min = 500, max = 599) int status) {
        SanitizedFailure failure = SanitizedFailure.ofStatus(status, null, null, null, null);

        assertEquals(new CatalogEntry(PlatformTable.of(status).code(), PlatformTable.of(status).message()),
                catalog.describe(failure));
    }
}
