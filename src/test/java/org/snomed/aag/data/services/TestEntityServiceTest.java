package org.snomed.aag.data.services;

import org.junit.jupiter.api.Test;
import org.snomed.aag.AbstractTest;
import org.snomed.aag.domain.TestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TestEntityServiceTest extends AbstractTest {

	@Autowired
	private TestEntityService service;

	@Test
	public void test() {
		final TestEntity testEntity = new TestEntity("1", "test");
		service.create(testEntity);
		final Page<TestEntity> all = service.findAll(PageRequest.of(0, 100));
		assertEquals(1, all.getTotalElements());
	}

}
