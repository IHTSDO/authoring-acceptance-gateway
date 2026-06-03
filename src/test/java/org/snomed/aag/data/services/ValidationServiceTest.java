package org.snomed.aag.data.services;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.snomed.aag.data.client.RVFClientFactory;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationServiceTest {

	private ValidationService validationService;

	@BeforeEach
	void setup() {
		validationService = new ValidationService(new RVFClientFactory());
	}

	@Test
	void doIsReportClean() throws IOException {
		final String validationReportString = StreamUtils.copyToString(getClass().getResourceAsStream("example-complete-validation-report.json"), StandardCharsets.UTF_8);
		assertTrue(validationService.doIsReportClean(validationReportString, "test-url", 1622714429925L, "MAIN/STORMTESTK"));
	}

	@Test
	void doIsReportClean_ShouldNotThrownException_WhenRvfValidationResultIsNull() throws IOException {
		// given
		ListAppender<ILoggingEvent> listAppender = captureLogs();
		String reportWithNoRvfResult = "{\"status\": \"COMPLETE\"}";

		// when
		validationService.doIsReportClean(reportWithNoRvfResult, "test-url", 1622714429925L, "MAIN/STORMTESTK");

		// then
		boolean noExceptionsLogged = listAppender.list.stream().noneMatch(e -> e.getLevel().isGreaterOrEqual(Level.WARN) && e.getThrowableProxy() != null);
		assertTrue(noExceptionsLogged);
	}

	@Test
	void doIsReportClean_ShouldNotThrowException_WhenTestResultIsNull() throws IOException {
		// given
		ListAppender<ILoggingEvent> listAppender = captureLogs();
		String reportWithNoTestResult = "{\"status\": \"COMPLETE\", \"rvfValidationResult\": {\"validationConfig\": {\"contentHeadTimestamp\": \"1622714429925\"}}}";

		// when
		validationService.doIsReportClean(reportWithNoTestResult, "test-url", 1622714429925L, "MAIN/STORMTESTK");

		// then
		boolean noExceptionsLogged = listAppender.list.stream().noneMatch(e -> e.getLevel().isGreaterOrEqual(Level.WARN) && e.getThrowableProxy() != null);
		assertTrue(noExceptionsLogged);
	}

	private ListAppender<ILoggingEvent> captureLogs() {
		ListAppender<ILoggingEvent> logCapture = new ListAppender<>();
		logCapture.start();
		((Logger) LoggerFactory.getLogger(ValidationService.class)).addAppender(logCapture);
		return logCapture;
	}
}
