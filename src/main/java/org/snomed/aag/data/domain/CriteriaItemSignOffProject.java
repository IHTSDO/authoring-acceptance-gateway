package org.snomed.aag.data.domain;

/**
 * Project level CriteriaItemSignOff, which includes a project's current iteration.
 */
public class CriteriaItemSignOffProject extends CriteriaItemSignOff {
	public CriteriaItemSignOffProject(String criteriaItemId, String branch, Long branchHeadTimestamp, Integer projectIteration, String userId) {
		super(criteriaItemId, branch, branchHeadTimestamp, projectIteration, userId);
	}
}
