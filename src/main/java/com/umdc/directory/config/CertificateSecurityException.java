package com.umdc.directory.config;

import java.io.Serial;

/**
 * Exception class for certificate security errors.
 * This class represents exceptions that occur during certificate security operations.
 */
public class CertificateSecurityException extends Exception {
    @Serial
    private static final long serialVersionUID = 5398385356196282905L;

    /**
     * Default constructor.
     * Creates a new instance of CertificateSecurityException with no detail message.
     */
    public CertificateSecurityException() {
        super();
    }

    /**
     * Constructor with a detail message.
     * Creates a new instance of CertificateSecurityException with the specified detail message.
     *
     * @param message the detail message
     */
    public CertificateSecurityException(String message) {
        super(message);
    }

    /**
     * Constructor with a detail message and cause.
     * Creates a new instance of CertificateSecurityException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     */
    public CertificateSecurityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with a cause.
     * Creates a new instance of CertificateSecurityException with the specified cause.
     *
     * @param cause the cause of the exception
     */
    public CertificateSecurityException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructor with a detail message, cause, suppression enabled or disabled, and writable stack trace enabled or disabled.
     * Creates a new instance of CertificateSecurityException with the specified detail message, cause, suppression enabled or disabled, and writable stack trace enabled or disabled.
     *
     * @param message the detail message
     * @param cause the cause of the exception
     * @param enableSuppression whether or not suppression is enabled or disabled
     * @param writableStackTrace whether or not the stack trace should be writable
     */
    public CertificateSecurityException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
