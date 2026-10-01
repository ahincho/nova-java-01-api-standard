package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ApplicationErrorTest {

    private static final FieldError EMAIL = FieldError.of("email", "El correo no es válido");
    private static final FieldError NAME = FieldError.of("name", "REQUIRED", "El nombre es obligatorio");

    @Test
    void invalidInputKeepsTheFieldErrorsInOrder() {
        ApplicationError error = ApplicationError.invalidInput("La solicitud tiene campos inválidos", List.of(EMAIL, NAME));

        assertEquals(ApplicationError.Type.INVALID_INPUT, error.type());
        assertEquals(Optional.empty(), error.code());
        assertEquals(List.of(EMAIL, NAME), error.fieldErrors());
    }

    @Test
    void invalidInputMayCarryItsOwnCode() {
        ApplicationError error = ApplicationError.invalidInput("INVALID_ORDER", "El pedido no es válido", List.of(EMAIL));

        assertEquals(Optional.of("INVALID_ORDER"), error.code());
    }

    @Test
    void theFieldErrorsAreAnImmutableCopy() {
        List<FieldError> fields = new ArrayList<>(List.of(EMAIL));
        ApplicationError error = ApplicationError.invalidInput("Campos inválidos", fields);

        fields.add(NAME);

        assertEquals(List.of(EMAIL), error.fieldErrors());
        assertThrows(UnsupportedOperationException.class, () -> error.fieldErrors().add(NAME));
    }

    @Test
    void invalidInputWithoutFieldsHasAnEmptyList() {
        // Un cuerpo ilegible es una entrada inválida que no se puede atribuir a ningún campo.
        assertTrue(ApplicationError.invalidInput("El cuerpo no es legible", null).fieldErrors().isEmpty());
        assertTrue(ApplicationError.invalidInput("El cuerpo no es legible", List.of()).fieldErrors().isEmpty());
    }

    @Test
    void conflictMayBeRetriedAfterAWait() {
        ApplicationError inProgress = ApplicationError.conflict(
                "IDEMPOTENCY_KEY_IN_USE", "La operación con esta clave sigue en curso", Duration.ofSeconds(1));

        assertEquals(ApplicationError.Type.CONFLICT, inProgress.type());
        assertEquals(Optional.of("IDEMPOTENCY_KEY_IN_USE"), inProgress.code());
        assertEquals(Optional.of(Duration.ofSeconds(1)), inProgress.retryAfter());
        assertEquals(Optional.of(Duration.ofSeconds(2)),
                ApplicationError.conflict("Sigue en curso", Duration.ofSeconds(2)).retryAfter());
    }

    @Test
    void aConflictWithoutWaitCannotBeRetried() {
        assertEquals(Optional.empty(), ApplicationError.conflict("Choca con otra operación").retryAfter());
        assertEquals(Optional.empty(), ApplicationError.conflict("SLOT_TAKEN", "El turno ya está tomado").retryAfter());
        assertEquals(Optional.of("SLOT_TAKEN"), ApplicationError.conflict("SLOT_TAKEN", "El turno ya está tomado").code());
    }

    @Test
    void rateLimitedCarriesTheWait() {
        ApplicationError limited = ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30));

        assertEquals(ApplicationError.Type.RATE_LIMITED, limited.type());
        assertEquals(Optional.of(Duration.ofSeconds(30)), limited.retryAfter());
        assertEquals(Optional.of("ORDER_QUOTA_EXCEEDED"),
                ApplicationError.rateLimited("ORDER_QUOTA_EXCEEDED", "Superaste el límite", null).code());
    }

    @Test
    void aLimitThatDoesNotSayWhenHasNoWait() {
        assertEquals(Optional.empty(), ApplicationError.rateLimited("Superaste el límite", null).retryAfter());
    }

    @Test
    void aNegativeWaitIsDiscardedInsteadOfFailingTheError() {
        // La espera puede salir de un cálculo con el reloj desfasado. Lanzar aquí cambiaría el 429 que se
        // iba a responder por un 500, así que el error nace igual, sin espera. NestJS hace lo mismo.
        ApplicationError limited = ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(-1));

        assertEquals(ApplicationError.Type.RATE_LIMITED, limited.type());
        assertEquals("Superaste el límite", limited.getMessage());
        assertEquals(Optional.empty(), limited.retryAfter());
        assertEquals(Optional.empty(),
                ApplicationError.conflict("ORDER_IN_PROGRESS", "Sigue en curso", Duration.ofMillis(-1)).retryAfter());
    }

    @Test
    void aZeroWaitIsKept() {
        // Cero es una espera válida: reintentar ya.
        assertEquals(Optional.of(Duration.ZERO),
                ApplicationError.rateLimited("Superaste el límite", Duration.ZERO).retryAfter());
    }

    @Test
    void eachFactoryCreatesItsType() {
        assertEquals(ApplicationError.Type.UNPROCESSABLE, ApplicationError.unprocessable("No se puede procesar").type());
        assertEquals(ApplicationError.Type.UNPROCESSABLE,
                ApplicationError.unprocessable("IDEMPOTENCY_KEY_REUSED", "Clave reusada").type());
        assertEquals(ApplicationError.Type.UNAUTHENTICATED, ApplicationError.unauthenticated("Falta el token").type());
        assertEquals(ApplicationError.Type.UNAUTHENTICATED,
                ApplicationError.unauthenticated("TOKEN_EXPIRED", "El token venció").type());
        assertEquals(ApplicationError.Type.FORBIDDEN, ApplicationError.forbidden("Sin permiso").type());
        assertEquals(ApplicationError.Type.FORBIDDEN,
                ApplicationError.forbidden("COURSE_NOT_ENROLLED", "No estás matriculado").type());
    }

    @Test
    void theOwnCodeReachesEveryType() {
        assertEquals(Optional.of("IDEMPOTENCY_KEY_REUSED"),
                ApplicationError.unprocessable("IDEMPOTENCY_KEY_REUSED", "Clave reusada").code());
        assertEquals(Optional.of("TOKEN_EXPIRED"), ApplicationError.unauthenticated("TOKEN_EXPIRED", "El token venció").code());
        assertEquals(Optional.of("COURSE_NOT_ENROLLED"),
                ApplicationError.forbidden("COURSE_NOT_ENROLLED", "No estás matriculado").code());
    }

    @Test
    void theMessageIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> ApplicationError.forbidden(null));
        assertThrows(IllegalArgumentException.class, () -> ApplicationError.invalidInput(" ", List.of(EMAIL)));
    }

    @Test
    void anApplicationErrorNeverNamesAnUpstream() {
        assertEquals(Optional.empty(), ApplicationError.unauthenticated("Falta el token").upstream());
    }
}
