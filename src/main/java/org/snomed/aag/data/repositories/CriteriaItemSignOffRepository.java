package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.CriteriaItemSignOff;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;


public interface CriteriaItemSignOffRepository extends ElasticsearchRepository<CriteriaItemSignOff, String> {
    List<CriteriaItemSignOff> findAllByBranchAndCriteriaItemIdIn(String branch, List<String> criteriaItemIdentifiers);
}
