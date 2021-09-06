package org.snomed.aag.data.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CriteriaItemSignOffFactoryTest {
	private final CriteriaItemSignOffFactory criteriaItemSignOffFactory = new CriteriaItemSignOffFactory();

	@Test
	void create_ShouldThrowException_WhenBranchIsProjectAndNoProjectIteration() {
		// given
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("MAIN", 0);

		// then
		assertThrows(IllegalArgumentException.class, () -> {
			// when
			criteriaItemSignOffFactory.create("", "MAIN", 1L, null, "", projectAcceptanceCriteria);
		});
	}

	@Test
	void create_ShouldReturnExpectedType_WhenBranchIsProject() {
		// given
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("MAIN", 0);

		// when
		CriteriaItemSignOff criteriaItemSignOff = criteriaItemSignOffFactory.create("", "MAIN", 1L, 0, "", projectAcceptanceCriteria);

		// then
		assertEquals(CriteriaItemSignOffProject.class, criteriaItemSignOff.getClass());
	}

	@Test
	void create_ShouldReturnExpectedType_WhenBranchIsTask() {
		// given
		ProjectAcceptanceCriteria projectAcceptanceCriteria = new ProjectAcceptanceCriteria("MAIN", 0);

		// when
		CriteriaItemSignOff criteriaItemSignOff = criteriaItemSignOffFactory.create("", "MAIN/taskA", 1L, 0, "", projectAcceptanceCriteria);

		// then
		assertEquals(CriteriaItemSignOffTask.class, criteriaItemSignOff.getClass());
	}
}
