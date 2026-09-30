package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * Ayudas para leer lo que escribe el {@link NovaErrorSerializer}.
 */
final class Envelopes {

    private Envelopes() {
    }

    /** El cuerpo, que en Nova es siempre el sobre. */
    static ApiResponse<?> bodyOf(SerializedError serialized) {
        return assertInstanceOf(ApiResponse.class, serialized.body());
    }

    /** El único código de un sobre con una sola entrada. */
    static String onlyCodeOf(SerializedError serialized) {
        ApiResponse<?> body = bodyOf(serialized);
        assertEquals(1, body.errors().size(), () -> "se esperaba una sola entrada: " + body.errors());
        return body.errors().getFirst().code();
    }

    /** El único mensaje de un sobre con una sola entrada. */
    static String onlyMessageOf(SerializedError serialized) {
        ApiResponse<?> body = bodyOf(serialized);
        assertEquals(1, body.errors().size(), () -> "se esperaba una sola entrada: " + body.errors());
        return body.errors().getFirst().message();
    }
}
