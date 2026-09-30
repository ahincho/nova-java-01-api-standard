package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.constantpool.ClassEntry;
import java.lang.classfile.constantpool.PoolEntry;
import java.lang.classfile.constantpool.Utf8Entry;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * El modelo de errores no importa ningún framework (ADR-031): ni Spring, ni Quarkus, ni Jakarta EE,
 * ni nada web. Es lo que deja correr el mismo caso de uso detrás de HTTP o de un consumidor de cola.
 * <p>
 * El repositorio no usa ArchUnit, así que la regla se comprueba con la Class-File API del JDK sobre
 * el bytecode, que ve también los nombres completos, las firmas genéricas y las anotaciones, no
 * solo los {@code import}.
 */
class FrameworkFreeTest {

    private static final String PACKAGE = "pe/edu/nova/java/libs/api/standard/error";

    /** Lo único que el modelo puede usar: el JDK y esta misma librería. */
    private static final List<String> ALLOWED = List.of("java/", "pe/edu/nova/java/libs/api/standard/");

    /** Un tipo dentro de un descriptor o una firma genérica, como {@code Ljava/util/List<...>;}. */
    private static final Pattern TYPE_IN_DESCRIPTOR = Pattern.compile("L((?:[\\w$]+/)+[\\w$]+)[;<]");

    @Test
    void theErrorModelDependsOnlyOnTheJdkAndThisLibrary() throws IOException, URISyntaxException {
        List<Path> classFiles = classFilesOf(PACKAGE);
        Map<String, Set<String>> foreign = new TreeMap<>();
        for (Path classFile : classFiles) {
            Set<String> outside = new TreeSet<>();
            for (String type : referencedTypes(Files.readAllBytes(classFile))) {
                if (ALLOWED.stream().noneMatch(type::startsWith)) {
                    outside.add(type);
                }
            }
            if (!outside.isEmpty()) {
                foreign.put(classFile.getFileName().toString(), outside);
            }
        }

        // Si no encontrara las clases, la prueba pasaría sin mirar nada.
        assertTrue(classFiles.size() >= 20, () -> "solo se encontraron " + classFiles);
        assertTrue(foreign.isEmpty(), () -> "ADR-031: el modelo de errores no importa ningún framework, pero " + foreign);
    }

    @Test
    void theScanSeesTheTypesOutsideTheLibrary() throws IOException {
        // Esta prueba usa JUnit: si el escaneo no lo viera, la anterior no probaría nada.
        byte[] bytes;
        try (InputStream in = FrameworkFreeTest.class.getResourceAsStream("FrameworkFreeTest.class")) {
            bytes = in.readAllBytes();
        }

        assertTrue(referencedTypes(bytes).contains("org/junit/jupiter/api/Test"));
    }

    /** Los tipos que nombra una clase: las clases que usa y las de sus descriptores y firmas. */
    private static Set<String> referencedTypes(byte[] classFile) {
        Set<String> types = new TreeSet<>();
        for (PoolEntry entry : ClassFile.of().parse(classFile).constantPool()) {
            if (entry instanceof ClassEntry type && !type.asInternalName().startsWith("[")) {
                types.add(type.asInternalName());
            } else if (entry instanceof Utf8Entry text) {
                Matcher matcher = TYPE_IN_DESCRIPTOR.matcher(text.stringValue());
                while (matcher.find()) {
                    types.add(matcher.group(1));
                }
            }
        }
        return types;
    }

    /** Los {@code .class} de un paquete de la librería, tal como los compiló Gradle. */
    private static List<Path> classFilesOf(String packagePath) throws IOException, URISyntaxException {
        Path root = Path.of(NovaError.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (Stream<Path> files = Files.list(root.resolve(packagePath))) {
            return files.filter(file -> file.getFileName().toString().endsWith(".class")).sorted().toList();
        }
    }
}
