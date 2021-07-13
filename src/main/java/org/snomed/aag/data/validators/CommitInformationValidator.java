package org.snomed.aag.data.validators;

import org.snomed.aag.data.pojo.CommitInformation;
import org.springframework.stereotype.Component;

@Component
public class CommitInformationValidator {
	/**
	 * Validate the given CommitInformation.
	 *
	 * @param commitInformation CommitInformation to validate.
	 * @throws IllegalArgumentException If CommitInformation is invalid.
	 */
	public void validate(CommitInformation commitInformation) {
		if (commitInformation == null) {
			throw new IllegalArgumentException("No CommitInformation given.");
		}

		CommitInformation.CommitType commitType = commitInformation.getCommitType();
		if (commitType == null) {
			throw new IllegalArgumentException("No CommitType specified.");
		}

		String path = commitInformation.getPath();
		if (path == null) {
			throw new IllegalArgumentException("No path specified.");
		}

		long headTime = commitInformation.getHeadTime();
		if (headTime == 0L) {
			throw new IllegalArgumentException("No headTime specified.");
		}

		if (headTime < 0L) {
			throw new IllegalArgumentException("headTime cannot be negative.");
		}
	}
}
