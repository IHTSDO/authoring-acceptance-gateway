package org.snomed.aag.data.pojo;

public class RvfValidationResult {
    private ValidationConfig validationConfig;
    private TestResult testResult;

    public Long getContentHeadTimestamp() {
        return validationConfig.getContentHeadTimestamp();
    }

    public boolean hasNoErrorsOrWarnings() {
        return testResult != null && testResult.getTotalFailures() == 0 && testResult.getTotalWarnings() == 0;
    }

    public ValidationConfig getValidationConfig() {
        return validationConfig;
    }

    public TestResult getTestResult() {
        return testResult;
    }

    private static final class ValidationConfig {

        private String contentHeadTimestamp;

        public Long getContentHeadTimestamp() {
            return contentHeadTimestamp != null ? Long.parseLong(contentHeadTimestamp) : null;
        }
    }
}
