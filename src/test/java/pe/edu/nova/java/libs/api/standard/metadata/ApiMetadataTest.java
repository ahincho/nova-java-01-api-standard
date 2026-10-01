package pe.edu.nova.java.libs.api.standard.metadata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.RecordComponent;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiMetadataTest {

    @Test
    void itIsARecordSoAJsonMapperSeesEveryField() {
        // Una clase con accesores sin "get" salía como {} en Jackson, y el traceId no llegaba al cliente.
        assertTrue(ApiMetadata.class.isRecord());
        assertEquals(List.of("timestamp", "traceId", "apiVersion", "processingTimeMs", "customFields"),
                Arrays.stream(ApiMetadata.class.getRecordComponents()).map(RecordComponent::getName).toList());
    }

    @Test
    void theBuilderGeneratesTheTimestampAndTheTraceIdWhenMissing() {
        ApiMetadata metadata = ApiMetadata.builder().build();

        assertNotNull(metadata.timestamp());
        assertNotNull(metadata.traceId());
        assertEquals(Map.of(), metadata.customFields());
    }

    @Test
    void theBuilderKeepsWhatItIsGiven() {
        Instant at = Instant.parse("2026-09-30T12:00:00Z");

        ApiMetadata metadata = ApiMetadata.builder()
                .timestamp(at)
                .traceId("4bf92f3577b34da6")
                .apiVersion("v1")
                .processingTimeMs(12L)
                .customField("region", "lima")
                .build();

        assertEquals(new ApiMetadata(at, "4bf92f3577b34da6", "v1", 12L, Map.of("region", "lima")), metadata);
    }

    @Test
    void theCustomFieldsCannotBeChangedAndAreNeverNull() {
        Map<String, Object> source = new HashMap<>(Map.of("region", "lima"));
        ApiMetadata metadata = new ApiMetadata(Instant.now(), "t", null, null, source);
        source.put("other", "x");

        assertEquals(Map.of("region", "lima"), metadata.customFields());
        assertThrows(UnsupportedOperationException.class, () -> metadata.customFields().put("k", "v"));
        assertEquals(Map.of(), new ApiMetadata(Instant.now(), "t", null, null, null).customFields());
    }

    @Test
    void theDefaultsCarryATraceId() {
        assertNotNull(ApiMetadata.defaults().traceId());
    }
}
