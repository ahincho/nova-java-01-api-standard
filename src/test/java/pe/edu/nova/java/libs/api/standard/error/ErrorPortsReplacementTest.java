package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyCodeOf;
import static pe.edu.nova.java.libs.api.standard.error.Envelopes.onlyMessageOf;

import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Cada puerto se reemplaza o se envuelve sin forkear, como lo haría el perfil de una organización, y
 * ninguno ve lo que protege el núcleo: el proveedor, la causa ni el mensaje de un incidente.
 */
class ErrorPortsReplacementTest {

    private final ErrorStatusMapper novaMapper = new NovaErrorStatusMapper();
    private final ErrorCatalog novaCatalog = new NovaErrorCatalog();
    private final ErrorSerializer novaSerializer = new NovaErrorSerializer();

    @Test
    void aCatalogOfItsOwnReplacesTheCodesAndTexts() {
        ErrorCatalog organization = failure -> new CatalogEntry("ORG-" + failure.status(),
                "Texto de la organización para " + failure.type().map(ErrorType::name).orElse("?"));
        ErrorPorts ports = new ErrorPorts(novaMapper, organization, novaSerializer);

        SerializedError serialized = ports.respond(DomainError.notFound("ORDER_NOT_FOUND", "El pedido no existe"));

        assertEquals(404, serialized.status());
        assertEquals("ORG-404", onlyCodeOf(serialized));
        assertEquals("Texto de la organización para NOT_FOUND", onlyMessageOf(serialized));
    }

    @Test
    void aCatalogCanWrapNovasToChangeOnlyTheGenericText() {
        // Otro idioma para los 5xx; todo lo demás sigue siendo la decisión de Nova.
        ErrorCatalog english = failure -> {
            CatalogEntry entry = novaCatalog.describe(failure);
            return failure.status() >= 500 ? new CatalogEntry(entry.code(), "Internal server error") : entry;
        };
        ErrorPorts ports = new ErrorPorts(novaMapper, english, novaSerializer);

        SerializedError timeout = ports.respond(InfrastructureError.timeout("pagos", null));
        SerializedError notFound = ports.respond(DomainError.notFound("El pedido no existe"));

        assertEquals("GATEWAY_TIMEOUT", onlyCodeOf(timeout));
        assertEquals("Internal server error", onlyMessageOf(timeout));
        assertEquals("NOT_FOUND", onlyCodeOf(notFound));
        assertEquals("El pedido no existe", onlyMessageOf(notFound));
    }

    @Test
    void aStatusMapperOfItsOwnChangesTheStatusAndTheCatalogFollows() {
        // Una organización que responde las reglas de negocio con 400 en vez de 422.
        ErrorStatusMapper organization =
                type -> type == DomainError.Type.RULE_VIOLATION ? 400 : novaMapper.statusOf(type);
        ErrorPorts ports = new ErrorPorts(organization, novaCatalog, novaSerializer);

        SerializedError ruleViolation = ports.respond(DomainError.ruleViolation("Supera el crédito"));
        SerializedError conflict = ports.respond(DomainError.conflict("Está cancelado"));

        assertEquals(400, ruleViolation.status());
        assertEquals("BAD_REQUEST", onlyCodeOf(ruleViolation));
        assertEquals("Supera el crédito", onlyMessageOf(ruleViolation));
        assertEquals(409, conflict.status());
    }

    @Test
    void aSerializerOfItsOwnReplacesTheBody() {
        // RFC 7807 queda posible como otro serializador, detrás del mismo puerto.
        ErrorSerializer problemJson = failure -> new SerializedError(failure.status(),
                Map.of("status", failure.status(), "title", failure.code(), "detail", failure.message()),
                Map.of("Content-Type", "application/problem+json"));
        ErrorPorts ports = new ErrorPorts(novaMapper, novaCatalog, problemJson);

        SerializedError serialized = ports.respond(ApplicationError.forbidden("Sin permiso"));

        assertEquals(403, serialized.status());
        assertEquals(Map.of("status", 403, "title", "FORBIDDEN", "detail", "Sin permiso"),
                assertInstanceOf(Map.class, serialized.body()));
        assertEquals("application/problem+json", serialized.headers().get("Content-Type"));
    }

    @Test
    void aSerializerCanWrapNovasToAddAHeader() {
        ErrorSerializer withOrganizationHeader = failure -> {
            SerializedError serialized = novaSerializer.serialize(failure);
            Map<String, String> headers = new LinkedHashMap<>(serialized.headers());
            headers.put("X-Organization", "nova");
            return new SerializedError(serialized.status(), serialized.body(), headers);
        };
        ErrorPorts ports = new ErrorPorts(novaMapper, novaCatalog, withOrganizationHeader);

        SerializedError serialized =
                ports.respond(ApplicationError.conflict("La operación sigue en curso", Duration.ofSeconds(1)));

        assertEquals(Map.of("Retry-After", "1", "X-Organization", "nova"), serialized.headers());
        assertEquals("CONFLICT", onlyCodeOf(serialized));
    }

    @Test
    void noPortSeesTheUpstreamTheCauseNorTheMessageOfAnIncident() {
        // Un catálogo y un serializador que vuelcan todo lo que reciben igual no pueden filtrar nada.
        List<String> seen = new ArrayList<>();
        ErrorCatalog spyCatalog = failure -> {
            seen.add(failure.toString());
            return novaCatalog.describe(failure);
        };
        ErrorSerializer leaky = failure -> {
            seen.add(failure.toString());
            return new SerializedError(failure.status(), failure.toString(), Map.of());
        };
        ErrorPorts ports = new ErrorPorts(novaMapper, spyCatalog, leaky);

        SerializedError serialized = ports.respond(
                InfrastructureError.timeout("pagos", new SocketTimeoutException("Read timed out on 10.0.3.7")));

        assertEquals(504, serialized.status());
        assertEquals(2, seen.size());
        for (String text : seen) {
            assertFalse(text.contains("pagos"), text);
            assertFalse(text.contains("10.0.3.7"), text);
        }
        assertTrue(serialized.body().toString().contains("GATEWAY_TIMEOUT"));
    }

    @Test
    void aMapperThatAnswersAnIncidentWithA4xxStillHidesItsMessage() {
        // Un mapeador propio no convierte un incidente en algo que se pueda mostrar.
        ErrorPorts ports = new ErrorPorts(type -> 400, novaCatalog, novaSerializer);

        SerializedError serialized = ports.respond(PlatformError.internal("Invariante rota en el saldo"));

        assertEquals(400, serialized.status());
        assertEquals("BAD_REQUEST", onlyCodeOf(serialized));
        assertEquals("La solicitud no es válida", onlyMessageOf(serialized));
    }
}
