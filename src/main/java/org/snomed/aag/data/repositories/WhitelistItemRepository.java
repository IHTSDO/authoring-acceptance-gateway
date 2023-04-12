package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.WhitelistItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


public interface WhitelistItemRepository extends ElasticsearchRepository<WhitelistItem, String> {
}
