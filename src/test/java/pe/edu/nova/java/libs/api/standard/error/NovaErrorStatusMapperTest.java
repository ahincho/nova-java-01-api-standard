package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class NovaErrorStatusMapperTest {

    private final ErrorStatusMapper mapper = new NovaErrorStatusMapper();

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("pe.edu.nova.java.libs.api.standard.error.AdrTable#rows")
    void eachLayerAndTypeMapsToTheStatusOfTheAdr(NovaError error, int status, String code, String message) {
        assertEquals(status, mapper.statusOf(error.type()), error.layer().label() + " " + error.type().name());
    }

    @Test
    void theTableCoversEveryType() {
        Set<ErrorType> covered = AdrTable.rows()
                .map(arguments -> ((NovaError) arguments.get()[0]).type())
                .collect(Collectors.toSet());
        Set<ErrorType> all = new HashSet<>();
        all.addAll(List.of(DomainError.Type.values()));
        all.addAll(List.of(ApplicationError.Type.values()));
        all.addAll(List.of(InfrastructureError.Type.values()));
        all.addAll(List.of(PlatformError.Type.values()));

        assertEquals(all, covered);
    }
}
