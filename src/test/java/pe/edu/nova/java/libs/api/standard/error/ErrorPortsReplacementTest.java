package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyCodeOf;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyMessageOf;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Cada puerto se reemplaza o se envuelve sin forkear, como lo haría el perfil de una organización.
 */
class ErrorPortsReplacementTest {

    private final ErrorStatusMapper novaMapper = new NovaErrorStatusMapper();
    private final ErrorCatalog novaCatalog = new NovaErrorCatalog();

    @Test
    void aCatalogOfItsOwnReplacesTheCodesAndTexts() {
        ErrorCatalog organization = (error, status) ->
                new CatalogEntry("ORG-" + status, "Texto de la organización para " + error.type().name());
        ErrorSerializer serializer = new NovaErrorSerializer(novaMapper, organization);

        SerializedError serialized = serializer.serialize(DomainError.notFound("ORDER_NOT_FOUND", "El pedido no existe"));

        assertEquals(404, serialized.status());
        assertEquals("ORG-404", onlyCodeOf(serialized));
        assertEquals("Texto de la organización para NOT_FOUND", onlyMessageOf(serialized));
    }

    @Test
    void aCatalogCanWrapNovasToChangeOnlyTheGenericText() {
        // Otro idioma para los 5xx; todo lo demás sigue siendo la decisión de Nova.
        ErrorCatalog english = (error, status) -> {
            CatalogEntry entry = novaCatalog.describe(error, status);
            return status >= 500 ? new CatalogEntry(entry.code(), "Internal server error") : entry;
        };
        ErrorSerializer serializer = new NovaErrorSerializer(novaMapper, english);

        SerializedError timeout = serializer.serialize(InfrastructureError.timeout("pagos", null));
        SerializedError notFound = serializer.serialize(DomainError.notFound("El pedido no existe"));

        assertEquals("GATEWAY_TIMEOUT", onlyCodeOf(timeout));
        assertEquals("Internal server error", onlyMessageOf(timeout));
        assertEquals("NOT_FOUND", onlyCodeOf(notFound));
        assertEquals("El pedido no existe", onlyMessageOf(notFound));
    }

    @Test
    void aStatusMapperOfItsOwnChangesTheStatusAndTheCatalogFollows() {
        // Una organización que responde las reglas de negocio con 400 en vez de 422.
        ErrorStatusMapper organization = error ->
                error.type() == DomainError.Type.RULE_VIOLATION ? 400 : novaMapper.statusOf(error);
        ErrorSerializer serializer = new NovaErrorSerializer(organization, novaCatalog);

        SerializedError ruleViolation = serializer.serialize(DomainError.ruleViolation("Supera el crédito"));
        SerializedError conflict = serializer.serialize(DomainError.conflict("Está cancelado"));

        assertEquals(400, ruleViolation.status());
        assertEquals("BAD_REQUEST", onlyCodeOf(ruleViolation));
        assertEquals(409, conflict.status());
    }

    @Test
    void aSerializerOfItsOwnReplacesTheBody() {
        // RFC 7807 queda posible como otro serializador, detrás del mismo puerto.
        ErrorSerializer problemJson = error -> {
            int status = novaMapper.statusOf(error);
            CatalogEntry entry = novaCatalog.describe(error, status);
            return new SerializedError(status,
                    Map.of("status", status, "title", entry.code(), "detail", entry.message()),
                    Map.of("Content-Type", "application/problem+json"));
        };

        SerializedError serialized = problemJson.serialize(ApplicationError.forbidden("Sin permiso"));

        assertEquals(403, serialized.status());
        assertEquals(Map.of("status", 403, "title", "FORBIDDEN", "detail", "Sin permiso"),
                assertInstanceOf(Map.class, serialized.body()));
        assertEquals("application/problem+json", serialized.headers().get("Content-Type"));
    }

    @Test
    void aSerializerCanWrapNovasToAddAHeader() {
        ErrorSerializer nova = new NovaErrorSerializer();
        ErrorSerializer withOrganizationHeader = error -> {
            SerializedError serialized = nova.serialize(error);
            Map<String, String> headers = new LinkedHashMap<>(serialized.headers());
            headers.put("X-Organization", "nova");
            return new SerializedError(serialized.status(), serialized.body(), headers);
        };

        SerializedError serialized = withOrganizationHeader.serialize(
                ApplicationError.conflict("La operación sigue en curso", Duration.ofSeconds(1)));

        assertEquals(Map.of("Retry-After", "1", "X-Organization", "nova"), serialized.headers());
        assertEquals("CONFLICT", onlyCodeOf(serialized));
    }
}
