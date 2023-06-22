package org.snomed.aag.data.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.snomed.aag.data.Constants.PAGE_OF_ONE;

class CriteriaItemServiceTest extends AbstractTest {

	@Autowired
	private CriteriaItemService service;

	@Autowired
	private ProjectAcceptanceCriteriaService projectAcceptanceCriteriaService;

	@Test
	void testFindAllByGiveBranch() {
		final CriteriaItem criteriaItem1 = new CriteriaItem("classification-clean-1");
		criteriaItem1.setLabel("Content has been classified.");
		criteriaItem1.setDescription("If there are any content changes requiring classification then classification must be run and the results saved.");
		criteriaItem1.setMandatory(true);
		criteriaItem1.setExpiresOnCommit(true);
		criteriaItem1.setOrder(2);
		service.create(criteriaItem1);

		final CriteriaItem criteriaItem2 = new CriteriaItem("classification-clean-2");
		criteriaItem2.setLabel("Content has been classified.");
		criteriaItem2.setDescription("If there are any content changes requiring classification then classification must be run and the results saved.");
		criteriaItem2.setMandatory(true);
		criteriaItem2.setExpiresOnCommit(true);
		criteriaItem2.setOrder(3);
		criteriaItem2.setForCodeSystems(new HashSet<>(Arrays.asList("SNOMEDCT-TEST")));
		service.create(criteriaItem2);

		final CriteriaItem criteriaItem3 = new CriteriaItem("classification-clean-3");
		criteriaItem3.setLabel("Content has been classified.");
		criteriaItem3.setDescription("If there are any content changes requiring classification then classification must be run and the results saved.");
		criteriaItem3.setMandatory(true);
		criteriaItem3.setExpiresOnCommit(true);
		criteriaItem3.setOrder(4);
		criteriaItem3.setNotForCodeSystems(new HashSet<>(Arrays.asList("SNOMEDCT")));
		service.create(criteriaItem3);

		Page<CriteriaItem> all = service.findAll(PageRequest.of(0, 100));
		assertEquals(3, all.getTotalElements());

		all = service.findByBranch("MAIN", PageRequest.of(0, 100));
		assertEquals(1, all.getTotalElements());
		assertEquals("CriteriaItem{id='classification-clean-1', " +
				"label='Content has been classified.', " +
				"description='If there are any content changes requiring classification then classification must be run and the results saved.', " +
				"order=2, authoringLevel=null, " +
				"mandatory=true, manual=false, expiresOnCommit=true, requiredRoles='null'}", all.getContent().get(0).toString());

		all = service.findByBranch("MAIN/SNOMEDCT-XX/XX/XX-123", PageRequest.of(0, 100));
		assertEquals(2, all.getTotalElements());
		assertEquals("CriteriaItem{id='classification-clean-1', " +
				"label='Content has been classified.', " +
				"description='If there are any content changes requiring classification then classification must be run and the results saved.', " +
				"order=2, authoringLevel=null, " +
				"mandatory=true, manual=false, expiresOnCommit=true, requiredRoles='null'}", all.getContent().get(0).toString());
		assertEquals("CriteriaItem{id='classification-clean-3', " +
				"label='Content has been classified.', " +
				"description='If there are any content changes requiring classification then classification must be run and the results saved.', " +
				"order=4, authoringLevel=null, " +
				"mandatory=true, manual=false, expiresOnCommit=true, requiredRoles='null'}", all.getContent().get(1).toString());

		all = service.findByBranch("MAIN/SNOMEDCT-TEST/TEST/TEST-123", PageRequest.of(0, 100));
		assertEquals(3, all.getTotalElements());
		assertEquals("CriteriaItem{id='classification-clean-1', " +
				"label='Content has been classified.', " +
				"description='If there are any content changes requiring classification then classification must be run and the results saved.', " +
				"order=2, authoringLevel=null, " +
				"mandatory=true, manual=false, expiresOnCommit=true, requiredRoles='null'}", all.getContent().get(0).toString());
		assertEquals("CriteriaItem{id='classification-clean-2', " +
				"label='Content has been classified.', " +
				"description='If there are any content changes requiring classification then classification must be run and the results saved.', " +
				"order=3, authoringLevel=null, " +
				"mandatory=true, manual=false, expiresOnCommit=true, requiredRoles='null'}", all.getContent().get(1).toString());
		assertEquals("CriteriaItem{id='classification-clean-3', " +
				"label='Content has been classified.', " +
				"description='If there are any content changes requiring classification then classification must be run and the results saved.', " +
				"order=4, authoringLevel=null, " +
				"mandatory=true, manual=false, expiresOnCommit=true, requiredRoles='null'}", all.getContent().get(2).toString());
	}

	@Test
	void testCreateLoad() {
		final CriteriaItem criteriaItem = new CriteriaItem("classification-clean");
		criteriaItem.setLabel("Content has been classified.");
		criteriaItem.setDescription("If there are any content changes requiring classification then classification must be run and the results saved.");
		criteriaItem.setMandatory(true);
		criteriaItem.setExpiresOnCommit(true);
		criteriaItem.setOrder(2);
		service.create(criteriaItem);

		final Page<CriteriaItem> all = service.findAll(PageRequest.of(0, 100));
		assertEquals(1, all.getTotalElements());

		assertEquals("CriteriaItem{id='classification-clean', label='Content has been classified.', " +
				"description='If there are any content changes requiring classification then classification must be run" +
				" and the results saved.', order=2, authoringLevel=null, mandatory=true, manual=false, " +
				"expiresOnCommit=true, requiredRoles='null'}", all.getContent().get(0).toString());
	}

	@Test
	void testDelete() {
		final CriteriaItem thing = new CriteriaItem("thing");
		service.create(thing);

		assertEquals(1, service.findAll(PAGE_OF_ONE).getTotalElements());

		final ProjectAcceptanceCriteria criteria = new ProjectAcceptanceCriteria("A", 1);
		criteria.getSelectedTaskCriteriaIds().add(thing.getId());
		projectAcceptanceCriteriaService.create(criteria);

		Assertions.assertThrows(ServiceRuntimeException.class, () -> {service.delete(thing);});

		criteria.getSelectedTaskCriteriaIds().clear();
		criteria.getSelectedProjectCriteriaIds().add(thing.getId());
		projectAcceptanceCriteriaService.update(criteria);

		Assertions.assertThrows(ServiceRuntimeException.class, () -> {service.delete(thing);});

		criteria.getSelectedProjectCriteriaIds().clear();
		projectAcceptanceCriteriaService.update(criteria);

		service.delete(thing);

		assertEquals(0, service.findAll(PAGE_OF_ONE).getTotalElements());
	}

}
