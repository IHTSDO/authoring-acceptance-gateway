package org.snomed.aag.data.validators;

import org.junit.jupiter.api.Test;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.services.ServiceRuntimeException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectAcceptanceCriteriaUpdateValidatorTest {
    private final ProjectAcceptanceCriteriaUpdateValidator target = new ProjectAcceptanceCriteriaUpdateValidator();

    @Test
    void validate_ShouldThrowException_WhenGivenNullProjectAcceptanceCriteria() {
        // given
        String branch = "branch";
        Integer projectIteration = 1;

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            target.validate(null, branch, projectIteration);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenGivenNullBranch() {
        // given
        String branch = "branch";
        Integer projectIteration = 1;
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branch, projectIteration);

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            target.validate(projectAcceptanceCriteria, null, projectIteration);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenGivenNullProjectIteration() {
        // given
        String branch = "branch";
        Integer projectIteration = 1;
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branch, projectIteration);

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            target.validate(projectAcceptanceCriteria, branch, null);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenGivenNegativeProjectIteration() {
        // given
        String branch = "branch";
        Integer projectIteration = 1;
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria(branch, projectIteration);

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            target.validate(projectAcceptanceCriteria, branch, -1);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenAmbiguousBranch() {
        // given
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("branchA", 1);

        // then
        assertThrows(ServiceRuntimeException.class, () -> {
            // when
            target.validate(projectAcceptanceCriteria, "branchB", 1);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenAmbiguousProjectIteration() {
        // given
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("branchA", 1);

        // then
        assertThrows(ServiceRuntimeException.class, () -> {
            // when
            target.validate(projectAcceptanceCriteria, "branchA", 2);
        });
    }

    @Test
    void validate_ShouldNotThrowException_WhenGivenValid() {
        // given
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("branchA", 1);

        // then
        assertDoesNotThrow(() -> {
            // when
            target.validate(projectAcceptanceCriteria, "branchA", 1);
        });
    }
}
