package org.snomed.aag.data.services;

import org.snomed.aag.data.repositories.CriteriaItemRepository;
import org.snomed.aag.domain.CriteriaItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CriteriaItemService {

	@Autowired
	private CriteriaItemRepository repository;

	public void create(CriteriaItem criteriaItem) {
		repository.save(criteriaItem);
	}

	public Page<CriteriaItem> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

}
