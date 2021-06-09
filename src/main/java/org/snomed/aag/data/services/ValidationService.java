package org.snomed.aag.data.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ValidationService {

	private final RestTemplate rvfRestTemplate;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	public ValidationService() {
		rvfRestTemplate = new RestTemplate();
	}

	public boolean isReportClean(String reportUrl, long headTimestampNow, String branchPath) {
		// Check execution status, not stale and no failures
		final ValidationReport validationReport = rvfRestTemplate.getForObject(reportUrl, ValidationReport.class);

		if (validationReport == null) {
			throw new ServiceRuntimeException(String.format("Validation report for %s was fetched as null from %s.", branchPath, reportUrl));
		}

		if (!validationReport.isComplete()) {
			logger.info("Validation report on {} executionStatus is not {}", branchPath, ValidationReport.COMPLETED);
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

		public static final String COMPLETED = "COMPLETED";
		private String executionStatus;
		private Report report;

		public boolean isComplete() {
			return COMPLETED.equals(executionStatus);
		}

		public Long getContentHeadTimestamp() {
			return report.getContentHeadTimestamp();
		}

		public boolean hasNoErrorsOrWarnings() {
			return report.hasNoErrorsOrWarnings();
		}

		private static final class Report {

			private RvfValidationResult rvfValidationResult;

			public Long getContentHeadTimestamp() {
				return rvfValidationResult.getContentHeadTimestamp();
			}

			public boolean hasNoErrorsOrWarnings() {
				return rvfValidationResult.hasNoErrorsOrWarnings();
			}

			private static final class RvfValidationResult {

				private ValidationConfig validationConfig;
				private TestResult testResult;

				public Long getContentHeadTimestamp() {
					return validationConfig.getContentHeadTimestamp();
				}

				public boolean hasNoErrorsOrWarnings() {
					return testResult.totalFailures == 0 && testResult.totalWarnings == 0;
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

				}
			}
		}
	}
}
