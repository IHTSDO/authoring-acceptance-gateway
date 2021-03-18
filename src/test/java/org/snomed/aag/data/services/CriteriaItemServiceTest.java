package org.snomed.aag.data.services;

import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.domain.CriteriaItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CriteriaItemServiceTest extends AbstractTest {

	@Autowired
	private CriteriaItemService service;

	@Test
	public void testCreateLoad() {
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
				"expiresOnCommit=true, requiredRole='null'}", all.getContent().get(0).toString());
	}

}
