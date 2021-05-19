package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.ProjectAcceptanceCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectAcceptanceCriteriaRepository extends ElasticsearchRepository<ProjectAcceptanceCriteria, String> {
	ProjectAcceptanceCriteria findByBranchPath(String branch);

	Page<ProjectAcceptanceCriteria> findAllBySelectedProjectCriteriaIdsOrSelectedTaskCriteriaIds(String projectCriteriaId, String taskCriteriaId, Pageable page);

	Optional<ProjectAcceptanceCriteria> findByBranchPathAndProjectIteration(String branch, Integer projectIteration);

	List<ProjectAcceptanceCriteria> findAllByBranchPathOrderByProjectIterationDesc(String branchPath);
}
