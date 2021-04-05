package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface CriteriaItemSignOffRepository extends ElasticsearchRepository<CriteriaItemSignOff, String> {
    Optional<CriteriaItemSignOff> findByBranchAndCriteriaItemId(String branchPath, String criteriaItemIdentifier);
}
