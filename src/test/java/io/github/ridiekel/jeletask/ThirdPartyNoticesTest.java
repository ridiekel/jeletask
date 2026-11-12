package io.github.ridiekel.jeletask;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every library that ends up in the images has to bring its licence text along. The build collects the texts the jars
 * ship; this test makes a new library without one fail here, instead of going out without its licence.
 * Checks the build output, not the application: once in the JVM run is enough.
 */
@DisabledInNativeImage
class ThirdPartyNoticesTest {
    private static final Path GENERATED = Path.of("target", "classes", "META-INF", "third-party");
    private static final Path LICENSES = Path.of("src", "main", "third-party", "licenses");
    private static final Pattern LIBRARY = Pattern.compile("\\(([^:()\\s]+):([^:()\\s]+):[^:()\\s]+ - ");

    @Test
    void everyLibraryHasALicenceText() throws IOException {
        Properties mapped = mapped();

        Set<String> missing = new TreeSet<>();
        for (String library : libraries()) {
            String artifactId = library.substring(library.indexOf(':') + 1);
            if (!shipsALicence(artifactId) && !mapped.containsKey(library)) {
                missing.add(library);
            }
        }

        assertThat(missing).as("libraries without a licence text: add one to %s and a line to libraries.properties", LICENSES).isEmpty();
    }

    @Test
    void theLicenceTextsInTheMappingExist() throws IOException {
        Set<String> absent = new TreeSet<>();
        for (Object files : mapped().values()) {
            Arrays.stream(files.toString().split(",")).map(String::trim).filter(f -> !Files.isRegularFile(LICENSES.resolve(f))).forEach(absent::add);
        }

        assertThat(absent).isEmpty();
    }

    @Test
    void theMappingNamesNoLibraryThatIsGone() throws IOException {
        Set<String> stale = new TreeSet<>(mapped().stringPropertyNames());
        stale.removeAll(libraries());

        assertThat(stale).as("lines in libraries.properties for libraries the application no longer uses").isEmpty();
    }

    @Test
    void theImagesGetOurOwnLicence() {
        assertThat(GENERATED.getParent().resolve("LICENSE.md")).isRegularFile();
        assertThat(GENERATED.resolve("README.txt")).isRegularFile();
    }

    private static Set<String> libraries() throws IOException {
        Set<String> libraries = new TreeSet<>();
        Matcher matcher = LIBRARY.matcher(Files.readString(GENERATED.resolve("THIRD-PARTY.txt")));
        while (matcher.find()) {
            libraries.add(matcher.group(1) + ":" + matcher.group(2));
        }
        assertThat(libraries).as("libraries in THIRD-PARTY.txt").isNotEmpty();
        return libraries;
    }

    private static boolean shipsALicence(String artifactId) throws IOException {
        Path dir = GENERATED.resolve("libraries").resolve(artifactId);
        if (!Files.isDirectory(dir)) {
            return false;
        }
        try (Stream<Path> files = Files.walk(dir)) {
            return files.map(f -> f.getFileName().toString().toLowerCase()).anyMatch(n -> n.startsWith("license"));
        }
    }

    private static Properties mapped() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(LICENSES.resolve("libraries.properties"))) {
            properties.load(reader);
        }
        return properties;
    }
}
