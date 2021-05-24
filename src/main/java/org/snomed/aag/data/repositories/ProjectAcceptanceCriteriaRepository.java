package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ProjectAcceptanceCriteriaRepository extends ElasticsearchRepository<ProjectAcceptanceCriteria, String> {
	Page<ProjectAcceptanceCriteria> findAllBySelectedProjectCriteriaIdsOrSelectedTaskCriteriaIds(String projectCriteriaId, String taskCriteriaId, Pageable page);

	ProjectAcceptanceCriteria findByBranchPathAndProjectIteration(String branch, Integer projectIteration);

	List<ProjectAcceptanceCriteria> findAllByBranchPathOrderByProjectIterationDesc(String branchPath);
}
