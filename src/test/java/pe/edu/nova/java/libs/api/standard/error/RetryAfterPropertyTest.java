package pe.edu.nova.java.libs.api.standard.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.LongRange;

/**
 * {@code Retry-After} va en segundos enteros y nunca invita a reintentar antes de tiempo.
 */
class RetryAfterPropertyTest {

    private final ErrorPorts ports = ErrorPorts.defaults();

    @Property(tries = 300)
    void theHeaderIsTheWaitRoundedUpToAWholeSecond(@ForAll @LongRange(min = 0, max = 86_400_000) long millis) {
        Duration wait = Duration.ofMillis(millis);

        long seconds = Long.parseLong(ports.respond(ApplicationError.rateLimited("Superaste el límite", wait))
                .headers().get("Retry-After"));

        assertTrue(Duration.ofSeconds(seconds).compareTo(wait) >= 0, () -> seconds + " s es menos que " + wait);
        assertTrue(Duration.ofSeconds(seconds).minus(wait).compareTo(Duration.ofSeconds(1)) < 0,
                () -> seconds + " s redondea de más " + wait);
        assertEquals((millis + 999) / 1000, seconds);
    }
}
