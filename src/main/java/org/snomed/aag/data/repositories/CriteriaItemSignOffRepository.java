package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;


public interface CriteriaItemSignOffRepository extends ElasticsearchRepository<CriteriaItemSignOff, String> {
    List<CriteriaItemSignOff> findAllByBranchAndProjectIterationAndCriteriaItemIdIn(String branch, Integer projectIteration, Collection<String> criteriaItemIdentifiers);

    void deleteByCriteriaItemIdAndBranchAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration);

    Optional<CriteriaItemSignOff> findByCriteriaItemIdAndBranchAndProjectIteration(String criteriaItemId, String branchPath, Integer projectIteration);
}
