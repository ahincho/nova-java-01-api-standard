package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DomainErrorTest {

    @Test
    void notFoundCarriesItsOwnCodeAndMessage() {
        DomainError error = DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe");

        assertEquals(DomainError.Type.NOT_FOUND, error.type());
        assertEquals(Optional.of("ORDER_NOT_FOUND"), error.code());
        assertEquals("El pedido 42 no existe", error.getMessage());
    }

    @Test
    void eachFactoryCreatesItsType() {
        assertEquals(DomainError.Type.NOT_FOUND, DomainError.notFound("No existe").type());
        assertEquals(DomainError.Type.CONFLICT, DomainError.conflict("Está cancelado").type());
        assertEquals(DomainError.Type.CONFLICT, DomainError.conflict("ORDER_CANCELLED", "Está cancelado").type());
        assertEquals(DomainError.Type.RULE_VIOLATION, DomainError.ruleViolation("Supera el crédito").type());
        assertEquals(DomainError.Type.RULE_VIOLATION,
                DomainError.ruleViolation("CREDIT_LIMIT_EXCEEDED", "Supera el crédito").type());
    }

    @Test
    void theCodeIsOptional() {
        assertEquals(Optional.empty(), DomainError.conflict("Está cancelado").code());
        assertEquals(Optional.of("ORDER_CANCELLED"), DomainError.conflict("ORDER_CANCELLED", "Está cancelado").code());
        assertEquals(Optional.of("CREDIT_LIMIT_EXCEEDED"),
                DomainError.ruleViolation("CREDIT_LIMIT_EXCEEDED", "Supera el crédito").code());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void aBlankCodeIsTreatedAsAbsent(String code) {
        assertEquals(Optional.empty(), DomainError.notFound(code, "No existe").code());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void theMessageIsRequired(String message) {
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> DomainError.notFound("ORDER_NOT_FOUND", message));

        assertEquals("message es obligatorio", error.getMessage());
    }

    @Test
    void anExpectedErrorCarriesNothingForTheLogOnly() {
        DomainError error = DomainError.ruleViolation("Supera el crédito");

        assertTrue(error.fieldErrors().isEmpty());
        assertEquals(Optional.empty(), error.retryAfter());
        assertEquals(Optional.empty(), error.upstream());
        assertNull(error.getCause());
    }
}
