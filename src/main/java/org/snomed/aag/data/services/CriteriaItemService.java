package org.snomed.aag.data.services;

import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.repositories.CriteriaItemRepository;
import org.snomed.aag.data.repositories.ProjectAcceptanceCriteriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.lang.String.format;

@Service
public class CriteriaItemService {

	@Autowired
	private CriteriaItemRepository repository;

	@Autowired
	private ProjectAcceptanceCriteriaRepository acceptanceCriteriaRepository;

	public void create(CriteriaItem criteriaItem) {
		repository.save(criteriaItem);
	}

	public Page<CriteriaItem> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

	public CriteriaItem findOrThrow(String id) {
		final Optional<CriteriaItem> itemOptional = repository.findById(id);
		if (!itemOptional.isPresent()) {
			throw new NotFoundException(format("Criteria Item with id '%s' not found.", id));
		}
		return itemOptional.get();
	}

	public CriteriaItem update(CriteriaItem item) {
		return repository.save(item);
	}

	public void delete(CriteriaItem item) {
		final Page<ProjectAcceptanceCriteria> found = acceptanceCriteriaRepository.findAllBySelectedProjectCriteriaIdsOrSelectedTaskCriteriaIds(item.getId(), item.getId(), Constants.PAGE_OF_ONE);
		if (!found.isEmpty()) {
			throw new IllegalArgumentException(format("Criteria can not be deleted, it is used in %s project criteria.", found.getTotalElements()));
		}

		repository.delete(item);
	}
}
