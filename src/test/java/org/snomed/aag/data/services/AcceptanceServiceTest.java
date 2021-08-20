package org.snomed.aag.data.services;

import org.ihtsdo.otf.rest.client.RestClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.pojo.CommitInformation;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.snomed.aag.data.domain.CriteriaItem.PROJECT_CLASSIFICATION_CLEAN;
import static org.snomed.aag.data.domain.CriteriaItem.TASK_CLASSIFICATION_CLEAN;

class AcceptanceServiceTest extends AbstractTest {

	@Autowired
	private CriteriaItemService criteriaItemService;

	@Autowired
	private ProjectAcceptanceCriteriaService criteriaService;

	@Autowired
	private AcceptanceService acceptanceService;

	@BeforeEach
	void setup() {
		criteriaItemService.create(new CriteriaItem(CriteriaItem.PROJECT_CLASSIFICATION_CLEAN, AuthoringLevel.PROJECT, true, false, true));
		criteriaItemService.create(new CriteriaItem(TASK_CLASSIFICATION_CLEAN, AuthoringLevel.TASK, true, false, true));
		criteriaService.create(new ProjectAcceptanceCriteria("MAIN/A", 1));
	}

	@Test
	void testProcessCommit() throws RestClientException {
		givenBranchDoesExist();
		final String projectBranch = "MAIN/A";
		final String taskBranch = projectBranch + "/A-10";
		final ProjectAcceptanceCriteria acceptanceCriteria = criteriaService.findByBranchPathWithEffectiveCriteria(taskBranch);
		assertNotNull(acceptanceCriteria);
		Map<String, CriteriaItem> items = criteriaService.findItemsAndMarkSignOff(acceptanceCriteria, taskBranch).stream().collect(Collectors.toMap(CriteriaItem::getId, Function.identity()));
		assertEquals(2, items.size());
		assertFalse(items.get(TASK_CLASSIFICATION_CLEAN).isComplete(), "task classification not complete");

		acceptanceService.processCommit(new CommitInformation(taskBranch, CommitInformation.CommitType.CONTENT, new Date().getTime(), Map.of(CommitInformation.INTERNAL,
				Map.of(CommitInformation.CLASSIFIED, "true"))));

		items = criteriaService.findItemsAndMarkSignOff(acceptanceCriteria, taskBranch).stream().collect(Collectors.toMap(CriteriaItem::getId, Function.identity()));
		assertEquals(2, items.size());
		assertTrue(items.get(TASK_CLASSIFICATION_CLEAN).isComplete(), "task classification is complete");

		acceptanceService.processCommit(new CommitInformation(taskBranch, CommitInformation.CommitType.REBASE, new Date().getTime(), Map.of(CommitInformation.INTERNAL,
				Map.of(CommitInformation.CLASSIFIED, "false"))));

		items = criteriaService.findItemsAndMarkSignOff(acceptanceCriteria, taskBranch).stream().collect(Collectors.toMap(CriteriaItem::getId, Function.identity()));
		assertEquals(2, items.size());
		assertFalse(items.get(TASK_CLASSIFICATION_CLEAN).isComplete(), "task classification not complete");

		acceptanceService.processCommit(new CommitInformation(taskBranch, CommitInformation.CommitType.CONTENT, new Date().getTime(), Map.of(CommitInformation.INTERNAL,
				Map.of(CommitInformation.CLASSIFIED, "true"))));

		items = criteriaService.findItemsAndMarkSignOff(acceptanceCriteria, taskBranch).stream().collect(Collectors.toMap(CriteriaItem::getId, Function.identity()));
		assertEquals(2, items.size());
		assertTrue(items.get(TASK_CLASSIFICATION_CLEAN).isComplete(), "task classification is complete");

		acceptanceService.processCommit(new CommitInformation(projectBranch, CommitInformation.CommitType.PROMOTION, new Date().getTime(), Map.of(CommitInformation.INTERNAL,
				Map.of(CommitInformation.CLASSIFIED, "true"))));

		items = criteriaService.findItemsAndMarkSignOff(acceptanceCriteria, projectBranch).stream().collect(Collectors.toMap(CriteriaItem::getId, Function.identity()));
		assertEquals(2, items.size());
		assertTrue(items.get(PROJECT_CLASSIFICATION_CLEAN).isComplete(), "task classification is complete");

	}
}
