package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LayerTest {

    @Test
    void labelsAreTheLowercaseNamesOfTheAdr() {
        // Son las cadenas que escriben el log y las métricas en los tres stacks.
        assertEquals("domain", Layer.DOMAIN.label());
        assertEquals("application", Layer.APPLICATION.label());
        assertEquals("infrastructure", Layer.INFRASTRUCTURE.label());
        assertEquals("platform", Layer.PLATFORM.label());
    }

    @Test
    void onlyInfrastructureAndPlatformAreIncidents() {
        assertFalse(Layer.DOMAIN.isIncident());
        assertFalse(Layer.APPLICATION.isIncident());
        assertTrue(Layer.INFRASTRUCTURE.isIncident());
        assertTrue(Layer.PLATFORM.isIncident());
    }

    @Test
    void everyTypeBelongsToTheLayerOfItsClass() {
        assertLayer(Layer.DOMAIN, DomainError.Type.values());
        assertLayer(Layer.APPLICATION, ApplicationError.Type.values());
        assertLayer(Layer.INFRASTRUCTURE, InfrastructureError.Type.values());
        assertLayer(Layer.PLATFORM, PlatformError.Type.values());
    }

    @Test
    void theTypesAreExactlyTheOnesOfTheAdrTable() {
        assertEquals(List.of("NOT_FOUND", "CONFLICT", "RULE_VIOLATION"), names(DomainError.Type.values()));
        assertEquals(
                List.of("INVALID_INPUT", "CONFLICT", "UNPROCESSABLE", "UNAUTHENTICATED", "FORBIDDEN", "RATE_LIMITED"),
                names(ApplicationError.Type.values()));
        assertEquals(List.of("UNAVAILABLE", "TIMEOUT", "BAD_GATEWAY"), names(InfrastructureError.Type.values()));
        assertEquals(List.of("INTERNAL"), names(PlatformError.Type.values()));
    }

    @Test
    void anErrorReportsTheLayerOfItsType() {
        assertEquals(Layer.DOMAIN, DomainError.notFound("El pedido no existe").layer());
        assertEquals(Layer.APPLICATION, ApplicationError.forbidden("Sin permiso").layer());
        assertEquals(Layer.INFRASTRUCTURE, InfrastructureError.timeout("pagos", null).layer());
        assertEquals(Layer.PLATFORM, PlatformError.internal("Invariante rota").layer());
    }

    private static void assertLayer(Layer expected, ErrorType[] types) {
        for (ErrorType type : types) {
            assertEquals(expected, type.layer(), type.name());
        }
    }

    private static List<String> names(ErrorType[] types) {
        return Stream.of(types).map(ErrorType::name).toList();
    }
}
