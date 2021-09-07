package org.snomed.aag.data.domain;

/**
 * Task level CriteriaItemSignOff, which does not include a project's current iteration.
 */
public class CriteriaItemSignOffTask extends CriteriaItemSignOff {
	public CriteriaItemSignOffTask(String criteriaItemId, String branch, Long branchHeadTimestamp, String userId) {
		super(criteriaItemId, branch, branchHeadTimestamp, null, userId);
	}
}
