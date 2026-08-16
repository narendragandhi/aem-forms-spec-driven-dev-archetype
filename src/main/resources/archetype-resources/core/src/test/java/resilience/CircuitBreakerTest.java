package ${package}.resilience;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {
    @Test
    void opensAfterConfiguredFailuresAndRecovers() throws Exception {
        CircuitBreaker breaker = new CircuitBreaker(2, 1);
        breaker.recordFailure();
        assertTrue(breaker.allowRequest());
        breaker.recordFailure();
        assertEquals(CircuitBreaker.State.OPEN, breaker.getState());
        assertFalse(breaker.allowRequest());
        Thread.sleep(3);
        assertTrue(breaker.allowRequest());
        breaker.recordSuccess();
        assertEquals(CircuitBreaker.State.CLOSED, breaker.getState());
    }
}
