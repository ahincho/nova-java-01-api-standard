package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlatformErrorTest {

    @Test
    void internalWrapsAnyExceptionWithTheMessageOfItsCause() {
        IllegalStateException cause = new IllegalStateException("el pool de conexiones está cerrado");

        PlatformError error = PlatformError.internal(cause);

        assertEquals(PlatformError.Type.INTERNAL, error.type());
        assertSame(cause, error.getCause());
        assertEquals("java.lang.IllegalStateException: el pool de conexiones está cerrado", error.getMessage());
    }

    @Test
    void internalWithoutCauseStillHasAMessageForTheLog() {
        assertEquals("Un defecto o una falla del propio servicio", PlatformError.internal((Throwable) null).getMessage());
    }

    @Test
    void internalMayDescribeTheDefect() {
        IllegalStateException cause = new IllegalStateException("sin líneas");

        PlatformError withCause = PlatformError.internal("El pedido quedó sin líneas", cause);
        PlatformError withoutCause = PlatformError.internal("El pedido quedó sin líneas");

        assertEquals("El pedido quedó sin líneas", withCause.getMessage());
        assertSame(cause, withCause.getCause());
        assertNull(withoutCause.getCause());
    }

    @Test
    void theDescriptionIsRequired() {
        assertThrows(IllegalArgumentException.class, () -> PlatformError.internal((String) null));
        assertThrows(IllegalArgumentException.class, () -> PlatformError.internal(" ", new IllegalStateException()));
    }

    @Test
    void aPlatformErrorCarriesNoCodeNorUpstream() {
        PlatformError error = PlatformError.internal("Invariante rota");

        assertEquals(Optional.empty(), error.code());
        assertEquals(Optional.empty(), error.upstream());
        assertEquals(Optional.empty(), error.retryAfter());
    }
}
