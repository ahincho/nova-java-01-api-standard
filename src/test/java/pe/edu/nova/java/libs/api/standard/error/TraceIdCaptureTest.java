package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TraceIdCaptureTest {

    private static final String SERVICES_FILE = "META-INF/services/" + TraceIdSource.class.getName();

    @Test
    void theSourceRegisteredInMetaInfServicesIsDiscovered() {
        List<TraceIdSource> sources = TraceIdCapture.discover(TraceIdCaptureTest.class.getClassLoader());

        assertEquals(1, sources.size());
        assertInstanceOf(FakeTraceIdSource.class, sources.getFirst());
    }

    @Test
    void withNoSourceRegisteredNothingIsDiscovered() {
        assertEquals(List.of(), TraceIdCapture.discover(hidingEveryServicesFile()));
    }

    @Test
    void withNoSourceThereIsNoTraceId() {
        assertEquals(Optional.empty(), TraceIdCapture.current(List.of()));
    }

    @Test
    void theFirstSourceWithAValueWins() {
        List<TraceIdSource> sources = List.of(Optional::empty, () -> Optional.of("primera"), () -> Optional.of("segunda"));

        assertEquals(Optional.of("primera"), TraceIdCapture.current(sources));
    }

    @Test
    void blankAndNullAnswersCountAsNoTraceId() {
        List<TraceIdSource> sources = List.of(() -> Optional.of("  "), () -> null, () -> Optional.of("válida"));

        assertEquals(Optional.of("válida"), TraceIdCapture.current(sources));
        assertEquals(Optional.empty(), TraceIdCapture.current(List.of(() -> Optional.of(""), () -> null)));
    }

    @Test
    void aFailingSourceIsSkippedInsteadOfThrowing() {
        TraceIdSource failing = () -> {
            throw new IllegalStateException("el contexto de trazas no está listo");
        };

        assertEquals(Optional.of("siguiente"), TraceIdCapture.current(List.of(failing, () -> Optional.of("siguiente"))));
        assertEquals(Optional.empty(), TraceIdCapture.current(List.of(failing)));
    }

    @Test
    void aBrokenRegistrationIsSkippedAndTheOthersStillLoad(@TempDir Path directory) throws IOException {
        // Una clase que no existe y otra que no implementa la interfaz: las dos se saltan. La segunda
        // tiene que estar en el classpath: ServiceLoader descarta en silencio las de un módulo con nombre.
        Path services = directory.resolve(SERVICES_FILE);
        Files.createDirectories(services.getParent());
        Files.writeString(services, String.join("\n",
                "pe.edu.nova.java.libs.api.standard.error.DoesNotExist",
                Envelopes.class.getName(),
                FakeTraceIdSource.class.getName()) + "\n");

        List<TraceIdSource> sources = TraceIdCapture.discover(servingOnly(services.toUri().toURL()));

        assertEquals(1, sources.size());
        assertInstanceOf(FakeTraceIdSource.class, sources.getFirst());
    }

    @Test
    void aServicesFileWithIllegalSyntaxDoesNotBreakTheLookup(@TempDir Path directory) throws IOException {
        // Un nombre con espacios invalida el archivo entero, no una sola clase: ServiceLoader lo reporta
        // al pedir el siguiente proveedor, y la búsqueda tiene que seguir en pie con lo que haya.
        Path services = directory.resolve(SERVICES_FILE);
        Files.createDirectories(services.getParent());
        Files.writeString(services, "esto no es un nombre de clase\n");

        assertEquals(List.of(), TraceIdCapture.discover(servingOnly(services.toUri().toURL())));
    }

    @Test
    void withNoSourceRegisteredAnErrorIsBuiltWithoutTraceId() throws Exception {
        // La librería se carga sola, sin los recursos de las pruebas: no ve ninguna TraceIdSource.
        URL mainClasses = NovaError.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader library = new URLClassLoader(new URL[] {mainClasses}, ClassLoader.getPlatformClassLoader())) {
            Class<?> domainError = library.loadClass(DomainError.class.getName());
            Method notFound = domainError.getMethod("notFound", String.class);
            Method traceId = domainError.getMethod("traceId");

            // Aunque haya una petición en curso, sin fuente no hay de dónde tomar el traceId.
            Object error = FakeTraceIdSource.withTraceId("4bf92f3577b34da6", () -> invoke(notFound, "No existe"));

            assertNotSame(DomainError.class, domainError);
            assertEquals(Optional.empty(), traceId.invoke(error));
        }
    }

    @Test
    void anErrorCapturesTheTraceIdOfTheRequestItIsBornIn() {
        DomainError error = FakeTraceIdSource.withTraceId("4bf92f3577b34da6", () -> DomainError.notFound("No existe"));

        assertEquals(Optional.of("4bf92f3577b34da6"), error.traceId());
    }

    @Test
    void anErrorBornOutsideARequestHasNoTraceId() {
        assertEquals(Optional.empty(), DomainError.notFound("No existe").traceId());
    }

    @Test
    void aFailingSourceNeverBreaksTheConstructionOfAnError() {
        InfrastructureError error = FakeTraceIdSource.failing(() -> InfrastructureError.timeout("pagos", null));

        assertEquals(InfrastructureError.Type.TIMEOUT, error.type());
        assertEquals(Optional.empty(), error.traceId());
    }

    @Test
    void everyLayerCapturesTheTraceId() {
        List<NovaError> errors = FakeTraceIdSource.withTraceId("t-1", () -> List.of(
                DomainError.conflict("Está cancelado"),
                ApplicationError.forbidden("Sin permiso"),
                InfrastructureError.badGateway("pagos", null),
                PlatformError.internal("Invariante rota")));

        errors.forEach(error -> assertEquals(Optional.of("t-1"), error.traceId(), error.layer().label()));
    }

    private static Object invoke(Method factory, String message) {
        try {
            return factory.invoke(null, message);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    /** Un class loader que ve todo el classpath de las pruebas menos los archivos de servicios. */
    private static ClassLoader hidingEveryServicesFile() {
        return new ClassLoader(TraceIdCaptureTest.class.getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return name.startsWith("META-INF/services/") ? Collections.emptyEnumeration() : super.getResources(name);
            }
        };
    }

    /** Un class loader cuyo único archivo de servicios de TraceIdSource es el que se le pasa. */
    private static ClassLoader servingOnly(URL servicesFile) {
        return new ClassLoader(TraceIdCaptureTest.class.getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) throws IOException {
                return SERVICES_FILE.equals(name) ? Collections.enumeration(List.of(servicesFile)) : super.getResources(name);
            }
        };
    }
}
