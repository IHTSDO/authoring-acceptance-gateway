package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.CriteriaItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface CriteriaItemRepository extends ElasticsearchRepository<CriteriaItem, String> {
}
