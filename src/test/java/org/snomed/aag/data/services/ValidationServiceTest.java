package org.snomed.aag.data.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
