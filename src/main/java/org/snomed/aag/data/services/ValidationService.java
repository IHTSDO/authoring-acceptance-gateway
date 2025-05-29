package org.snomed.aag.data.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.client.RVFClientFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ValidationService {

	private final RVFClientFactory rvfClientFactory;
	private final ObjectMapper objectMapper;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ValidationService(@Autowired RVFClientFactory rvfClientFactory) {
		this.rvfClientFactory = rvfClientFactory;
		this.objectMapper = Jackson2ObjectMapperBuilder.json().failOnUnknownProperties(false).build();
	}

	public boolean isReportClean(String reportUrl, long headTimestampNow, String branchPath) {
		// Check execution status, not stale and no failures
		String validationReportString = rvfClientFactory.getClient().getValidationReport(reportUrl);

		try {
			return doIsReportClean(validationReportString, reportUrl, headTimestampNow, branchPath);
		} catch (JsonProcessingException e) {
			final String message = String.format("Failed to deserialise RVF report from URL '%s'", reportUrl);
			logger.error(message, e);
			throw new ServiceRuntimeException(message);
		}
	}

	boolean doIsReportClean(String validationReportString, String reportUrl, long headTimestampNow, String branchPath) throws JsonProcessingException {

		validationReportString = validationReportString.replace("\"TestResult\"", "\"testResult\"");

		final ValidationReport validationReport = objectMapper.readValue(validationReportString, ValidationReport.class);

		if (validationReport == null) {
			throw new ServiceRuntimeException(String.format("Validation report for %s was fetched as null from %s.", branchPath, reportUrl));
		}

		try {
			logger.info("Fetched {}", objectMapper.writeValueAsString(validationReport));
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}

		if (!validationReport.isComplete()) {
			logger.info("Validation report on {} status is not {}", branchPath, ValidationReport.COMPLETE);
			return false;
		}

		final Long reportContentHeadTimestamp = validationReport.getContentHeadTimestamp();
		if (reportContentHeadTimestamp == null) {
			logger.info("Validation report on {} completed but contentHeadTimestamp is missing.", branchPath);
			return false;
		}

		if (reportContentHeadTimestamp != headTimestampNow) {
			logger.info("Validation report on {} completed but is stale. Report contentHeadTimestamp:{}, latest branch head:{}",
					branchPath, reportContentHeadTimestamp, headTimestampNow);
			return false;
		}

		return validationReport.hasNoErrorsOrWarnings();
	}

	private static final class ValidationReport {

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

		private static final class RvfValidationResult {

			private ValidationConfig validationConfig;
			private TestResult testResult;

			public Long getContentHeadTimestamp() {
				return validationConfig.getContentHeadTimestamp();
			}

			public boolean hasNoErrorsOrWarnings() {
				return getTestResult().getTotalFailures() == 0 && getTestResult().getTotalWarnings() == 0;
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

			private static final class TestResult {

				private Integer totalFailures;
				private Integer totalWarnings;

				public Integer getTotalFailures() {
					return totalFailures;
				}

				public Integer getTotalWarnings() {
					return totalWarnings;
				}
			}
		}

	}
}
