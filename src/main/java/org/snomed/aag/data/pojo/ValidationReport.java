package org.snomed.aag.data.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ValidationReport {
    public static final String COMPLETE = "COMPLETE";

    private String status;
    private RvfValidationResult rvfValidationResult;

    public boolean isComplete() {
        return COMPLETE.equals(status);
    }

    @JsonIgnore
    public Long getContentHeadTimestamp() {
        return rvfValidationResult == null ? null : rvfValidationResult.getContentHeadTimestamp();
    }

    @JsonIgnore
    public boolean hasNoErrorsOrWarnings() {
        return rvfValidationResult != null && rvfValidationResult.hasNoErrorsOrWarnings();
    }

    public String getStatus() {
        return status;
    }

    public RvfValidationResult getRvfValidationResult() {
        return rvfValidationResult;
    }


}
