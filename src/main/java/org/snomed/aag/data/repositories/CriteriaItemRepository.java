package org.snomed.aag.data.repositories;

import org.snomed.aag.data.domain.AuthoringLevel;
import org.snomed.aag.data.domain.CriteriaItem;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface CriteriaItemRepository extends ElasticsearchRepository<CriteriaItem, String> {
    List<CriteriaItem> findAllByMandatoryAndAuthoringLevel(boolean mandatory, AuthoringLevel authoringLevel);
}
