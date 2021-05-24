package org.snomed.aag.data.validators;

import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.services.ServiceRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ProjectAcceptanceCriteriaUpdateValidator {
    public void validate(ProjectAcceptanceCriteria projectAcceptanceCriteria, String requestBranchPath, Integer requestProjectIteration) {
        if (projectAcceptanceCriteria == null || requestBranchPath == null || requestProjectIteration == null || requestProjectIteration < 0) {
            throw new IllegalArgumentException("Invalid parameters.");
        }

        //Branch cannot be ambiguous.
        String criteriaBranchPath = projectAcceptanceCriteria.getBranchPath();
        if (criteriaBranchPath != null && !criteriaBranchPath.equals(requestBranchPath)) {
            throw new ServiceRuntimeException("Branch in URL does not match branch in criteria.", HttpStatus.CONFLICT);
        }

        //Project Iteration cannot be ambiguous.
        Integer criteriaProjectIteration = projectAcceptanceCriteria.getProjectIteration();
        if (criteriaProjectIteration != null && !criteriaProjectIteration.equals(requestProjectIteration)) {
            throw new ServiceRuntimeException("Project Iteration in URL does not match Project Iteration in criteria.", HttpStatus.CONFLICT);
        }
    }
}
