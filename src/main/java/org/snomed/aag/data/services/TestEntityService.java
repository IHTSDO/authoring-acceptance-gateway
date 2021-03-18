package org.snomed.aag.data.services;

import org.snomed.aag.data.repositories.TestEntityRepository;
import org.snomed.aag.domain.TestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class TestEntityService {

	@Autowired
	private TestEntityRepository repository;

	public void create(TestEntity testEntity) {
		repository.save(testEntity);
	}

	public Page<TestEntity> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

}
