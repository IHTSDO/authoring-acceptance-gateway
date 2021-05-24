package org.snomed.aag.data.validators;

import org.junit.jupiter.api.Test;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class ProjectAcceptanceCriteriaCreateValidatorTest {
    private final ElasticsearchRestTemplate elasticsearchRestTemplate = mock(ElasticsearchRestTemplate.class);
    private final ProjectAcceptanceCriteriaCreateValidator target = new ProjectAcceptanceCriteriaCreateValidator(elasticsearchRestTemplate);

    @Test
    void validate_ShouldThrowException_WhenGivenNull() {
        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            target.validate(null);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenMissingProjectIteration() {
        // given
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("MAIN");

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            target.validate(projectAcceptanceCriteria);
        });
    }

    @Test
    void validate_ShouldThrowException_WhenProjectIterationIsNegative() {
        // given
        ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("MAIN", -1);

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            target.validate(projectAcceptanceCriteria);
        });
    }
}
