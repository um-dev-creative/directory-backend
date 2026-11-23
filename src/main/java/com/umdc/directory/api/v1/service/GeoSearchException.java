package com.umdc.directory.api.v1.service;

/// An invalid parameter of the geo search; the message is sent in the `message-error` header of the `400`.
public class GeoSearchException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public GeoSearchException(String message) {
        super(message);
    }

    public GeoSearchException(String message, Throwable cause) {
        super(message, cause);
    }
}
