package org.snomed.aag.data.services;

import org.snomed.aag.data.Constants;
import org.snomed.aag.data.domain.CriteriaItem;
import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.snomed.aag.data.repositories.ProjectAcceptanceCriteriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHitsIterator;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.lang.String.format;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

@Service
public class ProjectAcceptanceCriteriaService {

	@Autowired
	private ProjectAcceptanceCriteriaRepository repository;

	@Autowired
	private ElasticsearchRestTemplate elasticsearchTemplate;

	public Page<ProjectAcceptanceCriteria> findAll(PageRequest pageRequest) {
		return repository.findAll(pageRequest);
	}

	public ProjectAcceptanceCriteria findByBranchPath(String branch) {
		return repository.findByBranchPath(branch);
	}

	public ProjectAcceptanceCriteria findOrThrow(String branchPath) {
		final Optional<ProjectAcceptanceCriteria> itemOptional = repository.findById(branchPath);
		if (!itemOptional.isPresent()) {
			throw new NotFoundException(format("Branch Acceptance Criteria with branch path '%s' not found.", branchPath));
		}
		return itemOptional.get();
	}

	public void create(ProjectAcceptanceCriteria criteria) {
		validate(criteria);
		repository.save(criteria);
	}

	public ProjectAcceptanceCriteria update(ProjectAcceptanceCriteria criteria) {
		// Must exist
		findOrThrow(criteria.getBranchPath());
		validate(criteria);
		return repository.save(criteria);
	}

	private void validate(ProjectAcceptanceCriteria criteria) {
		Set<String> allIds = new HashSet<>();
		allIds.addAll(criteria.getSelectedProjectCriteriaIds());
		allIds.addAll(criteria.getSelectedTaskCriteriaIds());
		if (!allIds.isEmpty()) {
			final Iterable<CriteriaItem> found = findAllById(allIds);
			for (CriteriaItem criteriaItem : found) {
				allIds.remove(criteriaItem.getId());
			}
			if (!allIds.isEmpty()) {
				throw new IllegalArgumentException(format("The following criteria items were not found: %s", allIds));
			}
		}
	}

	public void delete(ProjectAcceptanceCriteria item) {
		repository.delete(item);
	}

	public ProjectAcceptanceCriteria findByBranchPathOrThrow(String branch) {
		final ProjectAcceptanceCriteria criteria = findByBranchPath(branch);
		if (criteria == null) {
			throw new NotFoundException("No project acceptance criteria found for this branch path.");
		}
		return criteria;
	}

	// This method is required because the default Repository implementation uses a multi-get request
	// which is blocked by SI AWS index prefix security settings
	private List<CriteriaItem> findAllById(Set<String> allIds) {
		List<CriteriaItem> all = new ArrayList<>();
		final NativeSearchQuery query = new NativeSearchQueryBuilder()
				.withQuery(termsQuery("id", allIds))
				.withPageable(Constants.LARGE_PAGE)
				.build();
		try (final SearchHitsIterator<CriteriaItem> hits = elasticsearchTemplate.searchForStream(query, CriteriaItem.class)) {
			hits.forEachRemaining(item -> all.add(item.getContent()));
		}
		return all;
	}
}
