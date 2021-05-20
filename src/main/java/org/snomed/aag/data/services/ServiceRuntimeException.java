package org.snomed.aag.data.services;

import org.springframework.http.HttpStatus;

/**
 * Generic, service-wide runtime exception.
 */
public class ServiceRuntimeException extends RuntimeException {
    private final HttpStatus httpStatus;

    /**
     * Constructor.
     */
    public ServiceRuntimeException() {
        this.httpStatus = null;
    }

    /**
     * Constructor.
     *
     * @param message Message for exception.
     */
    public ServiceRuntimeException(String message) {
        super(message);
        this.httpStatus = null;
    }

    /**
     * Constructor.
     *
     * @param message    Message for exception.
     * @param httpStatus HTTP status for exception.
     */
    public ServiceRuntimeException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /**
     * Return HTTP status associated with exception.
     *
     * @return HTTP status associated with exception.
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Return HTTP status code associated with exception.
     *
     * @return HTTP status code associated with exception.
     */
    public Integer getHttpStatusCode() {
        if (httpStatus == null) {
            return null;
        }

        return httpStatus.value();
    }

    @Override
    public String toString() {
        return "ServiceRuntimeException{" +
                "httpStatus=" + httpStatus +
                ", message='" + this.getMessage() + '\'' +
                '}';
    }
}
