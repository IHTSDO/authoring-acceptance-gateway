package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface CriteriaItemSignOffRepository extends ElasticsearchRepository<CriteriaItemSignOff, String> {
    List<CriteriaItemSignOff> findAllByBranchAndProjectIterationAndCriteriaItemIdIn(String branch, Integer projectIteration, Collection<String> criteriaItemIdentifiers);

    List<CriteriaItemSignOff> findAllByBranchAndCriteriaItemIdIn(String branch, Collection<String> criteriaItemIdentifiers);

    Optional<CriteriaItemSignOff> findByCriteriaItemIdAndBranchAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration);

    Optional<CriteriaItemSignOff> findByCriteriaItemIdAndBranch(String criteriaItemId, String branchPath);

    void deleteByCriteriaItemIdAndBranchAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration);

    void deleteByCriteriaItemIdAndBranch(String criteriaItemId, String branchPath);
}
