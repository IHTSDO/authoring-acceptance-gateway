package org.snomed.aag.data.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.client.RVFClientFactory;
import org.snomed.aag.data.pojo.ValidationReport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;

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
			logger.warn("Failed to serialise validation report for logging", e);
		}

		if (!validationReport.isComplete()) {
			logger.info("Validation report on {} status is not {}", branchPath, ValidationReport.COMPLETE);
			return false;
		}

		if (validationReport.getRvfValidationResult() == null) {
			logger.warn("Validation report on {} completed but the rvfValidationResult is null", branchPath);
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
}
