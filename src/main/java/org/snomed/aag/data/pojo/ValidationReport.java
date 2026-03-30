package org.snomed.aag.data.pojo;

public class ValidationReport {
    public static final String COMPLETE = "COMPLETE";

    private String status;
    private RvfValidationResult rvfValidationResult;

    public boolean isComplete() {
        return COMPLETE.equals(status);
    }

    public Long getContentHeadTimestamp() {
        return rvfValidationResult.getContentHeadTimestamp();
    }

    public boolean hasNoErrorsOrWarnings() {
        return rvfValidationResult.hasNoErrorsOrWarnings();
    }

    public String getStatus() {
        return status;
    }

    public RvfValidationResult getRvfValidationResult() {
        return rvfValidationResult;
    }


}
