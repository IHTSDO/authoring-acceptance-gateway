package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProjectAcceptanceCriteriaRepository extends ElasticsearchRepository<ProjectAcceptanceCriteria, String> {
	ProjectAcceptanceCriteria findByBranchPath(String branch);

	Page<ProjectAcceptanceCriteria> findAllBySelectedProjectCriteriaIdsOrSelectedTaskCriteriaIds(String projectCriteriaId, String taskCriteriaId, Pageable page);
}
