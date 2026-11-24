package com.umdc.directory.client.mercury;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.umdc.directory.constant.DirectoryAppConstants.MESSAGE_ERROR_HEADER;

/// Turns the Mercury failures into the answer Directory gives its caller: 429 with `Retry-After` when Mercury throttles, 503 otherwise,
/// both with `message-error`. Nothing sensitive goes in the message.
public final class MercuryErrors {

    private MercuryErrors() {
        throw new IllegalStateException("Utility class");
    }

    /// @param error a [MercuryThrottledException] or a [MercuryUnavailableException]
    /// @param <T>   body type of the answer (always empty)
    /// @return the answer; for any other exception, a 503
    public static <T> ResponseEntity<T> toResponse(RuntimeException error) {
        if (error instanceof MercuryThrottledException throttled) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .header(HttpHeaders.RETRY_AFTER, String.valueOf(throttled.getRetryAfterSeconds()))
                    .header(MESSAGE_ERROR_HEADER, "Mercury is busy. Try again later.")
                    .build();
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(MESSAGE_ERROR_HEADER, "Mercury is not available. Try again later.")
                .build();
    }
}
