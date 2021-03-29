package org.snomed.aag;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.snomed.aag.data.repositories.CriteriaItemRepository;
import org.snomed.aag.data.repositories.CriteriaItemSignOffRepository;
import org.snomed.aag.data.services.BranchService;
import org.snomed.aag.data.services.CriteriaItemService;
import org.snomed.aag.data.services.CriteriaItemSignOffService;
import org.snomed.aag.data.services.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@Testcontainers
@ContextConfiguration(classes = TestConfig.class)
public class AbstractTest {

	@Autowired
	protected CriteriaItemRepository criteriaItemRepository;

	@Autowired
	protected CriteriaItemSignOffRepository criteriaItemSignOffRepository;

	@Autowired
	protected CriteriaItemService criteriaItemService;

	@Autowired
	protected CriteriaItemSignOffService criteriaItemSignOffService;

	@Autowired
	protected BranchService branchService;

	@MockBean
	protected SecurityService securityService;

	@AfterEach
	void defaultTearDown() {
		criteriaItemRepository.deleteAll();
		criteriaItemSignOffRepository.deleteAll();
	}

}
