package org.snomed.aag.data.domain;

import org.springframework.stereotype.Component;

@Component
public class CriteriaItemSignOffFactory {
	/**
	 * Return CriteriaItemSignOff. If branch is the same as ProjectAcceptanceCriteria.branchPath, CriteriaItemSignOffProject will be returned. Otherwise, CriteriaItemSignOffTask
	 * will be returned.
	 *
	 * @param criteriaItemId            Value for CriteriaItemSignOff.
	 * @param branch                    Value for CriteriaItemSignOff.
	 * @param branchHeadTimestamp       Value for CriteriaItemSignOff.
	 * @param projectIteration          Optional value for CriteriaItemSignOff.
	 * @param userId                    Value for CriteriaItemSignOff.
	 * @param projectAcceptanceCriteria Required for determining type of CriteriaItemSignOff
	 * @return CriteriaItemSignOff
	 */
	public CriteriaItemSignOff create(String criteriaItemId, String branch, Long branchHeadTimestamp, Integer projectIteration, String userId, ProjectAcceptanceCriteria projectAcceptanceCriteria) {
		boolean branchProjectLevel = projectAcceptanceCriteria.isBranchProjectLevel(branch);
		if (branchProjectLevel) {
			if (projectIteration == null || projectIteration < 0) {
				throw new IllegalArgumentException("Cannot create CriteriaItemSignOff as projectIteration is invalid.");
			}

			// Project level
			return new CriteriaItemSignOffProject(criteriaItemId, branch, branchHeadTimestamp, projectIteration, userId);
		}

		return new CriteriaItemSignOffTask(criteriaItemId, branch, branchHeadTimestamp, userId);
	}
}
