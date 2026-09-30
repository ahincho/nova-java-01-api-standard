package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Los records que acompañan al modelo: {@link FieldError}, {@link CatalogEntry} y
 * {@link SerializedError}.
 */
class ValueTypesTest {

    @Test
    void aFieldErrorMayOmitItsCode() {
        FieldError withoutCode = FieldError.of("email", "El correo no es válido");
        FieldError withCode = FieldError.of("email", "INVALID_FORMAT", "El correo no es válido");

        assertNull(withoutCode.code());
        assertEquals("INVALID_FORMAT", withCode.code());
        assertEquals("email", withCode.field());
        assertEquals("El correo no es válido", withCode.message());
    }

    @Test
    void aBlankFieldCodeIsTreatedAsAbsent() {
        assertNull(FieldError.of("email", " ", "El correo no es válido").code());
    }

    @Test
    void aFieldErrorNeedsItsFieldAndMessage() {
        assertEquals("field es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> FieldError.of(" ", "Mensaje")).getMessage());
        assertEquals("field es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> FieldError.of(null, "Mensaje")).getMessage());
        assertEquals("message es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> FieldError.of("email", null)).getMessage());
        assertEquals("message es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> FieldError.of("email", "CODE", "")).getMessage());
    }

    @Test
    void aCatalogEntryNeedsItsCodeAndMessage() {
        assertEquals("code es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> new CatalogEntry(null, "Mensaje")).getMessage());
        assertEquals("code es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> new CatalogEntry(" ", "Mensaje")).getMessage());
        assertEquals("message es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> new CatalogEntry("CODE", null)).getMessage());
        assertEquals("message es obligatorio",
                assertThrows(IllegalArgumentException.class, () -> new CatalogEntry("CODE", "")).getMessage());
    }

    @Test
    void theHeadersOfASerializedErrorAreAnImmutableCopy() {
        Map<String, String> headers = new LinkedHashMap<>(Map.of("Retry-After", "1"));
        SerializedError serialized = new SerializedError(409, "cuerpo", headers);

        headers.put("X-Otro", "valor");

        assertEquals(Map.of("Retry-After", "1"), serialized.headers());
        assertThrows(UnsupportedOperationException.class, () -> serialized.headers().put("X-Otro", "valor"));
    }

    @Test
    void aSerializedErrorWithoutHeadersHasAnEmptyMap() {
        assertTrue(new SerializedError(500, List.of(), null).headers().isEmpty());
        assertTrue(new SerializedError(500, List.of(), Map.of()).headers().isEmpty());
    }
}
