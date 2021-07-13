package org.snomed.aag.data.validators;

import org.junit.jupiter.api.Test;
import org.snomed.aag.data.pojo.CommitInformation;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CommitInformationValidatorTest {
	private final CommitInformationValidator target = new CommitInformationValidator();

	@Test
	void validate_ShouldThrowException_WhenGivenNull() {
		// then
		assertThrows(IllegalArgumentException.class, () -> {
			// when
			target.validate(null);
		});
	}

	@Test
	void validate_ShouldThrowException_WhenGivenNoCommitType() {
		// given
		CommitInformation commitInformation = new CommitInformation("path", null, 10L, Collections.emptyMap());

		// then
		assertThrows(IllegalArgumentException.class, () -> {
			// when
			target.validate(commitInformation);
		});
	}

	@Test
	void validate_ShouldThrowException_WhenGivenNoPath() {
		// given
		CommitInformation commitInformation = new CommitInformation(null, CommitInformation.CommitType.PROMOTION, 10L, Collections.emptyMap());

		// then
		assertThrows(IllegalArgumentException.class, () -> {
			// when
			target.validate(commitInformation);
		});
	}

	@Test
	void validate_ShouldThrowException_WhenGivenNoHeadTime() {
		// given
		CommitInformation commitInformation = new CommitInformation("path", CommitInformation.CommitType.PROMOTION, 0, Collections.emptyMap());

		// then
		assertThrows(IllegalArgumentException.class, () -> {
			// when
			target.validate(commitInformation);
		});
	}

	@Test
	void validate_ShouldThrowException_WhenGivenNegativeHeadTime() {
		// given
		CommitInformation commitInformation = new CommitInformation("path", CommitInformation.CommitType.PROMOTION, -10, Collections.emptyMap());

		// then
		assertThrows(IllegalArgumentException.class, () -> {
			// when
			target.validate(commitInformation);
		});
	}
}
