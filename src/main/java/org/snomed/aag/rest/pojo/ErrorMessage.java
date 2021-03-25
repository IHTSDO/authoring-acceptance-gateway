package org.snomed.aag.rest.pojo;

public class ErrorMessage {
    private final String error;
    private final String message;

    public ErrorMessage(String error, String message) {
        this.error = error;
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "{\n\t\"error\" : \"" + this.error + "\",\n\t\"message\" : \"" + this.message + "\"\n}";
    }
}
