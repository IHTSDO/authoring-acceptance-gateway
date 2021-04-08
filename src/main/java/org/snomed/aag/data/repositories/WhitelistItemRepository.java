package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.WhitelistItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Collection;
import java.util.List;

public interface WhitelistItemRepository extends ElasticsearchRepository<WhitelistItem, String> {
    List<WhitelistItem> findAllByValidationRuleIdIn(Collection<String> validationRuleIds);
}
