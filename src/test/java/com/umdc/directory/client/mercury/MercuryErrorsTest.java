package com.umdc.directory.client.mercury;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MercuryErrorsTest {

    @Test
    void throttlingIs429WithRetryAfterAndMessage() {
        var response = MercuryErrors.<Void>toResponse(new MercuryThrottledException(37));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("37", response.getHeaders().getFirst("Retry-After"));
        assertNotNull(response.getHeaders().getFirst("message-error"));
    }

    @Test
    void unavailableAndAnythingElseIs503WithMessage() {
        var unavailable = MercuryErrors.<Void>toResponse(new MercuryUnavailableException("down", new RuntimeException("x")));
        var other = MercuryErrors.<Void>toResponse(new IllegalStateException("boom"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, unavailable.getStatusCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, other.getStatusCode());
        assertNotNull(unavailable.getHeaders().getFirst("message-error"));
        assertTrue(unavailable.getHeaders().getFirst("Retry-After") == null);
    }

    @Test
    void theThrottleMessageNamesTheWait() {
        assertTrue(new MercuryThrottledException(5).getMessage().contains("5"));
    }

    @Test
    void theHelperIsNotInstantiable() throws Exception {
        var constructor = MercuryErrors.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var error = assertThrows(java.lang.reflect.InvocationTargetException.class, constructor::newInstance);
        assertTrue(error.getCause() instanceof IllegalStateException);
    }

    @Test
    void theFeignOptionsHaveTimeouts() {
        var auth = new com.umdc.directory.client.interceptor.MercuryAuthFeignConfig().mercuryAuthRequestOptions();
        assertEquals(3000, auth.connectTimeoutMillis());
        assertEquals(5000, auth.readTimeoutMillis());
    }
}
