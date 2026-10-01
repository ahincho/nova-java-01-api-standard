package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class InfrastructureErrorTest {

    @Test
    void timeoutKeepsTheUpstreamAndTheCauseForTheLog() {
        SocketTimeoutException cause = new SocketTimeoutException("Read timed out");

        InfrastructureError error = InfrastructureError.timeout("pagos", cause);

        assertEquals(InfrastructureError.Type.TIMEOUT, error.type());
        assertEquals(Optional.of("pagos"), error.upstream());
        assertSame(cause, error.getCause());
    }

    @Test
    void eachFactoryCreatesItsType() {
        assertEquals(InfrastructureError.Type.UNAVAILABLE, InfrastructureError.unavailable("pagos", null).type());
        assertEquals(InfrastructureError.Type.BAD_GATEWAY, InfrastructureError.badGateway("pagos", null).type());
    }

    @Test
    void unavailableMayBeRetriedAfterAWait() {
        InfrastructureError error = InfrastructureError.unavailable("pagos", new IOException("Connection refused"),
                Duration.ofSeconds(5));

        assertEquals(Optional.of(Duration.ofSeconds(5)), error.retryAfter());
        assertEquals(Optional.empty(), InfrastructureError.unavailable("pagos", null).retryAfter());
        assertEquals(Optional.empty(), InfrastructureError.timeout("pagos", null).retryAfter());
    }

    @Test
    void aNegativeWaitIsDiscardedInsteadOfFailingTheError() {
        // Lanzar aquí reemplazaría el 503 que se iba a responder por un 500.
        InfrastructureError error = InfrastructureError.unavailable("pagos", null, Duration.ofSeconds(-5));

        assertEquals(InfrastructureError.Type.UNAVAILABLE, error.type());
        assertEquals(Optional.empty(), error.retryAfter());
    }

    @Test
    void theMessageSaysWhatHappenedWithoutNamingTheUpstream() {
        // El proveedor va al log como campo aparte, nunca dentro del mensaje.
        List<InfrastructureError> errors = List.of(
                InfrastructureError.unavailable("pagos-core", null),
                InfrastructureError.timeout("pagos-core", null),
                InfrastructureError.badGateway("pagos-core", null));

        assertEquals(List.of(
                "Una dependencia no está disponible",
                "Una dependencia no respondió a tiempo",
                "Una dependencia respondió algo inválido"), errors.stream().map(Throwable::getMessage).toList());
        errors.forEach(error -> assertFalse(error.getMessage().contains("pagos")));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void theUpstreamIsRequired(String upstream) {
        // Perder el nombre del proveedor es el defecto que el modelo evita por diseño.
        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> InfrastructureError.timeout(upstream, null));

        assertEquals("upstream es obligatorio", error.getMessage());
    }

    @Test
    void anInfrastructureErrorHasNoCodeOfItsOwn() {
        InfrastructureError error = InfrastructureError.badGateway("pagos", null);

        assertEquals(Optional.empty(), error.code());
        assertNull(error.getCause());
    }
}
