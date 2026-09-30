package pe.edu.nova.java.libs.api.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.api.standard.error.ApplicationError;
import pe.edu.nova.java.libs.api.standard.error.CatalogEntry;
import pe.edu.nova.java.libs.api.standard.error.DomainError;
import pe.edu.nova.java.libs.api.standard.error.ErrorCatalog;
import pe.edu.nova.java.libs.api.standard.error.ErrorSerializer;
import pe.edu.nova.java.libs.api.standard.error.ErrorStatusMapper;
import pe.edu.nova.java.libs.api.standard.error.FieldError;
import pe.edu.nova.java.libs.api.standard.error.InfrastructureError;
import pe.edu.nova.java.libs.api.standard.error.NovaError;
import pe.edu.nova.java.libs.api.standard.error.NovaErrorCatalog;
import pe.edu.nova.java.libs.api.standard.error.NovaErrorSerializer;
import pe.edu.nova.java.libs.api.standard.error.NovaErrorStatusMapper;
import pe.edu.nova.java.libs.api.standard.error.PlatformError;
import pe.edu.nova.java.libs.api.standard.error.SerializedError;
import pe.edu.nova.java.libs.api.standard.response.ApiResponse;

/**
 * El modelo de errores usado como lo usará una integración: desde otro paquete y solo con la API
 * pública.
 * <p>
 * Las demás pruebas del modelo viven en su mismo paquete, y desde ahí un constructor o un tipo que
 * quedó con visibilidad de paquete se ve igual que uno público. Esta prueba no lo vería, y el starter de
 * Spring Boot o la extensión de Quarkus, que están en otro paquete, sí lo notarían.
 */
class ErrorModelPublicApiTest {

    @Test
    void anIntegrationBuildsClassifiesAndAnswersEveryLayer() {
        List<NovaError> errors = List.of(
                DomainError.notFound("ORDER_NOT_FOUND", "El pedido 42 no existe"),
                ApplicationError.invalidInput("Campos inválidos",
                        List.of(FieldError.of("email", "El correo no es válido"))),
                ApplicationError.rateLimited("Superaste el límite", Duration.ofSeconds(30)),
                InfrastructureError.timeout("pagos", new SocketTimeoutException("Read timed out")),
                PlatformError.internal(new IllegalStateException("db-orders-01 caído")));
        ErrorSerializer serializer = new NovaErrorSerializer(new NovaErrorStatusMapper(), new NovaErrorCatalog());

        List<String> answers = new ArrayList<>();
        for (NovaError error : errors) {
            // Un switch sin default sobre la jerarquía cerrada: si apareciera una capa nueva, no compilaría.
            String layer = switch (error) {
                case DomainError domain -> "domain";
                case ApplicationError application -> "application";
                case InfrastructureError infrastructure -> "infrastructure";
                case PlatformError platform -> "platform";
            };
            assertEquals(error.layer().label(), layer);

            SerializedError serialized = serializer.serialize(error);
            ApiResponse<?> body = assertInstanceOf(ApiResponse.class, serialized.body());
            answers.add(layer + " " + serialized.status() + " " + body.errors().getFirst().code());
        }

        assertEquals(List.of(
                "domain 404 ORDER_NOT_FOUND",
                "application 400 BAD_REQUEST",
                "application 429 TOO_MANY_REQUESTS",
                "infrastructure 504 GATEWAY_TIMEOUT",
                "platform 500 INTERNAL_SERVER_ERROR"), answers);
    }

    @Test
    void aServiceReplacesEachPortFromItsOwnPackage() {
        ErrorStatusMapper allAsBadRequest = error -> 400;
        ErrorCatalog ownCatalog = (error, status) -> new CatalogEntry("SVC_" + status, "Texto propio");
        ErrorSerializer plain = error -> new SerializedError(503, "cuerpo propio", Map.of("X-Own", "1"));

        SerializedError withOwnPorts = new NovaErrorSerializer(allAsBadRequest, ownCatalog)
                .serialize(PlatformError.internal("Invariante rota"));
        SerializedError withOwnSerializer = plain.serialize(DomainError.conflict("Está cancelado"));

        ApiResponse<?> body = assertInstanceOf(ApiResponse.class, withOwnPorts.body());
        assertEquals(400, withOwnPorts.status());
        assertEquals("SVC_400", body.errors().getFirst().code());
        assertEquals("Texto propio", body.errors().getFirst().message());
        assertEquals(503, withOwnSerializer.status());
        assertEquals(Map.of("X-Own", "1"), withOwnSerializer.headers());
    }
}
